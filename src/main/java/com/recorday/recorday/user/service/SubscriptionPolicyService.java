package com.recorday.recorday.user.service;

import java.time.LocalDateTime;
import java.time.YearMonth;

import org.springframework.stereotype.Service;

import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.PlanTier;
import com.recorday.recorday.user.exception.UserErrorCode;

@Service
public class SubscriptionPolicyService {

	public void assertAndConsumeVideoDownloadQuota(User user) {
		user.syncQuotaMonth(YearMonth.now());

		PlanTier planTier = user.getPlanTier();
		if (planTier.isVideoDownloadUnlimited()) {
			return;
		}

		if (user.getMonthlyVideoDownloadCount() >= planTier.getMonthlyVideoDownloadLimit()) {
			throw new BusinessException(UserErrorCode.PLAN_VIDEO_DOWNLOAD_LIMIT_EXCEEDED);
		}

		user.increaseMonthlyVideoDownloadCount();
	}

	public void assertAndConsumeFrameCreateQuota(User user) {
		user.syncQuotaMonth(YearMonth.now());

		PlanTier planTier = user.getPlanTier();
		if (planTier.isFrameCreateUnlimited()) {
			return;
		}

		if (user.getMonthlyFrameCreateCount() >= planTier.getMonthlyFrameCreateLimit()) {
			throw new BusinessException(UserErrorCode.PLAN_FRAME_CREATE_LIMIT_EXCEEDED);
		}

		user.increaseMonthlyFrameCreateCount();
	}

	public LocalDateTime resolveHistoryCutoff(User user) {
		PlanTier planTier = user.getPlanTier();
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
}
