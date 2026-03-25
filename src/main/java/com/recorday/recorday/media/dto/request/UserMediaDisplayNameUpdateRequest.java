package com.recorday.recorday.media.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 미디어 표시 파일명 수정 요청")
public record UserMediaDisplayNameUpdateRequest(
	@NotBlank(message = "표시 파일명은 필수입니다.")
	@Size(max = 255, message = "표시 파일명은 255자 이하여야 합니다.")
	@Schema(description = "사용자에게 표시될 파일명", example = "my_holiday_video")
	String displayName
) {
}
