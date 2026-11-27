package org.ptit.meeting.modules.ai.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.config.AiRecordingProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.integration.storage.RecordingArtifactStorage;
import org.ptit.meeting.integration.storage.RecordingArtifactStorage.ArtifactObject;
import org.ptit.meeting.modules.ai.constant.AiErrorConstants;
import org.ptit.meeting.modules.ai.dto.internal.VerifiedRecordingResult;
import org.ptit.meeting.modules.ai.dto.request.RecordingResultCompleteRequest;
import org.ptit.meeting.modules.ai.dto.response.RecordingJobContextResponse;
import org.ptit.meeting.modules.ai.service.RecordingResultVerificationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Verifies the stored wire contract without holding database locks or rewriting AI text. */
@Service
@RequiredArgsConstructor
public class RecordingResultVerificationServiceImpl implements RecordingResultVerificationService {
  private static final Set<String> NAMES =
      Set.of(
          "input-manifest.json",
          "subtitles.json",
          "vi.vtt",
          "en.vtt",
          "word_timeline.json",
          "subtitles.before-global.json",
          "alignment_metrics.json");
  private static final Set<String> REQUIRED =
      Set.of("input-manifest.json", "subtitles.json", "vi.vtt", "en.vtt");
  private static final Set<String> TIMING =
      Set.of("ASR_NATIVE", "ASR_NATIVE_GLOBAL", "INTERPOLATED", "FORCED_ALIGNMENT", "HEURISTIC");
  private final ObjectMapper json;
  private final ObjectProvider<RecordingArtifactStorage> storage;
  private final AiRecordingProperties properties;

  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public VerifiedRecordingResult verify(
      RecordingJobContextResponse expected, RecordingResultCompleteRequest request) {
    if (!properties.isEnabled() || !properties.isCompletionEnabled())
      throw BusinessException.invalidInput(AiErrorConstants.RECORDING_DISABLED);
    if (!Objects.equals(properties.getArtifactStorage().getBucket(), request.bucket())
        || request.bucket().equals(expected.source().bucket())) throw invalid();
    RecordingArtifactStorage objects = storage.getIfAvailable();
    if (objects == null)
      throw BusinessException.invalidInput(AiErrorConstants.RECORDING_CONFIGURATION);
    try {
      objects.requirePrivate();
      byte[] manifestBytes =
          objects.readVerified(
              new ArtifactObject(
                  request.objectKey(), request.sha256(), request.sizeBytes(), "application/json"));
      JsonNode root = tree(manifestBytes);
      keys(
          root,
          "schemaVersion",
          "type",
          "context",
          "bucket",
          "extraction",
          "contextSha256",
          "executionSha256",
          "completedAt",
          "documentRevision",
          "durationMs",
          "segmentCounts",
          "warnings",
          "artifacts");
      require(
          integer(root, "schemaVersion", 1) == 1
              && "recording.result-manifest".equals(text(root, "type", 64))
              && request.bucket().equals(text(root, "bucket", 63)));
      JsonNode extraction = root.get("extraction");
      keys(extraction, "schema_version", "source_sha256", "config_sha256", "wav_sha256", "timing");
      require(integer(extraction, "schema_version", 1) == 1);
      String sourceHash = sha(extraction, "source_sha256");
      sha(extraction, "config_sha256");
      sha(extraction, "wav_sha256");
      sha(root, "executionSha256");
      JsonNode declaredContext = root.get("context");
      // Parse the expected wire bytes too: valueToTree retains LongNode while
      // parsed JSON uses IntNode for small integers, despite identical wire values.
      JsonNode expectedContext = tree(json.writeValueAsBytes(expected));
      if (!declaredContext.equals(expectedContext)) {
        // A prior claim can precede the first worker-computed checkpoint SHA.
        var comparable = expectedContext.deepCopy();
        if (declaredContext.path("source").path("expectedSha256").isNull()
            && Objects.equals(expected.source().expectedSha256(), sourceHash)) {
          ((com.fasterxml.jackson.databind.node.ObjectNode) comparable.get("source"))
              .putNull("expectedSha256");
        }
        require(declaredContext.equals(comparable));
      }
      require(
          expected.source().expectedSha256() == null
              || expected.source().expectedSha256().equals(sourceHash));
      JsonNode course = declaredContext.get("courseContext");
      Map<String, Object> ordered = course.isNull() ? null : new TreeMap<>();
      if (ordered != null)
        course
            .fields()
            .forEachRemaining(
                entry ->
                    ordered.put(entry.getKey(), json.convertValue(entry.getValue(), Object.class)));
      require(
          sha(root, "contextSha256")
              .equals(
                  hash(
                      json.writer()
                          .without(
                              com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
                          .writeValueAsString(ordered)
                          .getBytes(StandardCharsets.UTF_8))));
      OffsetDateTime.parse(text(root, "completedAt", 64));
      long duration = integer(root, "durationMs", 1),
          revision = integer(root, "documentRevision", 1);
      JsonNode time = extraction.get("timing");
      keys(
          time,
          "media_origin_us",
          "audio_origin_us",
          "samples",
          "inserted_silence_samples",
          "audio_stream_index");
      long mediaOrigin = signed(time, "media_origin_us"),
          audioOrigin = signed(time, "audio_origin_us");
      long samples = integer(time, "samples", 1),
          gaps = integer(time, "inserted_silence_samples", 0);
      integer(time, "audio_stream_index", 0);
      long originMs =
          BigDecimal.valueOf(Math.subtractExact(audioOrigin, mediaOrigin))
              .divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_EVEN)
              .longValueExact();
      require(
          audioOrigin >= mediaOrigin
              && gaps <= samples
              && Math.addExact(originMs, Math.addExact(samples, 15) / 16) <= duration);
      JsonNode counts = root.get("segmentCounts");
      keys(counts, "vi", "en");
      long viCount = integer(counts, "vi", 1), enCount = integer(counts, "en", 1);
      require(viCount <= 50000 && enCount <= 50000 && viCount + enCount <= 50000);
      JsonNode warnings = root.get("warnings");
      require(warnings.isArray() && warnings.size() <= 32);
      Set<String> uniqueWarnings = new HashSet<>();
      for (JsonNode warning : warnings)
        require(
            warning.isTextual()
                && warning.textValue().matches("[A-Z][A-Z0-9_]{0,63}")
                && uniqueWarnings.add(warning.textValue()));
      JsonNode artifacts = root.get("artifacts");
      require(artifacts.isArray() && artifacts.size() >= 4 && artifacts.size() <= 7);
      String prefix =
          request
              .objectKey()
              .substring(0, request.objectKey().length() - "result-manifest.json".length());
      Map<String, ArtifactObject> references = new HashMap<>();
      long total = 0;
      for (JsonNode row : artifacts) {
        keys(row, "name", "objectKey", "sha256", "sizeBytes", "contentType");
        String name = text(row, "name", 64);
        long size = integer(row, "sizeBytes", 1);
        String contentType = name.endsWith(".vtt") ? "text/vtt; charset=utf-8" : "application/json";
        require(
            NAMES.contains(name)
                && size <= 16L * 1024 * 1024
                && (total += size) <= 32L * 1024 * 1024);
        require(
            (prefix + name).equals(text(row, "objectKey", 1024))
                && contentType.equals(text(row, "contentType", 64)));
        require(
            references.putIfAbsent(
                    name, new ArtifactObject(prefix + name, sha(row, "sha256"), size, contentType))
                == null);
      }
      require(references.keySet().containsAll(REQUIRED));
      Map<String, byte[]> payloads = new HashMap<>();
      for (var entry : references.entrySet()) {
        String name = entry.getKey();
        if (REQUIRED.contains(name) || name.equals("alignment_metrics.json"))
          payloads.put(name, objects.readVerified(entry.getValue()));
        else objects.verify(entry.getValue());
      }
      if (payloads.containsKey("alignment_metrics.json")) {
        JsonNode safe = tree(payloads.get("alignment_metrics.json")).path("safe_for_playback");
        require(safe.isBoolean() && safe.booleanValue());
      }
      JsonNode input = tree(payloads.get("input-manifest.json"));
      keys(
          input,
          "schemaVersion",
          "type",
          "context",
          "sourceSha256",
          "trustedChecksumVerified",
          "executionSha256");
      require(
          integer(input, "schemaVersion", 1) == 1
              && "recording.input-manifest".equals(text(input, "type", 64))
              && input.get("context").equals(declaredContext)
              && sha(input, "sourceSha256").equals(sourceHash)
              && sha(input, "executionSha256").equals(sha(root, "executionSha256"))
              && input.get("trustedChecksumVerified").isBoolean()
              && input.get("trustedChecksumVerified").booleanValue()
                  == !declaredContext.get("source").get("expectedSha256").isNull());
      JsonNode document = tree(payloads.get("subtitles.json"));
      validateDocument(document, expected, revision, duration, viCount, enCount, payloads);
      objects.requirePrivate();
      return new VerifiedRecordingResult(
          expected,
          new String(manifestBytes, StandardCharsets.UTF_8),
          new String(payloads.get("subtitles.json"), StandardCharsets.UTF_8),
          sourceHash,
          revision,
          duration,
          (int) viCount,
          (int) enCount);
    } catch (RecordingArtifactStorage.ArtifactMismatchException exception) {
      throw invalid();
    } catch (RecordingArtifactStorage.StorageException exception) {
      throw new BusinessException(
          StatusCode.ERROR_IN_BACKEND, AiErrorConstants.RECORDING_RESULT_STORAGE);
    } catch (IOException
        | IllegalArgumentException
        | ArithmeticException
        | DateTimeException exception) {
      throw invalid();
    }
  }

