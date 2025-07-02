package com.aci.smart_onboarding.service;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import com.aci.smart_onboarding.dto.Api;
import reactor.core.publisher.Mono;

public interface IAuthService {
    Mono<ResponseEntity<Api<Void>>> logout(Authentication auth);
} 