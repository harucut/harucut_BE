package com.recorday.recorday.auth.local.service.mail;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recorday.recorday.auth.component.code.VerificationCodeGenerator;
import com.recorday.recorday.auth.exception.AuthErrorCode;
import com.recorday.recorday.auth.repository.VerificationTokenRepository;
import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.mail.service.EmailService;

@ExtendWith(MockitoExtension.class)
class MailAuthCodeServiceTest {

	@Mock
	private VerificationCodeGenerator generator;

	@Mock
	private VerificationTokenRepository repository;

	@Mock
	private EmailService emailService;

	@InjectMocks
	private MailAuthCodeService mailAuthCodeService;

	private static final long VERIFICATION_TTL = 300L;
	private static final String EMAIL_SUBJECT = "[Harucut] 이메일 인증 코드입니다.";

	@Test
	@DisplayName("인증코드 생성 후 저장 및 이메일 발송 성공")
	void sendCode_인증코드_생성_저장_이메일발송() {
		// given
		String email = "test@example.com";
		String generatedCode = "123456";

		given(generator.generate()).willReturn(generatedCode);
		willDoNothing().given(repository).save(email, generatedCode, VERIFICATION_TTL);
		willDoNothing().given(emailService).sendVerificationCode(email, EMAIL_SUBJECT, generatedCode);

		// when
		mailAuthCodeService.sendCode(email);

		// then
		then(generator).should(times(1)).generate();
		then(repository).should(times(1)).save(email, generatedCode, VERIFICATION_TTL);
		then(emailService).should(times(1)).sendVerificationCode(email, EMAIL_SUBJECT, generatedCode);
	}

	@Test
	@DisplayName("유효한 인증코드 검증 성공 시 코드 삭제")
	void verifyCode_유효한_인증코드_검증_성공() {
		// given
		String email = "test@example.com";
		String inputCode = "123456";
		String storedCode = "123456";

		given(repository.getCode(email)).willReturn(storedCode);
		willDoNothing().given(repository).remove(email);

		// when
		mailAuthCodeService.verifyCode(email, inputCode);

		// then
		then(repository).should(times(1)).getCode(email);
		then(repository).should(times(1)).remove(email);
	}

	@Test
	@DisplayName("저장된 코드가 없을 때 EMAIL_AUTH_FAILED 예외 발생")
	void verifyCode_저장된_코드가_없을때_예외발생() {
		// given
		String email = "test@example.com";
		String inputCode = "123456";

		given(repository.getCode(email)).willReturn(null);

		// when & then
		assertThatThrownBy(() -> mailAuthCodeService.verifyCode(email, inputCode))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException businessException = (BusinessException) exception;
				assertThat(businessException.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_AUTH_FAILED);
			});

		then(repository).should(times(1)).getCode(email);
		then(repository).should(never()).remove(anyString());
	}

	@Test
	@DisplayName("코드 불일치 시 EMAIL_AUTH_FAILED 예외 발생")
	void verifyCode_코드_불일치시_예외발생() {
		// given
		String email = "test@example.com";
		String inputCode = "123456";
		String storedCode = "654321";

		given(repository.getCode(email)).willReturn(storedCode);

		// when & then
		assertThatThrownBy(() -> mailAuthCodeService.verifyCode(email, inputCode))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException businessException = (BusinessException) exception;
				assertThat(businessException.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_AUTH_FAILED);
			});

		then(repository).should(times(1)).getCode(email);
		then(repository).should(never()).remove(anyString());
	}
}
