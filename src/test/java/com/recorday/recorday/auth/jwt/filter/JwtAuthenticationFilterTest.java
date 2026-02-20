package com.recorday.recorday.auth.jwt.filter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.recorday.recorday.auth.entity.CustomUserPrincipal;
import com.recorday.recorday.auth.exception.AuthErrorCode;
import com.recorday.recorday.auth.exception.CustomAuthenticationException;
import com.recorday.recorday.auth.jwt.service.JwtTokenService;
import com.recorday.recorday.auth.service.UserPrincipalLoader;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private UserPrincipalLoader userPrincipalLoader;

	@Mock
	private AuthenticationEntryPoint authenticationEntryPoint;

	@Mock
	private FilterChain filterChain;

	@Mock
	private CustomUserPrincipal customUserPrincipal;

	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@BeforeEach
	void setUp() {
		jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenService, userPrincipalLoader, authenticationEntryPoint);
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("shouldNotFilter는 /static 경로에 대해 true를 반환한다")
	void shouldNotFilter_staticPath() throws ServletException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/static/js/app.js");

		// when
		boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("shouldNotFilter는 /favicon.ico 경로에 대해 true를 반환한다")
	void shouldNotFilter_faviconPath() throws ServletException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/favicon.ico");

		// when
		boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("shouldNotFilter는 일반 API 경로에 대해 false를 반환한다")
	void shouldNotFilter_apiPath_returnsFalse() throws ServletException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

		// when
		boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("accessToken 쿠키가 없으면 인증 없이 필터 체인을 통과한다")
	void doFilter_noCookie_passesThrough() throws ServletException, IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
		MockHttpServletResponse response = new MockHttpServletResponse();

		// when
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// then
		then(jwtTokenService).shouldHaveNoInteractions();
		then(userPrincipalLoader).shouldHaveNoInteractions();
		then(filterChain).should().doFilter(request, response);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	@DisplayName("유효하지 않은 토큰이 쿠키에 있으면 AuthenticationEntryPoint가 호출된다")
	void doFilter_invalidTokenCookie_invokesEntryPoint() throws ServletException, IOException {
		// given
		String invalidToken = "invalid-token";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
		request.setCookies(new Cookie("accessToken", invalidToken));
		MockHttpServletResponse response = new MockHttpServletResponse();

		willThrow(new CustomAuthenticationException(AuthErrorCode.INVALID_TOKEN))
			.given(jwtTokenService).validateToken(invalidToken);

		// when
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// then
		then(jwtTokenService).should().validateToken(invalidToken);
		then(jwtTokenService).should(never()).getUserPublicId(anyString());
		then(userPrincipalLoader).shouldHaveNoInteractions();
		then(filterChain).should(never()).doFilter(request, response);
		then(authenticationEntryPoint).should().commence(eq(request), eq(response), any(CustomAuthenticationException.class));
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	@DisplayName("유효한 토큰 쿠키가 있고 기존 인증 정보가 없으면 SecurityContext에 인증을 설정한다")
	void doFilter_validTokenCookie_setsAuthentication() throws ServletException, IOException {
		// given
		String validToken = "valid-token";
		String publicId = "public-id";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
		request.setCookies(new Cookie("accessToken", validToken));
		MockHttpServletResponse response = new MockHttpServletResponse();

		given(jwtTokenService.getUserPublicId(validToken)).willReturn(publicId);
		given(userPrincipalLoader.loadUserByPublicId(publicId)).willReturn(customUserPrincipal);
		given(customUserPrincipal.getAuthorities()).willReturn(Collections.emptyList());

		// when
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// then
		then(jwtTokenService).should().validateToken(validToken);
		then(jwtTokenService).should().getUserPublicId(validToken);
		then(userPrincipalLoader).should().loadUserByPublicId(publicId);
		then(filterChain).should().doFilter(request, response);

		var authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
		assertThat(authentication.getPrincipal()).isEqualTo(customUserPrincipal);
		assertThat(authentication.getAuthorities()).isEmpty();
	}

	@Test
	@DisplayName("유효한 토큰 쿠키가 있지만 기존 인증 정보가 존재하면 덮어쓰지 않는다")
	void doFilter_validTokenCookie_existingAuth_doesNotOverride() throws ServletException, IOException {
		// given
		String validToken = "valid-token";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
		request.setCookies(new Cookie("accessToken", validToken));
		MockHttpServletResponse response = new MockHttpServletResponse();

		UsernamePasswordAuthenticationToken existingAuth =
			new UsernamePasswordAuthenticationToken("existingUser", null, Collections.emptyList());
		SecurityContextHolder.getContext().setAuthentication(existingAuth);

		// when
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// then
		then(jwtTokenService).should().validateToken(validToken);
		then(jwtTokenService).should(never()).getUserPublicId(anyString());
		then(userPrincipalLoader).shouldHaveNoInteractions();
		then(filterChain).should().doFilter(request, response);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existingAuth);
	}
}
