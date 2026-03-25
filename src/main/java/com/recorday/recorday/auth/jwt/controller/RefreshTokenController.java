package com.recorday.recorday.auth.jwt.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recorday.recorday.auth.entity.CustomUserPrincipal;
import com.recorday.recorday.auth.jwt.dto.AuthTokenCookies;
import com.recorday.recorday.auth.jwt.service.JwtTokenService;
import com.recorday.recorday.auth.jwt.service.RefreshTokenService;
import com.recorday.recorday.auth.utils.CookieUtil;
import com.recorday.recorday.util.response.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth (Token)", description = "JWT 토큰 재발급 및 로그아웃 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RefreshTokenController {

	private final RefreshTokenService refreshTokenService;
	private final JwtTokenService jwtTokenService;
	private final CookieUtil cookieUtil;

	@Operation(summary = "토큰 재발급", description = "쿠키에 포함된 Refresh Token을 검증하여 새로운 Access Token과 Refresh Token을 쿠키로 발급합니다.")
	@PostMapping("/harucut/reissue")
	public ResponseEntity<Response<Void>> reissue(
		@Parameter(description = "리프레시 토큰 쿠키", required = true)
		@CookieValue("refreshToken") String refreshToken
	) {
		AuthTokenCookies tokens = refreshTokenService.reissue(refreshToken);
		return Response.ok().toResponseEntity(tokens);
	}

	@Operation(summary = "로그아웃", description = "서버(Redis)에 저장된 해당 사용자의 Refresh Token을 삭제하고 브라우저 쿠키를 만료시킵니다.")
	@DeleteMapping("/harucut/logout")
	public ResponseEntity<Response<Void>> logout(
		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserPrincipal principal,
		@CookieValue(value = "refreshToken", required = false) String refreshToken
	) {
		String publicId = resolvePublicId(principal, refreshToken);
		if (StringUtils.hasText(publicId)) {
			refreshTokenService.logout(publicId);
		}

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookieUtil.createExpiredCookie("accessToken").toString())
			.header(HttpHeaders.SET_COOKIE, cookieUtil.createExpiredCookie("refreshToken").toString())
			.body(Response.ok());
	}

	private String resolvePublicId(CustomUserPrincipal principal, String refreshToken) {
		if (principal != null) {
			return principal.getPublicId();
		}

		if (!StringUtils.hasText(refreshToken)) {
			return null;
		}

		try {
			jwtTokenService.validateToken(refreshToken);
			if (!"REFRESH".equals(jwtTokenService.getTokenType(refreshToken))) {
				return null;
			}

			return jwtTokenService.getUserPublicId(refreshToken);
		} catch (Exception e) {
			return null;
		}
	}
}
