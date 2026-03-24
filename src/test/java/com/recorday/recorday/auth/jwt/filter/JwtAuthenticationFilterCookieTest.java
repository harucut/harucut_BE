package com.recorday.recorday.auth.jwt.filter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.http.ResponseCookie;

import com.recorday.recorday.auth.entity.CustomUserPrincipal;
import com.recorday.recorday.auth.jwt.dto.AuthTokenCookies;
import com.recorday.recorday.auth.jwt.service.JwtTokenService;
import com.recorday.recorday.auth.jwt.service.RefreshTokenService;
import com.recorday.recorday.auth.service.UserPrincipalLoader;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterCookieTest {

	@InjectMocks
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Mock
	private JwtTokenService jwtTokenService;
	@Mock
	private RefreshTokenService refreshTokenService;
	@Mock
	private UserPrincipalLoader userPrincipalLoader;
	@Mock
	private AuthenticationEntryPoint authenticationEntryPoint;

	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse response;
	@Mock
	private FilterChain filterChain;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("쿠키에 accessToken이 있고 유효하다면 인증 객체를 생성하고 다음 필터로 진행한다")
	void shouldAuthenticateWhenAccessTokenCookieIsValid() throws ServletException, IOException {
		// Given
		String validToken = "valid.jwt.token";
		String publicId = "user-public-id";
		Cookie accessCookie = new Cookie("accessToken", validToken);

		// request.getCookies()가 accessCookie를 포함한 배열을 반환하도록 설정
		given(request.getRequestURI()).willReturn("/api/some-endpoint");
		given(request.getCookies()).willReturn(new Cookie[] {accessCookie});

		given(jwtTokenService.getUserPublicId(validToken)).willReturn(publicId);

		CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
		given(userPrincipalLoader.loadUserByPublicId(publicId)).willReturn(principal);
		given(principal.getAuthorities()).willReturn(null);

		// When
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// Then
		then(jwtTokenService).should().validateToken(validToken);
		then(filterChain).should().doFilter(request, response);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(principal);
	}

	@Test
	@DisplayName("쿠키가 아예 존재하지 않으면(null) 필터 체인을 그대로 통과한다")
	void shouldPassFilterWhenCookiesAreNull() throws ServletException, IOException {
		// Given
		given(request.getRequestURI()).willReturn("/api/some-endpoint");
		given(request.getCookies()).willReturn(null);

		// When
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// Then
		then(jwtTokenService).should(never()).validateToken(any());
		then(filterChain).should().doFilter(request, response);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	@DisplayName("쿠키 배열은 있지만 accessToken이 없으면 필터 체인을 그대로 통과한다")
	void shouldPassFilterWhenAccessTokenCookieIsMissing() throws ServletException, IOException {
		// Given
		Cookie sessionCookie = new Cookie("sessionId", "session-value");
		given(request.getRequestURI()).willReturn("/api/some-endpoint");
		given(request.getCookies()).willReturn(new Cookie[] {sessionCookie});

		// When
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// Then
		then(jwtTokenService).should(never()).validateToken(any());
		then(refreshTokenService).should(never()).reissue(any());
		then(filterChain).should().doFilter(request, response);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	@DisplayName("accessToken이 없고 refreshToken이 유효하면 자동 재발급 후 인증한다")
	void shouldAutoReissueWhenRefreshTokenExists() throws ServletException, IOException {
		// Given
		String oldRefreshToken = "refresh-old";
		String newAccessToken = "new-access-token";
		String newRefreshToken = "new-refresh-token";
		String publicId = "user-public-id";
		Cookie refreshCookie = new Cookie("refreshToken", oldRefreshToken);

		given(request.getRequestURI()).willReturn("/api/some-endpoint");
		given(request.getCookies()).willReturn(new Cookie[] {refreshCookie});

		AuthTokenCookies reissued = new AuthTokenCookies(
			ResponseCookie.from("accessToken", newAccessToken).build(),
			ResponseCookie.from("refreshToken", newRefreshToken).build()
		);
		given(refreshTokenService.reissue(oldRefreshToken)).willReturn(reissued);
		given(jwtTokenService.getUserPublicId(newAccessToken)).willReturn(publicId);

		CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
		given(userPrincipalLoader.loadUserByPublicId(publicId)).willReturn(principal);
		given(principal.getAuthorities()).willReturn(null);

		// When
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// Then
		then(refreshTokenService).should().reissue(oldRefreshToken);
		then(jwtTokenService).should().validateToken(newAccessToken);
		then(filterChain).should().doFilter(request, response);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
	}
}
