package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.model.WalletronUsers;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface WalletronUsersRepository extends ReactiveMongoRepository<WalletronUsers, String> {

  Mono<WalletronUsers> findByEmail(String email);

  Flux<WalletronUsers> findByWalletronId(String walletronId);

  Flux<WalletronUsers> findByBrdId(String brdId);

  Mono<Boolean> existsByEmail(String email);
}
