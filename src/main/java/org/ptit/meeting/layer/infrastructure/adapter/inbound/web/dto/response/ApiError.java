package org.ptit.meeting.layer.infrastructure.adapter.inbound.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    String code,
    String message,
    Object fieldErrors
) {

}
