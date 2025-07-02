package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.model.WalletronExampleImages;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface WalletronExampleImagesRepository
    extends ReactiveMongoRepository<WalletronExampleImages, String> {
  Mono<WalletronExampleImages> findFirstByOrderByCreatedAtDesc();
}
