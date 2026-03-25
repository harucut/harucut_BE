package com.recorday.recorday.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.recorday.recorday.user.enums.PlanTier;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "billing.pricing")
public class PlanPricingProperties {
	private int basic = 0;
	private int plus = 3000;
	private int pro = 10000;

	public int getPrice(PlanTier tier) {
		return switch (tier) {
			case BASIC -> basic;
			case PLUS -> plus;
			case PRO -> pro;
		};
	}
}
