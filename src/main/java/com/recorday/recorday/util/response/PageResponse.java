package com.recorday.recorday.util.response;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "페이지 응답")
public record PageResponse<T>(
	@Schema(description = "현재 페이지 데이터")
	List<T> content,

	@Schema(description = "전체 데이터 수", example = "42")
	long totalElements,

	@Schema(description = "전체 페이지 수", example = "5")
	int totalPages,

	@Schema(description = "현재 페이지 번호(0부터 시작)", example = "0")
	int number,

	@Schema(description = "페이지 크기", example = "10")
	int size
) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
			page.getContent(),
			page.getTotalElements(),
			page.getTotalPages(),
			page.getNumber(),
			page.getSize()
		);
	}
}
