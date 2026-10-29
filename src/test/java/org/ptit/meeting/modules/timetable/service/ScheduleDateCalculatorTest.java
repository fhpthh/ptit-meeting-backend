package org.ptit.meeting.modules.timetable.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScheduleDateCalculatorTest {

  private ScheduleDateCalculator calculator;

  @BeforeEach
  void setUp() {
    calculator = new ScheduleDateCalculator();
  }

  @Test
  void shouldStartFirstWeekOnMonday() {
    LocalDate result = calculator.calculateWeekStart(
        LocalDate.of(2026, 9, 1),
        1
    );

    assertEquals(LocalDate.of(2026, 8, 31), result);
  }

  @Test
  void shouldCalculateStudyDateWithinCalendarWeek() {
    LocalDate firstDay = calculator.calculateStudyDate(
        LocalDate.of(2026, 9, 1),
        2,
        2
    );
    LocalDate lastDay = calculator.calculateStudyDate(
        LocalDate.of(2026, 9, 1),
        1,
        8
    );

    assertEquals(LocalDate.of(2026, 9, 7), firstDay);
    assertEquals(LocalDate.of(2026, 9, 6), lastDay);
  }

  @Test
  void shouldCalculateTotalAcademicWeeks() {
    long result = calculator.calculateTotalWeeks(
        LocalDate.of(2026, 9, 1),
        LocalDate.of(2027, 1, 31)
    );

    assertEquals(22, result);
  }

  @Test
  void shouldRejectInvalidCalendarValues() {
    LocalDate semesterStart = LocalDate.of(2026, 9, 1);

    assertThrows(
        IllegalArgumentException.class,
        () -> calculator.calculateWeekStart(semesterStart, 0)
    );
    assertThrows(
        IllegalArgumentException.class,
        () -> calculator.calculateStudyDate(semesterStart, 1, 9)
    );
  }
}
