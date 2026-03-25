package com.recorday.recorday.user.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.subscription.entity.UserSubscription;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.PlanTier;
import com.recorday.recorday.user.enums.UserRole;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.user.exception.UserErrorCode;

class SubscriptionPolicyServiceTest {

	private final SubscriptionPolicyService subscriptionPolicyService = new SubscriptionPolicyService();

	@Test
	@DisplayName("BASIC 요금제는 월간 영상 다운로드 1회를 초과하면 예외가 발생한다")
	void assertAndConsumeVideoDownloadQuota_basicLimitExceeded() {
		User user = createUser(PlanTier.BASIC);

		subscriptionPolicyService.assertAndConsumeVideoDownloadQuota(user);

		assertThatThrownBy(() -> subscriptionPolicyService.assertAndConsumeVideoDownloadQuota(user))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException businessException = (BusinessException) exception;
				assertThat(businessException.getErrorCode()).isEqualTo(UserErrorCode.PLAN_VIDEO_DOWNLOAD_LIMIT_EXCEEDED);
			});
	}

	@Test
	@DisplayName("PLUS 요금제는 월간 프레임 생성 10회까지 허용된다")
	void assertAndConsumeFrameCreateQuota_plusLimit() {
		User user = createUser(PlanTier.PLUS);

		for (int i = 0; i < 10; i++) {
			subscriptionPolicyService.assertAndConsumeFrameCreateQuota(user);
		}

		assertThatThrownBy(() -> subscriptionPolicyService.assertAndConsumeFrameCreateQuota(user))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException businessException = (BusinessException) exception;
				assertThat(businessException.getErrorCode()).isEqualTo(UserErrorCode.PLAN_FRAME_CREATE_LIMIT_EXCEEDED);
			});
	}

	@Test
	@DisplayName("PRO 요금제는 영상 다운로드 제한이 없다")
	void assertAndConsumeVideoDownloadQuota_proUnlimited() {
		User user = createUser(PlanTier.PRO);
		UserSubscription subscription = user.getSubscription();

		for (int i = 0; i < 100; i++) {
			subscriptionPolicyService.assertAndConsumeVideoDownloadQuota(user);
		}

		assertThat(subscription.getCurrentVideoDownloadCount()).isEqualTo(0);
	}

	@Test
	@DisplayName("31일 주기가 지난 뒤 첫 요청 시 사용량이 초기화되고 새 주기로 넘어간다")
	void assertAndConsumeVideoDownloadQuota_rolloverBy31DaysCycle() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime pastStart = now.minusDays(40);
		LocalDateTime pastEnd = pastStart.plusDays(31);

		User user = User.builder()
			.id(1L)
			.publicId("user-public-id")
			.provider(Provider.HARUCUT)
			.userRole(UserRole.ROLE_USER)
			.email("plan@test.com")
			.username("plan-user")
			.profileUrl("resources/defaults/userDefaultImage.png")
			.userStatus(UserStatus.ACTIVE)
			.build();
		UserSubscription subscription = UserSubscription.builder()
			.user(user)
			.planTier(PlanTier.BASIC)
			.currentCycleStartAt(pastStart)
			.currentCycleEndAt(pastEnd)
			.currentVideoDownloadCount(1)
			.currentFrameCreateCount(0)
			.build();
		user.attachSubscription(subscription);

		assertThatCode(() -> subscriptionPolicyService.assertAndConsumeVideoDownloadQuota(user))
			.doesNotThrowAnyException();

		assertThat(subscription.getCurrentVideoDownloadCount()).isEqualTo(1);
		assertThat(subscription.getCurrentCycleStartAt()).isEqualTo(pastEnd);
		assertThat(subscription.getCurrentCycleEndAt()).isEqualTo(pastEnd.plusDays(31));
	}

	@Test
	@DisplayName("BASIC 요금제는 7일 초과 기록 접근 시 예외가 발생한다")
	void assertHistoryAccessible_basicExceeded() {
		User user = createUser(PlanTier.BASIC);
		LocalDateTime oldRecord = LocalDateTime.now().minusDays(8);

		assertThatThrownBy(() -> subscriptionPolicyService.assertHistoryAccessible(user, oldRecord))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException businessException = (BusinessException) exception;
				assertThat(businessException.getErrorCode()).isEqualTo(UserErrorCode.PLAN_HISTORY_RETENTION_EXCEEDED);
			});
	}

	@Test
	@DisplayName("PRO 요금제는 오래된 기록도 접근 가능하다")
	void assertHistoryAccessible_proUnlimited() {
		User user = createUser(PlanTier.PRO);
		LocalDateTime oldRecord = LocalDateTime.now().minusYears(5);

		assertThatCode(() -> subscriptionPolicyService.assertHistoryAccessible(user, oldRecord))
			.doesNotThrowAnyException();
	}

	private User createUser(PlanTier tier) {
		User user = User.builder()
			.id(1L)
			.publicId("user-public-id")
			.provider(Provider.HARUCUT)
			.userRole(UserRole.ROLE_USER)
			.email("plan@test.com")
			.username("plan-user")
			.profileUrl("resources/defaults/userDefaultImage.png")
			.userStatus(UserStatus.ACTIVE)
			.build();
		user.attachSubscription(UserSubscription.builder()
			.user(user)
			.planTier(tier)
			.build());
		return user;
	}
}
