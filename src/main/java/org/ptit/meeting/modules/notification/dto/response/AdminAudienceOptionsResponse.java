package org.ptit.meeting.modules.notification.dto.response;

import java.util.List;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionOptionDto;

/**
 * Available active departments and class sections for PGV/Admin announcement targeting.
 */
public record AdminAudienceOptionsResponse(
    List<String> departments,
    List<ClassSectionOptionDto> classSections
) {
}
