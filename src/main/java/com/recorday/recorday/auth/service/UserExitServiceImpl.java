package com.recorday.recorday.auth.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.recorday.recorday.auth.jwt.service.RefreshTokenService;
import com.recorday.recorday.auth.oauth2.service.OAuth2UnlinkService;
import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.user.exception.UserErrorCode;
import com.recorday.recorday.util.user.UserReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserExitServiceImpl implements UserExitService {

	private final UserReader userReader;
	private final RefreshTokenService refreshTokenService;
	private final List<UserDeletionHandler> handlers;
	private final List<OAuth2UnlinkService> unlinkServices;

	@Override
	@Transactional
	public void requestExit(Long userId) {

		User user = userReader.getUserById(userId);

		user.deleteRequested();
		refreshTokenService.logout(user.getPublicId());
	}

	@Override
	@Transactional
	public void exit(Long userId) {

		User user = userReader.getUserById(userId);

		if (user.getUserStatus() != UserStatus.DELETED_REQUESTED) {
			throw new BusinessException(UserErrorCode.NOT_TARGET);
		}

		handlers.forEach(handler -> handler.handleUserDeletion(userId));

		// 영속성 컨텍스트 클리어 후 재조회 (handlers 내부의 @Modifying(clearAutomatically=true)로 인해 detached 상태)
		user = userReader.getUserById(userId);

		for (OAuth2UnlinkService unlinkService : unlinkServices) {
			if (unlinkService.supports(user.getProvider())) {
				unlinkService.unlink(user);
				break;
			}
		}

		refreshTokenService.logout(user.getPublicId());
		user.delete();
	}

	@Override
	@Transactional
	public void reActivate(Long userId) {

		User user = userReader.getUserById(userId);

		if (user.getUserStatus() != UserStatus.DELETED_REQUESTED) {
			throw new BusinessException(UserErrorCode.NOT_TARGET);
		}

		refreshTokenService.logout(user.getPublicId());
		user.reActivate();
	}
}
