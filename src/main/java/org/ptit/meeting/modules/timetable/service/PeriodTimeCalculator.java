package org.ptit.meeting.modules.timetable.service;

import jakarta.annotation.PostConstruct;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.timetable.config.TimetableProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PeriodTimeCalculator {

  private static final Map<Integer, PeriodSlot> DEFAULT_SLOTS = Map.ofEntries(
      Map.entry(1, new PeriodSlot(1, LocalTime.of(7, 0), LocalTime.of(7, 50))),
      Map.entry(2, new PeriodSlot(2, LocalTime.of(8, 0), LocalTime.of(8, 50))),
      Map.entry(3, new PeriodSlot(3, LocalTime.of(9, 0), LocalTime.of(9, 50))),
      Map.entry(4, new PeriodSlot(4, LocalTime.of(10, 0), LocalTime.of(10, 50))),
      Map.entry(5, new PeriodSlot(5, LocalTime.of(11, 0), LocalTime.of(11, 50))),
      Map.entry(6, new PeriodSlot(6, LocalTime.of(12, 0), LocalTime.of(12, 50))),
      Map.entry(7, new PeriodSlot(7, LocalTime.of(13, 0), LocalTime.of(13, 50))),
      Map.entry(8, new PeriodSlot(8, LocalTime.of(14, 0), LocalTime.of(14, 50))),
      Map.entry(9, new PeriodSlot(9, LocalTime.of(15, 0), LocalTime.of(15, 50))),
      Map.entry(10, new PeriodSlot(10, LocalTime.of(16, 0), LocalTime.of(16, 50))),
      Map.entry(11, new PeriodSlot(11, LocalTime.of(17, 0), LocalTime.of(17, 50))),
      Map.entry(12, new PeriodSlot(12, LocalTime.of(18, 0), LocalTime.of(18, 50))),
      Map.entry(13, new PeriodSlot(13, LocalTime.of(19, 0), LocalTime.of(19, 50))),
      Map.entry(14, new PeriodSlot(14, LocalTime.of(20, 0), LocalTime.of(20, 50))),
      Map.entry(15, new PeriodSlot(15, LocalTime.of(21, 0), LocalTime.of(21, 50)))
  );
  private final TimetableProperties timetableProperties;
  private final Map<Integer, PeriodSlot> activeSlots = new HashMap<>();

  public record PeriodSlot(int period, LocalTime start, LocalTime end) {

  }

  @PostConstruct
  public void init() {
    activeSlots.putAll(DEFAULT_SLOTS);
    if (timetableProperties != null && timetableProperties.getPeriods() != null && !timetableProperties.getPeriods().isEmpty()) {
      timetableProperties.getPeriods().forEach((period, cfg) -> {
        if (cfg.getStart() != null && cfg.getEnd() != null) {
          activeSlots.put(period, new PeriodSlot(period, cfg.getStart(), cfg.getEnd()));
        }
      });
      log.info("(init) Loaded {} period slots from application.yml", timetableProperties.getPeriods().size());
    }
  }
  public LocalTime calculateStartTime(Integer startPeriod) {
    validateStartPeriod(startPeriod);
    return activeSlots.get(startPeriod).start();
  }
  public LocalTime calculateEndTime(Integer startPeriod, Integer numPeriods) {
    validatePeriodRange(startPeriod, numPeriods);
    int endPeriod = startPeriod + numPeriods - 1;
    return activeSlots.get(endPeriod).end();
  }
  public void validatePeriodRange(Integer startPeriod, Integer numPeriods) {
    validateStartPeriod(startPeriod);
    if (numPeriods == null || numPeriods <= 0) {
      throw BusinessException.invalidInput("numPeriods can't be null or less than zero");
    }
    int endPeriod = startPeriod + numPeriods - 1;
    if (!activeSlots.containsKey(endPeriod)) {
      throw BusinessException.invalidInput("End period invalid");
    }
  }
  private void validateStartPeriod(Integer startPeriod) {
    if (startPeriod == null || !activeSlots.containsKey(startPeriod)) {
      throw BusinessException.invalidInput("Start period invalid");
    }
  }
}
