package org.ptit.meeting.common.base;

import java.util.List;

/**
 * Shared contract for feature-level entity/DTO mappers.
 */
public interface BaseMapper<E, D> {

  D toDto(E entity);

  E toEntity(D dto);

  List<D> toDto(List<E> entities);

  List<E> toEntity(List<D> dtos);
}
