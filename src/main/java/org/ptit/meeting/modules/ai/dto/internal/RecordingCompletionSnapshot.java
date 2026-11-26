package org.ptit.meeting.modules.ai.dto.internal;

import org.ptit.meeting.modules.ai.dto.response.RecordingJobContextResponse;
import org.ptit.meeting.modules.ai.dto.response.RecordingTranslationResultResponse;

public record RecordingCompletionSnapshot(
    RecordingJobContextResponse context, RecordingTranslationResultResponse existing) {}
