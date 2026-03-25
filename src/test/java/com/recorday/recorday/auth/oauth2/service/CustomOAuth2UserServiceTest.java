package com.recorday.recorday.auth.oauth2.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.recorday.recorday.auth.entity.CustomOAuth2User;
import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserRole;
import com.recorday.recorday.user.enums.UserStatus;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

	@Mock
	private SocialLoginService socialLoginService;

	@Test
	@DisplayName("OAuth2 사용자 처리 시 socialLoginService.processUser가 올바르게 호출된다")
	void processUser_OAuth2_사용자_처리_성공() {
		// given
		ClientRegistration clientRegistration = createClientRegistration("kakao");

		Map<String, Object> attributes = Map.of(
			"id", "12345",
			"kakao_account", Map.of("email", "kakao@example.com")
		);

		OAuth2User oAuth2User = new DefaultOAuth2User(
			java.util.Collections.emptyList(),
			attributes,
			"id"
		);

		User user = createUser();
		CustomOAuth2User expectedOAuth2User = new CustomOAuth2User(
			user, attributes, "id", Provider.KAKAO
		);

		given(socialLoginService.processUser(clientRegistration, oAuth2User))
			.willReturn(expectedOAuth2User);

		// when
		CustomOAuth2User result = socialLoginService.processUser(clientRegistration, oAuth2User);

		// then
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(expectedOAuth2User);
		assertThat(result.getProvider()).isEqualTo(Provider.KAKAO);

		then(socialLoginService).should(times(1)).processUser(clientRegistration, oAuth2User);
	}

	private ClientRegistration createClientRegistration(String registrationId) {
		return ClientRegistration.withRegistrationId(registrationId)
			.clientId("test-client-id")
			.clientSecret("test-client-secret")
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
			.authorizationUri("https://kauth.kakao.com/oauth/authorize")
			.tokenUri("https://kauth.kakao.com/oauth/token")
			.userInfoUri("https://kapi.kakao.com/v2/user/me")
			.userNameAttributeName("id")
			.clientName("Kakao")
			.build();
	}

	private User createUser() {
		return User.builder()
			.id(1L)
			.publicId("oauth2-user-public-id")
			.email("kakao@example.com")
			.username("kakaoUser")
			.profileUrl("http://profile.url")
			.provider(Provider.KAKAO)
			.providerId("12345")
			.userRole(UserRole.ROLE_USER)
			.userStatus(UserStatus.ACTIVE)
			.build();
	}
}
