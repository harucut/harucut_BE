package com.recorday.recorday.media.dto.response;

import java.time.LocalDateTime;

import com.recorday.recorday.media.enums.TranscodeTaskStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "변환 작업 제출 응답")
public record TranscodeTaskSubmitResponse(
	@Schema(description = "변환 추적 ID", example = "3dc5f9c5-27b5-4926-a887-6e96c9c5195e")
	String taskId,

	@Schema(description = "MediaConvert Job ID", example = "1705389394214-abcdef")
	String jobId,

	@Schema(description = "현재 상태", example = "SUBMITTED")
	TranscodeTaskStatus status,

	@Schema(description = "요청 시각")
	LocalDateTime requestedAt
) {
}