  private record Cue(
      String id,
      String language,
      long revision,
      String text,
      long start,
      long end,
      long displayStart,
      long displayEnd,
      List<String> sources) {}

  private void validateDocument(
      JsonNode document,
      RecordingJobContextResponse context,
      long revision,
      long duration,
      long viCount,
      long enCount,
      Map<String, byte[]> payloads) {
    keys(
        document,
        "schemaVersion",
        "type",
        "recordingId",
        "runId",
        "documentRevision",
        "durationMs",
        "segments");
    require(
        integer(document, "schemaVersion", 1) == 1
            && "recording.subtitles".equals(text(document, "type", 64))
            && context.attempt().recordingId().equals(text(document, "recordingId", 128))
            && context.attempt().runId().equals(text(document, "runId", 128))
            && integer(document, "documentRevision", 1) == revision
            && integer(document, "durationMs", 1) == duration);
    JsonNode rows = document.get("segments");
    require(rows.isArray() && rows.size() > 0 && rows.size() <= 50000);
    List<Cue> cues = new ArrayList<>();
    Map<String, Cue> vi = new HashMap<>();
    Set<String> keys = new HashSet<>();
    for (JsonNode row : rows) {
      keys(
          row,
          "segmentId",
          "language",
          "revision",
          "text",
          "semanticStartMs",
          "semanticEndMs",
          "displayStartMs",
          "displayEndMs",
          "timingSource",
          "alignmentConfidence",
          "sourceSegmentIds");
      String id = text(row, "segmentId", 128),
          language = text(row, "language", 2),
          content = text(row, "text", 8000);
      require(
          id.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
              && Set.of("vi", "en").contains(language)
              && !blank(content)
              && keys.add(language + ":" + id));
      long start = integer(row, "semanticStartMs", 0),
          end = integer(row, "semanticEndMs", 1),
          displayStart = integer(row, "displayStartMs", 0),
          displayEnd = integer(row, "displayEndMs", 1);
      require(
          start < end
              && displayStart < displayEnd
              && end <= duration
              && displayEnd <= duration
              && TIMING.contains(text(row, "timingSource", 32)));
      JsonNode confidence = row.get("alignmentConfidence");
      require(
          confidence.isNumber()
              && Double.isFinite(confidence.doubleValue())
              && confidence.doubleValue() >= 0
              && confidence.doubleValue() <= 1);
      JsonNode sourceIds = row.get("sourceSegmentIds");
      require(sourceIds.isArray() && sourceIds.size() <= 64);
      List<String> sources = new ArrayList<>();
      for (JsonNode source : sourceIds) {
        require(
            source.isTextual()
                && source.textValue().matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
                && !sources.contains(source.textValue()));
        sources.add(source.textValue());
      }
      require(language.equals("vi") ? sources.isEmpty() : !sources.isEmpty());
      Cue cue =
          new Cue(
              id,
              language,
              integer(row, "revision", 1),
              content,
              start,
              end,
              displayStart,
              displayEnd,
              sources);
      cues.add(cue);
      if (language.equals("vi")) vi.put(id, cue);
    }
    require(vi.size() == viCount && cues.size() - vi.size() == enCount);
    Set<String> translated = new HashSet<>();
    for (Cue cue : cues)
      if (cue.language().equals("en")) {
        long start = Long.MAX_VALUE, displayStart = Long.MAX_VALUE, end = 0, displayEnd = 0;
        for (String source : cue.sources()) {
          Cue original = vi.get(source);
          require(original != null);
          translated.add(source);
          start = Math.min(start, original.start());
          end = Math.max(end, original.end());
          displayStart = Math.min(displayStart, original.displayStart());
          displayEnd = Math.max(displayEnd, original.displayEnd());
        }
        require(
            cue.start() == start
                && cue.end() == end
                && cue.displayStart() == displayStart
                && cue.displayEnd() == displayEnd);
      }
    require(translated.equals(vi.keySet()));
    for (String language : List.of("vi", "en")) {
      List<Cue> ordered =
          cues.stream()
              .filter(cue -> cue.language().equals(language))
              .sorted(
                  Comparator.comparingLong(Cue::displayStart)
                      .thenComparingLong(Cue::displayEnd)
                      .thenComparing(Cue::id))
              .toList();
      long end = -1;
      StringBuilder vtt = new StringBuilder("WEBVTT\n\n");
      int index = 0;
      for (Cue cue : ordered) {
        require(cue.displayStart() >= end);
        end = cue.displayEnd();
        String escaped = cue.text().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String content =
            String.join(
                "\n",
                Arrays.stream(escaped.split("\\R|[\\u001c-\\u001e]", -1))
                    .filter(line -> !blank(line))
                    .toList());
        vtt.append(++index)
            .append('\n')
            .append(timestamp(cue.displayStart()))
            .append(" --> ")
            .append(timestamp(cue.displayEnd()))
            .append('\n')
            .append(content)
            .append('\n')
            .append('\n');
      }
      // Python joins a list ending in one empty element: final newline, not two.
      if (!ordered.isEmpty()) vtt.setLength(vtt.length() - 1);
      require(
          Arrays.equals(
              vtt.toString().getBytes(StandardCharsets.UTF_8), payloads.get(language + ".vtt")));
    }
  }

