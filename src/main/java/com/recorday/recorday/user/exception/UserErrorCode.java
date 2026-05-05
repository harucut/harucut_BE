package com.recorday.recorday.user.exception;

import org.springframework.http.HttpStatus;

import com.recorday.recorday.exception.ErrorCode;

import lombok.Getter;

@Getter
public enum UserErrorCode implements ErrorCode {

	NOT_TARGET("USR-001", HttpStatus.BAD_REQUEST, "탈퇴 대상이 아닌 사용자입니다."),
	PLAN_VIDEO_DOWNLOAD_LIMIT_EXCEEDED("USR-101", HttpStatus.FORBIDDEN, "요금제의 월간 영상 다운로드 횟수를 초과했습니다."),
	PLAN_FRAME_CREATE_LIMIT_EXCEEDED("USR-102", HttpStatus.FORBIDDEN, "요금제의 프레임 생성 한도를 초과했습니다."),
	PLAN_HISTORY_RETENTION_EXCEEDED("USR-103", HttpStatus.FORBIDDEN, "요금제에서 허용한 기록 조회 기간을 초과했습니다."),
	;

	private final String code;
	private final HttpStatus httpStatus;
	private final String message;

	UserErrorCode(String code, HttpStatus httpStatus, String message) {
		this.code = code;
		this.httpStatus = httpStatus;
		this.message = message;
	}
}
