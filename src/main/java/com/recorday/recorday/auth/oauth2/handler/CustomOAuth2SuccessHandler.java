package com.recorday.recorday.auth.oauth2.handler;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.recorday.recorday.auth.entity.CustomOAuth2User;
import com.recorday.recorday.auth.jwt.service.JwtTokenService;
import com.recorday.recorday.auth.jwt.service.RefreshTokenService;
import com.recorday.recorday.auth.oauth2.service.AuthCodeService;
import com.recorday.recorday.auth.utils.CookieUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	@Value("${redirect-url.frontend}")
	private String REDIRECT_URL;

	private final JwtTokenService jwtTokenService;
	private final RefreshTokenService refreshTokenService;
	private final CookieUtil cookieUtil;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {

		CustomOAuth2User principal = (CustomOAuth2User)authentication.getPrincipal();
		String publicId = principal.getPublicId();

		String accessToken = jwtTokenService.createAccessToken(publicId);
		String refreshToken = jwtTokenService.createRefreshToken(publicId);

		refreshTokenService.saveRefreshToken(publicId, refreshToken);

		ResponseCookie accessCookie = cookieUtil.createTokenCookie(
			"accessToken",
			accessToken,
			jwtTokenService.getAccessTokenValidityMillis()
		);

		ResponseCookie refreshCookie = cookieUtil.createTokenCookie(
			"refreshToken",
			refreshToken,
			jwtTokenService.getRefreshTokenValidityMillis()
		);

		response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

		getRedirectStrategy().sendRedirect(request, response, REDIRECT_URL + "/home");
	}
}