  private String timestamp(long ms) {
    return String.format(
        Locale.ROOT,
        "%02d:%02d:%02d.%03d",
        ms / 3600000,
        ms / 60000 % 60,
        ms / 1000 % 60,
        ms % 1000);
  }

  private boolean blank(String value) {
    return value
        .codePoints()
        .allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c) || c == 0x85);
  }

  private JsonNode tree(byte[] value) throws IOException {
    String utf8 =
        StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(value))
            .toString();
    JsonNode result =
        json.reader()
            .with(
                DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY,
                DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(utf8);
    require(result != null);
    return result;
  }

  private void keys(JsonNode value, String... names) {
    require(value != null && value.isObject());
    Set<String> fields = new HashSet<>();
    value.fieldNames().forEachRemaining(fields::add);
    require(fields.equals(Set.of(names)));
  }

  private String text(JsonNode node, String field, int maximum) {
    JsonNode value = node.get(field);
    require(value != null && value.isTextual());
    String result = value.textValue();
    require(
        result.codePointCount(0, result.length()) <= maximum
            && result.codePoints().noneMatch(c -> c >= 0xD800 && c <= 0xDFFF));
    return result;
  }

  private long signed(JsonNode node, String field) {
    JsonNode value = node.get(field);
    require(value != null && value.isIntegralNumber() && value.canConvertToLong());
    return value.longValue();
  }

  private long integer(JsonNode node, String field, long minimum) {
    long result = signed(node, field);
    require(result >= minimum);
    return result;
  }

  private String sha(JsonNode node, String field) {
    String result = text(node, field, 64);
    require(result.matches("[a-f0-9]{64}"));
    return result;
  }

  private String hash(byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private void require(boolean condition) {
    if (!condition) throw invalid();
  }

  private BusinessException invalid() {
    return BusinessException.invalidInput(AiErrorConstants.RECORDING_RESULT_INVALID);
  }
}
