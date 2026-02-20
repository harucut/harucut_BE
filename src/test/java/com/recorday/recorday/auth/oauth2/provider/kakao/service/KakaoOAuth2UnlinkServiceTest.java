package com.recorday.recorday.auth.oauth2.provider.kakao.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.recorday.recorday.auth.exception.AuthErrorCode;
import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.auth.oauth2.provider.kakao.adaptor.KakaoAuthProperties;
import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserRole;
import com.recorday.recorday.user.enums.UserStatus;

@ExtendWith(MockitoExtension.class)
class KakaoOAuth2UnlinkServiceTest {

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private KakaoAuthProperties kakaoProperties;

	@InjectMocks
	private KakaoOAuth2UnlinkService kakaoOAuth2UnlinkService;

	@Test
	@DisplayName("KAKAO 프로바이더 지원 시 true 반환")
	void supports_KAKAO_프로바이더_지원_true() {
		// given
		Provider provider = Provider.KAKAO;

		// when
		boolean result = kakaoOAuth2UnlinkService.supports(provider);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("다른 프로바이더(NAVER) 지원 시 false 반환")
	void supports_다른_프로바이더_지원_false() {
		// given
		Provider provider = Provider.NAVER;

		// when
		boolean result = kakaoOAuth2UnlinkService.supports(provider);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("카카오 연결끊기 API 호출 성공")
	void unlink_카카오_연결끊기_API_호출_성공() {
		// given
		User kakaoUser = createKakaoUser();
		String adminKey = "test-admin-key";
		String unlinkUrl = "https://kapi.kakao.com/v1/user/unlink";
		String contentType = "application/x-www-form-urlencoded";
		String targetIdType = "user_id";

		given(kakaoProperties.getAdminKey()).willReturn(adminKey);
		given(kakaoProperties.getUnlinkContentType()).willReturn(contentType);
		given(kakaoProperties.getUnlinkTargetIdType()).willReturn(targetIdType);
		given(kakaoProperties.getUnlinkUrl()).willReturn(unlinkUrl);
		given(restTemplate.postForEntity(eq(unlinkUrl), any(HttpEntity.class), eq(String.class)))
			.willReturn(ResponseEntity.ok("success"));

		// when
		kakaoOAuth2UnlinkService.unlink(kakaoUser);

		// then
		then(restTemplate).should(times(1)).postForEntity(eq(unlinkUrl), any(HttpEntity.class), eq(String.class));
	}

	@Test
	@DisplayName("카카오가 아닌 유저는 API 호출 없이 종료")
	void unlink_카카오가_아닌_유저_처리안함() {
		// given
		User naverUser = createNaverUser();

		// when
		kakaoOAuth2UnlinkService.unlink(naverUser);

		// then
		then(restTemplate).should(never()).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
	}

	@Test
	@DisplayName("API 호출 실패 시 OAUTH2_PROVIDER_UNLINK_ERROR 예외 발생")
	void unlink_API_호출_실패시_예외발생() {
		// given
		User kakaoUser = createKakaoUser();
		String adminKey = "test-admin-key";
		String unlinkUrl = "https://kapi.kakao.com/v1/user/unlink";
		String contentType = "application/x-www-form-urlencoded";
		String targetIdType = "user_id";

		given(kakaoProperties.getAdminKey()).willReturn(adminKey);
		given(kakaoProperties.getUnlinkContentType()).willReturn(contentType);
		given(kakaoProperties.getUnlinkTargetIdType()).willReturn(targetIdType);
		given(kakaoProperties.getUnlinkUrl()).willReturn(unlinkUrl);
		given(restTemplate.postForEntity(eq(unlinkUrl), any(HttpEntity.class), eq(String.class)))
			.willThrow(new RestClientException("API call failed"));

		// when & then
		assertThatThrownBy(() -> kakaoOAuth2UnlinkService.unlink(kakaoUser))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException businessException = (BusinessException) exception;
				assertThat(businessException.getErrorCode()).isEqualTo(AuthErrorCode.OAUTH2_PROVIDER_UNLINK_ERROR);
			});

		then(restTemplate).should(times(1)).postForEntity(eq(unlinkUrl), any(HttpEntity.class), eq(String.class));
	}

	private User createKakaoUser() {
		return User.builder()
			.id(1L)
			.publicId("kakao-user-public-id")
			.email("kakao@example.com")
			.username("kakaoUser")
			.profileUrl("http://profile.url")
			.provider(Provider.KAKAO)
			.providerId("kakao-provider-id-12345")
			.userRole(UserRole.ROLE_USER)
			.userStatus(UserStatus.ACTIVE)
			.build();
	}

	private User createNaverUser() {
		return User.builder()
			.id(2L)
			.publicId("naver-user-public-id")
			.email("naver@example.com")
			.username("naverUser")
			.profileUrl("http://profile.url")
			.provider(Provider.NAVER)
			.providerId("naver-provider-id-12345")
			.userRole(UserRole.ROLE_USER)
			.userStatus(UserStatus.ACTIVE)
			.build();
	}
}
