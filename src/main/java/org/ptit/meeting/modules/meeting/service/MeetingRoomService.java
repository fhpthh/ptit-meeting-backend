package org.ptit.meeting.modules.meeting.service;

import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomCreateRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomSettingUpdateRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomUpdateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRoomResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomSetting;
import org.ptit.meeting.modules.meeting.entity.Room;

import java.util.List;

public interface MeetingRoomService {
    MeetingRoomSetting initializeSetting(Room room);

    void refreshClassAccess(Long classSectionId);

    MeetingRoomSetting getSetting(String roomId);

    MeetingRoomResponse create(MeetingRoomCreateRequest request, Long currentUserId);

    MeetingRoomResponse getById(String roomId, Long currentUserId);

    List<MeetingRoomResponse> getMyRooms(Long currentUserId);

    MeetingRoomResponse update(
            String roomId,
            MeetingRoomUpdateRequest request,
            Long currentUserId
    );

    MeetingRoomResponse updateSetting(
            String roomId,
            MeetingRoomSettingUpdateRequest request,
            Long currentUserId
    );

    void archive(String roomId, Long currentUserId);
}
