package org.ptit.meeting.layer.domain.model;

import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

  private Long id;
  private String code;
  private String name;
  private String description;
  @Builder.Default
  private Set<String> permissions = new HashSet<>();
}
