package com.recorday.recorday.frame.dto.response;

import java.util.List;
import java.util.Map;

import com.recorday.recorday.frame.entity.attributes.BackgroundAttributes;
import com.recorday.recorday.frame.enums.ComponentType;
import com.recorday.recorday.frame.enums.FrameType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프레임 조회 응답 DTO")
public record FrameResponse(
	@Schema(description = "프레임 ID", example = "1")
	Long frameId,
	@Schema(description = "프레임 제목", example = "봄 여행 4컷")
	String title,
	@Schema(description = "프레임 설명", example = "벚꽃 배경의 여행 프레임")
	String description,
	@Schema(description = "프레임 소스", example = "frames/preview/frame-1.png")
	String source,
	@Schema(description = "프레임 타입", example = "FOUR_CUT")
	FrameType frameType,
	@Schema(description = "배경 속성")
	BackgroundAttributes background,
	@Schema(description = "컴포넌트 목록")
	List<ComponentResponse> components
) {
	@Schema(description = "프레임 컴포넌트 응답 DTO")
	public record ComponentResponse(
		@Schema(description = "컴포넌트 ID", example = "10")
		Long id,
		@Schema(description = "컴포넌트 타입", example = "IMAGE")
		ComponentType type,
		@Schema(description = "소스 URL 또는 key", example = "frames/item.png")
		String source,
		@Schema(description = "리소스 key", example = "uploads/frames/item.png")
		String key,
		@Schema(description = "X 좌표", example = "120.5")
		double x,
		@Schema(description = "Y 좌표", example = "220.0")
		double y,
		@Schema(description = "너비", example = "360.0")
		double width,
		@Schema(description = "높이", example = "480.0")
		double height,
		@Schema(description = "회전 각도", example = "0.0")
		double rotation,
		@Schema(description = "레이어 순서", example = "1")
		int zIndex,
		@Schema(description = "스타일 JSON 맵")
		Map<String, Object> style
	) {}
}
