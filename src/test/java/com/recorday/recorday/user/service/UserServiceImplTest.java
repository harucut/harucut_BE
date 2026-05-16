package com.recorday.recorday.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.storage.service.FileStorageService;
import com.recorday.recorday.subscription.entity.UserSubscription;
import com.recorday.recorday.user.config.PlanPricingProperties;
import com.recorday.recorday.user.dto.response.SubscriptionUsageResponse;
import com.recorday.recorday.user.dto.response.UserInfoResponse;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.PlanTier;
import com.recorday.recorday.user.enums.UserRole;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.util.user.UserReader;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private UserReader userReader;

	@Mock
	private FileStorageService fileStorageService;

	@Mock
	private PlanPricingProperties planPricingProperties;

	@InjectMocks
	private UserServiceImpl userService;

	@Test
	@DisplayName("사용자 정보 조회 성공 시 presignedUrl 포함 응답 반환")
	void getUserInfo_사용자_정보_조회_성공() {
		// given
		Long userId = 1L;
		String profileUrl = "profile/user-1/profile.jpg";
		String presignedUrl = "https://bucket.s3.amazonaws.com/profile/user-1/profile.jpg?signed=xxx";
		User user = createUser("test@example.com", profileUrl);

		given(userReader.getUserById(userId)).willReturn(user);
		given(fileStorageService.generatePresignedGetUrl(profileUrl)).willReturn(presignedUrl);
		given(planPricingProperties.getPrice(PlanTier.BASIC)).willReturn(0);

		// when
		UserInfoResponse response = userService.getUserInfo(userId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.id()).isEqualTo(userId);
		assertThat(response.email()).isEqualTo(user.getEmail());
		assertThat(response.username()).isEqualTo(user.getUsername());
		assertThat(response.profileUrl()).isEqualTo(presignedUrl);
		assertThat(response.planTier()).isEqualTo(PlanTier.BASIC.name());
		assertThat(response.monthlyPrice()).isEqualTo(0);

		then(userReader).should(times(1)).getUserById(userId);
		then(fileStorageService).should(times(1)).generatePresignedGetUrl(profileUrl);
		then(planPricingProperties).should(times(1)).getPrice(PlanTier.BASIC);
	}

	@Test
	@DisplayName("구독 사용량 조회 시 프레임 생성/영상 다운로드 사용량 및 남은 가능 횟수를 반환한다")
	void getSubscriptionUsage_성공() {
		// given
		Long userId = 1L;
		User user = createUser("test@example.com", "profile/user-1/profile.jpg");
		UserSubscription subscription = user.getSubscription();

		LocalDateTime now = LocalDateTime.now();
		subscription.startNewQuotaCycle(now.minusDays(1));
		subscription.increaseFrameCreateCount();
		subscription.increaseFrameCreateCount();
		subscription.increaseVideoDownloadCount();

		given(userReader.getUserById(userId)).willReturn(user);

		// when
		SubscriptionUsageResponse response = userService.getSubscriptionUsage(userId);

		// then
		assertThat(response.planTier()).isEqualTo(PlanTier.BASIC.name());
		assertThat(response.frameCreateTotalLimit()).isEqualTo(PlanTier.BASIC.getTotalFrameCreateLimit());
		assertThat(response.frameCreateUsedCount()).isEqualTo(2);
		assertThat(response.frameCreateRemainingCount()).isEqualTo(0);
		assertThat(response.frameCreateUnlimited()).isFalse();

		assertThat(response.videoDownloadMonthlyLimit()).isEqualTo(PlanTier.BASIC.getMonthlyVideoDownloadLimit());
		assertThat(response.videoDownloadUsedCount()).isEqualTo(1);
		assertThat(response.videoDownloadRemainingCount()).isEqualTo(0);
		assertThat(response.videoDownloadUnlimited()).isFalse();
		assertThat(response.currentCycleStartAt()).isNotNull();
		assertThat(response.currentCycleEndAt()).isNotNull();

		then(userReader).should(times(1)).getUserById(userId);
	}

	@Test
	@DisplayName("프로필 이미지 변경 성공")
	void changeProfileImage_프로필_이미지_변경_성공() {
		// given
		Long userId = 1L;
		String newS3Key = "profile/user-1/new-profile.jpg";
		User user = createUser("test@example.com", "profile/user-1/old-profile.jpg");

		given(userReader.getUserById(userId)).willReturn(user);

		// when
		userService.changeProfileImage(userId, newS3Key);

		// then
		assertThat(user.getProfileUrl()).isEqualTo(newS3Key);

		then(userReader).should(times(1)).getUserById(userId);
	}

	@Test
	@DisplayName("사용자명 변경 성공")
	void changeUsername_사용자명_변경_성공() {
		// given
		Long userId = 1L;
		String newUsername = "newUsername";
		User user = createUser("test@example.com", "profile/user-1/profile.jpg");

		given(userReader.getUserById(userId)).willReturn(user);

		// when
		userService.changeUsername(userId, newUsername);

		// then
		assertThat(user.getUsername()).isEqualTo(newUsername);

		then(userReader).should(times(1)).getUserById(userId);
	}

	private User createUser(String email, String profileUrl) {
		User user = User.builder()
			.id(1L)
			.publicId("user-public-id-123")
			.email(email)
			.username("testUser")
			.password("encoded-password")
			.profileUrl(profileUrl)
			.provider(Provider.HARUCUT)
			.userRole(UserRole.ROLE_USER)
			.userStatus(UserStatus.ACTIVE)
			.build();
		user.attachSubscription(UserSubscription.builder()
			.user(user)
			.planTier(PlanTier.BASIC)
			.build());
		return user;
	}
}
