package org.ptit.meeting.modules.meeting.service.impl;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;
import org.ptit.meeting.modules.meeting.constant.enums.MemberStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomType;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomMemberCreateRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomMemberRoleUpdateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRoomMemberResponse;
import org.ptit.meeting.modules.meeting.entity.MeetingRoom;
import org.ptit.meeting.modules.meeting.entity.MeetingRoomMember;
import org.ptit.meeting.modules.meeting.mapper.MeetingRoomMemberMapper;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomMemberRepository;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.meeting.service.MeetingRoomMemberService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingRoomMemberServiceImpl implements MeetingRoomMemberService {

  private final MeetingRoomRepository roomRepository;
  private final MeetingRoomMemberRepository memberRepository;
  private final MeetingAuthorizationService authorizationService;
  private final MeetingRoomMemberMapper memberMapper;
  private final UserService userService;

  @Override
  @Transactional
  public MeetingRoomMemberResponse add(
      String roomId,
      MeetingRoomMemberCreateRequest request,
      Long currentUserId
  ) {
    MeetingRoom room = findRoom(roomId);
    authorizationService.requireRoomOwner(room.getId(), currentUserId);
    requireActiveRoom(room);
    validateCreateRequest(request, room);

    User targetUser = userService.getUserById(request.userId());
    userService.validateUserStatus(targetUser);

    MeetingRoomMember member = memberRepository
        .findByRoomIdAndUserId(room.getId(), targetUser.getId())
        .map(existing -> reactivate(existing, request.role()))
        .orElseGet(() -> new MeetingRoomMember(room.getId(), targetUser.getId(), request.role()));

    return memberMapper.toResponse(saveMember(member));
  }

  @Override
  @Transactional(readOnly = true)
  public List<MeetingRoomMemberResponse> getActiveMembers(
      String roomId,
      Long currentUserId
  ) {
    MeetingRoom room = findRoom(roomId);
    authorizationService.requireActiveMember(room.getId(), currentUserId);
    return memberRepository
        .findAllByRoomIdAndStatusOrderByCreatedAtAsc(room.getId(), MemberStatus.ACTIVE)
        .stream()
        .map(memberMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public MeetingRoomMemberResponse updateRole(
      String roomId,
      Long memberUserId,
      MeetingRoomMemberRoleUpdateRequest request,
      Long currentUserId
  ) {
    MeetingRoom room = findRoom(roomId);
    authorizationService.requireRoomOwner(room.getId(), currentUserId);
    requireActiveRoom(room);
    MeetingRole role = request == null ? null : request.role();
    validateManagedRole(role, room);

    MeetingRoomMember member = findActiveMember(room.getId(), memberUserId);
    requireMutableMember(member);
    member.setRole(role);
    return memberMapper.toResponse(memberRepository.save(member));
  }

  @Override
  @Transactional
  public void remove(String roomId, Long memberUserId, Long currentUserId) {
    MeetingRoom room = findRoom(roomId);
    authorizationService.requireRoomOwner(room.getId(), currentUserId);
    requireActiveRoom(room);
    MeetingRoomMember member = findActiveMember(room.getId(), memberUserId);
    requireMutableMember(member);
    member.setStatus(MemberStatus.INACTIVE);
    memberRepository.save(member);
  }

  private MeetingRoomMember reactivate(MeetingRoomMember member, MeetingRole role) {
    if (member.getStatus() == MemberStatus.ACTIVE) {
      throw BusinessException.existed(MeetingErrorConstants.MEMBER_ALREADY_EXISTS);
    }
    if (member.getRole() == MeetingRole.OWNER) {
      throw BusinessException.invalidInput(MeetingErrorConstants.OWNER_IMMUTABLE);
    }
    member.setRole(role);
    member.setStatus(MemberStatus.ACTIVE);
    return member;
  }

  private MeetingRoomMember saveMember(MeetingRoomMember member) {
    try {
      return memberRepository.saveAndFlush(member);
    } catch (DataIntegrityViolationException exception) {
      throw BusinessException.existed(MeetingErrorConstants.MEMBER_ALREADY_EXISTS);
    }
  }

  private void validateCreateRequest(
      MeetingRoomMemberCreateRequest request,
      MeetingRoom room
  ) {
    if (request == null || request.userId() == null || request.userId() <= 0) {
      throw BusinessException.invalidInput(MeetingErrorConstants.MEMBER_NOT_FOUND);
    }
    validateManagedRole(request.role(), room);
  }

  private void validateManagedRole(MeetingRole role, MeetingRoom room) {
    if (role == null || role == MeetingRole.OWNER) {
      throw BusinessException.invalidInput(MeetingErrorConstants.MEMBER_ROLE_INVALID);
    }
    if (role == MeetingRole.VIEWER && room.getRoomType() != RoomType.EVENT) {
      throw BusinessException.invalidInput(MeetingErrorConstants.VIEWER_EVENT_ONLY);
    }
  }

  private void requireMutableMember(MeetingRoomMember member) {
    if (member.getRole() == MeetingRole.OWNER) {
      throw BusinessException.invalidInput(MeetingErrorConstants.OWNER_IMMUTABLE);
    }
  }

  private void requireActiveRoom(MeetingRoom room) {
    if (room.getStatus() != RoomStatus.ACTIVE) {
      throw BusinessException.invalidInput(MeetingErrorConstants.ROOM_ARCHIVED, room.getId());
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

  private MeetingRoomMember findActiveMember(String roomId, Long userId) {
    if (userId == null || userId <= 0) {
      throw BusinessException.notExisted(MeetingErrorConstants.MEMBER_NOT_FOUND);
    }
    return memberRepository.findByRoomIdAndUserId(roomId, userId)
        .filter(member -> member.getStatus() == MemberStatus.ACTIVE)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.MEMBER_NOT_FOUND,
            userId
        ));
  }
}
