package org.ptit.meeting.modules.meeting.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.meeting.constant.enums.MeetingRole;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomType;
import org.ptit.meeting.modules.meeting.constant.enums.SessionStatus;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomMemberCreateRequest;
import org.ptit.meeting.modules.meeting.dto.request.MeetingRoomMemberRoleUpdateRequest;
import org.ptit.meeting.modules.meeting.dto.response.MeetingRoomMemberResponse;
import org.ptit.meeting.modules.meeting.entity.Room;
import org.ptit.meeting.modules.meeting.event.MeetingParticipantRemovedEvent;
import org.ptit.meeting.modules.meeting.event.MeetingRoomAccessChangedEvent;
import org.ptit.meeting.modules.meeting.mapper.MeetingRoomMemberMapper;
import org.ptit.meeting.modules.meeting.repository.MeetingRoomRepository;
import org.ptit.meeting.modules.meeting.repository.MeetingRuntimeSessionRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.ptit.meeting.modules.meeting.service.MeetingRoomMemberService;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;
import org.ptit.meeting.modules.timetable.repository.ClassEnrollmentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingRoomMemberServiceImpl implements MeetingRoomMemberService {

  private final MeetingRoomRepository roomRepository;
  private final ClassEnrollmentRepository memberRepository;
  private final MeetingAuthorizationService authorizationService;
  private final MeetingRoomMemberMapper memberMapper;
  private final UserService userService;
  private final MeetingRuntimeSessionRepository sessionRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public MeetingRoomMemberResponse add(
      String roomId,
      MeetingRoomMemberCreateRequest request,
      Long currentUserId
  ) {
    Room room = findRoom(roomId);
    authorizationService.requireRoomOwner(room.getPublicId(), currentUserId);
    requireActiveRoom(room);
    validateCreateRequest(request, room);

    User targetUser = userService.getUserById(request.userId());
    userService.validateUserStatus(targetUser);
    requireMutableUser(room, targetUser.getId());

    ClassEnrollment member = findMember(room, targetUser.getId())
        .map(existing -> reactivate(existing, request.role()))
        .orElseGet(() -> ClassEnrollment.builder()
            .classSectionId(room.getClassSectionId())
            .roomId(room.getClassSectionId() == null ? room.getPublicId() : null)
            .userId(targetUser.getId())
            .role(request.role())
            .build());

    return memberMapper.toResponse(saveMember(member), room.getPublicId());
  }

  @Override
  @Transactional(readOnly = true)
  public List<MeetingRoomMemberResponse> getActiveMembers(
      String roomId,
      Long currentUserId
  ) {
    Room room = findRoom(roomId);
    authorizationService.requireActiveMember(room.getPublicId(), currentUserId);
    List<ClassEnrollment> members = room.getClassSectionId() == null
        ? memberRepository.findAllByRoomIdAndStatusOrderByEnrolledAtAsc(
            room.getPublicId(), EnrollmentStatus.ENROLLED)
        : memberRepository.findAllByClassSectionIdAndStatusOrderByEnrolledAtAsc(
            room.getClassSectionId(), EnrollmentStatus.ENROLLED);
    Long lecturerId = room.getClassSectionId() == null ? null
        : roomRepository.findClassLecturerId(room.getPublicId()).orElse(null);
    List<MeetingRoomMemberResponse> response = new ArrayList<>();
    if (lecturerId != null) {
      response.add(memberMapper.toResponse(
          authorizationService.requireActiveMember(room.getPublicId(), lecturerId), room.getPublicId()));
    }
    members.stream()
        .filter(member -> !member.getUserId().equals(lecturerId))
        .map(member -> memberMapper.toResponse(member, room.getPublicId()))
        .forEach(response::add);
    return response;
  }

  @Override
  @Transactional
  public MeetingRoomMemberResponse updateRole(
      String roomId,
      Long memberUserId,
      MeetingRoomMemberRoleUpdateRequest request,
      Long currentUserId
  ) {
    Room room = findRoom(roomId);
    authorizationService.requireRoomOwner(room.getPublicId(), currentUserId);
    requireActiveRoom(room);
    MeetingRole role = request == null ? null : request.role();
    validateManagedRole(role, room);

    requireMutableUser(room, memberUserId);
    ClassEnrollment member = findActiveMember(room, memberUserId);
    requireMutableMember(member);
    member.setRole(role);
    MeetingRoomMemberResponse response = memberMapper.toResponse(
        memberRepository.saveAndFlush(member), room.getPublicId());
    eventPublisher.publishEvent(new MeetingRoomAccessChangedEvent(room.getPublicId()));
    return response;
  }

  @Override
  @Transactional
  public void remove(String roomId, Long memberUserId, Long currentUserId) {
    Room room = findRoom(roomId);
    authorizationService.requireRoomOwner(room.getPublicId(), currentUserId);
    requireActiveRoom(room);
    requireMutableUser(room, memberUserId);
    ClassEnrollment member = findActiveMember(room, memberUserId);
    requireMutableMember(member);
    member.setStatus(EnrollmentStatus.DROPPED);
    memberRepository.save(member);
    sessionRepository.findAllByRoomIdAndStatus(room.getId(), SessionStatus.LIVE)
        .forEach(session -> eventPublisher.publishEvent(new MeetingParticipantRemovedEvent(
            session.getProviderRoomName(), "user-" + memberUserId)));
  }

  private ClassEnrollment reactivate(ClassEnrollment member, MeetingRole role) {
    if (member.getStatus() == EnrollmentStatus.ENROLLED) {
      throw BusinessException.existed(MeetingErrorConstants.MEMBER_ALREADY_EXISTS);
    }
    if (member.getRole() == MeetingRole.OWNER) {
      throw BusinessException.invalidInput(MeetingErrorConstants.OWNER_IMMUTABLE);
    }
    member.setRole(role);
    member.setStatus(EnrollmentStatus.ENROLLED);
    return member;
  }

  private ClassEnrollment saveMember(ClassEnrollment member) {
    try {
      return memberRepository.saveAndFlush(member);
    } catch (DataIntegrityViolationException exception) {
      throw BusinessException.existed(MeetingErrorConstants.MEMBER_ALREADY_EXISTS);
    }
  }

  private void validateCreateRequest(
      MeetingRoomMemberCreateRequest request,
      Room room
  ) {
    if (request == null || request.userId() == null || request.userId() <= 0) {
      throw BusinessException.invalidInput(MeetingErrorConstants.MEMBER_NOT_FOUND);
    }
    validateManagedRole(request.role(), room);
  }

  private void validateManagedRole(MeetingRole role, Room room) {
    if (role == null || role == MeetingRole.OWNER) {
      throw BusinessException.invalidInput(MeetingErrorConstants.MEMBER_ROLE_INVALID);
    }
    if (role == MeetingRole.VIEWER && room.getRoomType() != RoomType.EVENT) {
      throw BusinessException.invalidInput(MeetingErrorConstants.VIEWER_EVENT_ONLY);
    }
  }

  private void requireMutableMember(ClassEnrollment member) {
    if (member.getRole() == MeetingRole.OWNER) {
      throw BusinessException.invalidInput(MeetingErrorConstants.OWNER_IMMUTABLE);
    }
  }

  private void requireMutableUser(Room room, Long userId) {
    if (room.getClassSectionId() != null
        && roomRepository.findClassLecturerId(room.getPublicId()).filter(id -> id.equals(userId)).isPresent()) {
      throw BusinessException.invalidInput(MeetingErrorConstants.OWNER_IMMUTABLE);
    }
  }

  private void requireActiveRoom(Room room) {
    if (room.getStatus() != RoomStatus.ACTIVE) {
      throw BusinessException.invalidInput(MeetingErrorConstants.ROOM_ARCHIVED, room.getPublicId());
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

  private Optional<ClassEnrollment> findMember(Room room, Long userId) {
    return room.getClassSectionId() == null
        ? memberRepository.findByRoomIdAndUserId(room.getPublicId(), userId)
        : memberRepository.findByClassSectionIdAndUserId(room.getClassSectionId(), userId);
  }

  private ClassEnrollment findActiveMember(Room room, Long userId) {
    if (userId == null || userId <= 0) {
      throw BusinessException.notExisted(MeetingErrorConstants.MEMBER_NOT_FOUND);
    }
    return findMember(room, userId)
        .filter(member -> member.getStatus() == EnrollmentStatus.ENROLLED)
        .orElseThrow(() -> BusinessException.notExisted(
            MeetingErrorConstants.MEMBER_NOT_FOUND,
            userId
        ));
  }
}
