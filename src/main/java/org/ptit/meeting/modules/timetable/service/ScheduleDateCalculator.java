package org.ptit.meeting.modules.timetable.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import org.springframework.stereotype.Component;

@Component
public class ScheduleDateCalculator {

  private static final int MONDAY_VALUE = 2;
  private static final int SUNDAY_VALUE = 8;

  public LocalDate calculateStudyDate(LocalDate semesterStartDate, int weekNumber, int dayOfWeek) {
    validateWeekNumber(weekNumber);
    validateDayOfWeek(dayOfWeek);

    return calculateWeekStart(semesterStartDate, weekNumber)
        .plusDays(dayOfWeek - MONDAY_VALUE);
  }

  public LocalDate calculateWeekStart(LocalDate semesterStartDate, int weekNumber) {
    validateWeekNumber(weekNumber);
    return semesterStartDate
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .plusWeeks(weekNumber - 1L);
  }

  public long calculateTotalWeeks(LocalDate semesterStartDate, LocalDate semesterEndDate) {
    LocalDate firstWeekStart = calculateWeekStart(semesterStartDate, 1);
    long totalDays = ChronoUnit.DAYS.between(firstWeekStart, semesterEndDate) + 1;
    return (totalDays + 6) / 7;
  }

  private void validateWeekNumber(int weekNumber) {
    if (weekNumber < 1) {
      throw new IllegalArgumentException("Week number must be greater than zero");
    }
  }

  private void validateDayOfWeek(int dayOfWeek) {
    if (dayOfWeek < MONDAY_VALUE || dayOfWeek > SUNDAY_VALUE) {
      throw new IllegalArgumentException("Day of week must be between 2 and 8");
    }
  }
}
