package org.ptit.meeting.integration.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

public final class S3RecordingArtifactStorage implements RecordingArtifactStorage {
  private final S3Client client;
  private final String bucket;
  private final ObjectMapper json;

  public S3RecordingArtifactStorage(S3Client client, String bucket, ObjectMapper json) {
    this.client = client;
    this.bucket = bucket;
    this.json = json;
  }

  @Override
  public void requirePrivate() {
    try {
      String policy =
          client.getBucketPolicy(GetBucketPolicyRequest.builder().bucket(bucket).build()).policy();
      if (policy == null || policy.length() > 65536) throw new ArtifactMismatchException();
      JsonNode parsed = json.readTree(policy);
      if (parsed == null) throw new ArtifactMismatchException();
      JsonNode statements = parsed.path("Statement");
      if (!statements.isArray() || statements.size() > 128) throw new ArtifactMismatchException();
      for (JsonNode statement : statements) {
        String effect = statement.path("Effect").asText();
        if ("Deny".equals(effect)) continue;
        if (!"Allow".equals(effect)
            || statement.has("NotPrincipal")
            || !explicitPrincipal(statement.path("Principal")))
          throw new ArtifactMismatchException();
      }
    } catch (S3Exception exception) {
      if (exception.awsErrorDetails() != null
          && "NoSuchBucketPolicy".equals(exception.awsErrorDetails().errorCode())) return;
      throw new StorageException();
    } catch (IOException | SdkException exception) {
      throw new StorageException();
    }
  }

  private boolean explicitPrincipal(JsonNode node) {
    if (node.isTextual())
      return !node.textValue().isBlank()
          && !node.textValue().contains("*")
          && !node.textValue().contains("?");
    if ((!node.isArray() && !node.isObject()) || node.isEmpty()) return false;
    for (JsonNode value : node) if (!explicitPrincipal(value)) return false;
    return true;
  }

  @Override
  public byte[] readVerified(ArtifactObject object) {
    return read(object, true);
  }

  @Override
  public void verify(ArtifactObject object) {
    read(object, false);
  }

  private byte[] read(ArtifactObject object, boolean retain) {
    if (object.sizeBytes() < 1 || object.sizeBytes() > 64L * 1024 * 1024)
      throw new ArtifactMismatchException();
    ResponseInputStream<GetObjectResponse> input = null;
    boolean exhausted = false;
    try {
      input = client.getObject(GetObjectRequest.builder().bucket(bucket).key(object.key()).build());
      var response = input.response();
      if (response.contentLength() == null
          || response.contentLength() != object.sizeBytes()
          || !object.contentType().equals(response.contentType())
          || !"no-store".equals(response.cacheControl())
          || !object.sha256().equals(response.metadata().get("sha256")))
        throw new ArtifactMismatchException();
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      ByteArrayOutputStream body = retain ? new ByteArrayOutputStream() : null;
      byte[] block = new byte[65536];
      long total = 0;
      for (int count; (count = input.read(block)) != -1; ) {
        total += count;
        if (total > object.sizeBytes()) throw new ArtifactMismatchException();
        digest.update(block, 0, count);
        if (body != null) body.write(block, 0, count);
      }
      exhausted = true;
      if (total != object.sizeBytes()
          || !object.sha256().equals(HexFormat.of().formatHex(digest.digest())))
        throw new ArtifactMismatchException();
      return body == null ? null : body.toByteArray();
    } catch (IOException | SdkException exception) {
      throw new StorageException();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    } finally {
      if (input != null) {
        try {
          if (!exhausted) input.abort();
        } finally {
          try {
            input.close();
          } catch (IOException exception) {
            /* socket already aborted/closed */
          }
        }
      }
    }
  }
}
