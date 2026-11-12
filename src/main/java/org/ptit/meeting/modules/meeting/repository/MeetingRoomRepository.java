package org.ptit.meeting.modules.meeting.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

public interface MeetingRoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByPublicId(String publicId);

    Optional<Room> findByClassSectionId(Long classSectionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT room FROM Room room WHERE room.publicId = :roomId")
    Optional<Room> findByPublicIdForUpdate(@Param("roomId") String roomId);

    Optional<Room> findByCode(String code);

    boolean existsByCode(String code);

    List<Room> findAllByPublicIdInAndStatusOrderByCreatedAtDesc(
            Collection<String> ids,
            RoomStatus status
    );

    @Query("""
        SELECT section.lecturerId FROM Room room
        JOIN ClassSection section ON section.id = room.classSectionId
        WHERE room.publicId = :roomId
          AND section.status = org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus.ACTIVE
        """)
    Optional<Long> findClassLecturerId(@Param("roomId") String roomId);

    @Query("""
        SELECT COUNT(section) > 0 FROM Room room
        JOIN ClassSection section ON section.id = room.classSectionId
        WHERE room.publicId = :roomId
          AND section.status = org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus.ACTIVE
        """)
    boolean hasActiveClassSection(@Param("roomId") String roomId);

    @Query("""
        SELECT room FROM Room room
        JOIN ClassSection section ON section.id = room.classSectionId
        WHERE room.status = :status
          AND section.status = org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus.ACTIVE
          AND (section.lecturerId = :userId OR EXISTS (
            SELECT enrollment.id FROM ClassEnrollment enrollment
            WHERE enrollment.classSectionId = section.id AND enrollment.userId = :userId
              AND enrollment.status = org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus.ENROLLED
          ))
        ORDER BY room.createdAt DESC
        """)
    List<Room> findClassRoomsForUser(@Param("userId") Long userId, @Param("status") RoomStatus status);
}
