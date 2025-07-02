package com.aci.smart_onboarding.service.implementation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


import com.aci.smart_onboarding.dto.WalletronRequest;
import com.aci.smart_onboarding.dto.WalletronResponse;

import com.aci.smart_onboarding.mapper.DtoModelMapper;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.Walletron;

import com.aci.smart_onboarding.repository.BRDRepository;
import com.aci.smart_onboarding.repository.WalletronRepository;
import com.aci.smart_onboarding.repository.WalletronUsersRepository;
import com.aci.smart_onboarding.security.service.BRDSecurityService;
import com.aci.smart_onboarding.service.IFileService;
import com.aci.smart_onboarding.util.FileProcessorUtil;
import com.aci.smart_onboarding.util.walletron.AciCash;
import com.aci.smart_onboarding.util.walletron.DataExchange;
import com.aci.smart_onboarding.util.walletron.EnrollmentStrategy;
import com.aci.smart_onboarding.util.walletron.EnrollmentUrls;
import com.aci.smart_onboarding.util.walletron.NotificationsOptions;
import com.aci.smart_onboarding.util.walletron.SiteConfiguration;
import com.aci.smart_onboarding.util.walletron.TargetedCommunication;
import com.aci.smart_onboarding.util.walletron.WalletronApprovals;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Method;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.Arrays;

import com.aci.smart_onboarding.util.walletron.ACIWalletronAgentPortal;

@ExtendWith(MockitoExtension.class)
class WalletronServiceTest {

  @Mock private WalletronRepository walletronRepository;

  @Mock private WalletronUsersRepository walletronUsersRepository;

  @Mock private FileProcessorUtil fileProcessorUtil;

  @Mock private BRDSecurityService securityService;

  @Mock private DtoModelMapper dtoModelMapper;

  @Mock private ObjectMapper objectMapper;

  @Mock private BRDRepository brdRepository;

  @Mock private IFileService fileService;

  @InjectMocks private WalletronService walletronService;

  private WalletronRequest walletronRequest;
  private Walletron walletron;
  private WalletronResponse walletronResponse;
  private BRD brd;

  private static final String QUOTES_REGEX = "^\"|\"$";

  @BeforeEach
  void setUp() {
    walletronRequest = new WalletronRequest();
    walletronRequest.setWalletronId("W123");
    walletronRequest.setBrdId("B123");
    walletronRequest.setBrdName("Test BRD");

    walletron = new Walletron();
    walletron.setWalletronId("W123");
    walletron.setBrdId("B123");
    walletron.setBrdName("Test BRD");

    walletronResponse = new WalletronResponse();
    walletronResponse.setWalletronId("W123");
    walletronResponse.setBrdId("B123");
    walletronResponse.setBrdName("Test BRD");

    brd = new BRD();
    brd.setBrdId("B123");
  }

  @Test
  void createWalletron_Success() {
    when(walletronRepository.save(any(Walletron.class))).thenReturn(Mono.just(walletron));
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.just(brd));
    when(brdRepository.save(any(BRD.class))).thenReturn(Mono.just(brd));
    when(dtoModelMapper.mapToWalletronResponse(any(Walletron.class))).thenReturn(walletronResponse);

