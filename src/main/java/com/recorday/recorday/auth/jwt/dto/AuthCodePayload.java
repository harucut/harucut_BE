package com.recorday.recorday.auth.jwt.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인가 코드 교환 결과 페이로드 DTO")
public record AuthCodePayload(
	@Schema(description = "사용자 Public ID", example = "usr_AbCdEf12")
	String publicId,
	@Schema(description = "OAuth 공급자", example = "NAVER")
	String provider
) {
}
