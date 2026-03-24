package com.recorday.recorday.subscription.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.recorday.recorday.subscription.entity.UserSubscription;
import com.recorday.recorday.user.entity.User;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

	Optional<UserSubscription> findByUser(User user);

	Optional<UserSubscription> findByUserId(Long userId);
}
