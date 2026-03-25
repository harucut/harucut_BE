package com.recorday.recorday.auth.dto.response;

import com.recorday.recorday.user.enums.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 상태 응답 DTO")
public record AuthStatusResponse(
	@Schema(description = "사용자 상태", example = "ACTIVE")
	UserStatus userStatus
) {
}
