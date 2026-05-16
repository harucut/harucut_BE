package com.recorday.recorday.user.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recorday.recorday.subscription.entity.UserSubscription;
import com.recorday.recorday.storage.service.FileStorageService;
import com.recorday.recorday.user.config.PlanPricingProperties;
import com.recorday.recorday.user.dto.response.SubscriptionUsageResponse;
import com.recorday.recorday.user.dto.response.UserInfoResponse;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.PlanTier;
import com.recorday.recorday.util.user.UserReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserReader userReader;
	private final FileStorageService fileStorageService;
	private final PlanPricingProperties planPricingProperties;

	@Override
	public UserInfoResponse getUserInfo(Long userId) {

		User user = userReader.getUserById(userId);
		UserSubscription subscription = resolveSubscription(user);

		String profilePresignedUrl = fileStorageService.generatePresignedGetUrl(user.getProfileUrl());
		int monthlyPrice = planPricingProperties.getPrice(subscription.getPlanTier());

		return new UserInfoResponse(
			user.getId(),
			user.getEmail(),
			user.getUsername(),
			profilePresignedUrl,
			user.getProvider().name(),
			subscription.getPlanTier().name(),
			monthlyPrice
		);
	}

	@Override
	@Transactional
	public SubscriptionUsageResponse getSubscriptionUsage(Long userId) {
		User user = userReader.getUserById(userId);
		UserSubscription subscription = resolveSubscription(user);
		subscription.syncQuotaCycle(LocalDateTime.now());

		PlanTier planTier = subscription.getPlanTier();

		int frameCreateLimit = planTier.getTotalFrameCreateLimit();
		int frameCreateUsed = subscription.getTotalFrameCreateCount();
		boolean frameCreateUnlimited = planTier.isFrameCreateUnlimited();
		int frameCreateRemaining = resolveRemainingCount(frameCreateLimit, frameCreateUsed, frameCreateUnlimited);

		int videoDownloadLimit = planTier.getMonthlyVideoDownloadLimit();
		int videoDownloadUsed = subscription.getCurrentVideoDownloadCount();
		boolean videoDownloadUnlimited = planTier.isVideoDownloadUnlimited();
		int videoDownloadRemaining = resolveRemainingCount(videoDownloadLimit, videoDownloadUsed, videoDownloadUnlimited);

		return new SubscriptionUsageResponse(
			planTier.name(),
			frameCreateLimit,
			frameCreateUsed,
			frameCreateRemaining,
			frameCreateUnlimited,
			videoDownloadLimit,
			videoDownloadUsed,
			videoDownloadRemaining,
			videoDownloadUnlimited,
			subscription.getCurrentCycleStartAt(),
			subscription.getCurrentCycleEndAt()
		);
	}

	@Override
	@Transactional
	public void changeUsername(Long userId, String username) {

		User user = userReader.getUserById(userId);

		user.changeUsername(username);
	}

	@Override
	@Transactional
	public void changeProfileImage(Long userId, String s3Key) {

		User user = userReader.getUserById(userId);

		user.changeProfileUrl(s3Key);
	}

	private UserSubscription resolveSubscription(User user) {
		UserSubscription subscription = user.getSubscription();
		if (subscription != null) {
			return subscription;
		}

		return UserSubscription.createDefault(user);
	}

	private int resolveRemainingCount(int limit, int used, boolean unlimited) {
		if (unlimited) {
			return -1;
		}
		return Math.max(limit - used, 0);
	}
}
