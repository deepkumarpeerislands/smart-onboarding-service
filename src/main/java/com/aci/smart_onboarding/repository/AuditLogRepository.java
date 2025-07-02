package com.aci.smart_onboarding.repository;

import com.aci.smart_onboarding.model.AuditLog;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends ReactiveMongoRepository<AuditLog, String> {}
