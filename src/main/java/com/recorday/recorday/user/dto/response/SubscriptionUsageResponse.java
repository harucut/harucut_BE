package com.recorday.recorday.user.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 구독 사용량 조회 응답")
public record SubscriptionUsageResponse(
	@Schema(description = "요금제 단계", example = "BASIC")
	String planTier,

	@Schema(description = "프레임 생성 계정 누적 한도 (-1이면 무제한)", example = "5")
	int frameCreateTotalLimit,

	@Schema(description = "프레임 생성 누적 사용 횟수", example = "3")
	int frameCreateUsedCount,

	@Schema(description = "프레임 생성 남은 가능 횟수 (-1이면 무제한)", example = "2")
	int frameCreateRemainingCount,

	@Schema(description = "프레임 생성 무제한 여부", example = "false")
	boolean frameCreateUnlimited,

	@Schema(description = "영상 다운로드 월 한도 (-1이면 무제한)", example = "10")
	int videoDownloadMonthlyLimit,

	@Schema(description = "영상 다운로드 사용 횟수", example = "2")
	int videoDownloadUsedCount,

	@Schema(description = "영상 다운로드 남은 가능 횟수 (-1이면 무제한)", example = "8")
	int videoDownloadRemainingCount,

	@Schema(description = "영상 다운로드 무제한 여부", example = "false")
	boolean videoDownloadUnlimited,

	@Schema(description = "현재 사용량 사이클 시작 시각")
	LocalDateTime currentCycleStartAt,

	@Schema(description = "현재 사용량 사이클 종료 시각")
	LocalDateTime currentCycleEndAt
) {
}
