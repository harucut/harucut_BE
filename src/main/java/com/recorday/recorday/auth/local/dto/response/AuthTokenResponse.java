package com.recorday.recorday.auth.local.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 토큰 응답 DTO")
public record AuthTokenResponse(
	@Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsIn...")
	String accessToken,
	@Schema(description = "JWT 리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsIn...")
	String refreshToken
) {}
