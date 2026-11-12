package org.ptit.meeting.integration.livekit.dto;

import java.util.ArrayList;
import java.util.List;

public record LiveKitJoinGrant(
    String roomName,
    String participantIdentity,
    String participantName,
    String participantMetadata,
    boolean canSubscribe,
    boolean canPublishMicrophone,
    boolean canPublishCamera,
    boolean canShareScreen
) {
  public List<String> publishSources() {
    List<String> sources = new ArrayList<>(4);
    if (canPublishMicrophone) sources.add("microphone");
    if (canPublishCamera) sources.add("camera");
    if (canShareScreen) {
      sources.add("screen_share");
      sources.add("screen_share_audio");
    }
    return List.copyOf(sources);
  }
}
