package com.aci.smart_onboarding.util;

import com.aci.smart_onboarding.constants.FileConstants;
import com.aci.smart_onboarding.exception.DecryptionException;
import com.aci.smart_onboarding.exception.EncryptionException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class for encryption and decryption operations.
 *
 * <p>Provides methods for secure AES encryption and decryption of file content. Uses
 * AES/CBC/NoPadding with manual zero padding handling for compatibility with various file formats.
 */
@Slf4j
@Component
public class EncryptionUtil {

  private static final String ALGORITHM = "AES";
  private static final String CIPHER_TRANSFORMATION = "AES/CBC/NoPadding";
  private static final int IV_LENGTH = 16; // 16 bytes for AES
  private static final int AES_BLOCK_SIZE = 16; // AES operates on 16-byte blocks

  // String constants for logging
  private static final String LOG_IV_BYTES = "IV bytes: ";
  private static final String LOG_DECRYPTED_BEFORE_PADDING =
      "First bytes of decrypted data (before padding removal): ";
  private static final String LOG_FINAL_DECRYPTED = "First bytes of final decrypted data: ";
  private static final String LOG_DECRYPTED_LENGTH =
      "Decrypted data length (before padding removal): {} bytes";
  private static final String LOG_PADDING_REMOVAL = "Removing zero padding from {} bytes";
  private static final String LOG_PADDING_REMOVED =
      "Removed padding: original length={}, new length={}";
  private static final String LOG_EMPTY_AFTER_REMOVAL =
      "Final decrypted data is empty after padding removal!";
  private static final String LOG_ALL_ZEROS =
      "All bytes are zero in decrypted data - returning non-empty placeholder";
  private static final String LOG_LAST_NON_ZERO = "Last non-zero byte found at index: {}";
  private static final String LOG_EMPTY_DATA = "Cannot remove padding from null or empty data";
  private static final String LOG_PDF_WITH_PADDING =
      "PDF detected with {} trailing zeros - preserving file structure";
  private static final String LOG_PDF_EXCESS_PADDING =
      "PDF detected with excessive padding ({} bytes) - trimming";
  private static final String HEX_FORMAT = "%02x ";
  private static final String PDF_SIGNATURE = "PDF signature detected in decrypted data";

  @Value("${encryption.key:SmartOnboardingDefaultKey123}")
  private String encryptionKey;

  /**
   * Encrypts the data using AES encryption with the configured key. Uses CBC mode with manual zero
   * padding to ensure the data size is a multiple of the AES block size.
   *
   * @param data The data to encrypt
   * @return Encrypted data as byte array with IV prepended
   * @throws EncryptionException if encryption fails
   */
  public byte[] encrypt(byte[] data) {
    try {
      // When using NoPadding, the data length must be a multiple of the block size
      // We need to pad manually if necessary
      byte[] paddedData = padDataToBlockSize(data);

      // Generate a random IV
      byte[] iv = new byte[IV_LENGTH];
      SecureRandom secureRandom = new SecureRandom();
      secureRandom.nextBytes(iv);
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

      // Create secret key
      SecretKeySpec secretKeySpec = generateSecretKeySpec();

      // Initialize cipher for encryption
      Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

      // Encrypt the data
      byte[] encryptedData = cipher.doFinal(paddedData);

      // Prepend IV to the encrypted data (IV doesn't need to be secret, but must be sent with the
      // encrypted data)
      byte[] encryptedDataWithIv = new byte[IV_LENGTH + encryptedData.length];
      System.arraycopy(iv, 0, encryptedDataWithIv, 0, IV_LENGTH);
      System.arraycopy(encryptedData, 0, encryptedDataWithIv, IV_LENGTH, encryptedData.length);

      return encryptedDataWithIv;
    } catch (Exception e) {
      throw new EncryptionException("Unable to encrypt data", e);
    }
  }

