package org.ptit.meeting.modules.timetable.repository;

import java.util.Optional;
import org.ptit.meeting.modules.timetable.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Quản lý phòng học trực tuyến cố định của từng Lớp học phần (1 LHP = 1 Room).
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

  Optional<Room> findByClassSectionId(Long classSectionId);

  boolean existsByClassSectionId(Long classSectionId);

}
