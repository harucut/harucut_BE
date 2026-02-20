package com.recorday.recorday.auth.oauth2.provider.naver.service;

import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.auth.oauth2.provider.naver.component.NaverDecryptUniqueId;
import com.recorday.recorday.auth.oauth2.provider.naver.dto.NaverUnlinkRequest;
import com.recorday.recorday.auth.service.UserExitService;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserRole;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.util.user.UserReader;

@ExtendWith(MockitoExtension.class)
class NaverOAuth2UnlinkServiceTest {

	@Mock
	private NaverDecryptUniqueId decryptUniqueId;

	@Mock
	private UserReader userReader;

	@Mock
	private UserExitService userExitService;

	@InjectMocks
	private NaverOAuth2UnlinkService naverOAuth2UnlinkService;

	@Test
	@DisplayName("네이버 연결끊기 처리 성공")
	void unlink_네이버_연결끊기_처리_성공() throws Exception {
		// given
		String clientId = "naver-client-id";
		String encryptUniqueId = "encrypted-unique-id";
		String timestamp = "1234567890";
		String signature = "valid-signature";
		String decryptedProviderId = "naver-provider-id-12345";

		NaverUnlinkRequest request = new NaverUnlinkRequest(clientId, encryptUniqueId, timestamp, signature);
		User naverUser = createNaverUser(decryptedProviderId);

		given(decryptUniqueId.handleUnlinkNotification(request)).willReturn(decryptedProviderId);
		given(userReader.getUserByProviderAndProviderId(Provider.NAVER, decryptedProviderId)).willReturn(naverUser);
		willDoNothing().given(userExitService).requestExit(naverUser.getId());

		// when
		naverOAuth2UnlinkService.unlink(request);

		// then
		then(decryptUniqueId).should(times(1)).handleUnlinkNotification(request);
		then(userReader).should(times(1)).getUserByProviderAndProviderId(Provider.NAVER, decryptedProviderId);
		then(userExitService).should(times(1)).requestExit(naverUser.getId());
	}

	private User createNaverUser(String providerId) {
		return User.builder()
			.id(1L)
			.publicId("naver-user-public-id")
			.email("naver@example.com")
			.username("naverUser")
			.profileUrl("http://profile.url")
			.provider(Provider.NAVER)
			.providerId(providerId)
			.userRole(UserRole.ROLE_USER)
			.userStatus(UserStatus.ACTIVE)
			.build();
	}
}
