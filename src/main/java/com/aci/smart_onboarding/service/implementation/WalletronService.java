package com.aci.smart_onboarding.service.implementation;

import com.aci.smart_onboarding.dto.Api;
import com.aci.smart_onboarding.dto.WalletronImagesResponseDTO;
import com.aci.smart_onboarding.dto.WalletronRequest;
import com.aci.smart_onboarding.dto.WalletronResponse;
import com.aci.smart_onboarding.dto.WalletronUsersRequest;
import com.aci.smart_onboarding.dto.WalletronUsersResponse;
import com.aci.smart_onboarding.exception.AlreadyExistException;
import com.aci.smart_onboarding.exception.BadRequestException;
import com.aci.smart_onboarding.exception.NotFoundException;
import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.Walletron;
import com.aci.smart_onboarding.model.WalletronUsers;
import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.repository.WalletronRepository;
import com.aci.smart_onboarding.repository.WalletronUsersRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IFileService;
import com.aci.smart_onboarding.service.IWalletronService;
import com.aci.smart_onboarding.util.FileProcessorUtil;
import com.aci.smart_onboarding.util.walletron.ACIWalletronAgentPortal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class WalletronService implements IWalletronService {

  private static final String STATUS_ERROR = "ERROR";
  private static final String ERROR_KEY = "error";
  private static final String WALLETRON_NOT_FOUND = "Walletron not found with id: ";
  private static final String SUCCESS_STATUS = "SUCCESS";
  private static final String MESSAGE_KEY = "message";

  @SuppressWarnings("squid:S5850")
  private static final String QUOTES_REGEX = "(^\"|\"$)";

  private static final String AGENT_PORTAL_FIELD = "aciWalletronAgentPortal";

  private final WalletronRepository walletronRepository;
  private final WalletronUsersRepository walletronUsersRepository;
  private final FileProcessorUtil fileProcessorUtil;
  private final DtoModelMapper dtoModelMapper;
  private final ObjectMapper objectMapper;
  private final BRDRepository brdRepository;
  private final IFileService fileService;

  @Autowired
  public WalletronService(
      WalletronRepository walletronRepository,
      WalletronUsersRepository walletronUsersRepository,
      FileProcessorUtil fileProcessorUtil,
      BRDSecurityService securityService,
      DtoModelMapper dtoModelMapper,
      ObjectMapper objectMapper,
      BRDRepository brdRepository,
      IFileService fileService) {
    this.walletronRepository = walletronRepository;
    this.walletronUsersRepository = walletronUsersRepository;
    this.fileProcessorUtil = fileProcessorUtil;
    this.dtoModelMapper = dtoModelMapper;
    this.objectMapper = objectMapper;
    this.brdRepository = brdRepository;
    this.fileService = fileService;
  }

  @Override
  public Mono<ResponseEntity<Api<WalletronResponse>>> createWalletron(
      WalletronRequest walletronRequest) {
    return Mono.justOrEmpty(walletronRequest)
        .filter(Objects::nonNull)
        .switchIfEmpty(
            Mono.error(new IllegalArgumentException("Walletron request cannot be null or empty")))
        .map(this::mapToWalletron)
        .map(
            w -> {
              LocalDateTime now = LocalDateTime.now();
              w.setCreatedAt(now);
              w.setUpdatedAt(now);
              return w;
            })
        .flatMap(walletronRepository::save)
        .flatMap(
            savedWalletron ->
                // Update BRD with walletronId
                brdRepository
                    .findByBrdId(savedWalletron.getBrdId())
                    .switchIfEmpty(
                        Mono.error(
                            new NotFoundException(
                                "BRD not found with ID: " + savedWalletron.getBrdId())))
                    .flatMap(
                        brd -> {
                          brd.setWalletronId(savedWalletron.getWalletronId());
                          return brdRepository.save(brd).thenReturn(savedWalletron);
                        }))
        .map(dtoModelMapper::mapToWalletronResponse)
        .flatMap(
            response -> {
              ResponseEntity<Api<WalletronResponse>> responseEntity =
                  ResponseEntity.status(HttpStatus.CREATED)
                      .body(
                          new Api<>(
                              SUCCESS_STATUS,
                              "Walletron created",
                              Optional.of(response),
                              Optional.empty()));
              return Mono.just(responseEntity);
            })
        .onErrorResume(
            e -> {
              if (e instanceof AccessDeniedException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(
                            new Api<>(
                                STATUS_ERROR,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.of(Map.of(ERROR_KEY, e.getMessage())))));
              } else if (e instanceof IllegalArgumentException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(
                            new Api<>(
                                STATUS_ERROR,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.of(Map.of(ERROR_KEY, e.getMessage())))));
              }
              return Mono.just(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(
                          new Api<>(
                              STATUS_ERROR,
                              "Failed to save Walletron data",
                              Optional.empty(),
                              Optional.of(Map.of(ERROR_KEY, e.getMessage())))));
            });
  }

  private Walletron mapToWalletron(WalletronRequest request) {
    Walletron w = new Walletron();
    w.setWalletronId(request.getWalletronId());
    w.setBrdId(request.getBrdId());
    w.setBrdName(request.getBrdName());
    w.setWalletronEnabled(request.getWalletronEnabled());
    w.setSiteConfiguration(request.getSiteConfiguration());
    w.setNotificationsOptions(request.getNotificationsOptions());
    w.setAciWalletronAgentPortal(request.getAciWalletronAgentPortal());
    w.setAciWalletronDataExchange(request.getAciWalletronDataExchange());
    w.setAciWalletronEnrollmentStrategy(request.getAciWalletronEnrollmentStrategy());
    w.setEnrollmentUrl(request.getEnrollmentUrls());
    w.setTargetedCommunication(request.getTargetedCommunication());
    w.setAciCash(request.getAciCash());
    w.setWalletronApprovals(request.getWalletronApprovals());
    return w;
  }

  @Override
  public Mono<ResponseEntity<Api<WalletronResponse>>> getWalletronById(String walletronId) {
    return walletronRepository
        .findByWalletronId(walletronId)
        .switchIfEmpty(
            Mono.<Walletron>defer(
                () -> Mono.error(new NotFoundException(WALLETRON_NOT_FOUND + walletronId))))
        .map(
            walletron ->
                ResponseEntity.ok(
                    new Api<>(
                        SUCCESS_STATUS,
                        "Walletron retrieved successfully",
                        Optional.of(dtoModelMapper.mapToWalletronResponse(walletron)),
                        Optional.empty())))
        .onErrorResume(
            e -> {
              if (e instanceof NotFoundException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                            new Api<>(
                                STATUS_ERROR,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.of(Map.of(ERROR_KEY, e.getMessage())))));
              }
              return Mono.just(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(
                          new Api<>(
                              STATUS_ERROR,
                              "Failed to retrieve Walletron",
                              Optional.empty(),
                              Optional.of(Map.of(ERROR_KEY, e.getMessage())))));
            });
  }

  @Override
  @Transactional(
      propagation = Propagation.REQUIRED,
      timeout = 30,
      rollbackFor = {Exception.class},
      noRollbackFor = {NotFoundException.class})
  public Mono<ResponseEntity<Api<WalletronResponse>>> updateWalletronPartiallyWithOrderedOperations(
      String walletronId, Map<String, Object> fields) {
    // First validate the fields
    validateUpdateFields(fields);
    // Use walletronId consistently
    return walletronRepository
        .findByWalletronId(walletronId)
        .switchIfEmpty(
            Mono.<Walletron>defer(
                () -> Mono.error(new NotFoundException(WALLETRON_NOT_FOUND + walletronId))))
        .flatMap(
            existingWalletron -> {
              // Convert both existing and patch to JsonNode
              JsonNode existingNode = objectMapper.valueToTree(existingWalletron);
              JsonNode patchNode = objectMapper.valueToTree(fields);
              // Deep merge patchNode into existingNode
              JsonNode merged = deepMerge(existingNode, patchNode);
              // Convert merged node back to Walletron
              Walletron mergedWalletron = objectMapper.convertValue(merged, Walletron.class);
              // Preserve the original walletronId and id
              mergedWalletron.setWalletronId(existingWalletron.getWalletronId());
              mergedWalletron.setUpdatedAt(LocalDateTime.now());
              // Save the merged object
              return walletronRepository
                  .save(mergedWalletron)
                  .map(dtoModelMapper::mapToWalletronResponse)
                  .map(
                      response ->
                          ResponseEntity.ok(
                              new Api<>(
                                  SUCCESS_STATUS,
                                  "Walletron sections updated successfully",
                                  Optional.of(response),
                                  Optional.empty())));
            })
        .onErrorResume(
            e -> {
              if (e instanceof NotFoundException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                            new Api<>(
                                STATUS_ERROR,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.of(Map.of(ERROR_KEY, e.getMessage())))));
              } else if (e instanceof IllegalArgumentException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(
                            new Api<>(
                                STATUS_ERROR,
                                e.getMessage(),
                                Optional.empty(),
                                Optional.of(Map.of(ERROR_KEY, e.getMessage())))));
              }
              return Mono.just(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(
                          new Api<>(
                              STATUS_ERROR,
                              "Failed to update Walletron sections",
                              Optional.empty(),
                              Optional.of(Map.of(ERROR_KEY, e.getMessage())))));
            });
  }

  private void validateUpdateFields(Map<String, Object> fields) {
    if (fields == null || fields.isEmpty()) {
      throw new IllegalArgumentException("Update fields cannot be null or empty");
    }
    // List of allowed fields for update (use JSON property names)
    Set<String> allowedFields =
        Set.of(
            "brdId",
            "brdName",
            "walletronEnabled",
            "siteConfiguration",
            "notificationsOptions",
            AGENT_PORTAL_FIELD,
            "aciWalletronDataExchange",
            "aciWalletronEnrollmentStrategy",
            "enrollmentUrl",
            "targetedCommunication",
            "aciCash",
            "walletronApprovals");
    // Check if any field is not allowed
    fields.keySet().stream()
        .filter(field -> !allowedFields.contains(field))
        .findFirst()
        .ifPresent(
            field -> {
              throw new IllegalArgumentException("Field '" + field + "' is not allowed for update");
            });
  }

  // Improved deep merge logic
  private JsonNode deepMerge(JsonNode mainNode, JsonNode updateNode) {
    if (!(mainNode instanceof ObjectNode)) {
      return updateNode;
    }

    ObjectNode result = (ObjectNode) mainNode;
    Iterator<String> fieldNames = updateNode.fieldNames();

    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      JsonNode jsonNode = mainNode.get(fieldName);
      JsonNode updateField = updateNode.get(fieldName);

      if (jsonNode != null && jsonNode.isObject() && updateField.isObject()) {
        result.set(fieldName, deepMerge(jsonNode, updateField));
      } else if (jsonNode != null && jsonNode.isArray() && updateField.isArray()) {
        // For arrays, we'll replace the entire array
        result.set(fieldName, updateField);
      } else {
        result.set(fieldName, updateField);
      }
    }
    return result;
  }

  @Override
  public Mono<ResponseEntity<Map<String, Object>>> getWalletronSectionById(
      String walletronId, String section) {
    return walletronRepository
        .findByWalletronId(walletronId)
        .switchIfEmpty(
            Mono.<Walletron>defer(
                () -> Mono.error(new NotFoundException(WALLETRON_NOT_FOUND + walletronId))))
        .map(
            walletron -> {
              Object value =
                  switch (section) {
                    case "siteConfiguration" -> walletron.getSiteConfiguration();
                    case "notificationsOptions" -> walletron.getNotificationsOptions();
                    case AGENT_PORTAL_FIELD -> walletron.getAciWalletronAgentPortal();
                    case "aciWalletronDataExchange" -> walletron.getAciWalletronDataExchange();
                    case "aciWalletronEnrollmentStrategy" ->
                        walletron.getAciWalletronEnrollmentStrategy();
                    case "enrollmentUrls" -> walletron.getEnrollmentUrl();
                    case "targetedCommunication" -> walletron.getTargetedCommunication();
                    case "aciCash" -> walletron.getAciCash();
                    case "approvals" -> walletron.getWalletronApprovals();
                    default -> null;
                  };
              if (value == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(MESSAGE_KEY, "Section not found"));
              }
              return ResponseEntity.ok(Map.of(section, value));
            })
        .onErrorResume(
            e -> {
              if (e instanceof NotFoundException) {
                return Mono.just(
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(MESSAGE_KEY, e.getMessage())));
              }
              return Mono.just(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(Map.of(MESSAGE_KEY, "Failed to retrieve section")));
            })
        .cast(ResponseEntity.class)
        .map(responseEntity -> (ResponseEntity<Map<String, Object>>) responseEntity);
  }

  @Override
  public Mono<ResponseEntity<Api<WalletronImagesResponseDTO>>> getWalletronImages(
      String walletronId) {
    return walletronRepository
        .findByWalletronId(walletronId)
        .switchIfEmpty(
            Mono.<Walletron>defer(
                () -> Mono.error(new NotFoundException(WALLETRON_NOT_FOUND + walletronId))))
        .flatMap(this::extractImageUrls)
        .flatMap(this::convertUrlsToBase64)
        .onErrorResume(this::handleImageRetrievalError);
  }

  private Mono<Map<String, String>> extractImageUrls(Walletron walletron) {
    JsonNode siteConfigNode = objectMapper.valueToTree(walletron.getSiteConfiguration());
    Map<String, String> imageUrlToName = new HashMap<>();

    if (siteConfigNode != null) {
      extractMainConfigUrls(siteConfigNode, imageUrlToName);
      extractAgentPortalUrls(siteConfigNode, imageUrlToName);
    }

    log.info(
        "Found {} image URLs for walletronId: {}",
        imageUrlToName.size(),
        walletron.getWalletronId());
    return Mono.just(imageUrlToName);
  }

  private void extractMainConfigUrls(JsonNode siteConfigNode, Map<String, String> imageUrlToName) {
    addUrlWithName(siteConfigNode, "brandLogo", imageUrlToName);
    addUrlWithName(siteConfigNode, "iconImage", imageUrlToName);
    addUrlWithName(siteConfigNode, "stripImage", imageUrlToName);
    addUrlWithName(siteConfigNode, "thumbnailImage", imageUrlToName);
  }

  private void extractAgentPortalUrls(JsonNode siteConfigNode, Map<String, String> imageUrlToName) {
    JsonNode agentPortalNode = siteConfigNode.get(AGENT_PORTAL_FIELD);
    if (agentPortalNode != null) {
      addUrlWithName(agentPortalNode, "logo", imageUrlToName);
      addUrlWithName(agentPortalNode, "notificationLogo", imageUrlToName);
    }
  }

  private Mono<ResponseEntity<Api<WalletronImagesResponseDTO>>> convertUrlsToBase64(
      Map<String, String> imageUrlToName) {
    return fileService
        .getBase64EncodedImages(new ArrayList<>(imageUrlToName.keySet()))
        .map(base64Images -> createResponse(imageUrlToName, base64Images));
  }

  private ResponseEntity<Api<WalletronImagesResponseDTO>> createResponse(
      Map<String, String> imageUrlToName, Map<String, byte[]> base64Images) {
    Map<String, byte[]> namedImages = new HashMap<>();
    base64Images.forEach(
        (url, base64Bytes) -> {
          String imageName = imageUrlToName.get(url);
          if (imageName != null) {
            namedImages.put(imageName, base64Bytes);
          }
        });

    WalletronImagesResponseDTO responseDTO =
        WalletronImagesResponseDTO.builder().images(namedImages).build();

    return ResponseEntity.ok(
        new Api<>(
            SUCCESS_STATUS,
            "Walletron images retrieved successfully",
            Optional.of(responseDTO),
            Optional.empty()));
  }

  private Mono<ResponseEntity<Api<WalletronImagesResponseDTO>>> handleImageRetrievalError(
      Throwable e) {
    if (e instanceof NotFoundException) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(
                  new Api<WalletronImagesResponseDTO>(
                      STATUS_ERROR,
                      e.getMessage(),
                      Optional.empty(),
                      Optional.of(Map.of(ERROR_KEY, e.getMessage())))));
    }
    return Mono.just(
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                new Api<WalletronImagesResponseDTO>(
                    STATUS_ERROR,
                    "Failed to retrieve Walletron images",
                    Optional.empty(),
                    Optional.of(Map.of(ERROR_KEY, e.getMessage())))));
  }

  /** Helper method to add URL to map with its descriptive name if it exists */
  private void addUrlWithName(JsonNode node, String fieldName, Map<String, String> urlToName) {
    JsonNode urlNode = node.get(fieldName);
    if (urlNode != null && !urlNode.isNull() && !urlNode.asText().isEmpty()) {
      urlToName.put(urlNode.asText(), fieldName);
      log.debug("Added image URL for {}: {}", fieldName, urlNode.asText());
    }
  }

  @Override
  public Mono<ResponseEntity<Api<WalletronUsersResponse>>> createWalletronUsers(
      WalletronUsersRequest request) {
    return Mono.justOrEmpty(request)
        .filter(Objects::nonNull)
        .switchIfEmpty(Mono.error(new BadRequestException("WalletronUsers request cannot be null")))
        .flatMap(this::processWalletronUsersRequest)
        .onErrorMap(this::handleError);
  }

  @Override
  public Mono<Boolean> validateWalletronExists(String walletronId, String brdId) {
    // Strip quotes from both IDs if present
    String cleanWalletronId = walletronId.replaceAll(QUOTES_REGEX, "");
    String cleanBrdId = brdId.replaceAll(QUOTES_REGEX, "");

    return walletronRepository
        .findById(cleanWalletronId)
        .switchIfEmpty(
            Mono.error(new NotFoundException("Walletron not found with ID: " + walletronId)))
        .flatMap(
            walletron -> {
              if (!cleanBrdId.equals(walletron.getBrdId())) {
                return Mono.error(
                    new NotFoundException(
                        "BRD ID mismatch. Expected: "
                            + walletron.getBrdId()
                            + ", Provided: "
                            + brdId));
              }
              return Mono.just(true);
            });
  }

  @Override
  public Mono<ResponseEntity<Api<WalletronUsersResponse>>> createWalletronUsersFromFile(
      FilePart filePart, String walletronId, String brdId) {
    return readFileContent(filePart)
        .flatMap(
            fileContent ->
                processFileAndCreateUsers(filePart.filename(), fileContent, walletronId, brdId))
        .onErrorMap(this::handleError);
  }

  private Mono<byte[]> readFileContent(FilePart filePart) {
    return filePart
        .content()
        .map(
            dataBuffer -> {
              byte[] bytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(bytes);
              return bytes;
            })
        .reduce(this::combineByteArrays)
        .switchIfEmpty(Mono.error(new BadRequestException("File content is empty")));
  }

  private byte[] combineByteArrays(byte[] array1, byte[] array2) {
    byte[] combined = new byte[array1.length + array2.length];
    System.arraycopy(array1, 0, combined, 0, array1.length);
    System.arraycopy(array2, 0, combined, array1.length, array2.length);
    return combined;
  }

  private Mono<ResponseEntity<Api<WalletronUsersResponse>>> processFileAndCreateUsers(
      String filename, byte[] fileContent, String walletronId, String brdId) {

    String fileExtension = getFileExtension(filename);

    return switch (fileExtension.toLowerCase()) {
      case "csv" ->
          fileProcessorUtil
              .processCsvFile(fileContent)
              .flatMap(
                  users -> createUsersFromFileDataWithDuplicateCheck(users, walletronId, brdId));
      case "xlsx" ->
          fileProcessorUtil
              .processExcelFile(fileContent)
              .flatMap(
                  users -> createUsersFromFileDataWithDuplicateCheck(users, walletronId, brdId));
      default ->
          Mono.error(
              new BadRequestException(
                  "Unsupported file format. Only CSV and XLSX files are supported."));
    };
  }

  private String getFileExtension(String filename) {
    if (filename == null || !filename.contains(".")) {
      return "";
    }
    return filename.substring(filename.lastIndexOf('.') + 1);
  }

  private Mono<ResponseEntity<Api<WalletronUsersResponse>>>
      createUsersFromFileDataWithDuplicateCheck(
          List<FileProcessorUtil.FileUserData> fileUserDataList, String walletronId, String brdId) {

    return Flux.fromIterable(fileUserDataList)
        .map(
            fileUserData ->
                new WalletronUsersRequest.UserData(
                    fileUserData.getName(), fileUserData.getEmail(), fileUserData.getRole()))
        .collectList()
        .flatMap(
            userDataList -> processFileUsersWithDuplicateCheck(userDataList, walletronId, brdId));
  }

  private Mono<ResponseEntity<Api<WalletronUsersResponse>>> processFileUsersWithDuplicateCheck(
      List<WalletronUsersRequest.UserData> userDataList, String walletronId, String brdId) {

    return Flux.fromIterable(userDataList)
        .index()
        .collectList()
        .flatMap(
            indexedUsers -> {
              // Process duplicates in request based on email only
              Map<String, Long> emailToFirstIndex = new LinkedHashMap<>();
              List<WalletronUsersRequest.UserData> uniqueUsers = new ArrayList<>();
              List<String> requestDuplicateEmails = new ArrayList<>();

              for (var indexedUser : indexedUsers) {
                String email = indexedUser.getT2().getEmail().toLowerCase();
                Long currentIndex = indexedUser.getT1();

                Long firstIndex = emailToFirstIndex.get(email);
                if (firstIndex == null) {
                  // First occurrence in request
                  emailToFirstIndex.put(email, currentIndex);
                  uniqueUsers.add(indexedUser.getT2());
                } else {
                  // Duplicate occurrence in request
                  requestDuplicateEmails.add(indexedUser.getT2().getEmail());
                }
              }

              // Check if walletronId exists in walletron_users collection
              return walletronUsersRepository
                  .findByWalletronId(walletronId)
                  .collectList()
                  .flatMap(
                      existingUsers -> {
                        if (existingUsers.isEmpty()) {
                          // No existing users for this walletronId, create all users directly
                          log.info(
                              "No existing users found for walletronId: {}. Creating all {} users.",
                              walletronId,
                              uniqueUsers.size());
                          return createAllUsers(
                              uniqueUsers,
                              walletronId,
                              brdId,
                              requestDuplicateEmails,
                              userDataList.size());
                        } else {
                          // Existing users found, check line by line
                          log.info(
                              "Found {} existing users for walletronId: {}. Checking line by line.",
                              existingUsers.size(),
                              walletronId);
                          return processUsersLineByLine(
                              uniqueUsers,
                              existingUsers,
                              walletronId,
                              brdId,
                              requestDuplicateEmails,
                              userDataList.size());
                        }
                      });
            });
  }

  private Mono<ResponseEntity<Api<WalletronUsersResponse>>> createAllUsers(
      List<WalletronUsersRequest.UserData> uniqueUsers,
      String walletronId,
      String brdId,
      List<String> requestDuplicateEmails,
      int totalProcessed) {

    LocalDateTime now = LocalDateTime.now();

    return Flux.fromIterable(uniqueUsers)
        .map(userData -> createWalletronUser(userData, walletronId, brdId, now))
        .flatMap(this::saveUser, 10)
        .collectList()
        .flatMap(
            savedUsers ->
                // Update walletron collection's aciWalletronAgentPortal
                updateWalletronAgentPortal(savedUsers, walletronId)
                    .thenReturn(
                        buildWalletronUsersResponse(
                            savedUsers,
                            requestDuplicateEmails,
                            new ArrayList<>(),
                            totalProcessed)));
  }

  private Mono<ResponseEntity<Api<WalletronUsersResponse>>> processUsersLineByLine(
      List<WalletronUsersRequest.UserData> uniqueUsers,
      List<WalletronUsers> existingUsers,
      String walletronId,
      String brdId,
      List<String> requestDuplicateEmails,
      int totalProcessed) {

    // Create a set of existing users for quick lookup (name, email, role combination)
    Set<String> existingUserKeys =
        existingUsers.stream()
            .map(user -> createUserKey(user.getName(), user.getEmail(), user.getRole()))
            .collect(Collectors.toSet());

    List<WalletronUsersRequest.UserData> newUsers = new ArrayList<>();
    List<String> databaseDuplicateEmails = new ArrayList<>();

    // Check each user from the file line by line
    for (WalletronUsersRequest.UserData userData : uniqueUsers) {
      String userKey = createUserKey(userData.getName(), userData.getEmail(), userData.getRole());

      if (existingUserKeys.contains(userKey)) {
        // User already exists (same name, email, role), skip to next line
        databaseDuplicateEmails.add(userData.getEmail());
        log.debug("Skipping duplicate user: {}", userKey);
      } else {
        // New user, add to list
        newUsers.add(userData);
        log.debug("Adding new user: {}", userKey);
      }
    }

    if (newUsers.isEmpty()) {
      // All users already exist
      return Mono.error(
          new IllegalStateException(
              "All user details already exist in the system for this walletronId"));
    }

    LocalDateTime now = LocalDateTime.now();

    return Flux.fromIterable(newUsers)
        .map(userData -> createWalletronUser(userData, walletronId, brdId, now))
        .flatMap(this::saveUser, 10)
        .collectList()
        .flatMap(
            savedUsers ->
                // Update walletron collection's aciWalletronAgentPortal
                updateWalletronAgentPortal(savedUsers, walletronId)
                    .thenReturn(
                        buildWalletronUsersResponse(
                            savedUsers,
                            requestDuplicateEmails,
                            databaseDuplicateEmails,
                            totalProcessed)));
  }

  private String createUserKey(String name, String email, String role) {
    return name.toLowerCase() + "|" + email.toLowerCase() + "|" + role.toLowerCase();
  }

  private ResponseEntity<Api<WalletronUsersResponse>> buildWalletronUsersResponse(
      List<WalletronUsers> savedUsers,
      List<String> requestDuplicateEmails,
      List<String> databaseDuplicateEmails,
      int totalProcessed) {

    List<WalletronUsersResponse.SavedUser> savedUserResponses =
        savedUsers.stream().map(this::mapToSavedUserResponse).toList();

    // Combine all duplicate emails
    List<String> allDuplicateEmails = new ArrayList<>();
    allDuplicateEmails.addAll(requestDuplicateEmails);
    allDuplicateEmails.addAll(databaseDuplicateEmails);

    WalletronUsersResponse response = new WalletronUsersResponse();
    response.setSavedUsers(savedUserResponses);
    response.setDuplicateEmails(allDuplicateEmails);
    response.setTotalProcessed(totalProcessed);
    response.setTotalDuplicates(allDuplicateEmails.size());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new Api<>(
                SUCCESS_STATUS,
                "WalletronUsers processed successfully",
                Optional.of(response),
                Optional.empty()));
  }

  private Throwable handleError(Throwable ex) {
    if (ex instanceof BadRequestException
        || ex instanceof NotFoundException
        || ex instanceof AlreadyExistException
        || ex instanceof AccessDeniedException) {
      return ex;
    } else if (ex instanceof DuplicateKeyException) {
      return new AlreadyExistException("Already exist for given details");
    } else if (ex instanceof Exception && ex.getMessage().startsWith("Something went wrong:")) {
      return ex;
    }
    return new Exception("Something went wrong: " + ex.getMessage());
  }

  private WalletronUsersResponse.SavedUser mapToSavedUserResponse(WalletronUsers walletronUser) {
    WalletronUsersResponse.SavedUser savedUser = new WalletronUsersResponse.SavedUser();
    savedUser.setId(walletronUser.getId());
    savedUser.setName(walletronUser.getName());
    savedUser.setEmail(walletronUser.getEmail());
    savedUser.setRole(walletronUser.getRole());
    savedUser.setWalletronId(walletronUser.getWalletronId());
    savedUser.setBrdId(walletronUser.getBrdId());
    savedUser.setCreatedAt(walletronUser.getCreatedAt());
    return savedUser;
  }

  private Mono<ResponseEntity<Api<WalletronUsersResponse>>> processWalletronUsersRequest(
      WalletronUsersRequest req) {

    return Flux.fromIterable(req.getUsers())
        .index()
        .collectList()
        .flatMap(
            indexedUsers -> {
              // Process duplicates in request based on email only
              Map<String, Long> emailToFirstIndex = new LinkedHashMap<>();
              List<WalletronUsersRequest.UserData> uniqueUsers = new ArrayList<>();
              List<String> requestDuplicateEmails = new ArrayList<>();

              for (var indexedUser : indexedUsers) {
                String email = indexedUser.getT2().getEmail().toLowerCase();
                Long currentIndex = indexedUser.getT1();

                Long firstIndex = emailToFirstIndex.get(email);
                if (firstIndex == null) {
                  // First occurrence in request
                  emailToFirstIndex.put(email, currentIndex);
                  uniqueUsers.add(indexedUser.getT2());
                } else {
                  // Duplicate occurrence in request
                  requestDuplicateEmails.add(indexedUser.getT2().getEmail());
                }
              }

              LocalDateTime now = LocalDateTime.now();

              return Flux.fromIterable(uniqueUsers)
                  .map(
                      userData ->
                          createWalletronUser(userData, req.getWalletronId(), req.getBrdId(), now))
                  .flatMap(this::saveUser, 10) // Parallel processing with concurrency of 10
                  .collectList()
                  .flatMap(
                      savedUsers ->
                          // Update walletron collection's aciWalletronAgentPortal
                          updateWalletronAgentPortal(savedUsers, req.getWalletronId())
                              .thenReturn(
                                  buildWalletronUsersResponse(
                                      savedUsers,
                                      requestDuplicateEmails,
                                      new ArrayList<>(),
                                      req.getUsers().size())));
            });
  }

  private WalletronUsers createWalletronUser(
      WalletronUsersRequest.UserData userData,
      String walletronId,
      String brdId,
      LocalDateTime now) {
    WalletronUsers walletronUser = new WalletronUsers();
    walletronUser.setName(userData.getName());
    walletronUser.setEmail(userData.getEmail().toLowerCase()); // Normalize email to lowercase
    walletronUser.setRole(userData.getRole());
    walletronUser.setWalletronId(walletronId);
    walletronUser.setBrdId(brdId);
    walletronUser.setCreatedAt(now);
    walletronUser.setUpdatedAt(now);
    return walletronUser;
  }

  private Mono<WalletronUsers> saveUser(WalletronUsers user) {
    return walletronUsersRepository
        .save(user)
        .onErrorResume(DuplicateKeyException.class, ex -> Mono.empty());
  }

  private Mono<Void> updateWalletronAgentPortal(
      List<WalletronUsers> savedUsers, String walletronId) {
    String cleanWalletronId = walletronId.replaceAll(QUOTES_REGEX, "");

    return walletronRepository
        .findById(cleanWalletronId)
        .flatMap(
            walletron -> {
              // Get existing agent portal or create new one
              ACIWalletronAgentPortal agentPortal = walletron.getAciWalletronAgentPortal();
              if (agentPortal == null) {
                agentPortal = new ACIWalletronAgentPortal();
                agentPortal.setWalletronAgentPortal(new ArrayList<>());
              }

              List<ACIWalletronAgentPortal.WalletronAgentPortal> existingUsers =
                  agentPortal.getWalletronAgentPortal() != null
                      ? agentPortal.getWalletronAgentPortal()
                      : new ArrayList<>();

              // Add new users to agent portal
              for (WalletronUsers savedUser : savedUsers) {
                ACIWalletronAgentPortal.WalletronAgentPortal agentUser =
                    new ACIWalletronAgentPortal.WalletronAgentPortal();
                agentUser.setName(savedUser.getName());
                agentUser.setEmailAddress(savedUser.getEmail());
                agentUser.setRole(savedUser.getRole());
                existingUsers.add(agentUser);
              }

              agentPortal.setWalletronAgentPortal(existingUsers);
              agentPortal.setSectionStatus("Completed");

              walletron.setAciWalletronAgentPortal(agentPortal);
              walletron.setUpdatedAt(LocalDateTime.now());

              return walletronRepository.save(walletron).then();
            })
        .onErrorResume(
            e -> {
              log.error("Failed to update walletron agent portal: {}", e.getMessage());
              return Mono.empty(); // Don't fail the entire operation if this fails
            });
  }

  public Mono<List<Map<String, Object>>> getAllWalletronDocumentsForDebug() {
    return walletronRepository
        .findAll()
        .map(
            walletron -> {
              Map<String, Object> doc = new HashMap<>();
              doc.put("_id", walletron.getWalletronId());
              doc.put("walletronId", walletron.getWalletronId());
              doc.put("brdId", walletron.getBrdId());
              doc.put("brdName", walletron.getBrdName());
              return doc;
            })
        .collectList()
        .doOnNext(docs -> log.info("Found {} walletron documents: {}", docs.size(), docs));
  }
}
