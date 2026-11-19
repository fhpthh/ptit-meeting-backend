package org.ptit.meeting.modules.auth.exporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.constant.AuthErrorConstants;
import org.ptit.meeting.modules.auth.constant.enums.StudentAdminExportFormat;
import org.ptit.meeting.modules.auth.dto.internal.StudentAdminExportFile;
import org.ptit.meeting.modules.auth.dto.response.StudentAdminResponse;
import org.springframework.stereotype.Component;

@Component
public class StudentAdminExporter {
  private static final List<String> HEADERS = List.of(
      "STT", "code", "fullName", "email", "department", "status", "createdAt", "updatedAt");

  public StudentAdminExportFile export(List<StudentAdminResponse> students, StudentAdminExportFormat format) {
    return switch (format) {
      case CSV -> csv(students);
      case XLSX -> xlsx(students);
    };
  }

  private StudentAdminExportFile csv(List<StudentAdminResponse> students) {
    StringBuilder csv = new StringBuilder("\uFEFF");
    appendCsvRow(csv, HEADERS);
    for (int i = 0; i < students.size(); i++) {
      appendCsvRow(csv, columns(i + 1, students.get(i)));
    }
    return new StudentAdminExportFile("students.csv", "text/csv;charset=UTF-8",
        csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  private void appendCsvRow(StringBuilder csv, List<String> columns) {
    csv.append(columns.stream().map(this::csvCell).collect(Collectors.joining(","))).append("\r\n");
  }

  private String csvCell(String value) {
    String stripped = value.stripLeading();
    if (value.startsWith("\t") || value.startsWith("\r") || value.startsWith("\n")
        || (!stripped.isEmpty() && "=+-@＝＋－＠".indexOf(stripped.charAt(0)) >= 0)) {
      value = "'" + value;
    }
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }

  private StudentAdminExportFile xlsx(List<StudentAdminResponse> students) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Students");
      writeRow(sheet.createRow(0), HEADERS);
      for (int i = 0; i < students.size(); i++) {
        writeRow(sheet.createRow(i + 1), columns(i + 1, students.get(i)));
      }
      sheet.createFreezePane(0, 1);
      for (int i = 0; i < HEADERS.size(); i++) {
        sheet.setColumnWidth(i, 25 * 256);
      }
      workbook.write(output);
      return new StudentAdminExportFile("students.xlsx",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
    } catch (IOException exception) {
      throw new BusinessException(StatusCode.ERROR_IN_BACKEND, AuthErrorConstants.STUDENT_EXPORT_FAILED);
    }
  }

  private void writeRow(Row row, List<String> columns) {
    for (int i = 0; i < columns.size(); i++) {
      row.createCell(i).setCellValue(columns.get(i));
    }
  }

  private List<String> columns(int sequenceNumber, StudentAdminResponse student) {
    return List.of(Integer.toString(sequenceNumber), Objects.toString(student.code(), ""), student.fullName(),
        student.email(), Objects.toString(student.department(), ""), student.status().name(),
        student.createdAt().toString(), student.updatedAt().toString());
  }
}
