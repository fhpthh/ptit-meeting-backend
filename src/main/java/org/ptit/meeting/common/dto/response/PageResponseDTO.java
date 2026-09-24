package org.ptit.meeting.common.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponseDTO<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean last
) {

  public static <T> PageResponseDTO<T> from(Page<T> page) {
    return new PageResponseDTO<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast()
    );
  }
}
