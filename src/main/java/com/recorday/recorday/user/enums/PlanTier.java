package com.recorday.recorday.user.enums;

public enum PlanTier {
	BASIC(1, 1, 7),
	PLUS(10, 10, 30),
	PRO(-1, -1, -1);

	private final int monthlyVideoDownloadLimit;
	private final int monthlyFrameCreateLimit;
	private final int historyRetentionDays;

	PlanTier(int monthlyVideoDownloadLimit, int monthlyFrameCreateLimit, int historyRetentionDays) {
		this.monthlyVideoDownloadLimit = monthlyVideoDownloadLimit;
		this.monthlyFrameCreateLimit = monthlyFrameCreateLimit;
		this.historyRetentionDays = historyRetentionDays;
	}

	public int getMonthlyVideoDownloadLimit() {
		return monthlyVideoDownloadLimit;
	}

	public int getMonthlyFrameCreateLimit() {
		return monthlyFrameCreateLimit;
	}

	public int getHistoryRetentionDays() {
		return historyRetentionDays;
	}

	public boolean isVideoDownloadUnlimited() {
		return monthlyVideoDownloadLimit < 0;
	}

	public boolean isFrameCreateUnlimited() {
		return monthlyFrameCreateLimit < 0;
	}

	public boolean isHistoryUnlimited() {
		return historyRetentionDays < 0;
	}
}
