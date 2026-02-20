package com.recorday.recorday.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TranscodeRequest(
	@Schema(description = "S3에 업로드된 UUID 파일명 (확장자 포함)", example = "550e8400-e29b-41d4-a716-446655440000.webm")
	@NotBlank(message = "파일명은 필수입니다.")
	String filename
) {
}
