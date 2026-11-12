package org.ptit.meeting.modules.meeting.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomType;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomCreateRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomSettingUpdateRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomUpdateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRoomResponse;
import org.ptit.meeting.modules.meeting.entity.Room;
import org.ptit.meeting.modules.meeting.event.MeetingRoomAccessChangedEvent;
import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomSetting;
import org.ptit.meeting.modules.meeting.mapper.MeetingRoomMapper;
import org.ptit.meeting.modules.timetable.repository.ClassEnrollmentRepository;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomRepository;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomSettingRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.meeting.service.MeetingRoomService;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingRoomServiceImpl implements MeetingRoomService {

    private static final int ROOM_NAME_MAX_LENGTH = 150;
    private static final int ROOM_DESCRIPTION_MAX_LENGTH = 500;

    private final MeetingRoomRepository roomRepository;
    private final MeetingRoomSettingRepository settingRepository;
    private final ClassEnrollmentRepository memberRepository;
    private final MeetingAuthorizationService authorizationService;
    private final MeetingRoomMapper roomMapper;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

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

        Room room = roomRepository.save(new Room(
                generateRoomCode(),
                name,
                description,
                roomType,
                creator.getId()
        ));
        MeetingRoomSetting setting = initializeSetting(room);
        memberRepository.save(ClassEnrollment.builder()
                .roomId(room.getPublicId())
                .userId(creator.getId())
                .role(MeetingRole.OWNER)
                .build());

        return roomMapper.toResponse(room, setting);
    }

    @Override
    @Transactional
    public MeetingRoomResponse getById(String roomId, Long currentUserId) {
        Room room = findRoom(roomId);
        authorizationService.requireActiveMember(room.getPublicId(), currentUserId);
        return roomMapper.toResponse(room, findSetting(room.getPublicId()));
    }

    @Override
    @Transactional
    public List<MeetingRoomResponse> getMyRooms(Long currentUserId) {
        requireCurrentUserId(currentUserId);
        List<String> roomIds = memberRepository
                .findAllByUserIdAndStatus(currentUserId, EnrollmentStatus.ENROLLED)
                .stream()
                .map(ClassEnrollment::getRoomId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<String, Room> accessibleRooms = new LinkedHashMap<>();
        if (!roomIds.isEmpty()) {
            roomRepository.findAllByPublicIdInAndStatusOrderByCreatedAtDesc(roomIds, RoomStatus.ACTIVE)
                    .forEach(room -> accessibleRooms.put(room.getPublicId(), room));
        }
        roomRepository.findClassRoomsForUser(currentUserId, RoomStatus.ACTIVE)
                .forEach(room -> accessibleRooms.put(room.getPublicId(), room));
        if (accessibleRooms.isEmpty()) return List.of();

        List<Room> rooms = accessibleRooms.values().stream()
                .sorted(Comparator.comparing(Room::getCreatedAt).reversed()).toList();
        Map<String, MeetingRoomSetting> settings = settingRepository
                .findAllByRoomIdIn(List.copyOf(accessibleRooms.keySet()))
                .stream()
                .collect(Collectors.toMap(MeetingRoomSetting::getRoomId, Function.identity()));
        settings.values().forEach(this::normalizeSetting);

        return rooms.stream()
                .map(room -> roomMapper.toResponse(room,
                        settings.containsKey(room.getPublicId()) ? settings.get(room.getPublicId())
                                : initializeSetting(room)))
                .toList();
    }

    @Override
    @Transactional
    public MeetingRoomResponse update(
            String roomId,
            MeetingRoomUpdateRequest request,
            Long currentUserId
    ) {
        Room room = findRoom(roomId);
        authorizationService.requireRoomOwner(room.getPublicId(), currentUserId);
        requireActiveRoom(room);

        room.setName(requireValidName(request == null ? null : request.name()));
        room.setDescription(normalizeDescription(request.description()));
        roomRepository.save(room);

        return roomMapper.toResponse(room, findSetting(room.getPublicId()));
    }

    @Override
    @Transactional
    public MeetingRoomResponse updateSetting(
            String roomId,
            MeetingRoomSettingUpdateRequest request,
            Long currentUserId
    ) {
        Room room = findRoom(roomId);
        authorizationService.requireRoomOwner(room.getPublicId(), currentUserId);
        requireActiveRoom(room);
        validateSetting(request);

        MeetingRoomSetting setting = findSetting(room.getPublicId());
        setting.setParticipantMicEnabled(request.participantMicEnabled());
        setting.setParticipantCameraEnabled(request.participantCameraEnabled());
        setting.setParticipantScreenShareEnabled(request.participantScreenShareEnabled());
        setting.setMaxParticipants(request.maxParticipants());
        setting.setJoinBeforeMinutes(request.joinBeforeMinutes());
        settingRepository.save(setting);
        eventPublisher.publishEvent(new MeetingRoomAccessChangedEvent(room.getPublicId()));

        return roomMapper.toResponse(room, setting);
    }

    @Override
    @Transactional
    public void archive(String roomId, Long currentUserId) {
        Room room = findRoom(roomId);
        authorizationService.requireRoomOwner(room.getPublicId(), currentUserId);
        requireActiveRoom(room);
        if (room.getClassSectionId() != null) {
            throw BusinessException.invalidInput(MeetingErrorConstants.ROOM_TIMETABLE_MANAGED);
        }
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

    private Room findRoom(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            throw BusinessException.notExisted(MeetingErrorConstants.ROOM_NOT_FOUND);
        }
        return roomRepository.findByPublicId(roomId)
                .orElseThrow(() -> BusinessException.notExisted(
                        MeetingErrorConstants.ROOM_NOT_FOUND,
                        roomId
                ));
    }

    @Override
    @Transactional
    public MeetingRoomSetting getSetting(String roomId) {
        return findSetting(roomId);
    }

    private MeetingRoomSetting findSetting(String roomId) {
        return initializeSetting(findRoom(roomId));
    }

    @Override
    @Transactional
    public MeetingRoomSetting initializeSetting(Room room) {
        MeetingRoomSetting existing = settingRepository.findByRoomId(room.getPublicId()).orElse(null);
        if (existing == null) {
            roomRepository.findByPublicIdForUpdate(room.getPublicId())
                    .orElseThrow(() -> BusinessException.notExisted(MeetingErrorConstants.ROOM_NOT_FOUND));
            existing = settingRepository.findByRoomId(room.getPublicId())
                    .orElseGet(() -> settingRepository.save(createDefaultSetting(room)));
        }
        return normalizeSetting(existing);
    }

    private MeetingRoomSetting normalizeSetting(MeetingRoomSetting setting) {
        if (setting.getMaxParticipants() > MeetingRoomSetting.MAX_PARTICIPANTS) {
            setting.setMaxParticipants(MeetingRoomSetting.MAX_PARTICIPANTS);
            settingRepository.save(setting);
        }
        return setting;
    }

    @Override
    @Transactional
    public void refreshClassAccess(Long classSectionId) {
        roomRepository.findByClassSectionId(classSectionId).ifPresent(room -> {
            initializeSetting(room);
            eventPublisher.publishEvent(new MeetingRoomAccessChangedEvent(room.getPublicId()));
        });
    }

    private void requireActiveRoom(Room room) {
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw BusinessException.invalidInput(MeetingErrorConstants.ROOM_ARCHIVED, room.getPublicId());
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
                || request.maxParticipants() > MeetingRoomSetting.MAX_PARTICIPANTS
                || request.joinBeforeMinutes() == null
                || request.joinBeforeMinutes() < 0
                || request.joinBeforeMinutes() > 1440) {
            throw BusinessException.invalidInput(MeetingErrorConstants.SETTING_INVALID);
        }
    }

    private MeetingRoomSetting createDefaultSetting(Room room) {
        return new MeetingRoomSetting(room.getPublicId(), true, true,
                room.getRoomType() != RoomType.CLASS, MeetingRoomSetting.MAX_PARTICIPANTS,
                room.getRoomType() == RoomType.EVENT ? 30 : 15);
    }

    private String generateRoomCode() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return "ROOM-" + suffix.toUpperCase(Locale.ROOT);
    }
}