  /**
   * Decrypts the data using AES decryption with the configured key. Extracts the IV from the first
   * 16 bytes of the input data. Handles removing of zero padding after decryption.
   *
   * @param encryptedDataWithIv The encrypted data with IV prepended
   * @return Decrypted data as byte array
   * @throws EncryptionException if decryption fails
   */
  public byte[] decrypt(byte[] encryptedDataWithIv) {
    try {
      log.info(
          "Starting decryption of {} bytes",
          encryptedDataWithIv != null ? encryptedDataWithIv.length : 0);

      // Validate input data
      validateEncryptedData(encryptedDataWithIv);

      // Extract IV and encrypted data
      IvAndEncryptedData extracted = extractIvAndEncryptedData(encryptedDataWithIv);

      // Decrypt the data
      byte[] paddedDecryptedData = performDecryption(extracted.iv, extracted.encryptedData);

      // Log decrypted data info
      logDecryptedData(paddedDecryptedData);

      // Remove padding and return
      byte[] removedPaddingData = removeZeroPadding(paddedDecryptedData);
      log.info("Data length after padding removal: {} bytes", removedPaddingData.length);

      // Log final result
      logFinalDecryptedData(removedPaddingData);

      return removedPaddingData;
    } catch (EncryptionException e) {
      throw e; // Rethrow our custom exceptions
    } catch (Exception e) {
      log.error("Decryption failed: {}", e.getMessage(), e);
      throw new EncryptionException("Unable to decrypt data: " + e.getMessage(), e);
    }
  }

  /**
   * Validates that the encrypted data is valid for decryption
   *
   * @throws EncryptionException if data is invalid
   */
  private void validateEncryptedData(byte[] encryptedDataWithIv) {
    if (encryptedDataWithIv == null || encryptedDataWithIv.length <= IV_LENGTH) {
      log.error("Invalid encrypted data: too short or null");
      throw new EncryptionException("Invalid encrypted data: too short or null");
    }
  }

  /** Extracts the IV and encrypted data from the input */
  private IvAndEncryptedData extractIvAndEncryptedData(byte[] encryptedDataWithIv) {
    byte[] iv = new byte[IV_LENGTH];
    byte[] encryptedData = new byte[encryptedDataWithIv.length - IV_LENGTH];

    System.arraycopy(encryptedDataWithIv, 0, iv, 0, IV_LENGTH);
    System.arraycopy(encryptedDataWithIv, IV_LENGTH, encryptedData, 0, encryptedData.length);

    // Validate that encrypted data length is a multiple of block size
    if (encryptedData.length % AES_BLOCK_SIZE != 0) {
      log.error("Invalid encrypted data: length is not a multiple of AES block size");
      throw new EncryptionException(
          "Invalid encrypted data: length is not a multiple of AES block size");
    }

    // Log IV for debugging
    StringBuilder ivHex = new StringBuilder(LOG_IV_BYTES);
    for (byte b : iv) {
      ivHex.append(String.format(HEX_FORMAT, b & 0xff));
    }
    log.info(ivHex.toString());

    return new IvAndEncryptedData(iv, encryptedData);
  }

  /** Performs the actual decryption using AES */
  private byte[] performDecryption(byte[] iv, byte[] encryptedData) throws DecryptionException {
    try {
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
      SecretKeySpec secretKeySpec = generateSecretKeySpec();

      Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

      return cipher.doFinal(encryptedData);
    } catch (NoSuchAlgorithmException e) {
      log.error("Decryption failed: Algorithm not available", e);
      throw new DecryptionException("Decryption algorithm not available: " + e.getMessage(), e);
    } catch (InvalidKeyException e) {
      log.error("Decryption failed: Invalid encryption key", e);
      throw new DecryptionException("Invalid encryption key: " + e.getMessage(), e);
    } catch (IllegalBlockSizeException e) {
      log.error("Decryption failed: Invalid block size", e);
      throw new DecryptionException("Invalid data block size: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Decryption failed with unexpected error", e);
      throw new DecryptionException("Failed to decrypt data: " + e.getMessage(), e);
    }
  }

