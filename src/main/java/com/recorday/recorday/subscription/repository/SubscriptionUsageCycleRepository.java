package com.recorday.recorday.subscription.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.recorday.recorday.subscription.entity.SubscriptionUsageCycle;
import com.recorday.recorday.subscription.entity.UserSubscription;

@Repository
public interface SubscriptionUsageCycleRepository extends JpaRepository<SubscriptionUsageCycle, Long> {

	List<SubscriptionUsageCycle> findAllBySubscriptionOrderByCycleStartAtDesc(UserSubscription subscription);
}
