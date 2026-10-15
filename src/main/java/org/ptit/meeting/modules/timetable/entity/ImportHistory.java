package org.ptit.meeting.modules.timetable.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.modules.timetable.constant.enums.ImportStatus;
import org.ptit.meeting.modules.timetable.constant.enums.ImportType;

@Entity
@Table(name = "import_histories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "semester_id", nullable = false)
  private Long semesterId;

  @Column(name = "uploaded_by", nullable = false)
  private Long uploadedBy;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "file_url", length = 500)
  private String fileUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "import_type", nullable = false, length = 50)
  private ImportType importType;

  @Column(name = "total_rows", nullable = false)
  @Builder.Default
  private Integer totalRows = 0;

  @Column(name = "success_rows", nullable = false)
  @Builder.Default
  private Integer successRows = 0;

  @Column(name = "failed_rows", nullable = false)
  @Builder.Default
  private Integer failedRows = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private ImportStatus status = ImportStatus.PROCESSING;

  @Column(name = "error_log", columnDefinition = "json")
  private String errorLog;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void onPrePersist() {
    LocalDateTime now = LocalDateTime.now();
    if (this.createdAt == null) {
      this.createdAt = now;
    }
    if (this.updatedAt == null) {
      this.updatedAt = now;
    }
    if (this.status == null) {
      this.status = ImportStatus.PROCESSING;
    }
  }

  @PreUpdate
  public void onPreUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}