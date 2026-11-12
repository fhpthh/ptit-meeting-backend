package org.ptit.meeting.modules.meeting.service.impl;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.entity.Room;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;
import org.ptit.meeting.modules.timetable.repository.ClassEnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingAuthorizationServiceImpl implements MeetingAuthorizationService {

    private final ClassEnrollmentRepository memberRepository;
    private final MeetingRoomRepository roomRepository;

    @Override
    public ClassEnrollment requireActiveMember(String roomId, Long userId) {
        return findActiveMember(roomId, userId);
    }

    @Override
    public void requireRoomOwner(String roomId, Long userId) {
        ClassEnrollment member = findActiveMember(roomId, userId);
        if (member.getRole() != MeetingRole.OWNER) {
            throw BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED);
        }
    }

    @Override
    public void requireRuntimeSessionHost(String roomId, Long userId) {
        MeetingRole role = findActiveMember(roomId, userId).getRole();
        if (role != MeetingRole.OWNER && role != MeetingRole.HOST) {
            throw BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED);
        }
    }

    private ClassEnrollment findActiveMember(String roomId, Long userId) {
        if (roomId == null || roomId.isBlank() || userId == null || userId <= 0) {
            throw BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED);
        }

        Room room = roomRepository.findByPublicId(roomId)
                .orElseThrow(() -> BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED));
        if (room.getClassSectionId() != null) {
            if (!roomRepository.hasActiveClassSection(roomId)) {
                throw BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED);
            }
            if (roomRepository.findClassLecturerId(roomId).filter(userId::equals).isPresent()) {
                return ClassEnrollment.builder()
                        .classSectionId(room.getClassSectionId())
                        .userId(userId)
                        .role(MeetingRole.OWNER)
                        .enrolledAt(room.getCreatedAt())
                        .updatedAt(room.getUpdatedAt())
                        .build();
            }
            return memberRepository.findByClassSectionIdAndUserId(room.getClassSectionId(), userId)
                    .filter(member -> member.getStatus() == EnrollmentStatus.ENROLLED)
                    .orElseThrow(() -> BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED));
        }

        return memberRepository.findByRoomIdAndUserId(roomId, userId)
                .filter(member -> member.getStatus() == EnrollmentStatus.ENROLLED)
                .orElseThrow(() -> BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED));
    }
}
