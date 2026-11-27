package org.ptit.meeting.modules.meeting.service.impl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomType;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.entity.Room;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.ptit.meeting.modules.timetable.repository.ClassEnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingAuthorizationServiceImpl implements MeetingAuthorizationService {

    private final ClassEnrollmentRepository memberRepository;
    private final MeetingRoomRepository roomRepository;
    private final UserService userService;

    @Override
    public MeetingRole requireRoomAccess(String roomId, Long userId) {
        Room room = findRoom(roomId, userId);
        Optional<ClassEnrollment> member = findActiveMember(room, userId);
        if (room.getRoomType() == RoomType.EVENT && room.getStatus() == RoomStatus.ACTIVE) {
            userService.validateUserStatus(userService.getUserById(userId));
            return member.map(ClassEnrollment::getRole).orElse(MeetingRole.VIEWER);
        }
        return member.orElseThrow(() ->
                BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED)).getRole();
    }

    @Override
    public ClassEnrollment requireActiveMember(String roomId, Long userId) {
        return findActiveMember(findRoom(roomId, userId), userId)
                .orElseThrow(() -> BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED));
    }

    @Override
    public void requireRoomOwner(String roomId, Long userId) {
        ClassEnrollment member = requireActiveMember(roomId, userId);
        if (member.getRole() != MeetingRole.OWNER) {
            throw BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED);
        }
    }

    @Override
    public void requireRuntimeSessionHost(String roomId, Long userId) {
        MeetingRole role = requireActiveMember(roomId, userId).getRole();
        if (role != MeetingRole.OWNER && role != MeetingRole.HOST) {
            throw BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED);
        }
    }

    private Room findRoom(String roomId, Long userId) {
        if (roomId == null || roomId.isBlank() || userId == null || userId <= 0) {
            throw BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED);
        }

        return roomRepository.findByPublicId(roomId)
                .orElseThrow(() -> BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED));
    }

    private Optional<ClassEnrollment> findActiveMember(Room room, Long userId) {
        if (room.getClassSectionId() != null) {
            ClassSection section = roomRepository.findActiveClassSection(room.getPublicId())
                    .orElseThrow(() -> BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED));
            if (userId.equals(section.getLecturerId())) {
                return Optional.of(ClassEnrollment.builder()
                        .classSectionId(room.getClassSectionId())
                        .userId(userId)
                        .role(MeetingRole.OWNER)
                        .enrolledAt(room.getCreatedAt())
                        .updatedAt(room.getUpdatedAt())
                        .build());
            }
            return memberRepository.findByClassSectionIdAndUserId(room.getClassSectionId(), userId)
                    .filter(member -> member.getStatus() == EnrollmentStatus.ENROLLED);
        }

        return memberRepository.findByRoomIdAndUserId(room.getPublicId(), userId)
                .filter(member -> member.getStatus() == EnrollmentStatus.ENROLLED);
    }
}
