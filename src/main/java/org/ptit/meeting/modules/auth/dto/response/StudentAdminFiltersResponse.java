package org.ptit.meeting.modules.auth.dto.response;

import java.util.List;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.student.dto.response.StudentStructureOptionsResponse;

public record StudentAdminFiltersResponse(
    List<UserStatus> statuses,
    List<String> departments,
    StudentStructureOptionsResponse academicStructure
) {}
