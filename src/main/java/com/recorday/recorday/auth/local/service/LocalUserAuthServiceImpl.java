package com.recorday.recorday.auth.local.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recorday.recorday.auth.exception.AuthErrorCode;
import com.recorday.recorday.auth.local.dto.request.LocalRegisterRequest;
import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserRole;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocalUserAuthServiceImpl implements LocalUserAuthService{

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final StringRedisTemplate stringRedisTemplate;

	private static final String KEY_PREFIX = "REGISTER:EMAIL:";

	@Override
	@Transactional
	public void register(LocalRegisterRequest request) {

		if (isExistLocal(request.email())) {
			throw new BusinessException(AuthErrorCode.EMAIL_DUPLICATED);
		}

		String key = KEY_PREFIX + request.email();

		if (stringRedisTemplate.opsForValue().get(key) == null) {
			throw new BusinessException(AuthErrorCode.REGISTER_EMAIL_ERROR);
		}
		stringRedisTemplate.delete(key);

		User user = User.builder()
			.provider(Provider.HARUCUT)
			.userRole(UserRole.ROLE_USER)
			.email(request.email())
			.username(request.username())
			.password(passwordEncoder.encode(request.password()))
			.profileUrl("resources/defaults/userDefaultImage.png")
			.userStatus(UserStatus.ACTIVE)
			.build();

		userRepository.save(user);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isExistLocal(String email) {
		return userRepository.existsByProviderAndEmail(Provider.HARUCUT, email);
	}
}