    StepVerifier.create(walletronService.createWalletron(walletronRequest))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.CREATED, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("SUCCESS", response.getBody().getStatus());
            })
        .verifyComplete();

    verify(walletronRepository).save(any(Walletron.class));
    verify(brdRepository).findByBrdId(anyString());
    verify(brdRepository).save(any(BRD.class));
  }

  @Test
  void createWalletron_NullRequest() {
    StepVerifier.create(walletronService.createWalletron(null))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              assertEquals("ERROR", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void createWalletron_BRDNotFound() {
    when(walletronRepository.save(any(Walletron.class))).thenReturn(Mono.just(walletron));
    when(brdRepository.findByBrdId(anyString())).thenReturn(Mono.empty());

    StepVerifier.create(walletronService.createWalletron(walletronRequest))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertEquals("ERROR", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void getWalletronById_Success() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    when(dtoModelMapper.mapToWalletronResponse(any(Walletron.class))).thenReturn(walletronResponse);

    StepVerifier.create(walletronService.getWalletronById("W123"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("SUCCESS", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void getWalletronById_NotFound() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.empty());

    StepVerifier.create(walletronService.getWalletronById("W123"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              assertEquals("ERROR", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void updateWalletronPartially_Success() {
    Map<String, Object> updateFields = Map.of("brdName", "Updated BRD");
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    when(walletronRepository.save(any(Walletron.class))).thenReturn(Mono.just(walletron));
    when(dtoModelMapper.mapToWalletronResponse(any(Walletron.class))).thenReturn(walletronResponse);
    when(objectMapper.valueToTree(any()))
        .thenReturn(new com.fasterxml.jackson.databind.node.ObjectNode(null));
    when(objectMapper.convertValue(any(), eq(Walletron.class))).thenReturn(walletron);

    StepVerifier.create(
            walletronService.updateWalletronPartiallyWithOrderedOperations("W123", updateFields))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("SUCCESS", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void updateWalletronPartially_InvalidFields() {
    Map<String, Object> updateFields = Map.of("invalidField", "value");
    assertThrows(
        IllegalArgumentException.class,
        () -> walletronService.updateWalletronPartiallyWithOrderedOperations("W123", updateFields));
  }

  @Test
  void updateWalletronPartially_NotFound() {
    Map<String, Object> updateFields = Map.of("brdName", "Updated BRD");
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.empty());

    StepVerifier.create(
            walletronService.updateWalletronPartiallyWithOrderedOperations("W123", updateFields))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              assertEquals("ERROR", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void getWalletronSectionById_Success() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    // Set up a minimal valid SiteConfiguration
    SiteConfiguration siteConfig = new SiteConfiguration();
    siteConfig.setBrandLogo("brandLogo");
    siteConfig.setThumbnailImage("thumbnailImage");
    siteConfig.setClientName("clientName");
    siteConfig.setLogo("logo");
    siteConfig.setBackPassInformation(
        java.util.List.of(new com.aci.smart_onboarding.util.walletron.subsections.BackPassField()));
    walletron.setSiteConfiguration(siteConfig);

    StepVerifier.create(walletronService.getWalletronSectionById("W123", "siteConfiguration"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              assertTrue(response.getBody().containsKey("siteConfiguration"));
            })
        .verifyComplete();
  }

  @Test
  void getWalletronSectionById_InvalidSection() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));

    StepVerifier.create(walletronService.getWalletronSectionById("W123", "invalidSection"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              assertNotNull(response.getBody());
            })
        .verifyComplete();
  }

  @Test
  void getWalletronSectionById_NotFound() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.empty());

    StepVerifier.create(walletronService.getWalletronSectionById("W123", "siteConfiguration"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              assertNotNull(response.getBody());
            })
        .verifyComplete();
  }

  @Test
  void createWalletron_AccessDenied() {
    when(walletronRepository.save(any(Walletron.class)))
        .thenThrow(new AccessDeniedException("Forbidden"));
    StepVerifier.create(walletronService.createWalletron(walletronRequest))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
              assertEquals("ERROR", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void createWalletron_InternalServerError() {
    when(walletronRepository.save(any(Walletron.class)))
        .thenThrow(new RuntimeException("DB error"));
    StepVerifier.create(walletronService.createWalletron(walletronRequest))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertEquals("ERROR", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void getWalletronSectionById_NotificationsOptions() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    NotificationsOptions notificationsOptions = new NotificationsOptions();
    walletron.setNotificationsOptions(notificationsOptions);
    StepVerifier.create(walletronService.getWalletronSectionById("W123", "notificationsOptions"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              assertTrue(response.getBody().containsKey("notificationsOptions"));
            })
        .verifyComplete();
  }

  @Test
  void getWalletronSectionById_AciWalletronAgentPortal() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    ACIWalletronAgentPortal agentPortal = mock(ACIWalletronAgentPortal.class);
    walletron.setAciWalletronAgentPortal(agentPortal);
    StepVerifier.create(walletronService.getWalletronSectionById("W123", "aciWalletronAgentPortal"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              assertTrue(response.getBody().containsKey("aciWalletronAgentPortal"));
            })
        .verifyComplete();
  }

  @Test
  void getWalletronSectionById_AciWalletronDataExchange() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    DataExchange dataExchange = mock(DataExchange.class);
    walletron.setAciWalletronDataExchange(dataExchange);
    StepVerifier.create(
            walletronService.getWalletronSectionById("W123", "aciWalletronDataExchange"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              assertTrue(response.getBody().containsKey("aciWalletronDataExchange"));
            })
        .verifyComplete();
  }

  @Test
  void getWalletronSectionById_AciWalletronEnrollmentStrategy() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    EnrollmentStrategy enrollmentStrategy = mock(EnrollmentStrategy.class);
    walletron.setAciWalletronEnrollmentStrategy(enrollmentStrategy);
    StepVerifier.create(
            walletronService.getWalletronSectionById("W123", "aciWalletronEnrollmentStrategy"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              assertTrue(response.getBody().containsKey("aciWalletronEnrollmentStrategy"));
            })
        .verifyComplete();
  }

  @Test
  void getWalletronSectionById_EnrollmentUrls() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    EnrollmentUrls enrollmentUrls = mock(EnrollmentUrls.class);
    walletron.setEnrollmentUrl(enrollmentUrls);
    StepVerifier.create(walletronService.getWalletronSectionById("W123", "enrollmentUrls"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              assertTrue(response.getBody().containsKey("enrollmentUrls"));
            })
        .verifyComplete();
  }

  @Test
  void getWalletronSectionById_TargetedCommunication() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    TargetedCommunication targetedCommunication = mock(TargetedCommunication.class);
    walletron.setTargetedCommunication(targetedCommunication);
    StepVerifier.create(walletronService.getWalletronSectionById("W123", "targetedCommunication"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              assertTrue(response.getBody().containsKey("targetedCommunication"));
            })
        .verifyComplete();
  }

  @Test
  void getWalletronSectionById_AciCash() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    AciCash aciCash = mock(AciCash.class);
    walletron.setAciCash(aciCash);
    StepVerifier.create(walletronService.getWalletronSectionById("W123", "aciCash"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              assertTrue(response.getBody().containsKey("aciCash"));
            })
        .verifyComplete();
  }

  @Test
  void getWalletronSectionById_Approvals() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    WalletronApprovals approvals = mock(WalletronApprovals.class);
    walletron.setWalletronApprovals(approvals);
    StepVerifier.create(walletronService.getWalletronSectionById("W123", "approvals"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              assertTrue(response.getBody().containsKey("approvals"));
            })
        .verifyComplete();
  }

  @Test
  void updateWalletronPartially_EmptyFields() {
    Map<String, Object> updateFields = Map.of();
    assertThrows(
        IllegalArgumentException.class,
        () -> walletronService.updateWalletronPartiallyWithOrderedOperations("W123", updateFields));
  }

  @Test
  void updateWalletronPartially_NullFields() {
    assertThrows(
        IllegalArgumentException.class,
        () -> walletronService.updateWalletronPartiallyWithOrderedOperations("W123", null));
  }

  @Test
  void updateWalletronPartially_ComplexObjectMerge() {
    Map<String, Object> updateFields =
        Map.of(
            "aciCash",
            Map.of(
                "numberOfAccounts",
                "10",
                "aciCashPaymentFees",
                List.of(
                    Map.of(
                        "viewValue", "Test Fee",
                        "selected", true,
                        "indeterminate", false))));

    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    when(walletronRepository.save(any(Walletron.class))).thenReturn(Mono.just(walletron));
    when(dtoModelMapper.mapToWalletronResponse(any(Walletron.class))).thenReturn(walletronResponse);
    when(objectMapper.valueToTree(any()))
        .thenReturn(new com.fasterxml.jackson.databind.node.ObjectNode(null));
    when(objectMapper.convertValue(any(), eq(Walletron.class))).thenReturn(walletron);

    StepVerifier.create(
            walletronService.updateWalletronPartiallyWithOrderedOperations("W123", updateFields))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("SUCCESS", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void updateWalletronPartially_ArrayMerge() {
    Map<String, Object> updateFields =
        Map.of(
            "notificationsOptions",
            Map.of(
                "notificationOptions",
                List.of(
                    Map.of(
                        "isChecked", "true",
                        "notificationType", "Email"))));

    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    when(walletronRepository.save(any(Walletron.class))).thenReturn(Mono.just(walletron));
    when(dtoModelMapper.mapToWalletronResponse(any(Walletron.class))).thenReturn(walletronResponse);
    when(objectMapper.valueToTree(any()))
        .thenReturn(new com.fasterxml.jackson.databind.node.ObjectNode(null));
    when(objectMapper.convertValue(any(), eq(Walletron.class))).thenReturn(walletron);

    StepVerifier.create(
            walletronService.updateWalletronPartiallyWithOrderedOperations("W123", updateFields))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("SUCCESS", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void updateWalletronPartially_InternalServerError() {
    Map<String, Object> updateFields = Map.of("brdName", "Updated BRD");
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    when(objectMapper.valueToTree(any())).thenThrow(new RuntimeException("Mapping error"));

    StepVerifier.create(
            walletronService.updateWalletronPartiallyWithOrderedOperations("W123", updateFields))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertEquals("ERROR", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void getWalletronSectionById_NullSection() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));

    StepVerifier.create(walletronService.getWalletronSectionById("W123", null))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertNotNull(response.getBody());
            })
        .verifyComplete();
  }

  @Test
  void getWalletronSectionById_EmptySection() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));

    StepVerifier.create(walletronService.getWalletronSectionById("W123", ""))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              assertNotNull(response.getBody());
            })
        .verifyComplete();
  }

  @Test
  void getWalletronSectionById_RepositoryError() {
    when(walletronRepository.findByWalletronId(anyString()))
        .thenReturn(Mono.error(new RuntimeException("DB error")));

    StepVerifier.create(walletronService.getWalletronSectionById("W123", "siteConfiguration"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertNotNull(response.getBody());
            })
        .verifyComplete();
  }

  @Test
  void createWalletron_ValidationError() {
    walletronRequest.setBrdId(null);
    StepVerifier.create(walletronService.createWalletron(walletronRequest))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertEquals("ERROR", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void createWalletron_SaveError() {
    when(walletronRepository.save(any(Walletron.class)))
        .thenReturn(Mono.error(new RuntimeException("Save error")));
    StepVerifier.create(walletronService.createWalletron(walletronRequest))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertEquals("ERROR", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void getWalletronById_RepositoryError() {
    when(walletronRepository.findByWalletronId(anyString()))
        .thenReturn(Mono.error(new RuntimeException("DB error")));

    StepVerifier.create(walletronService.getWalletronById("W123"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertEquals("ERROR", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void testDeepMerge_NonObjectNode() throws Exception {
    // If mainNode is not an ObjectNode, should return updateNode
    Method deepMerge =
        WalletronService.class.getDeclaredMethod("deepMerge", JsonNode.class, JsonNode.class);
    deepMerge.setAccessible(true);
    JsonNode mainNode = objectMapper.readTree("[1,2,3]");
    JsonNode updateNode = objectMapper.readTree("[4,5,6]");
    JsonNode result = (JsonNode) deepMerge.invoke(walletronService, mainNode, updateNode);
    assertEquals(updateNode, result);
  }

  @Test
  void testDeepMerge_ObjectNodeMerge() throws Exception {
    Method deepMerge =
        WalletronService.class.getDeclaredMethod("deepMerge", JsonNode.class, JsonNode.class);
    deepMerge.setAccessible(true);
    ObjectNode mainNode = JsonNodeFactory.instance.objectNode();
    mainNode.put("a", 1);
    ObjectNode updateNode = JsonNodeFactory.instance.objectNode();
    updateNode.put("a", 2);
    JsonNode result = (JsonNode) deepMerge.invoke(walletronService, mainNode, updateNode);
    assertEquals(2, result.get("a").asInt());
  }

  @Test
  void testMapToWalletron_AllFields() throws Exception {
    WalletronRequest req = new WalletronRequest();
    req.setWalletronId("wid");
    req.setBrdId("bid");
    req.setBrdName("bname");
    req.setWalletronEnabled(true);
    req.setSiteConfiguration(new SiteConfiguration());
    req.setNotificationsOptions(new NotificationsOptions());
    req.setAciWalletronAgentPortal(new ACIWalletronAgentPortal());
    req.setAciWalletronDataExchange(new DataExchange());
    req.setAciWalletronEnrollmentStrategy(new EnrollmentStrategy());
    req.setEnrollmentUrls(new EnrollmentUrls());
    req.setTargetedCommunication(new TargetedCommunication());
    req.setAciCash(new AciCash());
    req.setWalletronApprovals(new WalletronApprovals());
    WalletronService service =
        new WalletronService(
            walletronRepository,
            walletronUsersRepository,
            fileProcessorUtil,
            securityService,
            dtoModelMapper,
            objectMapper,
            brdRepository,
            fileService);
    Walletron w =
        (Walletron)
            getPrivateMethod(service, "mapToWalletron", WalletronRequest.class)
                .invoke(service, req);
    assertEquals("wid", w.getWalletronId());
    assertEquals("bid", w.getBrdId());
    assertEquals("bname", w.getBrdName());
    assertTrue(w.getWalletronEnabled());
  }

  @Test
  void testConstructor_InitializesFields() {
    WalletronService service =
        new WalletronService(
            walletronRepository,
            walletronUsersRepository,
            fileProcessorUtil,
            securityService,
            dtoModelMapper,
            objectMapper,
            brdRepository,
            fileService);
    assertNotNull(service);
  }

  @Test
  void getWalletronImages_Success() {
    // Setup
    SiteConfiguration siteConfig = new SiteConfiguration();
    siteConfig.setBrandLogo("https://example.com/brand.png");
    siteConfig.setIconImage("https://example.com/icon.png");
    siteConfig.setStripImage("https://example.com/strip.png");
    siteConfig.setThumbnailImage("https://example.com/thumbnail.png");

    ACIWalletronAgentPortal agentPortal = new ACIWalletronAgentPortal();
    agentPortal.setUploadFile("https://example.com/agent-portal.csv");
    agentPortal.setSectionStatus("Completed");

    walletron.setSiteConfiguration(siteConfig);
    walletron.setAciWalletronAgentPortal(agentPortal);

    // Create a mock JsonNode that returns the image URLs
    JsonNode mockSiteConfigNode = mock(JsonNode.class);
    JsonNode mockBrandLogoNode = mock(JsonNode.class);
    JsonNode mockIconImageNode = mock(JsonNode.class);
    JsonNode mockStripImageNode = mock(JsonNode.class);
    JsonNode mockThumbnailImageNode = mock(JsonNode.class);

    when(mockBrandLogoNode.asText()).thenReturn("https://example.com/brand.png");
    when(mockIconImageNode.asText()).thenReturn("https://example.com/icon.png");
    when(mockStripImageNode.asText()).thenReturn("https://example.com/strip.png");
    when(mockThumbnailImageNode.asText()).thenReturn("https://example.com/thumbnail.png");

    when(mockSiteConfigNode.get("brandLogo")).thenReturn(mockBrandLogoNode);
    when(mockSiteConfigNode.get("iconImage")).thenReturn(mockIconImageNode);
    when(mockSiteConfigNode.get("stripImage")).thenReturn(mockStripImageNode);
    when(mockSiteConfigNode.get("thumbnailImage")).thenReturn(mockThumbnailImageNode);
    when(mockSiteConfigNode.get("aciWalletronAgentPortal")).thenReturn(null);

    Map<String, byte[]> base64Images = new HashMap<>();
    base64Images.put("https://example.com/brand.png", "brand".getBytes());
    base64Images.put("https://example.com/icon.png", "icon".getBytes());
    base64Images.put("https://example.com/strip.png", "strip".getBytes());
    base64Images.put("https://example.com/thumbnail.png", "thumbnail".getBytes());

    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    when(objectMapper.valueToTree(any())).thenReturn(mockSiteConfigNode);
    when(fileService.getBase64EncodedImages(anyList())).thenReturn(Mono.just(base64Images));

    // Test
    StepVerifier.create(walletronService.getWalletronImages("W123"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("SUCCESS", response.getBody().getStatus());
              assertEquals(4, response.getBody().getData().get().getImages().size());
            })
        .verifyComplete();

    verify(walletronRepository).findByWalletronId("W123");
    verify(fileService).getBase64EncodedImages(anyList());
  }

  @Test
  void getWalletronImages_NotFound() {
    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.empty());

    StepVerifier.create(walletronService.getWalletronImages("W123"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              assertEquals("ERROR", response.getBody().getStatus());
              assertTrue(response.getBody().getData().isEmpty());
            })
        .verifyComplete();

    verify(walletronRepository).findByWalletronId("W123");
    verify(fileService, never()).getBase64EncodedImages(anyList());
  }

  @Test
  void getWalletronImages_NoImages() {
    walletron.setSiteConfiguration(new SiteConfiguration());
    walletron.setAciWalletronAgentPortal(new ACIWalletronAgentPortal());

    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    when(objectMapper.valueToTree(any())).thenReturn(mock(JsonNode.class));
    when(fileService.getBase64EncodedImages(anyList())).thenReturn(Mono.just(new HashMap<>()));

    StepVerifier.create(walletronService.getWalletronImages("W123"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().getData().isPresent());
              assertEquals("SUCCESS", response.getBody().getStatus());
              assertTrue(response.getBody().getData().get().getImages().isEmpty());
            })
        .verifyComplete();

    verify(walletronRepository).findByWalletronId("W123");
    verify(fileService).getBase64EncodedImages(anyList());
  }

  @Test
  void getWalletronImages_FileServiceError() {
    SiteConfiguration siteConfig = new SiteConfiguration();
    siteConfig.setBrandLogo("https://example.com/brand.png");
    walletron.setSiteConfiguration(siteConfig);

    when(walletronRepository.findByWalletronId(anyString())).thenReturn(Mono.just(walletron));
    when(objectMapper.valueToTree(any())).thenReturn(mock(JsonNode.class));
    when(fileService.getBase64EncodedImages(anyList()))
        .thenReturn(Mono.error(new RuntimeException("File service error")));

    StepVerifier.create(walletronService.getWalletronImages("W123"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertEquals("ERROR", response.getBody().getStatus());
              assertTrue(response.getBody().getData().isEmpty());
            })
        .verifyComplete();

    verify(walletronRepository).findByWalletronId("W123");
    verify(fileService).getBase64EncodedImages(anyList());
  }

  @Test
  void getWalletronImages_RepositoryError() {
    when(walletronRepository.findByWalletronId(anyString()))
        .thenReturn(Mono.error(new RuntimeException("DB error")));

    StepVerifier.create(walletronService.getWalletronImages("W123"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertEquals("ERROR", response.getBody().getStatus());
              assertTrue(response.getBody().getData().isEmpty());
            })
        .verifyComplete();

    verify(walletronRepository).findByWalletronId("W123");
    verify(fileService, never()).getBase64EncodedImages(anyList());
  }

  private Method getPrivateMethod(Object obj, String name, Class<?>... params) throws Exception {
    Method m = obj.getClass().getDeclaredMethod(name, params);
    m.setAccessible(true);
    return m;
  }

  // Additional test cases for 100% code coverage

  @Test
  void createWalletronUsers_Success() {
    com.aci.smart_onboarding.dto.WalletronUsersRequest request = createValidWalletronUsersRequest();
    com.aci.smart_onboarding.model.WalletronUsers savedUser = createMockWalletronUser();

    lenient().when(walletronRepository.findById(anyString())).thenReturn(Mono.just(walletron));
    lenient()
        .when(walletronUsersRepository.findByWalletronId(anyString()))
        .thenReturn(reactor.core.publisher.Flux.empty());
    when(walletronRepository.save(any())).thenReturn(Mono.just(walletron));

    when(walletronUsersRepository.save(any())).thenReturn(Mono.just(savedUser));

    StepVerifier.create(walletronService.createWalletronUsers(request))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.CREATED, response.getStatusCode());
              assertEquals("SUCCESS", response.getBody().getStatus());
              assertTrue(response.getBody().getData().isPresent());
            })
        .verifyComplete();
  }

  @Test
  void createWalletronUsers_NullRequest() {
    StepVerifier.create(walletronService.createWalletronUsers(null))
        .expectError(com.aci.smart_onboarding.exception.BadRequestException.class)
        .verify();
  }

  @Test
  void createWalletronUsers_EmptyUsersList() {
    com.aci.smart_onboarding.dto.WalletronUsersRequest request =
        new com.aci.smart_onboarding.dto.WalletronUsersRequest();
    request.setWalletronId("WAL-123");
    request.setBrdId("BRD-123");
    request.setUsers(Collections.emptyList());

    lenient().when(walletronRepository.findById(anyString())).thenReturn(Mono.just(walletron));
    lenient()
        .when(walletronUsersRepository.findByWalletronId(anyString()))
        .thenReturn(reactor.core.publisher.Flux.empty());
    when(walletronRepository.save(any())).thenReturn(Mono.just(walletron));

    StepVerifier.create(walletronService.createWalletronUsers(request))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.CREATED, response.getStatusCode());
              assertEquals("SUCCESS", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void createWalletronUsers_DuplicateEmails() {
    com.aci.smart_onboarding.dto.WalletronUsersRequest request = createRequestWithDuplicateEmails();
    com.aci.smart_onboarding.model.WalletronUsers savedUser = createMockWalletronUser();

    lenient().when(walletronRepository.findById(anyString())).thenReturn(Mono.just(walletron));
    lenient()
        .when(walletronUsersRepository.findByWalletronId(anyString()))
        .thenReturn(reactor.core.publisher.Flux.empty());
    when(walletronRepository.save(any())).thenReturn(Mono.just(walletron));

    when(walletronUsersRepository.save(any())).thenReturn(Mono.just(savedUser));

    StepVerifier.create(walletronService.createWalletronUsers(request))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.CREATED, response.getStatusCode());
              assertEquals("SUCCESS", response.getBody().getStatus());
              assertTrue(response.getBody().getData().isPresent());
              assertTrue(response.getBody().getData().get().getDuplicateEmails().size() > 0);
            })
        .verifyComplete();
  }

  @Test
  void createWalletronUsers_DuplicateKeyException() {
    com.aci.smart_onboarding.dto.WalletronUsersRequest request = createValidWalletronUsersRequest();

    lenient().when(walletronRepository.findById(anyString())).thenReturn(Mono.just(walletron));
    lenient()
        .when(walletronUsersRepository.findByWalletronId(anyString()))
        .thenReturn(reactor.core.publisher.Flux.empty());
    when(walletronRepository.save(any())).thenReturn(Mono.just(walletron));

    when(walletronUsersRepository.save(any()))
        .thenReturn(Mono.error(new org.springframework.dao.DuplicateKeyException("Duplicate key")));

    StepVerifier.create(walletronService.createWalletronUsers(request))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.CREATED, response.getStatusCode());
              assertEquals("SUCCESS", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void createWalletronUsersFromFile_CsvFile_Success() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    when(filePart.filename()).thenReturn("test.csv");

    // Create a simple DataBuffer without complex mocking
    byte[] testData = "name,email,role\nJohn Doe,john@example.com,USER".getBytes();
    org.springframework.core.io.buffer.DataBuffer dataBuffer =
        mock(org.springframework.core.io.buffer.DataBuffer.class);
    when(dataBuffer.readableByteCount()).thenReturn(testData.length);

    when(filePart.content()).thenReturn(reactor.core.publisher.Flux.just(dataBuffer));

    List<com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData> fileUsers =
        createMockFileUserDataList();
    when(fileProcessorUtil.processCsvFile(any())).thenReturn(Mono.just(fileUsers));
    when(walletronRepository.findById(anyString())).thenReturn(Mono.just(walletron));
    when(walletronUsersRepository.findByWalletronId(anyString()))
        .thenReturn(reactor.core.publisher.Flux.empty());
    when(walletronUsersRepository.save(any())).thenReturn(Mono.just(createMockWalletronUser()));

    StepVerifier.create(
            walletronService.createWalletronUsersFromFile(filePart, "WAL-123", "BRD-123"))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.CREATED, response.getStatusCode());
              assertEquals("SUCCESS", response.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void createWalletronUsersFromFile_ExcelFile_Success() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    when(filePart.filename()).thenReturn("test.xlsx");

    byte[] fileContent = "test excel content".getBytes();
    org.springframework.core.io.buffer.DataBuffer dataBuffer =
        new org.springframework.core.io.buffer.DefaultDataBufferFactory().wrap(fileContent);
    when(filePart.content()).thenReturn(reactor.core.publisher.Flux.just(dataBuffer));

    List<com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData> processedUsers =
        Arrays.asList(
            new com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData(
                "John Doe", "john@example.com", "USER"),
            new com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData(
                "Jane Smith", "jane@example.com", "ADMIN"));


      when(fileProcessorUtil.processExcelFile(fileContent)).thenReturn(Mono.just(processedUsers));
    lenient().when(walletronRepository.findById(anyString())).thenReturn(Mono.just(walletron));
    lenient()
        .when(walletronUsersRepository.findByWalletronId(anyString()))
        .thenReturn(reactor.core.publisher.Flux.empty());
    when(walletronRepository.save(any())).thenReturn(Mono.just(walletron));
    when(walletronUsersRepository.save(any())).thenReturn(Mono.just(createMockWalletronUser()));

    StepVerifier.create(walletronService.createWalletronUsersFromFile(filePart, "WAL-123", "BRD-123"))
        .assertNext(
            responseEntity -> {
              assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
              assertEquals("SUCCESS", responseEntity.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void createWalletronUsersFromFile_UnsupportedFileFormat() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    when(filePart.filename()).thenReturn("test.txt");

    byte[] fileContent = "test content".getBytes();
    org.springframework.core.io.buffer.DataBuffer dataBuffer =
        new org.springframework.core.io.buffer.DefaultDataBufferFactory().wrap(fileContent);
    when(filePart.content()).thenReturn(reactor.core.publisher.Flux.just(dataBuffer));

    StepVerifier.create(walletronService.createWalletronUsersFromFile(filePart, "WAL-123", "BRD-123"))
        .expectError(com.aci.smart_onboarding.exception.BadRequestException.class)
        .verify();
  }

  @Test
  void createWalletronUsersFromFile_EmptyFileContent() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    lenient().when(filePart.filename()).thenReturn("test.csv");
    lenient().when(filePart.content()).thenReturn(reactor.core.publisher.Flux.empty());

    StepVerifier.create(walletronService.createWalletronUsersFromFile(filePart, "WAL-123", "BRD-123"))
        .expectError(Exception.class)
        .verify();
  }

  @Test
  void createWalletronUsersFromFile_WithExistingUsers_DuplicateCheck() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    when(filePart.filename()).thenReturn("test.csv");

    byte[] fileContent = "test csv content".getBytes();
    org.springframework.core.io.buffer.DataBuffer dataBuffer =
        new org.springframework.core.io.buffer.DefaultDataBufferFactory().wrap(fileContent);
    when(filePart.content()).thenReturn(reactor.core.publisher.Flux.just(dataBuffer));

    List<com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData> processedUsers =
        Arrays.asList(
            new com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData(
                "John Doe", "john@example.com", "USER"),
            new com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData(
                "Jane Smith", "jane@example.com", "ADMIN"));

    // Create existing users in database
    com.aci.smart_onboarding.model.WalletronUsers existingUser = createMockWalletronUser();
    existingUser.setName("John Doe");
    existingUser.setEmail("john@example.com");
    existingUser.setRole("USER");


      when(fileProcessorUtil.processCsvFile(fileContent)).thenReturn(Mono.just(processedUsers));
    lenient().when(walletronRepository.findById(anyString())).thenReturn(Mono.just(walletron));
    lenient()
        .when(walletronUsersRepository.findByWalletronId(anyString()))
        .thenReturn(reactor.core.publisher.Flux.just(existingUser));
    when(walletronRepository.save(any())).thenReturn(Mono.just(walletron));
    when(walletronUsersRepository.save(any())).thenReturn(Mono.just(createMockWalletronUser()));

    StepVerifier.create(walletronService.createWalletronUsersFromFile(filePart, "WAL-123", "BRD-123"))
        .assertNext(
            responseEntity -> {
              assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
              assertEquals("SUCCESS", responseEntity.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void createWalletronUsersFromFile_AllUsersAlreadyExist() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    when(filePart.filename()).thenReturn("test.csv");

    byte[] fileContent = "test csv content".getBytes();
    org.springframework.core.io.buffer.DataBuffer dataBuffer =
        new org.springframework.core.io.buffer.DefaultDataBufferFactory().wrap(fileContent);
    when(filePart.content()).thenReturn(reactor.core.publisher.Flux.just(dataBuffer));

    List<com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData> processedUsers =
        Arrays.asList(
            new com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData(
                "John Doe", "john@example.com", "USER"));

    // Create existing user with same details
    com.aci.smart_onboarding.model.WalletronUsers existingUser = createMockWalletronUser();
    existingUser.setName("John Doe");
    existingUser.setEmail("john@example.com");
    existingUser.setRole("USER");

    when(fileProcessorUtil.processCsvFile(fileContent)).thenReturn(Mono.just(processedUsers));
    lenient().when(walletronRepository.findById(anyString())).thenReturn(Mono.just(walletron));
    lenient()
        .when(walletronUsersRepository.findByWalletronId(anyString()))
        .thenReturn(reactor.core.publisher.Flux.just(existingUser));

    StepVerifier.create(walletronService.createWalletronUsersFromFile(filePart, "WAL-123", "BRD-123"))
        .expectError(Exception.class)
        .verify();
  }

  @Test
  void createWalletronUsersFromFile_WithRequestDuplicates() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    when(filePart.filename()).thenReturn("test.csv");

    byte[] fileContent = "test csv content".getBytes();
    org.springframework.core.io.buffer.DataBuffer dataBuffer =
        new org.springframework.core.io.buffer.DefaultDataBufferFactory().wrap(fileContent);
    when(filePart.content()).thenReturn(reactor.core.publisher.Flux.just(dataBuffer));

    List<com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData> processedUsers =
        Arrays.asList(
            new com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData(
                "John Doe", "john@example.com", "USER"),
            new com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData(
                "Jane Smith", "john@example.com", "ADMIN")); // Duplicate email


      when(fileProcessorUtil.processCsvFile(fileContent)).thenReturn(Mono.just(processedUsers));
    lenient().when(walletronRepository.findById(anyString())).thenReturn(Mono.just(walletron));
    lenient()
        .when(walletronUsersRepository.findByWalletronId(anyString()))
        .thenReturn(reactor.core.publisher.Flux.empty());
    when(walletronRepository.save(any())).thenReturn(Mono.just(walletron));
    when(walletronUsersRepository.save(any())).thenReturn(Mono.just(createMockWalletronUser()));

    StepVerifier.create(walletronService.createWalletronUsersFromFile(filePart, "WAL-123", "BRD-123"))
        .assertNext(
            responseEntity -> {
              assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
              assertEquals("SUCCESS", responseEntity.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void createWalletronUsersFromFile_FileProcessingError() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    when(filePart.filename()).thenReturn("test.csv");

    byte[] fileContent = "test csv content".getBytes();
    org.springframework.core.io.buffer.DataBuffer dataBuffer =
        new org.springframework.core.io.buffer.DefaultDataBufferFactory().wrap(fileContent);
    when(filePart.content()).thenReturn(reactor.core.publisher.Flux.just(dataBuffer));

    when(fileProcessorUtil.processCsvFile(fileContent))
        .thenReturn(Mono.error(new RuntimeException("File processing error")));

    StepVerifier.create(walletronService.createWalletronUsersFromFile(filePart, "WAL-123", "BRD-123"))
        .expectError(Exception.class)
        .verify();
  }

  @Test
  void createWalletronUsersFromFile_UpdateWalletronAgentPortalError() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    when(filePart.filename()).thenReturn("test.csv");

    byte[] fileContent = "test csv content".getBytes();
    org.springframework.core.io.buffer.DataBuffer dataBuffer =
        new org.springframework.core.io.buffer.DefaultDataBufferFactory().wrap(fileContent);
    when(filePart.content()).thenReturn(reactor.core.publisher.Flux.just(dataBuffer));

    List<com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData> processedUsers =
        Arrays.asList(
            new com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData(
                "John Doe", "john@example.com", "USER"));

    when(fileProcessorUtil.processCsvFile(fileContent)).thenReturn(Mono.just(processedUsers));
    lenient().when(walletronRepository.findById(anyString())).thenReturn(Mono.just(walletron));
    lenient()
        .when(walletronUsersRepository.findByWalletronId(anyString()))
        .thenReturn(reactor.core.publisher.Flux.empty());
    when(walletronUsersRepository.save(any())).thenReturn(Mono.just(createMockWalletronUser()));
    when(walletronRepository.save(any()))
        .thenReturn(Mono.error(new RuntimeException("Update error")));

    StepVerifier.create(walletronService.createWalletronUsersFromFile(filePart, "WAL-123", "BRD-123"))
        .assertNext(
            responseEntity -> {
              assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
              assertEquals("SUCCESS", responseEntity.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void createWalletronUsersFromFile_WithQuotedIds() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    when(filePart.filename()).thenReturn("test.csv");

    byte[] fileContent = "test csv content".getBytes();
    org.springframework.core.io.buffer.DataBuffer dataBuffer =
        new org.springframework.core.io.buffer.DefaultDataBufferFactory().wrap(fileContent);
    when(filePart.content()).thenReturn(reactor.core.publisher.Flux.just(dataBuffer));

    List<com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData> processedUsers =
        Arrays.asList(
            new com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData(
                "John Doe", "john@example.com", "USER"));


      when(fileProcessorUtil.processCsvFile(fileContent)).thenReturn(Mono.just(processedUsers));
    lenient().when(walletronRepository.findById(anyString())).thenReturn(Mono.just(walletron));
    lenient()
        .when(walletronUsersRepository.findByWalletronId(anyString()))
        .thenReturn(reactor.core.publisher.Flux.empty());
    when(walletronRepository.save(any())).thenReturn(Mono.just(walletron));
    when(walletronUsersRepository.save(any())).thenReturn(Mono.just(createMockWalletronUser()));

    // Test with quoted IDs
    StepVerifier.create(walletronService.createWalletronUsersFromFile(filePart, "\"WAL-123\"", "\"BRD-123\""))
        .assertNext(
            responseEntity -> {
              assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
              assertEquals("SUCCESS", responseEntity.getBody().getStatus());
            })
        .verifyComplete();
  }

  @Test
  void createWalletronUsersFromFile_MultipleDataBuffers() {
    org.springframework.http.codec.multipart.FilePart filePart =
        mock(org.springframework.http.codec.multipart.FilePart.class);
    when(filePart.filename()).thenReturn("test.csv");

    byte[] fileContent1 = "test csv content part 1".getBytes();
    byte[] fileContent2 = "test csv content part 2".getBytes();
    org.springframework.core.io.buffer.DataBuffer dataBuffer1 =
        new org.springframework.core.io.buffer.DefaultDataBufferFactory().wrap(fileContent1);
    org.springframework.core.io.buffer.DataBuffer dataBuffer2 =
        new org.springframework.core.io.buffer.DefaultDataBufferFactory().wrap(fileContent2);
    when(filePart.content()).thenReturn(reactor.core.publisher.Flux.just(dataBuffer1, dataBuffer2));

    List<com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData> processedUsers =
        Arrays.asList(
            new com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData(
                "John Doe", "john@example.com", "USER"));


      // Combined content should be processed
    byte[] combinedContent = new byte[fileContent1.length + fileContent2.length];
    System.arraycopy(fileContent1, 0, combinedContent, 0, fileContent1.length);
    System.arraycopy(fileContent2, 0, combinedContent, fileContent1.length, fileContent2.length);

    when(fileProcessorUtil.processCsvFile(combinedContent)).thenReturn(Mono.just(processedUsers));
    lenient().when(walletronRepository.findById(anyString())).thenReturn(Mono.just(walletron));
    lenient()
        .when(walletronUsersRepository.findByWalletronId(anyString()))
        .thenReturn(reactor.core.publisher.Flux.empty());
    when(walletronRepository.save(any())).thenReturn(Mono.just(walletron));
    when(walletronUsersRepository.save(any())).thenReturn(Mono.just(createMockWalletronUser()));

    StepVerifier.create(walletronService.createWalletronUsersFromFile(filePart, "WAL-123", "BRD-123"))
        .assertNext(
            responseEntity -> {
              assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
              assertEquals("SUCCESS", responseEntity.getBody().getStatus());
            })
        .verifyComplete();
  }

  // Helper methods for creating test data

  private com.aci.smart_onboarding.dto.WalletronUsersRequest createValidWalletronUsersRequest() {
    com.aci.smart_onboarding.dto.WalletronUsersRequest request =
        new com.aci.smart_onboarding.dto.WalletronUsersRequest();
    request.setWalletronId("WAL-123");
    request.setBrdId("BRD-123");

    com.aci.smart_onboarding.dto.WalletronUsersRequest.UserData userData =
        new com.aci.smart_onboarding.dto.WalletronUsersRequest.UserData();
    userData.setName("John Doe");
    userData.setEmail("john@example.com");
    userData.setRole("USER");

    request.setUsers(List.of(userData));
    return request;
  }

  private com.aci.smart_onboarding.dto.WalletronUsersRequest createRequestWithDuplicateEmails() {
    com.aci.smart_onboarding.dto.WalletronUsersRequest request =
        new com.aci.smart_onboarding.dto.WalletronUsersRequest();
    request.setWalletronId("WAL-123");
    request.setBrdId("BRD-123");

    com.aci.smart_onboarding.dto.WalletronUsersRequest.UserData userData1 =
        new com.aci.smart_onboarding.dto.WalletronUsersRequest.UserData();
    userData1.setName("John Doe");
    userData1.setEmail("john@example.com");
    userData1.setRole("USER");

    com.aci.smart_onboarding.dto.WalletronUsersRequest.UserData userData2 =
        new com.aci.smart_onboarding.dto.WalletronUsersRequest.UserData();
    userData2.setName("John Smith");
    userData2.setEmail("john@example.com"); // Duplicate email
    userData2.setRole("ADMIN");

    request.setUsers(List.of(userData1, userData2));
    return request;
  }

  private com.aci.smart_onboarding.model.WalletronUsers createMockWalletronUser() {
    com.aci.smart_onboarding.model.WalletronUsers user =
        new com.aci.smart_onboarding.model.WalletronUsers();
    user.setId("1");
    user.setName("John Doe");
    user.setEmail("john@example.com");
    user.setRole("USER");
    user.setWalletronId("WAL-123");
    user.setBrdId("BRD-123");
    user.setCreatedAt(java.time.LocalDateTime.now());
    user.setUpdatedAt(java.time.LocalDateTime.now());
    return user;
  }

  private List<com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData>
      createMockFileUserDataList() {
    com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData userData =
        new com.aci.smart_onboarding.util.FileProcessorUtil.FileUserData();
    userData.setName("John Doe");
    userData.setEmail("john@example.com");
    userData.setRole("USER");
    return List.of(userData);
  }

  @ParameterizedTest
  @CsvSource({
    "walletronId, Test ID with quotes, \"test-value\", test-value",
    "brdId, Test BRD ID with quotes, \"brd-123\", brd-123",
    "email, Test email with quotes, \"test@example.com\", test@example.com"
  })
  void testQuotesRegexPattern(String fieldName, String description, String input, String expected) {
    String actual = input.replaceAll(QUOTES_REGEX, "");
    assertEquals(expected, actual, "Failed to strip quotes for " + fieldName + ": " + description);
  }

}
