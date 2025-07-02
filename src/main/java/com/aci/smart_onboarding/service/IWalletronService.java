package com.aci.smart_onboarding.service;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.WalletronImagesResponseDTO;
import com.aci.smart_onboarding.dto.WalletronRequest;
import com.aci.smart_onboarding.dto.WalletronResponse;
import com.aci.smart_onboarding.dto.WalletronUsersRequest;
import com.aci.smart_onboarding.dto.WalletronUsersResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface IWalletronService {
  Mono<ResponseEntity<Api<WalletronResponse>>> createWalletron(WalletronRequest walletronRequest);

  Mono<ResponseEntity<Api<WalletronResponse>>> getWalletronById(String walletronId);

  Mono<ResponseEntity<Api<WalletronResponse>>> updateWalletronPartiallyWithOrderedOperations(
      String walletronId, Map<String, Object> fields);

  Mono<ResponseEntity<Map<String, Object>>> getWalletronSectionById(
      String walletronId, String section);

  /**
   * Retrieves images associated with a Walletron by its ID
   *
   * @param walletronId The ID of the Walletron
   * @return Mono containing ResponseEntity with Api wrapper containing WalletronImagesResponseDTO
   */
  Mono<ResponseEntity<Api<WalletronImagesResponseDTO>>> getWalletronImages(String walletronId);

  Mono<ResponseEntity<Api<WalletronUsersResponse>>> createWalletronUsers(
      WalletronUsersRequest walletronUsersRequest);

  Mono<ResponseEntity<Api<WalletronUsersResponse>>> createWalletronUsersFromFile(
      FilePart filePart, String walletronId, String brdId);

  Mono<Boolean> validateWalletronExists(String walletronId, String brdId);
}
