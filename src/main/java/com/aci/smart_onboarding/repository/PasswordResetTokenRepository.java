package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.model.PasswordResetToken;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface PasswordResetTokenRepository
    extends ReactiveMongoRepository<PasswordResetToken, String> {

  Mono<PasswordResetToken> findByToken(String token);

  Mono<PasswordResetToken> findByTokenAndUsedFalseAndExpiryDateGreaterThan(
      String token, java.time.Instant now);

  Mono<Void> deleteByExpiryDateLessThan(java.time.Instant now);
}
