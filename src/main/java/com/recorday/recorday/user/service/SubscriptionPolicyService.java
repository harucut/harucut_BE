package com.recorday.recorday.user.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.subscription.entity.UserSubscription;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.PlanTier;
import com.recorday.recorday.user.exception.UserErrorCode;

@Service
public class SubscriptionPolicyService {

	public void assertAndConsumeVideoDownloadQuota(User user) {
		UserSubscription subscription = resolveSubscription(user);
		subscription.syncQuotaCycle(LocalDateTime.now());

		PlanTier planTier = subscription.getPlanTier();
		if (planTier.isVideoDownloadUnlimited()) {
			return;
		}

		if (subscription.getCurrentVideoDownloadCount() >= planTier.getMonthlyVideoDownloadLimit()) {
			throw new BusinessException(UserErrorCode.PLAN_VIDEO_DOWNLOAD_LIMIT_EXCEEDED);
		}

		subscription.increaseVideoDownloadCount();
	}

	public void assertAndConsumeFrameCreateQuota(User user) {
		UserSubscription subscription = resolveSubscription(user);

		PlanTier planTier = subscription.getPlanTier();
		if (planTier.isFrameCreateUnlimited()) {
			return;
		}

		if (subscription.getTotalFrameCreateCount() >= planTier.getTotalFrameCreateLimit()) {
			throw new BusinessException(UserErrorCode.PLAN_FRAME_CREATE_LIMIT_EXCEEDED);
		}

		subscription.increaseTotalFrameCreateCount();
	}

	public LocalDateTime resolveHistoryCutoff(User user) {
		UserSubscription subscription = resolveSubscription(user);
		PlanTier planTier = subscription.getPlanTier();
		if (planTier.isHistoryUnlimited()) {
			return null;
		}

		return LocalDateTime.now().minusDays(planTier.getHistoryRetentionDays());
	}

	public void assertHistoryAccessible(User user, LocalDateTime createdAt) {
		LocalDateTime cutoff = resolveHistoryCutoff(user);
		if (cutoff == null) {
			return;
		}

		if (createdAt != null && !createdAt.isBefore(cutoff)) {
			return;
		}

		throw new BusinessException(UserErrorCode.PLAN_HISTORY_RETENTION_EXCEEDED);
	}

	private UserSubscription resolveSubscription(User user) {
		UserSubscription subscription = user.getSubscription();
		if (subscription != null) {
			return subscription;
		}

		return UserSubscription.createDefault(user);
	}
}
