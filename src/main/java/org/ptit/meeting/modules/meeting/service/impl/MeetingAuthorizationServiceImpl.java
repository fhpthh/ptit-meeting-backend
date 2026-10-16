package org.ptit.meeting.modules.meeting.service.impl;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;
import org.ptit.meeting.modules.meeting.constant.enums.MemberStatus;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomMember;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomMemberRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingAuthorizationServiceImpl implements MeetingAuthorizationService {

    private final MeetingRoomMemberRepository memberRepository;

    @Override
    public void requireActiveMember(String roomId, Long userId) {
        findActiveMember(roomId, userId);
    }

    @Override
    public void requireRoomOwner(String roomId, Long userId) {
        MeetingRoomMember member = findActiveMember(roomId, userId);
        if (member.getRole() != MeetingRole.OWNER) {
            throw BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED);
        }
    }

    @Override
    public void requireSessionHost(String roomId, Long userId) {
        MeetingRole role = findActiveMember(roomId, userId).getRole();
        if (role != MeetingRole.OWNER && role != MeetingRole.HOST) {
            throw BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED);
        }
    }

    private MeetingRoomMember findActiveMember(String roomId, Long userId) {
        if (roomId == null || roomId.isBlank() || userId == null || userId <= 0) {
            throw BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED);
        }

        return memberRepository.findByRoomIdAndUserId(roomId, userId)
                .filter(member -> member.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.unauthorized(MeetingErrorConstants.ACCESS_DENIED));
    }
}
