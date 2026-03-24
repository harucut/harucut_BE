package com.recorday.recorday.auth.oauth2.enums;

public enum Provider {
	GOOGLE, KAKAO, NAVER, HARUCUT;

	public static Provider from(String registrationId) {
		if (registrationId == null) {
			throw new IllegalArgumentException("registrationId is null");
		}

		return switch (registrationId.toLowerCase()) {
			case "harucut" -> HARUCUT;
			case "google" -> GOOGLE;
			case "kakao" -> KAKAO;
			case "naver" -> NAVER;
			default -> throw new IllegalArgumentException("Unknown provider: " + registrationId);
		};
	}

}
