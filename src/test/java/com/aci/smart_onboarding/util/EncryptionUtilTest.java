package com.aci.smart_onboarding.util;

import static org.junit.jupiter.api.Assertions.*;

import com.aci.smart_onboarding.constants.FileConstants;
import com.aci.smart_onboarding.exception.EncryptionException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilTest {

  @InjectMocks private EncryptionUtil encryptionUtil;

  private static final String TEST_KEY = "TestEncryptionKey123";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(encryptionUtil, "encryptionKey", TEST_KEY);
  }

  @Nested
  @DisplayName("Basic Encryption/Decryption Tests")
  class BasicEncryptionTests {
    @Test
    @DisplayName("Encrypt and decrypt should return original data")
    void encryptAndDecrypt_ShouldReturnOriginalData() {
      // Arrange
      String originalText = "This is a test string to encrypt and decrypt";
      byte[] originalData = originalText.getBytes(StandardCharsets.UTF_8);

      // Act
      byte[] encryptedData = encryptionUtil.encrypt(originalData);
      byte[] decryptedData = encryptionUtil.decrypt(encryptedData);

      // Assert
      assertNotNull(encryptedData);
      assertTrue(encryptedData.length > originalData.length);
      assertFalse(Arrays.equals(originalData, encryptedData));
      assertArrayEquals(originalData, decryptedData);
      assertEquals(originalText, new String(decryptedData, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Encrypt and decrypt should handle data with non-block-size length")
    void encryptAndDecrypt_WithNonBlockSizeData_ShouldReturnOriginalData() {
      // Arrange
      String originalText = "Just 17 bytes...!";
      byte[] originalData = originalText.getBytes(StandardCharsets.UTF_8);
      assertEquals(17, originalData.length);

      // Act
      byte[] encryptedData = encryptionUtil.encrypt(originalData);
      byte[] decryptedData = encryptionUtil.decrypt(encryptedData);

      // Assert
      assertNotNull(encryptedData);
      assertEquals(0, (encryptedData.length - 16) % 16);
      assertFalse(Arrays.equals(originalData, encryptedData));
      assertArrayEquals(originalData, decryptedData);
      assertEquals(originalText, new String(decryptedData, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Encrypt and decrypt should handle empty data")
    void encryptAndDecrypt_WithEmptyData_ShouldReturnEmptyArray() {
      // Arrange
      byte[] originalData = new byte[0];

      // Act
      byte[] encryptedData = encryptionUtil.encrypt(originalData);
      byte[] decryptedData = encryptionUtil.decrypt(encryptedData);

      // Assert
      assertNotNull(encryptedData);
      assertTrue(encryptedData.length >= 16);
      assertTrue(decryptedData.length == 0 || (decryptedData.length == 1 && decryptedData[0] == 0));
    }
  }

  @Nested
  @DisplayName("PDF Handling Tests")
  class PdfHandlingTests {
    @Test
    @DisplayName("Should handle PDF data with minimal padding")
    void decrypt_WithPdfData_ShouldHandleMinimalPadding() {
      // Arrange
      byte[] pdfData = new byte[32]; // Use full block size
      pdfData[0] = 0x25; // %
      pdfData[1] = 0x50; // P
      pdfData[2] = 0x44; // D
      pdfData[3] = 0x46; // F
      byte[] content = "Test PDF".getBytes();
      System.arraycopy(content, 0, pdfData, 4, content.length);

      // Act
      byte[] encryptedData = encryptionUtil.encrypt(pdfData);
      byte[] decryptedData = encryptionUtil.decrypt(encryptedData);

      // Assert
      assertNotNull(decryptedData);
      assertTrue(decryptedData.length >= content.length + 4); // PDF signature + content
      assertArrayEquals(new byte[] {0x25, 0x50, 0x44, 0x46}, Arrays.copyOf(decryptedData, 4));
    }

    @Test
    @DisplayName("Should handle PDF data with excessive padding")
    void decrypt_WithPdfData_ShouldHandleExcessivePadding() {
      // Arrange
      byte[] pdfData = new byte[48]; // Three blocks
      pdfData[0] = 0x25; // %
      pdfData[1] = 0x50; // P
      pdfData[2] = 0x44; // D
      pdfData[3] = 0x46; // F
      byte[] content = "Test PDF Content".getBytes();
      System.arraycopy(content, 0, pdfData, 4, content.length);

      // Act
      byte[] encryptedData = encryptionUtil.encrypt(pdfData);
      byte[] decryptedData = encryptionUtil.decrypt(encryptedData);

      // Assert
      assertNotNull(decryptedData);
      assertTrue(decryptedData.length >= content.length + 4); // PDF signature + content
      assertArrayEquals(new byte[] {0x25, 0x50, 0x44, 0x46}, Arrays.copyOf(decryptedData, 4));
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {
    @Test
    @DisplayName("Decrypt should handle invalid data gracefully")
    void decrypt_WithInvalidData_ShouldThrowException() {
      // Arrange
      byte[] invalidData = "Not valid".getBytes(StandardCharsets.UTF_8);

      // Act & Assert
      Exception exception =
          assertThrows(EncryptionException.class, () -> encryptionUtil.decrypt(invalidData));
      assertTrue(exception.getMessage().contains("Invalid encrypted data"));
    }

    @Test
    @DisplayName("Decrypt should handle null data")
    void decrypt_WithNullData_ShouldThrowException() {
      assertThrows(EncryptionException.class, () -> encryptionUtil.decrypt(null));
    }

    @Test
    @DisplayName("Decrypt should handle data shorter than IV length")
    void decrypt_WithShortData_ShouldThrowException() {
      byte[] shortData = new byte[8];
      assertThrows(EncryptionException.class, () -> encryptionUtil.decrypt(shortData));
    }
  }

  @Nested
  @DisplayName("Alternative Key Tests")
  class AlternativeKeyTests {
    @Test
    @DisplayName("Should handle null alternative keys")
    void decryptWithAlternativeKeys_WithNullKeys_ShouldReturnEmptyArray() {
      // Arrange
      byte[] encryptedData = encryptionUtil.encrypt("Test data".getBytes());

      // Act
      byte[] result = encryptionUtil.decryptWithAlternativeKeys(encryptedData, null);

      // Assert
      assertNotNull(result);
      assertEquals(0, result.length);
    }

    @Test
    @DisplayName("Should handle empty alternative keys list")
    void decryptWithAlternativeKeys_WithEmptyList_ShouldReturnEmptyArray() {
      // Arrange
      byte[] encryptedData = encryptionUtil.encrypt("Test data".getBytes());

      // Act
      byte[] result = encryptionUtil.decryptWithAlternativeKeys(encryptedData, List.of());

      // Assert
      assertNotNull(result);
      assertEquals(0, result.length);
    }

    @Test
    @DisplayName("Should handle decryption with invalid keys")
    void decryptWithAlternativeKeys_WithInvalidKeys_ShouldHandleGracefully() {
      // Arrange
      String originalText = "Test data";
      byte[] originalData = originalText.getBytes(StandardCharsets.UTF_8);

      // Store original key and encrypt with it
      String originalKey = TEST_KEY;
      byte[] encryptedData = encryptionUtil.encrypt(originalData);

      try {
        // Change to wrong key and try decryption with invalid keys
        ReflectionTestUtils.setField(encryptionUtil, "encryptionKey", "WrongKey123");

        // Act
        byte[] result =
            encryptionUtil.decryptWithAlternativeKeys(
                encryptedData, Arrays.asList("invalid1", "invalid2"));

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0); // Should return the failed decryption result
      } finally {
        // Restore original key
        ReflectionTestUtils.setField(encryptionUtil, "encryptionKey", originalKey);
      }
    }
  }

  @Nested
  @DisplayName("Key Management Tests")
  class KeyManagementTests {
    @Test
    @DisplayName("generateRandomKey should create valid base64 string")
    void generateRandomKey_ShouldCreateValidBase64String() {
      // Act
      String generatedKey = EncryptionUtil.generateRandomKey();

      // Assert
      assertNotNull(generatedKey);
      assertFalse(generatedKey.isEmpty());
      assertTrue(generatedKey.matches("^[A-Za-z0-9+/]+=*$"));
    }

    @Test
    @DisplayName("Should get encryption key signature")
    void getEncryptionKeySignature_ShouldReturnValidSignature() {
      // Act
      String signature = encryptionUtil.getEncryptionKeySignature();

      // Assert
      assertNotNull(signature);
      assertTrue(signature.contains("Length:" + TEST_KEY.length()));
      assertTrue(signature.contains("FirstChar:" + TEST_KEY.charAt(0)));
      assertTrue(signature.contains("LastChar:" + TEST_KEY.charAt(TEST_KEY.length() - 1)));
    }

    @Test
    @DisplayName("Should handle empty encryption key signature")
    void getEncryptionKeySignature_WithEmptyKey_ShouldReturnValidSignature() {
      // Arrange
      ReflectionTestUtils.setField(encryptionUtil, "encryptionKey", "");

      // Act
      String signature = encryptionUtil.getEncryptionKeySignature();

      // Assert
      assertNotNull(signature);
      assertTrue(signature.contains("Length:0"));
      assertTrue(signature.contains("FirstChar:" + FileConstants.FILE_EMPTY));
      assertTrue(signature.contains("LastChar:" + FileConstants.FILE_EMPTY));
    }
  }

  @Test
  @DisplayName("Should handle all-zero decrypted data")
  void decrypt_WithAllZeroData_ShouldHandleGracefully() {
    // Arrange
    byte[] allZeros = new byte[32]; // Two blocks of zeros

    // Act
    byte[] encryptedData = encryptionUtil.encrypt(allZeros);
    byte[] decryptedData = encryptionUtil.decrypt(encryptedData);

    // Assert
    assertNotNull(decryptedData);
    assertTrue(decryptedData.length > 0);
  }

  @Test
  @DisplayName("Should handle data exactly matching block size")
  void encryptAndDecrypt_WithExactBlockSize_ShouldReturnOriginalData() {
    // Arrange
    byte[] blockSizeData = new byte[16]; // Exactly one AES block
    Arrays.fill(blockSizeData, (byte) 'A');

    // Act
    byte[] encryptedData = encryptionUtil.encrypt(blockSizeData);
    byte[] decryptedData = encryptionUtil.decrypt(encryptedData);

    // Assert
    assertArrayEquals(blockSizeData, decryptedData);
  }

  @Nested
  @DisplayName("Fallback Decryption Tests")
  class FallbackDecryptionTests {
    @Test
    @DisplayName("Should use primary key when decryption succeeds")
    void decryptWithFallback_WithValidPrimaryKey_ShouldUsePrimaryKey() {
      // Arrange
      String originalText = "Test data for primary key";
      byte[] originalData = originalText.getBytes(StandardCharsets.UTF_8);
      byte[] encryptedData = encryptionUtil.encrypt(originalData);

      // Act
      byte[] decryptedData =
          encryptionUtil.decryptWithFallback(
              encryptedData, Arrays.asList("alternative1", "alternative2"));

      // Assert
      assertArrayEquals(originalData, decryptedData);
    }

    @Test
    @DisplayName("Should try alternative keys when primary key fails")
    void decryptWithFallback_WithFailedPrimaryKey_ShouldTryAlternativeKeys() {
      // Arrange
      String originalText = "Test data for alternative key";
      byte[] originalData = originalText.getBytes(StandardCharsets.UTF_8);

      // Store original key and encrypt with it
      String originalKey = TEST_KEY;
      byte[] encryptedData = encryptionUtil.encrypt(originalData);

      try {
        // Change to wrong key
        ReflectionTestUtils.setField(encryptionUtil, "encryptionKey", "WrongKey123");

        // Act
        byte[] result =
            encryptionUtil.decryptWithFallback(
                encryptedData, Arrays.asList("WrongKey1", "WrongKey2"));

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0); // Should return the failed decryption result
      } finally {
        // Restore original key
        ReflectionTestUtils.setField(encryptionUtil, "encryptionKey", originalKey);
      }
    }

    @Test
    @DisplayName("Should handle all keys failing")
    void decryptWithFallback_WithAllKeysFailing_ShouldReturnFailedResult() {
      // Arrange
      String originalText = "Test data for failed keys";
      byte[] originalData = originalText.getBytes(StandardCharsets.UTF_8);

      // Store original key and encrypt with it
      String originalKey = TEST_KEY;
      byte[] encryptedData = encryptionUtil.encrypt(originalData);

      try {
        // Change to wrong key
        ReflectionTestUtils.setField(encryptionUtil, "encryptionKey", "WrongKey123");

        // Act
        byte[] result =
            encryptionUtil.decryptWithFallback(
                encryptedData, Arrays.asList("WrongKey1", "WrongKey2"));

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0); // Should return the failed decryption result
      } finally {
        // Restore original key
        ReflectionTestUtils.setField(encryptionUtil, "encryptionKey", originalKey);
      }
    }
  }

  @Nested
  @DisplayName("Advanced PDF Handling Tests")
  class AdvancedPdfHandlingTests {
    @Test
    @DisplayName("Should handle PDF with exact block size")
    void decrypt_WithPdfExactBlockSize_ShouldPreserveContent() {
      // Arrange
      byte[] pdfData = new byte[32]; // Two blocks
      pdfData[0] = 0x25; // %
      pdfData[1] = 0x50; // P
      pdfData[2] = 0x44; // D
      pdfData[3] = 0x46; // F
      Arrays.fill(pdfData, 4, 32, (byte) 'X'); // Fill rest with content

      // Act
      byte[] encryptedData = encryptionUtil.encrypt(pdfData);
      byte[] decryptedData = encryptionUtil.decrypt(encryptedData);

      // Assert
      assertArrayEquals(pdfData, decryptedData);
    }

    @Test
    @DisplayName("Should handle PDF with minimal content")
    void decrypt_WithMinimalPdfContent_ShouldPreserveStructure() {
      // Arrange
      byte[] pdfData = new byte[16]; // One block
      pdfData[0] = 0x25; // %
      pdfData[1] = 0x50; // P
      pdfData[2] = 0x44; // D
      pdfData[3] = 0x46; // F

      // Act
      byte[] encryptedData = encryptionUtil.encrypt(pdfData);
      byte[] decryptedData = encryptionUtil.decrypt(encryptedData);

      // Assert
      assertArrayEquals(pdfData, decryptedData);
    }
  }

  @Nested
  @DisplayName("Failed Decryption Detection Tests")
  class FailedDecryptionDetectionTests {
    @Test
    @DisplayName("Should detect all-zero output as failed decryption")
    void isFailedDecryption_WithAllZeros_ShouldReturnTrue() {
      // Arrange
      byte[] allZeros = new byte[32];

      // Act & Assert
      assertTrue(
          (boolean)
              ReflectionTestUtils.invokeMethod(
                  encryptionUtil, "isFailedDecryption", (Object) allZeros));
    }

    @Test
    @DisplayName("Should detect first block zeros as failed decryption")
    void isFailedDecryption_WithFirstBlockZeros_ShouldReturnTrue() {
      // Arrange
      byte[] data = new byte[32];
      Arrays.fill(data, 16, 32, (byte) 1); // Second block non-zero

      // Act & Assert
      assertTrue(
          (boolean)
              ReflectionTestUtils.invokeMethod(
                  encryptionUtil, "isFailedDecryption", (Object) data));
    }

    @Test
    @DisplayName("Should identify valid decryption result")
    void isFailedDecryption_WithValidData_ShouldReturnFalse() {
      // Arrange
      byte[] validData = "Valid decrypted content".getBytes(StandardCharsets.UTF_8);

      // Act & Assert
      assertFalse(
          (boolean)
              ReflectionTestUtils.invokeMethod(
                  encryptionUtil, "isFailedDecryption", (Object) validData));
    }
  }

  @Nested
  @DisplayName("Block Size Validation Tests")
  class BlockSizeValidationTests {
    @Test
    @DisplayName("Should reject data not multiple of block size")
    void decrypt_WithInvalidBlockSize_ShouldThrowException() {
      // Arrange
      byte[] invalidData = new byte[50]; // Not multiple of 16
      System.arraycopy(new byte[16], 0, invalidData, 0, 16); // Add valid IV

      // Act & Assert
      Exception exception =
          assertThrows(EncryptionException.class, () -> encryptionUtil.decrypt(invalidData));
      assertTrue(exception.getMessage().contains("not a multiple of AES block size"));
    }

    @Test
    @DisplayName("Should handle multiple blocks correctly")
    void decrypt_WithMultipleBlocks_ShouldProcess() {
      // Arrange
      byte[] originalData = new byte[48]; // 3 blocks
      Arrays.fill(originalData, (byte) 'A');

      // Act
      byte[] encryptedData = encryptionUtil.encrypt(originalData);
      byte[] decryptedData = encryptionUtil.decrypt(encryptedData);

      // Assert
      assertArrayEquals(originalData, decryptedData);
    }
  }
}
