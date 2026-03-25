package com.recorday.recorday.frame.dto.request;

import java.util.List;
import java.util.Map;

import com.recorday.recorday.frame.entity.attributes.BackgroundAttributes;
import com.recorday.recorday.frame.enums.ComponentType;
import com.recorday.recorday.frame.enums.FrameType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "프레임 생성/수정 요청 DTO")
public record FrameCreateRequest(
	@Schema(description = "프레임 제목", example = "봄 여행 4컷")
	@NotBlank(message = "제목은 필수입니다.")
	String title,

	@Schema(description = "프레임 설명", example = "벚꽃 배경의 여행 프레임")
	String description,

	@Schema(description = "프리뷰 리소스 key", example = "frames/preview/frame-1.png")
	@NotBlank(message = "주소는 필수입니다.")
	String previewKey,

	@Schema(description = "프레임 타입", example = "FOUR_CUT")
	@NotNull(message = "프레임 타입은 필수입니다.")
	FrameType frameType,

	@Schema(description = "캔버스 너비", example = "1080")
	int canvasWidth,
	@Schema(description = "캔버스 높이", example = "1920")
	int canvasHeight,

	@Schema(description = "배경 속성")
	@NotNull(message = "배경 정보는 필수입니다.")
	BackgroundAttributes background,

	@Schema(description = "프레임 컴포넌트 목록")
	@Valid
	List<ComponentRequest> components
) {
	@Schema(description = "프레임 컴포넌트 요청 DTO")
	public record ComponentRequest(
		@Schema(description = "클라이언트 컴포넌트 식별자", example = "comp-1")
		String id,
		@Schema(description = "컴포넌트 타입", example = "IMAGE")
		@NotNull ComponentType type,
		@Schema(description = "소스 URL 또는 key", example = "s3://bucket/frames/item.png")
		@NotBlank String source,
		@Schema(description = "X 좌표", example = "120.5")
		double x,
		@Schema(description = "Y 좌표", example = "220.0")
		double y,
		@Schema(description = "너비", example = "360.0")
		double width,
		@Schema(description = "높이", example = "480.0")
		double height,
		@Schema(description = "스케일", example = "1.0")
		double scale,
		@Schema(description = "회전 각도", example = "0.0")
		double rotation,
		@Schema(description = "레이어 순서", example = "1")
		int zIndex,
		@Schema(description = "스타일 JSON 맵")
		Map<String, Object> styleJson
	) {}
}
