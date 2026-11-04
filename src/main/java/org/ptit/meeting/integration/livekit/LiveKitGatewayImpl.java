package org.ptit.meeting.integration.livekit;

import io.livekit.server.LiveKitAPI;
import java.io.IOException;
import java.util.List;
import livekit.LivekitModels.DataPacket;
import livekit.LivekitModels.TrackInfo;
import org.ptit.meeting.integration.livekit.dto.LiveKitJoinGrant;
import org.ptit.meeting.integration.livekit.dto.LiveKitTokenResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

final class LiveKitGatewayImpl implements LiveKitGateway {

  private static final Logger log = LoggerFactory.getLogger(LiveKitGatewayImpl.class);

  private final LiveKitAPI liveKitApi;
  private final LiveKitTokenFactory tokenFactory;

  LiveKitGatewayImpl(
      LiveKitAPI liveKitApi,
      LiveKitTokenFactory tokenFactory
  ) {
    this.liveKitApi = liveKitApi;
    this.tokenFactory = tokenFactory;
  }

  @Override
  public LiveKitTokenResult createJoinToken(LiveKitJoinGrant grant) {
    return tokenFactory.create(grant);
  }

  @Override
  public void deleteRoom(String roomName) {
    requireText(roomName, "LiveKit room name is invalid");

    try {
      Response<Void> response = liveKitApi.getRoom().deleteRoom(roomName).execute();
      if (response.isSuccessful()) {
        return;
      }

      int httpStatus = response.code();
      closeErrorBody(response);
      if (httpStatus == 404) {
        return;
      }

      log.warn(
          "LiveKit room deletion failed: roomName={}, httpStatus={}",
          roomName,
          httpStatus
      );
      throw new LiveKitIntegrationException("LiveKit room deletion failed");
    } catch (LiveKitIntegrationException exception) {
      throw exception;
    } catch (IOException exception) {
      log.warn("LiveKit room deletion request failed: roomName={}", roomName);
      throw new LiveKitIntegrationException(
          "Unable to reach LiveKit while deleting room",
          exception
      );
    } catch (RuntimeException exception) {
      log.warn("Unexpected LiveKit room deletion failure: roomName={}", roomName);
      throw new LiveKitIntegrationException("LiveKit room deletion failed", exception);
    }
  }

  @Override
  public boolean mutePublishedTrack(
      String roomName,
      String participantIdentity,
      String trackSid
  ) {
    requireText(roomName, "LiveKit room name is invalid");
    requireText(participantIdentity, "LiveKit participant identity is invalid");
    requireText(trackSid, "LiveKit track SID is invalid");

    try {
      Response<TrackInfo> response = liveKitApi.getRoom()
          .mutePublishedTrack(roomName, participantIdentity, trackSid, true)
          .execute();
      if (response.isSuccessful()) {
        return true;
      }
      return handleParticipantOperationFailure(
          response,
          "mute participant track",
          roomName,
          participantIdentity
      );
    } catch (LiveKitIntegrationException exception) {
      throw exception;
    } catch (IOException exception) {
      throw connectionFailure("mute participant track", exception);
    } catch (RuntimeException exception) {
      throw unexpectedFailure("mute participant track", exception);
    }
  }

  @Override
  public boolean removeParticipant(String roomName, String participantIdentity) {
    requireText(roomName, "LiveKit room name is invalid");
    requireText(participantIdentity, "LiveKit participant identity is invalid");

    try {
      Response<Void> response = liveKitApi.getRoom()
          .removeParticipant(roomName, participantIdentity)
          .execute();
      if (response.isSuccessful()) {
        return true;
      }
      return handleParticipantOperationFailure(
          response,
          "remove participant",
          roomName,
          participantIdentity
      );
    } catch (LiveKitIntegrationException exception) {
      throw exception;
    } catch (IOException exception) {
      throw connectionFailure("remove participant", exception);
    } catch (RuntimeException exception) {
      throw unexpectedFailure("remove participant", exception);
    }
  }

  @Override
  public void broadcastReliableData(String roomName, byte[] data, String topic) {
    requireText(roomName, "LiveKit room name is invalid");
    requireText(topic, "LiveKit data topic is invalid");
    if (data == null || data.length == 0) {
      throw new LiveKitIntegrationException("LiveKit data payload is invalid");
    }

    try {
      Response<Void> response = liveKitApi.getRoom()
          .sendData(
              roomName,
              data,
              DataPacket.Kind.RELIABLE,
              List.of(),
              List.of(),
              topic
          )
          .execute();
      if (response.isSuccessful()) {
        return;
      }

      int httpStatus = response.code();
      closeErrorBody(response);
      log.warn(
          "LiveKit reliable data broadcast failed: roomName={}, topic={}, httpStatus={}",
          roomName,
          topic,
          httpStatus
      );
      throw new LiveKitIntegrationException("LiveKit data broadcast failed");
    } catch (LiveKitIntegrationException exception) {
      throw exception;
    } catch (IOException exception) {
      throw new LiveKitIntegrationException(
          "Unable to reach LiveKit while broadcasting data",
          exception
      );
    } catch (RuntimeException exception) {
      throw new LiveKitIntegrationException("LiveKit data broadcast failed", exception);
    }
  }

  private boolean handleParticipantOperationFailure(
      Response<?> response,
      String operation,
      String roomName,
      String participantIdentity
  ) {
    int httpStatus = response.code();
    closeErrorBody(response);
    if (httpStatus == 400 || httpStatus == 404) {
      return false;
    }

    log.warn(
        "LiveKit {} failed: roomName={}, participantIdentity={}, httpStatus={}",
        operation,
        roomName,
        participantIdentity,
        httpStatus
    );
    throw new LiveKitIntegrationException("LiveKit participant operation failed");
  }

  private LiveKitIntegrationException connectionFailure(
      String operation,
      IOException exception
  ) {
    log.warn("Unable to reach LiveKit while attempting to {}", operation);
    return new LiveKitIntegrationException("Unable to reach LiveKit", exception);
  }

  private LiveKitIntegrationException unexpectedFailure(
      String operation,
      RuntimeException exception
  ) {
    log.warn("Unexpected LiveKit failure while attempting to {}", operation);
    return new LiveKitIntegrationException("LiveKit operation failed", exception);
  }

  private void requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new LiveKitIntegrationException(message);
    }
  }

  private void closeErrorBody(Response<?> response) {
    if (response.errorBody() != null) {
      response.errorBody().close();
    }
  }
}