  /** Logs information about the decrypted data before padding removal */
  private void logDecryptedData(byte[] paddedDecryptedData) {
    log.info(LOG_DECRYPTED_LENGTH, paddedDecryptedData.length);

    if (paddedDecryptedData.length > 0) {
      // Log first few bytes
      StringBuilder decHex = new StringBuilder(LOG_DECRYPTED_BEFORE_PADDING);
      int bytesToLog = Math.min(paddedDecryptedData.length, 16);
      for (int i = 0; i < bytesToLog; i++) {
        decHex.append(String.format(HEX_FORMAT, paddedDecryptedData[i] & 0xff));
      }
      log.info(decHex.toString());

      // Check for PDF signature
      checkForPdfSignature(paddedDecryptedData);
    }
  }

  /** Checks if the decrypted data has a PDF signature */
  private void checkForPdfSignature(byte[] data) {
    if (data.length >= 4
        && data[0] == 0x25
        && // %
        data[1] == 0x50
        && // P
        data[2] == 0x44
        && // D
        data[3] == 0x46) { // F
      log.info(PDF_SIGNATURE);
    }
  }

  /** Logs information about the final decrypted data after padding removal */
  private void logFinalDecryptedData(byte[] removedPaddingData) {
    if (removedPaddingData.length > 0) {
      StringBuilder finalHex = new StringBuilder(LOG_FINAL_DECRYPTED);
      int bytesToLog = Math.min(removedPaddingData.length, 16);
      for (int i = 0; i < bytesToLog; i++) {
        finalHex.append(String.format(HEX_FORMAT, removedPaddingData[i] & 0xff));
      }
      log.info(finalHex.toString());
    } else {
      log.warn(LOG_EMPTY_AFTER_REMOVAL);
    }
  }

  /** Helper class to hold IV and encrypted data */
  private static class IvAndEncryptedData {
    private final byte[] iv;
    private final byte[] encryptedData;

    public IvAndEncryptedData(byte[] iv, byte[] encryptedData) {
      this.iv = iv;
      this.encryptedData = encryptedData;
    }
  }

  /**
   * Pads data to AES block size (16 bytes) by appending zeros. Ensures data is a multiple of 16
   * bytes, which is required for AES/CBC/NoPadding.
   *
   * @param data The data to pad
   * @return The padded data
   */
  private byte[] padDataToBlockSize(byte[] data) {
    // For empty data or data of length 0, we still need to provide a full block
    if (data.length == 0) {
      return new byte[AES_BLOCK_SIZE]; // Return a full empty block for empty data
    }

    int paddingLength = AES_BLOCK_SIZE - (data.length % AES_BLOCK_SIZE);
    if (paddingLength == AES_BLOCK_SIZE) {
      // If data is already a multiple of block size, no padding needed
      return data;
    }

    byte[] paddedData = new byte[data.length + paddingLength];
    System.arraycopy(data, 0, paddedData, 0, data.length);
    // The rest of the array is filled with zeros by default

    return paddedData;
  }

