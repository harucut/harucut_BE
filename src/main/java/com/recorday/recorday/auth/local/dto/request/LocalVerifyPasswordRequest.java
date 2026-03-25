package com.recorday.recorday.auth.local.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기존 비밀번호 검증 요청 DTO")
public record LocalVerifyPasswordRequest(
	@Schema(description = "검증할 기존 비밀번호", example = "oldPassword123!")
	String password
) {
}
