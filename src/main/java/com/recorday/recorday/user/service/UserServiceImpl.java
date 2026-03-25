package com.recorday.recorday.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recorday.recorday.subscription.entity.UserSubscription;
import com.recorday.recorday.storage.service.FileStorageService;
import com.recorday.recorday.user.config.PlanPricingProperties;
import com.recorday.recorday.user.dto.response.UserInfoResponse;
import com.recorday.recorday.user.entity.User;
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
}
