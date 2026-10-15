package org.ptit.meeting.modules.timetable.adapter;

import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.ptit.meeting.modules.timetable.constant.enums.ImportType;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;

public interface ExcelImportAdapter<T> {

  ImportType getSupportedType();

  void initializeHeader(Sheet sheet);

  int getDataStartRow();

  default int getHeaderRowIndex() {
    return 0;
  }

  default int getDetectedWeeksCount() {
    return 0;
  }

  default int getMappedColumnsCount() {
    return 0;
  }

  T adapt(Row row, int rowNumber, List<ImportRowError> errors);

  boolean validate(T data, int rowNumber, List<ImportRowError> errors);
}