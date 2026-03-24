package com.recorday.recorday.media.dto.response;

import java.time.LocalDateTime;

import com.recorday.recorday.media.enums.UserMediaType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 미디어 응답")
public record UserMediaResponse(
	@Schema(description = "미디어 ID", example = "1")
	Long mediaId,

	@Schema(description = "미디어 타입", example = "VIDEO")
	UserMediaType mediaType,

	@Schema(description = "S3 Key", example = "uploads/users/AbCdEf12Gh/mp4/550e8400-e29b-41d4-a716-446655440000.mp4")
	String s3Key,

	@Schema(description = "사용자 표시 파일명", example = "harucut_20260318_102030.mp4")
	String displayName,

	@Schema(description = "다운로드 URL (Presigned URL)", example = "https://harucuts3.s3.ap-northeast-2.amazonaws.com/...")
	String downloadUrl,

	@Schema(description = "원본 S3 Key", example = "uploads/users/AbCdEf12Gh/webm/550e8400-e29b-41d4-a716-446655440000.webm")
	String originalS3Key,

	@Schema(description = "원본 파일명", example = "550e8400-e29b-41d4-a716-446655440000.webm")
	String originalFileName,

	@Schema(description = "변환 작업 ID", example = "1705389394214-abcdef")
	String transcodeJobId,

	@Schema(description = "등록 시각")
	LocalDateTime createdAt
) {
}
