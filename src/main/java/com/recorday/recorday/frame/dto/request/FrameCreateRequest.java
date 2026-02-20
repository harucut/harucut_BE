package com.recorday.recorday.frame.dto.request;

import java.util.List;
import java.util.Map;

import com.recorday.recorday.frame.entity.attributes.BackgroundAttributes;
import com.recorday.recorday.frame.enums.ComponentType;
import com.recorday.recorday.frame.enums.FrameType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FrameCreateRequest(
	@NotBlank(message = "제목은 필수입니다.")
	String title,

	String description,

	@NotBlank(message = "주소는 필수입니다.")
	String previewKey,

	@NotNull(message = "프레임 타입은 필수입니다.")
	FrameType frameType,

	int canvasWidth,
	int canvasHeight,

	@NotNull(message = "배경 정보는 필수입니다.")
	BackgroundAttributes background,

	@Valid
	List<ComponentRequest> components
) {
	public record ComponentRequest(
		String id,
		@NotNull ComponentType type,
		@NotBlank String source,
		double x,
		double y,
		double width,
		double height,
		double scale,
		double rotation,
		int zIndex,
		Map<String, Object> styleJson
	) {}
}
