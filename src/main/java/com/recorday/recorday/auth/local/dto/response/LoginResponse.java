package com.recorday.recorday.auth.local.dto.response;

import com.recorday.recorday.user.enums.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답 DTO")
public record LoginResponse(
	@Schema(description = "사용자 상태", example = "ACTIVE")
	UserStatus userStatus
) {
}
