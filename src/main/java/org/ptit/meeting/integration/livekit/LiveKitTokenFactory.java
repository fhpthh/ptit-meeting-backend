package org.ptit.meeting.integration.livekit;

import io.livekit.server.AccessToken;
import io.livekit.server.CanPublish;
import io.livekit.server.CanPublishData;
import io.livekit.server.CanPublishSources;
import io.livekit.server.CanSubscribe;
import io.livekit.server.CanUpdateOwnMetadata;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.ptit.meeting.config.LiveKitProperties;
import org.ptit.meeting.integration.livekit.dto.LiveKitJoinGrant;
import org.ptit.meeting.integration.livekit.dto.LiveKitTokenResult;

final class LiveKitTokenFactory {

  private static final String MICROPHONE_SOURCE = "microphone";
  private static final String CAMERA_SOURCE = "camera";
  private static final String SCREEN_SHARE_SOURCE = "screen_share";
  private static final String SCREEN_SHARE_AUDIO_SOURCE = "screen_share_audio";

  private final LiveKitProperties properties;

  LiveKitTokenFactory(LiveKitProperties properties) {
    this.properties = properties;
  }

  LiveKitTokenResult create(LiveKitJoinGrant grant) {
    validateGrant(grant);

    try {
      List<String> publishSources = resolvePublishSources(grant);
      AccessToken token = new AccessToken(properties.getApiKey(), properties.getApiSecret());
      token.setIdentity(grant.participantIdentity());
      setOptionalParticipantData(token, grant);
      token.setTtl(properties.getJoinTokenTtlMs());
      token.addGrants(
          new RoomJoin(true),
          new RoomName(grant.roomName()),
          new CanSubscribe(grant.canSubscribe()),
          new CanPublish(!publishSources.isEmpty()),
          new CanPublishSources(publishSources),
          new CanPublishData(false),
          new CanUpdateOwnMetadata(false)
      );

      String participantToken = token.toJwt();
      Instant expiresAt = Instant.now().plusMillis(properties.getJoinTokenTtlMs());
      return new LiveKitTokenResult(
          properties.getWebsocketUrl(),
          participantToken,
          expiresAt
      );
    } catch (LiveKitIntegrationException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new LiveKitIntegrationException("Unable to create LiveKit join token", exception);
    }
  }

  private void setOptionalParticipantData(AccessToken token, LiveKitJoinGrant grant) {
    if (grant.participantName() != null && !grant.participantName().isBlank()) {
      token.setName(grant.participantName().trim());
    }
    if (grant.participantMetadata() != null && !grant.participantMetadata().isBlank()) {
      token.setMetadata(grant.participantMetadata());
    }
  }

  private List<String> resolvePublishSources(LiveKitJoinGrant grant) {
    List<String> sources = new ArrayList<>(4);
    if (grant.canPublishMicrophone()) {
      sources.add(MICROPHONE_SOURCE);
    }
    if (grant.canPublishCamera()) {
      sources.add(CAMERA_SOURCE);
    }
    if (grant.canShareScreen()) {
      sources.add(SCREEN_SHARE_SOURCE);
      sources.add(SCREEN_SHARE_AUDIO_SOURCE);
    }
    return List.copyOf(sources);
  }

  private void validateGrant(LiveKitJoinGrant grant) {
    if (grant == null
        || grant.roomName() == null
        || grant.roomName().isBlank()
        || grant.participantIdentity() == null
        || grant.participantIdentity().isBlank()) {
      throw new LiveKitIntegrationException("LiveKit join grant is invalid");
    }
  }
}
