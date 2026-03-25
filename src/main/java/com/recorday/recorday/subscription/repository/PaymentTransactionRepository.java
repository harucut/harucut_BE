package com.recorday.recorday.subscription.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.recorday.recorday.subscription.entity.PaymentTransaction;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

	Optional<PaymentTransaction> findByProviderAndProviderEventId(String provider, String providerEventId);
}
