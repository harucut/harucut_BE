package com.recorday.recorday.auth.oauth2.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.recorday.recorday.auth.entity.CustomOAuth2User;
import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserRole;
import com.recorday.recorday.user.enums.UserStatus;

@ExtendWith(MockitoExtension.class)
class CustomOidcUserServiceTest {

	@Mock
	private SocialLoginService socialLoginService;

	@Test
	@DisplayName("OIDC 사용자 처리 시 socialLoginService.processUser가 올바르게 호출된다")
	void processUser_OIDC_사용자_처리_성공() {
		// given
		ClientRegistration clientRegistration = createClientRegistration("google");

		Map<String, Object> claims = Map.of(
			"sub", "google-sub-12345",
			"email", "google@example.com",
			"name", "Google User"
		);

		OidcIdToken idToken = new OidcIdToken(
			"test-id-token",
			Instant.now(),
			Instant.now().plusSeconds(3600),
			claims
		);

		OAuth2User oidcUser = new DefaultOidcUser(
			Collections.emptyList(),
			idToken
		);

		User user = createUser();
		CustomOAuth2User expectedOAuth2User = new CustomOAuth2User(
			user, claims, "sub", Provider.GOOGLE, idToken, null
		);

		given(socialLoginService.processUser(clientRegistration, oidcUser))
			.willReturn(expectedOAuth2User);

		// when
		CustomOAuth2User result = socialLoginService.processUser(clientRegistration, oidcUser);

		// then
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(expectedOAuth2User);
		assertThat(result.getProvider()).isEqualTo(Provider.GOOGLE);
		assertThat(result.getIdToken()).isEqualTo(idToken);

		then(socialLoginService).should(times(1)).processUser(clientRegistration, oidcUser);
	}

	private ClientRegistration createClientRegistration(String registrationId) {
		return ClientRegistration.withRegistrationId(registrationId)
			.clientId("test-client-id")
			.clientSecret("test-client-secret")
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
			.scope("openid", "profile", "email")
			.authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
			.tokenUri("https://www.googleapis.com/oauth2/v4/token")
			.userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
			.jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
			.userNameAttributeName("sub")
			.clientName("Google")
			.build();
	}

	private User createUser() {
		return User.builder()
			.id(1L)
			.publicId("oidc-user-public-id")
			.email("google@example.com")
			.username("googleUser")
			.profileUrl("http://profile.url")
			.provider(Provider.GOOGLE)
			.providerId("google-sub-12345")
			.userRole(UserRole.ROLE_USER)
			.userStatus(UserStatus.ACTIVE)
			.build();
	}
}
