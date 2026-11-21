package org.ptit.meeting.modules.student.service;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Resolves only facts encoded in identifiers; never guesses a campus from D/E. */
@Service
public class StudentStructureResolver {
  private static final Pattern CLASS_CODE =
      Pattern.compile("^([A-Z])(\\d{2})([A-Z]+)(\\d+)(?:-([BN]))?$");
  private static final Pattern STUDENT_CODE =
      Pattern.compile("^([BN])(\\d{2})([A-Z]+)(\\d+)$");

  public Optional<Resolution> resolve(String studentCode, String administrativeClass) {
    Matcher classMatcher = CLASS_CODE.matcher(normalize(administrativeClass));
    if (!classMatcher.matches()) return Optional.empty();

    Matcher studentMatcher = STUDENT_CODE.matcher(normalize(studentCode));
    if (studentCode != null && !studentCode.isBlank() && !studentMatcher.matches()) {
      return Optional.empty();
    }
    String campus = classMatcher.group(5);
    if (studentMatcher.matches()) {
      if (!studentMatcher.group(2).equals(classMatcher.group(2))) return Optional.empty();
      String studentCampus = studentMatcher.group(1);
      if (campus != null && !campus.equals(studentCampus)) return Optional.empty();
      campus = studentCampus;
    }
    if (campus == null) return Optional.empty();

    String trainingCode = classMatcher.group(3);
    // CQ denotes the training mode, not a separate major.
    String majorCode = trainingCode.startsWith("CQ") && trainingCode.length() > 2
        ? trainingCode.substring(2) : trainingCode;
    return Optional.of(new Resolution(normalize(administrativeClass),
        campus.equals("B") ? "HN" : "HCM",
        classMatcher.group(1) + classMatcher.group(2), majorCode));
  }

  private String normalize(String value) {
    return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
  }

  public record Resolution(String classCode, String campusCode, String cohortCode,
                           String majorCode) {}
}
