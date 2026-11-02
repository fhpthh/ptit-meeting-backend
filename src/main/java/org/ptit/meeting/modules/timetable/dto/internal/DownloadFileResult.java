package org.ptit.meeting.modules.timetable.dto.internal;

import org.springframework.core.io.Resource;

public record DownloadFileResult(
    Resource resource,
    String fileName,
    long contentLength
) {

}