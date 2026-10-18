package org.ptit.meeting.integration.livekit;

import org.ptit.meeting.integration.livekit.dto.LiveKitJoinGrant;
import org.ptit.meeting.integration.livekit.dto.LiveKitTokenResult;

public interface LiveKitGateway {

  LiveKitTokenResult createJoinToken(LiveKitJoinGrant grant);

  void deleteRoom(String roomName);
}