  /**
   * Removes zero padding from decrypted data. Trims trailing zeros that were added during
   * encryption.
   *
   * @param paddedData The data with potential zero padding
   * @return The data with padding removed
   */
  private byte[] removeZeroPadding(byte[] paddedData) {
    log.info(LOG_PADDING_REMOVAL, paddedData.length);

    if (paddedData == null || paddedData.length == 0) {
      log.warn(LOG_EMPTY_DATA);
      return paddedData;
    }

    // Find the last non-zero byte (scanning from end to beginning)
    int lastNonZeroIndex = paddedData.length - 1;
    while (lastNonZeroIndex >= 0 && paddedData[lastNonZeroIndex] == 0) {
      lastNonZeroIndex--;
    }

    log.info(LOG_LAST_NON_ZERO, lastNonZeroIndex);

    // If all bytes are zero, preserve at least one byte
    if (lastNonZeroIndex < 0) {
      log.warn(LOG_ALL_ZEROS);
      return new byte[1]; // Return a non-empty array to avoid empty file
    }

    // Handle the special case of PDF files - if we see the PDF signature, keep all data
    if (paddedData.length >= 4
        && paddedData[0] == 0x25
        && // %
        paddedData[1] == 0x50
        && // P
        paddedData[2] == 0x44
        && // D
        paddedData[3] == 0x46) { // F

      // For PDFs, check if there's legitimate trailing padding vs corruption
      int trailingZeros = paddedData.length - lastNonZeroIndex - 1;
      if (trailingZeros <= 16) { // If padding is at most one AES block
        log.info(LOG_PDF_WITH_PADDING, trailingZeros);
        return paddedData; // Return with padding to preserve PDF structure
      }
      log.info(LOG_PDF_EXCESS_PADDING, trailingZeros);
    }

    // Extract the data without padding (keep data up to and including lastNonZeroIndex)
    byte[] result = Arrays.copyOf(paddedData, lastNonZeroIndex + 1);

    log.info(LOG_PADDING_REMOVED, paddedData.length, result.length);
    return result;
  }

  /**
   * Generates a SecretKeySpec from the configured encryption key. Ensures the key is properly sized
   * for AES encryption.
   *
   * @return A SecretKeySpec for AES encryption/decryption
   */
  private SecretKeySpec generateSecretKeySpec() {
    // Use the provided key or generate a key if none is provided
    byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);

    log.info("Using encryption key of length: {} characters", encryptionKey.length());

    // Ensure the key is exactly 16, 24, or 32 bytes for AES-128, AES-192, or AES-256
    byte[] fixedKeyBytes = new byte[32]; // Use AES-256
    System.arraycopy(keyBytes, 0, fixedKeyBytes, 0, Math.min(keyBytes.length, 32));

    // If the key was too short, warn about it
    if (keyBytes.length < 32) {
      log.warn(
          "Encryption key is shorter than optimal (provided: {}, needed: 32 bytes). Key will be zero-padded.",
          keyBytes.length);
    }

