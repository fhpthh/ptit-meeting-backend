package org.ptit.meeting.modules.timetable.adapter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.constant.enums.ImportType;
import org.springframework.stereotype.Component;

@Component
public class ExcelAdapterFactory {

  private final Map<ImportType, ExcelImportAdapter<?>> adapterMap;

  public ExcelAdapterFactory(List<ExcelImportAdapter<?>> adapters) {
    this.adapterMap = adapters.stream()
        .collect(Collectors.toMap(ExcelImportAdapter::getSupportedType, Function.identity()));
  }

  @SuppressWarnings("unchecked")
  public <T> ExcelImportAdapter<T> getAdapter(ImportType type) {
    ExcelImportAdapter<?> adapter = adapterMap.get(type);
    if (adapter == null) {
      throw BusinessException.invalidInputFile(TimetableErrorConstants.FILE_INVALID_FORMAT);
    }
    return (ExcelImportAdapter<T>) adapter;
  }
}