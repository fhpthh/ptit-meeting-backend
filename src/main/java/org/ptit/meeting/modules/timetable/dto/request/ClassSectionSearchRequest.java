package org.ptit.meeting.modules.timetable.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ClassSectionSearchRequest {

  @NotBlank(message = "{validation.timetable.subject.cohort.required}")
  private String cohort;
  private String keyword;
  @Min(value = 0, message = "{validation.page.number.min}")
  private int page = 0;
  @Min(value = 1, message = "{validation.page.size.min}")
  @Max(value = 100, message = "{validation.page.size.max}")
  private int size = 10;
  private String sortBy = "classCode";
  private String direction = "ASC";

  public String getCohort() {
    return cohort;
  }

  public void setCohort(String cohort) {
    this.cohort = cohort;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
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

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }
}
