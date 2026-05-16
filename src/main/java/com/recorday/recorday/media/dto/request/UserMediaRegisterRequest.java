package com.recorday.recorday.media.dto.request;

import com.recorday.recorday.media.enums.UserMediaType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "사용자 미디어 등록 요청")
public record UserMediaRegisterRequest(
	@NotNull(message = "미디어 타입은 필수입니다.")
	@Schema(description = "미디어 타입", example = "PHOTO")
	UserMediaType mediaType,

	@NotBlank(message = "S3 Key는 필수입니다.")
	@Schema(description = "S3 Object Key", example = "uploads/users/AbCdEf12Gh/fourcuts/550e8400-e29b-41d4-a716-446655440000.png")
	String s3Key,

	@Schema(description = "사용자 표시 파일명", example = "나의 기록.mp4")
	String displayName
) {
}