    return new SecretKeySpec(fixedKeyBytes, ALGORITHM);
  }

  /**
   * Generates a new random AES key. Useful for creating secure encryption keys.
   *
   * @return Base64 encoded AES key
   * @throws EncryptionException if key generation fails
   */
  public static String generateRandomKey() {
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
      keyGenerator.init(256); // 256-bit key size
      SecretKey secretKey = keyGenerator.generateKey();
      return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    } catch (NoSuchAlgorithmException e) {
      throw new EncryptionException("Error generating encryption key", e);
    }
  }

  /**
   * Returns a signature of the encryption key for diagnostic purposes. Does not expose the actual
   * key but provides a way to verify if the same key is being used.
   *
   * @return A signature string based on the encryption key
   */
  public String getEncryptionKeySignature() {
    // Don't expose the actual key but return info about it
    return "Length:"
        + encryptionKey.length()
        + "_FirstChar:"
        + (encryptionKey.isEmpty() ? FileConstants.FILE_EMPTY : encryptionKey.charAt(0))
        + "_LastChar:"
        + (encryptionKey.isEmpty()
            ? FileConstants.FILE_EMPTY
            : encryptionKey.charAt(encryptionKey.length() - 1));
  }

  /**
   * Checks if the decrypted data appears to be invalid (all zeros or too small). This is a quick
   * check to detect failed decryption.
   *
   * @param data The decrypted data to validate
   * @return true if the data appears invalid
   */
  private boolean isFailedDecryption(byte[] data) {
    if (data == null || data.length == 0) {
      return true;
    }

    // For very small outputs, check if all bytes are zero
    if (data.length <= 16) {
      boolean allZeros = true;
      for (byte b : data) {
        if (b != 0) {
          allZeros = false;
          break;
        }
      }
      return allZeros;
    }

    // For larger outputs, check if first block is all zeros
    // This is a good indicator of failed decryption
    boolean firstBlockZeros = true;
    for (int i = 0; i < 16; i++) {
      if (data[i] != 0) {
        firstBlockZeros = false;
        break;
      }
    }

    return firstBlockZeros;
  }

  /**
   * Decrypts the data, validating the result and automatically trying alternative keys if the
   * primary key appears to fail.
   *
   * @param encryptedDataWithIv The encrypted data with IV prepended
   * @param alternativeKeys List of alternative keys to try if primary key fails
   * @return Decrypted data, or null if all keys fail
   */
  public byte[] decryptWithFallback(byte[] encryptedDataWithIv, List<String> alternativeKeys) {
    try {
      // Try with primary key first
      byte[] result = decrypt(encryptedDataWithIv);

      // If primary key produced valid-looking results, return it
      if (!isFailedDecryption(result)) {
        log.info("Primary key produced valid-looking decryption result");
        return result;
      }

      log.warn("Primary key produced suspicious output (all zeros), trying alternative keys");

      // If primary key failed, try alternatives
      if (alternativeKeys != null && !alternativeKeys.isEmpty()) {
        return decryptWithAlternativeKeys(encryptedDataWithIv, alternativeKeys);
      }

      return result;

    } catch (Exception e) {
      log.error("Decryption with primary key failed with exception: {}", e.getMessage());

      // If primary key throws exception, try alternatives
      if (alternativeKeys != null && !alternativeKeys.isEmpty()) {
        try {
          return decryptWithAlternativeKeys(encryptedDataWithIv, alternativeKeys);
        } catch (Exception fallbackEx) {
          log.error("All alternative keys also failed: {}", fallbackEx.getMessage());
          throw new DecryptionException("Decryption failed with all available keys", fallbackEx);
        }
      }

      throw new DecryptionException("Decryption failed and no alternative keys available", e);
    }
  }

  /**
   * Attempts to decrypt data with a list of possible keys. Tries each key in sequence until
   * successful decryption or all keys fail. The original encryption key is restored after attempts.
   *
   * @param encryptedDataWithIv The encrypted data with IV prepended
   * @param alternativeKeys List of alternative keys to try
   * @return Decrypted data as byte array, or null if all keys fail
   */
  public byte[] decryptWithAlternativeKeys(
      byte[] encryptedDataWithIv, List<String> alternativeKeys) {
    if (alternativeKeys == null || alternativeKeys.isEmpty()) {
      log.warn("No alternative keys provided for decryption attempt");
      return new byte[0];
    }

    // Store the original key
    String originalKey = this.encryptionKey;
    String originalKeySignature = getEncryptionKeySignature();

    try {
      // Try each key in sequence
      for (String key : alternativeKeys) {
        if (key == null || key.isEmpty()) {
          log.debug("Skipping null/empty alternative key");
          continue;
        }

        try {
          // Set current alternative key
          this.encryptionKey = key;
          String keySignature =
              "Length:"
                  + key.length()
                  + "_FirstChar:"
                  + (key.isEmpty() ? "EMPTY" : key.charAt(0))
                  + "_LastChar:"
                  + (key.isEmpty() ? "EMPTY" : key.charAt(key.length() - 1));
          log.info("Trying alternative key: {}", keySignature);

          // Attempt decryption with this key
          byte[] result = decrypt(encryptedDataWithIv);

          // Validate the decryption result
          if (!isFailedDecryption(result)) {
            log.info("Successful decryption with alternative key: {}", keySignature);
            return result;
          }

          log.warn("Alternative key {} produced suspicious output - trying next key", keySignature);
        } catch (Exception e) {
          log.debug("Alternative key failed: {}", e.getMessage());
          // Continue to next key
        }
      }

      log.warn("All alternative keys failed to decrypt the data");
      return new byte[0];
    } finally {
      // Always restore the original key
      this.encryptionKey = originalKey;
      log.info("Restored original encryption key: {}", originalKeySignature);
    }
  }
}
