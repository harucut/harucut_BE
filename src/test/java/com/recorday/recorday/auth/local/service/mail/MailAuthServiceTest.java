package com.recorday.recorday.auth.local.service.mail;

import static org.mockito.BDDMockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class MailAuthServiceTest {

	@Mock
	private MailAuthCodeService mailAuthCodeService;

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private MailAuthService mailAuthService;

	private static final String KEY_PREFIX = "REGISTER:EMAIL:";
	private static final long EXPIRATION = 60 * 10L;

	@Test
	@DisplayName("인증코드 발송 성공 시 mailAuthCodeService.sendCode 호출")
	void sendAuthCode_인증코드_발송_성공() {
		// given
		String email = "test@example.com";

		willDoNothing().given(mailAuthCodeService).sendCode(email);

		// when
		mailAuthService.sendAuthCode(email);

		// then
		then(mailAuthCodeService).should(times(1)).sendCode(email);
	}

	@Test
	@DisplayName("인증코드 검증 성공 시 Redis에 VERIFIED 저장")
	void verifyAuthCode_인증코드_검증_성공_Redis에_VERIFIED_저장() {
		// given
		String email = "test@example.com";
		String inputCode = "123456";
		String key = KEY_PREFIX + email;

		willDoNothing().given(mailAuthCodeService).verifyCode(email, inputCode);
		given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

		// when
		mailAuthService.verifyAuthCode(email, inputCode);

		// then
		then(mailAuthCodeService).should(times(1)).verifyCode(email, inputCode);
		then(stringRedisTemplate).should(times(1)).opsForValue();
		then(valueOperations).should(times(1)).set(key, "VERIFIED", EXPIRATION, TimeUnit.SECONDS);
	}
}
