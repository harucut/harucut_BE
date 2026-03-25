package com.recorday.recorday.storage.exception;

import org.springframework.http.HttpStatus;

import com.recorday.recorday.exception.ErrorCode;

import lombok.Getter;

@Getter
public enum StorageErrorCode implements ErrorCode {
	UNSUPPORTED_UPLOAD_TYPE("STOR-000", HttpStatus.BAD_REQUEST, "UNSUPPORTED_UPLOAD_TYPE"),
	TRANSCODE_FAILED("STOR-001", HttpStatus.INTERNAL_SERVER_ERROR, "TRANSCODE_FAILED"),
	;

	private final String code;
	private final HttpStatus httpStatus;
	private final String message;

	StorageErrorCode(String code, HttpStatus httpStatus, String message) {
		this.code = code;
		this.httpStatus = httpStatus;
		this.message = message;
	}
}
