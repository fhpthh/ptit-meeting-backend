package org.ptit.meeting.modules.timetable.dto.internal;

import java.time.LocalTime;
import java.util.Set;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterTimetableRowDto {
  private int rowNumber;
  private String subjectCode;
  private String subjectName;
  private String cohort;
  private String classCode;
  private String groupNumber;
  private String practiceGroup;
  private String managementClass;
  private String lecturerName;
  private String department;
  private Integer dayOfWeek;
  private Integer startPeriod;
  private Integer numPeriods;
  private LocalTime startTime;
  private LocalTime endTime;
  private String roomText;
  private String building;
  private Integer credit;
  private Set<Integer> activeWeeks; // Tập hợp các tuần có đánh dấu 'x'
  private boolean isOnline;
}