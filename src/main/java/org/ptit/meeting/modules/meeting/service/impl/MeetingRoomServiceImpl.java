package org.ptit.meeting.modules.meeting.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;
import org.ptit.meeting.modules.meeting.constant.enums.MemberStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomType;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomCreateRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomSettingUpdateRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomUpdateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRoomResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingRoom;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomMember;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomSetting;
import org.ptit.meeting.modules.meeting.mapper.MeetingRoomMapper;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomMemberRepository;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomRepository;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomSettingRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.meeting.service.MeetingRoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingRoomServiceImpl implements MeetingRoomService {

    private static final int ROOM_NAME_MAX_LENGTH = 150;
    private static final int ROOM_DESCRIPTION_MAX_LENGTH = 500;

    private final MeetingRoomRepository roomRepository;
    private final MeetingRoomSettingRepository settingRepository;
    private final MeetingRoomMemberRepository memberRepository;
    private final MeetingAuthorizationService authorizationService;
    private final MeetingRoomMapper roomMapper;
    private final UserService userService;

    @Override
    @Transactional
    public MeetingRoomResponse create(MeetingRoomCreateRequest request, Long currentUserId) {
        User creator = requireActiveUser(currentUserId);
        String name = requireValidName(request == null ? null : request.name());
        String description = normalizeDescription(request.description());
        RoomType roomType = request.roomType();
        if (roomType == null) {
            throw BusinessException.invalidInput(MeetingErrorConstants.ROOM_TYPE_INVALID);
        }

        MeetingRoom room = roomRepository.save(new MeetingRoom(
                generateRoomCode(),
                name,
                description,
                roomType,
                creator.getId()
        ));
        MeetingRoomSetting setting = settingRepository.save(createDefaultSetting(room));
        memberRepository.save(new MeetingRoomMember(
                room.getId(),
                creator.getId(),
                MeetingRole.OWNER
        ));

        return roomMapper.toResponse(room, setting);
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingRoomResponse getById(String roomId, Long currentUserId) {
        MeetingRoom room = findRoom(roomId);
        authorizationService.requireActiveMember(room.getId(), currentUserId);
        return roomMapper.toResponse(room, findSetting(room.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingRoomResponse> getMyRooms(Long currentUserId) {
        requireCurrentUserId(currentUserId);
        List<String> roomIds = memberRepository
                .findAllByUserIdAndStatus(currentUserId, MemberStatus.ACTIVE)
                .stream()
                .map(MeetingRoomMember::getRoomId)
                .distinct()
                .toList();

        if (roomIds.isEmpty()) {
            return List.of();
        }

        List<MeetingRoom> rooms = roomRepository
                .findAllByIdInAndStatusOrderByCreatedAtDesc(roomIds, RoomStatus.ACTIVE);
        Map<String, MeetingRoomSetting> settings = settingRepository.findAllByRoomIdIn(roomIds)
                .stream()
                .collect(Collectors.toMap(MeetingRoomSetting::getRoomId, Function.identity()));

        return rooms.stream()
                .map(room -> roomMapper.toResponse(room, requireSetting(settings, room.getId())))
                .toList();
    }

    @Override
    @Transactional
    public MeetingRoomResponse update(
            String roomId,
            MeetingRoomUpdateRequest request,
            Long currentUserId
    ) {
        MeetingRoom room = findRoom(roomId);
        authorizationService.requireRoomOwner(room.getId(), currentUserId);
        requireActiveRoom(room);

        room.setName(requireValidName(request == null ? null : request.name()));
        room.setDescription(normalizeDescription(request.description()));
        roomRepository.save(room);

        return roomMapper.toResponse(room, findSetting(room.getId()));
    }

    @Override
    @Transactional
    public MeetingRoomResponse updateSetting(
            String roomId,
            MeetingRoomSettingUpdateRequest request,
            Long currentUserId
    ) {
        MeetingRoom room = findRoom(roomId);
        authorizationService.requireRoomOwner(room.getId(), currentUserId);
        requireActiveRoom(room);
        validateSetting(request);

        MeetingRoomSetting setting = findSetting(room.getId());
        setting.setParticipantMicEnabled(request.participantMicEnabled());
        setting.setParticipantCameraEnabled(request.participantCameraEnabled());
        setting.setParticipantScreenShareEnabled(request.participantScreenShareEnabled());
        setting.setMaxParticipants(request.maxParticipants());
        setting.setJoinBeforeMinutes(request.joinBeforeMinutes());
        settingRepository.save(setting);

        return roomMapper.toResponse(room, setting);
    }

    @Override
    @Transactional
    public void archive(String roomId, Long currentUserId) {
        MeetingRoom room = findRoom(roomId);
        authorizationService.requireRoomOwner(room.getId(), currentUserId);
        requireActiveRoom(room);
        room.setStatus(RoomStatus.ARCHIVED);
        roomRepository.save(room);
    }

    private User requireActiveUser(Long userId) {
        requireCurrentUserId(userId);
        User user = userService.getUserById(userId);
        userService.validateUserStatus(user);
        return user;
    }

    private void requireCurrentUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED);
        }
    }

    private MeetingRoom findRoom(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            throw BusinessException.notExisted(MeetingErrorConstants.ROOM_NOT_FOUND);
        }
        return roomRepository.findById(roomId)
                .orElseThrow(() -> BusinessException.notExisted(
                        MeetingErrorConstants.ROOM_NOT_FOUND,
                        roomId
                ));
    }

    private MeetingRoomSetting findSetting(String roomId) {
        return settingRepository.findByRoomId(roomId)
                .orElseThrow(() -> new BusinessException(
                        StatusCode.ERROR_IN_BACKEND,
                        MeetingErrorConstants.SETTING_NOT_FOUND,
                        roomId
                ));
    }

    private MeetingRoomSetting requireSetting(
            Map<String, MeetingRoomSetting> settings,
            String roomId
    ) {
        MeetingRoomSetting setting = settings.get(roomId);
        if (setting == null) {
            throw new BusinessException(
                    StatusCode.ERROR_IN_BACKEND,
                    MeetingErrorConstants.SETTING_NOT_FOUND,
                    roomId
            );
        }
        return setting;
    }

    private void requireActiveRoom(MeetingRoom room) {
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw BusinessException.invalidInput(MeetingErrorConstants.ROOM_ARCHIVED, room.getId());
        }
    }

    private String requireValidName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > ROOM_NAME_MAX_LENGTH) {
            throw BusinessException.invalidInput(MeetingErrorConstants.ROOM_NAME_INVALID);
        }
        return name.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.length() > ROOM_DESCRIPTION_MAX_LENGTH) {
            throw BusinessException.invalidInput(MeetingErrorConstants.ROOM_DESCRIPTION_INVALID);
        }
        return normalized;
    }

    private void validateSetting(MeetingRoomSettingUpdateRequest request) {
        if (request == null
                || request.participantMicEnabled() == null
                || request.participantCameraEnabled() == null
                || request.participantScreenShareEnabled() == null
                || request.maxParticipants() == null
                || request.maxParticipants() < 2
                || request.maxParticipants() > 1000
                || request.joinBeforeMinutes() == null
                || request.joinBeforeMinutes() < 0
                || request.joinBeforeMinutes() > 1440) {
            throw BusinessException.invalidInput(MeetingErrorConstants.SETTING_INVALID);
        }
    }

    private MeetingRoomSetting createDefaultSetting(MeetingRoom room) {
        return switch (room.getRoomType()) {
            case CLASS -> new MeetingRoomSetting(room.getId(), true, true, false, 200, 15);
            case MEETING -> new MeetingRoomSetting(room.getId(), true, true, true, 100, 15);
            case EVENT -> new MeetingRoomSetting(room.getId(), true, true, true, 1000, 30);
        };
    }

    private String generateRoomCode() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return "ROOM-" + suffix.toUpperCase(Locale.ROOT);
    }
}
