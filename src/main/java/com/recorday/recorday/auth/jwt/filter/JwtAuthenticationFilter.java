package com.recorday.recorday.auth.jwt.filter;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.recorday.recorday.auth.entity.CustomUserPrincipal;
import com.recorday.recorday.auth.exception.AuthErrorCode;
import com.recorday.recorday.auth.exception.CustomAuthenticationException;
import com.recorday.recorday.auth.jwt.dto.AuthTokenCookies;
import com.recorday.recorday.auth.jwt.service.JwtTokenService;
import com.recorday.recorday.auth.jwt.service.RefreshTokenService;
import com.recorday.recorday.auth.service.UserPrincipalLoader;
import com.recorday.recorday.exception.BusinessException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
	private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

	private final JwtTokenService jwtTokenService;
	private final RefreshTokenService refreshTokenService;
	private final UserPrincipalLoader userPrincipalLoader;
	private final AuthenticationEntryPoint authenticationEntryPoint;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		log.info("JWT 필터 접근 URI: {}", request.getRequestURI());

		String accessToken = resolveTokenFromCookie(request, ACCESS_TOKEN_COOKIE_NAME);

		try {
			if (accessToken != null) {
				try {
					authenticateWithAccessToken(request, accessToken);
					filterChain.doFilter(request, response);
					return;
				} catch (CustomAuthenticationException accessTokenException) {
					// accessToken이 만료/무효라면 refreshToken으로 1회 자동 복구를 시도한다.
					log.info("AccessToken 검증 실패. RefreshToken 재발급 경로를 시도합니다. uri={}", request.getRequestURI());
					if (tryAuthenticateByRefreshToken(request, response)) {
						log.info("RefreshToken 기반 자동 재발급/인증 성공. uri={}", request.getRequestURI());
						filterChain.doFilter(request, response);
						return;
					}
					throw accessTokenException;
				}
			}

			// accessToken이 없어도 refreshToken이 있으면 자동 재발급 후 요청을 계속 처리한다.
			if (tryAuthenticateByRefreshToken(request, response)) {
				log.info("AccessToken 없음. RefreshToken 기반 자동 재발급/인증 성공. uri={}", request.getRequestURI());
				filterChain.doFilter(request, response);
				return;
			}
		} catch (CustomAuthenticationException ex) {
			handleAuthFailure(request, response, ex);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private void authenticateWithAccessToken(HttpServletRequest request, String accessToken) {
		jwtTokenService.validateToken(accessToken);

		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			String publicId = jwtTokenService.getUserPublicId(accessToken);
			CustomUserPrincipal principal = userPrincipalLoader.loadUserByPublicId(publicId);
			setAuthentication(request, principal);
		}
	}

	private boolean tryAuthenticateByRefreshToken(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = resolveTokenFromCookie(request, REFRESH_TOKEN_COOKIE_NAME);
		if (refreshToken == null) {
			return false;
		}

		try {
			AuthTokenCookies reissuedCookies = refreshTokenService.reissue(refreshToken);
			response.addHeader(HttpHeaders.SET_COOKIE, reissuedCookies.accessTokenCookie().toString());
			response.addHeader(HttpHeaders.SET_COOKIE, reissuedCookies.refreshTokenCookie().toString());
			authenticateWithAccessToken(request, reissuedCookies.accessTokenCookie().getValue());
			return true;
		} catch (BusinessException ex) {
			if (ex.getErrorCode() instanceof AuthErrorCode authErrorCode) {
				throw new CustomAuthenticationException(authErrorCode);
			}
			throw new CustomAuthenticationException(AuthErrorCode.INVALID_CREDENTIALS);
		}
	}

	private void setAuthentication(HttpServletRequest request, CustomUserPrincipal principal) {
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(
				principal,
				null,
				principal.getAuthorities()
			);

		authentication.setDetails(
			new WebAuthenticationDetailsSource().buildDetails(request)
		);

		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private void handleAuthFailure(HttpServletRequest request, HttpServletResponse response, CustomAuthenticationException ex)
		throws IOException, ServletException {
		// 검증 실패(만료, 위조 등) or 사용자 조회 실패 시 공통 처리
		log.error("JWT 인증 실패: {}", ex.getMessage());
		SecurityContextHolder.clearContext();
		authenticationEntryPoint.commence(request, response, ex);
	}

	private String resolveTokenFromCookie(HttpServletRequest request, String cookieName) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}

		return Arrays.stream(cookies)
			.filter(cookie -> cookieName.equals(cookie.getName()))
			.map(Cookie::getValue)
			.filter(StringUtils::hasText)
			.reduce((first, second) -> second)
			.orElse(null);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		String path = request.getRequestURI();
		return path.startsWith("/static")
			|| path.startsWith("/favicon.ico")
			|| "/api/harucut/reissue".equals(path)
			|| "/api/harucut/logout".equals(path);
	}
}
