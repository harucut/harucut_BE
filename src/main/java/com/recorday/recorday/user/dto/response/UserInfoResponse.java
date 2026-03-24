package com.recorday.recorday.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 정보 조회 응답")
public record UserInfoResponse(
	@Schema(description = "사용자 ID (PK)", example = "adsDF32DSF")
	Long id,

	@Schema(description = "사용자 이메일", example = "user@recorday.com")
	String email,

	@Schema(description = "사용자 닉네임", example = "레코데이")
	String username,

	@Schema(description = "프로필 이미지 URL", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/profile/123.jpg")
	String profileUrl,

	@Schema(description = "로그인 플랫폼", example = "NAVER, KAKAO, HARUCUT")
	String loginPlatform,

	@Schema(description = "요금제 단계", example = "BASIC, PLUS, PRO")
	String planTier,

	@Schema(description = "월 구독료(원)", example = "3000")
	Integer monthlyPrice
) {}
