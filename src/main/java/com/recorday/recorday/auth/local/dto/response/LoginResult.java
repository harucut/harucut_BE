package com.recorday.recorday.auth.local.dto.response;

import com.recorday.recorday.auth.jwt.dto.AuthTokenCookies;
import com.recorday.recorday.user.enums.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 처리 결과 DTO")
public record LoginResult(
	@Schema(description = "발급된 토큰 쿠키")
	AuthTokenCookies cookies,
	@Schema(description = "사용자 상태", example = "ACTIVE")
	UserStatus userStatus
) {
}
