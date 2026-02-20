package com.recorday.recorday.auth.oauth2.handler;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import com.recorday.recorday.auth.entity.CustomOAuth2User;
import com.recorday.recorday.auth.jwt.service.JwtTokenService;
import com.recorday.recorday.auth.jwt.service.RefreshTokenService;
import com.recorday.recorday.auth.utils.CookieUtil;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2SuccessHandlerTest {

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private RefreshTokenService refreshTokenService;

	@Mock
	private CookieUtil cookieUtil;

	@Mock
	private CustomOAuth2User customOAuth2User;

	@Mock
	private Authentication authentication;

	@InjectMocks
	private CustomOAuth2SuccessHandler successHandler;

	@Test
	@DisplayName("OAuth2 로그인 성공 시 토큰 쿠키를 설정하고 프론트엔드 홈으로 리다이렉트한다")
	void onAuthenticationSuccess_setsTokenCookiesAndRedirectsToHome() throws Exception {
		// given
		String publicId = "user-public-id-123";
		String accessToken = "test-access-token";
		String refreshToken = "test-refresh-token";
		String redirectBaseUrl = "http://localhost:3000";
		long accessTokenValidity = 3600000L;
		long refreshTokenValidity = 86400000L;

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		ReflectionTestUtils.setField(successHandler, "REDIRECT_URL", redirectBaseUrl);

		given(authentication.getPrincipal()).willReturn(customOAuth2User);
		given(customOAuth2User.getPublicId()).willReturn(publicId);
		given(jwtTokenService.createAccessToken(publicId)).willReturn(accessToken);
		given(jwtTokenService.createRefreshToken(publicId)).willReturn(refreshToken);
		given(jwtTokenService.getAccessTokenValidityMillis()).willReturn(accessTokenValidity);
		given(jwtTokenService.getRefreshTokenValidityMillis()).willReturn(refreshTokenValidity);

		ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
			.httpOnly(true).secure(true).path("/")
			.maxAge(Duration.ofMillis(accessTokenValidity))
			.sameSite("Lax").domain("harucut.com").build();

		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true).secure(true).path("/")
			.maxAge(Duration.ofMillis(refreshTokenValidity))
			.sameSite("Lax").domain("harucut.com").build();

		given(cookieUtil.createTokenCookie("accessToken", accessToken, accessTokenValidity)).willReturn(accessCookie);
		given(cookieUtil.createTokenCookie("refreshToken", refreshToken, refreshTokenValidity)).willReturn(refreshCookie);
		willDoNothing().given(refreshTokenService).saveRefreshToken(publicId, refreshToken);

		// when
		successHandler.onAuthenticationSuccess(request, response, authentication);

		// then
		then(jwtTokenService).should().createAccessToken(publicId);
		then(jwtTokenService).should().createRefreshToken(publicId);
		then(refreshTokenService).should().saveRefreshToken(publicId, refreshToken);
		then(cookieUtil).should().createTokenCookie("accessToken", accessToken, accessTokenValidity);
		then(cookieUtil).should().createTokenCookie("refreshToken", refreshToken, refreshTokenValidity);

		assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("accessToken");
		assertThat(response.getRedirectedUrl()).isEqualTo(redirectBaseUrl + "/home");
	}
}
