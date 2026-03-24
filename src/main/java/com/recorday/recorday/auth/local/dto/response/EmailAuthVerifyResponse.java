package com.recorday.recorday.auth.local.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 인증 코드 검증 응답 DTO")
public record EmailAuthVerifyResponse(
	@Schema(description = "비밀번호 재설정 토큰", example = "reset-token-example")
	String resetToken
) {
}
