package com.recorday.recorday.exception.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "필드 단위 검증 에러 응답")
public record FieldErrorResponse (
	@Schema(description = "에러가 발생한 필드명", example = "email")
	String field,
	@Schema(description = "에러 메시지", example = "유효한 이메일 형식이 아닙니다.")
	String message,
	@Schema(description = "요청에 전달된 잘못된 값")
	Object rejectedValue
) {}
