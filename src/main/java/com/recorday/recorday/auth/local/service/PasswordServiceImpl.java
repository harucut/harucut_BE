package com.recorday.recorday.auth.local.service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recorday.recorday.auth.exception.AuthErrorCode;
import com.recorday.recorday.auth.jwt.dto.TokenResponse;
import com.recorday.recorday.auth.local.dto.response.EmailAuthVerifyResponse;
import com.recorday.recorday.auth.local.service.mail.MailAuthCodeService;
import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.util.user.UserReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

	private final UserReader userReader;
	private final PasswordEncoder passwordEncoder;
	private final MailAuthCodeService mailAuthCodeService;
	private final StringRedisTemplate stringRedisTemplate;

	private static final String KEY_PREFIX = "reset_token:";
	private static final long RESET_TOKEN_EXPIRATION = 60 * 10L;

	@Override
	@Transactional
	public void resetPassword(String resetToken, String newPassword) {

		String key = KEY_PREFIX + resetToken;

		String email = stringRedisTemplate.opsForValue().get(key);
		System.out.println(email);

		if (email == null) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}

		User user = userReader.getUserByEmailAndProvider(email, Provider.RECORDAY);

		user.changePassword(passwordEncoder.encode(newPassword));

		stringRedisTemplate.delete(key);
	}

	@Override
	@Transactional
	public EmailAuthVerifyResponse verifyAuthCode(String email, String inputCode) {
		mailAuthCodeService.verifyCode(email, inputCode);

		String resetToken = UUID.randomUUID().toString();

		String key = KEY_PREFIX + resetToken;

		stringRedisTemplate.opsForValue().set(key, email, RESET_TOKEN_EXPIRATION, TimeUnit.SECONDS);

		return new EmailAuthVerifyResponse(resetToken);
	}

	@Override
	@Transactional(readOnly = true)
	public void verifyOldPassword(Long userId, String oldPassword) {

		User user = userReader.getUserById(userId);

		if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
			throw new BusinessException(AuthErrorCode.WRONG_PASSWORD);
		}
	}

	@Override
	@Transactional
	public void changePassword(Long userId, String password, String oldPassword, String newPassword) {

		if (!passwordEncoder.matches(oldPassword, password)) {
			throw new BusinessException(AuthErrorCode.WRONG_PASSWORD);
		}

		User user = userReader.getUserById(userId);

		user.changePassword(passwordEncoder.encode(newPassword));
	}
}
