package com.recorday.recorday.auth.utils;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

	public ResponseCookie createTokenCookie(String name, String value, long maxAgeMillis) {
		return ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(true)
			.path("/")
			.maxAge(Duration.ofMillis(maxAgeMillis))
			.sameSite("Lax")
			.domain("harucut.com")
			.build();
	}

	public ResponseCookie createExpiredCookie(String name) {
		return ResponseCookie.from(name, "")
			.httpOnly(true)
			.secure(true)
			.path("/")
			.maxAge(0)
			.sameSite("Lax")
			.domain("harucut.com")
			.build();
	}
}
