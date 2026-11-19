package org.ptit.meeting.modules.student.dto.response;

import java.util.List;

public record StudentStructureTreeResponse(List<CampusNode> campuses, long assignedStudents) {
  public record CampusNode(Long id, String code, String name, long studentCount,
                           List<CohortNode> cohorts) {}
  public record CohortNode(Long id, String code, String name, long studentCount,
                           List<MajorNode> majors) {}
  public record MajorNode(Long id, String code, String name, long studentCount,
                          List<ClassNode> administrativeClasses) {}
  public record ClassNode(Long id, String code, String name, long studentCount) {}
}
