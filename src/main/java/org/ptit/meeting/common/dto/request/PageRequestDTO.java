package org.ptit.meeting.common.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageRequestDTO {

  @Min(0)
  private int page = 0;

  @Min(1)
  @Max(100)
  private int size = 20;

  private String sortBy = "createdAt";

  private Sort.Direction direction = Sort.Direction.DESC;

  public Pageable toPageable() {
    return PageRequest.of(page, size, Sort.by(direction, sortBy));
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public String getSortBy() {
    return sortBy;
  }

  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }

  public Sort.Direction getDirection() {
    return direction;
  }

  public void setDirection(Sort.Direction direction) {
    this.direction = direction;
  }
}
