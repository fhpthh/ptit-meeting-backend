package org.ptit.meeting.integration.livekit;

import io.livekit.server.LiveKitAPI;
import java.io.IOException;
import org.ptit.meeting.integration.livekit.dto.LiveKitJoinGrant;
import org.ptit.meeting.integration.livekit.dto.LiveKitTokenResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

final class LiveKitGatewayImpl implements LiveKitGateway {

  private static final Logger log = LoggerFactory.getLogger(LiveKitGatewayImpl.class);

  private final LiveKitAPI liveKitApi;
  private final LiveKitTokenFactory tokenFactory;

  LiveKitGatewayImpl(LiveKitAPI liveKitApi, LiveKitTokenFactory tokenFactory) {
    this.liveKitApi = liveKitApi;
    this.tokenFactory = tokenFactory;
  }

  @Override
  public LiveKitTokenResult createJoinToken(LiveKitJoinGrant grant) {
    return tokenFactory.create(grant);
  }

  @Override
  public void deleteRoom(String roomName) {
    if (roomName == null || roomName.isBlank()) {
      throw new LiveKitIntegrationException("LiveKit room name is invalid");
    }

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

  private void closeErrorBody(Response<?> response) {
    if (response.errorBody() != null) {
      response.errorBody().close();
    }
  }
}
