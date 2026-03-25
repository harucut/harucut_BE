package com.recorday.recorday.auth.jwt.dto;

import org.springframework.http.ResponseCookie;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 토큰 쿠키 묶음 DTO")
public record AuthTokenCookies(
	@Schema(description = "Access Token 쿠키")
	ResponseCookie accessTokenCookie,
	@Schema(description = "Refresh Token 쿠키")
	ResponseCookie refreshTokenCookie
) {
}
