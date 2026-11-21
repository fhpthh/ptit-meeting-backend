package org.ptit.meeting.modules.student;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.ptit.meeting.modules.student.service.StudentStructureResolver;

class StudentStructureResolverTest {
  private final StudentStructureResolver resolver = new StudentStructureResolver();

  @Test
  void infersNorthernCampusFromStudentCodeWithoutClassSuffix() {
    var value = resolver.resolve(" b22dccn033 ", " d22cnpm04 ").orElseThrow();
    assertEquals("HN", value.campusCode());
    assertEquals("D22", value.cohortCode());
    assertEquals("CNPM", value.majorCode());
    assertEquals("D22CNPM04", value.classCode());
  }

  @Test
  void recognizesSouthernClassAndRemovesTrainingModeFromGroupingCode() {
    var value = resolver.resolve("N22DCCN052", "D22CQCN01-N").orElseThrow();
    assertEquals("HCM", value.campusCode());
    assertEquals("CN", value.majorCode());
  }

  @Test
  void supportsProgramPrefixesAndLongSpecializationCodes() {
    assertEquals("E22", resolver.resolve("N22DCCN052", "E22CQCN01-N")
        .orElseThrow().cohortCode());
    assertEquals("CNHT", resolver.resolve("N21DCCN052", "D21CQCNHT01-N")
        .orElseThrow().majorCode());
  }

  @Test
  void refusesConflictingCampusOrAdmissionYear() {
    assertTrue(resolver.resolve("B22DCCN033", "D22CQCN01-N").isEmpty());
    assertTrue(resolver.resolve("N23DCCN052", "D22CQCN01-N").isEmpty());
  }

  @Test
  void doesNotGuessCampusFromClassProgramPrefix() {
    assertTrue(resolver.resolve(null, "D22CNPM04").isEmpty());
    assertEquals("HCM", resolver.resolve(null, "D22CQCN01-N").orElseThrow().campusCode());
    assertTrue(resolver.resolve("invalid", "D22CQCN01-N").isEmpty());
    assertTrue(resolver.resolve("B22DCCN033", "D22CNPM04,D22CNPM05").isEmpty());
  }
}
