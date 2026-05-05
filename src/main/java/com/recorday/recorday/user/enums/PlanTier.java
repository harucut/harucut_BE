package com.recorday.recorday.user.enums;

public enum PlanTier {
	BASIC(1, 1, 3),
	PLUS(10, 5, -1),
	PRO(30, 10, -1);

	private final int monthlyVideoDownloadLimit;
	private final int totalFrameCreateLimit;
	private final int historyRetentionDays;

	PlanTier(int monthlyVideoDownloadLimit, int totalFrameCreateLimit, int historyRetentionDays) {
		this.monthlyVideoDownloadLimit = monthlyVideoDownloadLimit;
		this.totalFrameCreateLimit = totalFrameCreateLimit;
		this.historyRetentionDays = historyRetentionDays;
	}

	public int getMonthlyVideoDownloadLimit() {
		return monthlyVideoDownloadLimit;
	}

	public int getTotalFrameCreateLimit() {
		return totalFrameCreateLimit;
	}

	public int getMonthlyFrameCreateLimit() {
		return totalFrameCreateLimit;
	}

	public int getHistoryRetentionDays() {
		return historyRetentionDays;
	}

	public boolean isVideoDownloadUnlimited() {
		return monthlyVideoDownloadLimit < 0;
	}

	public boolean isFrameCreateUnlimited() {
		return totalFrameCreateLimit < 0;
	}

	public boolean isHistoryUnlimited() {
		return historyRetentionDays < 0;
	}
}
