package org.ptit.meeting.modules.timetable.service;

import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class ScheduleDateCalculator {

  public LocalDate calculateStudyDate(LocalDate semesterStartDate, int weekNumber, int dayOfWeek) {
    // dayOfWeek: 2 (Thứ Hai) -> 8 (Chủ Nhật).
    // Độ lệch ngày trong tuần: Thứ 2 = 0, Thứ 3 = 1, ..., CN = 6
    long dayOffset = dayOfWeek - 2;
    long totalDaysToAdd = ((long) (weekNumber - 1) * 7) + dayOffset;
    return semesterStartDate.plusDays(totalDaysToAdd);
  }
}