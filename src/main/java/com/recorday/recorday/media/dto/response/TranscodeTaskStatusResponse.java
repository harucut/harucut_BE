package com.recorday.recorday.media.dto.response;

import java.time.LocalDateTime;

import com.recorday.recorday.media.enums.TranscodeTaskStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "변환 작업 상태 조회 응답")
public record TranscodeTaskStatusResponse(
	@Schema(description = "변환 추적 ID", example = "3dc5f9c5-27b5-4926-a887-6e96c9c5195e")
	String taskId,

	@Schema(description = "MediaConvert Job ID", example = "1705389394214-abcdef")
	String jobId,

	@Schema(description = "현재 상태", example = "COMPLETE")
	TranscodeTaskStatus status,

	@Schema(description = "실패 메시지", example = "Input file not found")
	String errorMessage,

	@Schema(description = "완료 시 저장된 사용자 미디어 정보")
	UserMediaResponse media,

	@Schema(description = "작업 생성 시각")
	LocalDateTime createdAt,

	@Schema(description = "최종 갱신 시각")
	LocalDateTime updatedAt
) {
}
