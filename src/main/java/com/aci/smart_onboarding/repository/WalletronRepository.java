package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.model.Walletron;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface WalletronRepository extends ReactiveMongoRepository<Walletron, String> {
  Mono<Walletron> findByBrdId(String brdId);

  Mono<Walletron> findByWalletronId(String walletronId);
}
