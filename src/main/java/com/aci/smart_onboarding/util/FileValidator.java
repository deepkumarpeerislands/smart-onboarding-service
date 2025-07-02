package com.aci.smart_onboarding.util;

import com.aci.smart_onboarding.exception.InvalidFileException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class FileValidator {

  private static final long MAX_FILE_SIZE = 5_000_000; // 5MB
  private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "txt");
  private static final List<String> ALLOWED_MIME_TYPES = List.of("application/pdf", "text/plain");
  private static final Tika tika = new Tika();

  private static final String INVALID_FILENAME = "Invalid file name";
  private static final String INVALID_EXTENSION =
      "Invalid file extension. Only PDF and TXT files are allowed.";
  private static final String FILE_TOO_LARGE = "File size exceeds the 5MB limit";
  private static final String INVALID_MIME_TYPE =
      "Invalid file type. Only PDF and TXT files are allowed.";
  private static final String INVALID_PDF = "File is not a valid PDF document";

  private static final byte[] PDF_SIGNATURE = new byte[] {0x25, 0x50, 0x44, 0x46}; // %PDF

  /** Legacy method for file validation We're keeping it for backward compatibility */
  public Mono<Void> validateFile(FilePart filePart) {
    return readFileContent(filePart)
        .flatMap(
            fileBytes -> {
              try {
                validateFileName(filePart.filename());
                validateFileExtension(filePart.filename());
                validateFileSize(fileBytes.length);
                validateMimeType(fileBytes);

                // Only validate PDF signature for PDF files
                String extension = FilenameUtils.getExtension(filePart.filename()).toLowerCase();
                if ("pdf".equals(extension)) {
                  validatePdfSignature(fileBytes);
                }

                return Mono.empty();
              } catch (InvalidFileException e) {
                log.warn("File validation failed: {}", e.getMessage());
                return Mono.error(e);
              }
            })
        .switchIfEmpty(Mono.empty())
        .then();
  }

  /** Validates filename for security issues */
  public static void validateFileName(String filename) {
    if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
      throw new InvalidFileException(INVALID_FILENAME);
    }
  }

  /** Validates that the file extension is allowed */
  public static void validateFileExtension(String filename) {
    String extension = FilenameUtils.getExtension(filename).toLowerCase();
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new InvalidFileException(INVALID_EXTENSION);
    }
  }

  /** Validates that the file is not too large */
  public static void validateFileSize(int fileSize) {
    if (fileSize > MAX_FILE_SIZE) {
      throw new InvalidFileException(FILE_TOO_LARGE);
    }
  }

  /** Validates the MIME type of the file */
  public static void validateMimeType(byte[] fileBytes) {
    String mimeType = tika.detect(fileBytes);
    if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
      log.warn("Invalid MIME type detected: {}", mimeType);
      throw new InvalidFileException(INVALID_MIME_TYPE);
    }
  }

  /** Validates that the file has a proper PDF signature */
  public static void validatePdfSignature(byte[] fileBytes) {
    validateBasicFileProperties(fileBytes);
    logFileSignature(fileBytes);
    validatePdfHeader(fileBytes);
    validatePdfSize(fileBytes);
    validatePdfEof(fileBytes);

    log.info("PDF signature validated successfully");
  }

  /** Validates basic file properties (null check, minimum size) */
  private static void validateBasicFileProperties(byte[] fileBytes) {
    if (fileBytes == null) {
      log.error("Cannot validate PDF signature: file content is null");
      throw new InvalidFileException("File content is null");
    }

    log.info("Validating PDF signature for file of size: {} bytes", fileBytes.length);

    if (fileBytes.length < 4) {
      log.error("File too small to be a valid PDF: {} bytes", fileBytes.length);
      throw new InvalidFileException("File too small to be a valid PDF");
    }
  }

  /** Logs the file signature for debugging */
  private static void logFileSignature(byte[] fileBytes) {
    // Log the first bytes for debugging
    StringBuilder firstBytesHex = new StringBuilder("First bytes of file: ");
    for (int i = 0; i < Math.min(fileBytes.length, 8); i++) {
      firstBytesHex.append(String.format("%02X ", fileBytes[i] & 0xFF));
    }
    log.info(firstBytesHex.toString());

    // Expected PDF signature: %PDF (hex: 25 50 44 46)
    StringBuilder expectedSignature = new StringBuilder("Expected PDF signature: ");
    for (byte b : PDF_SIGNATURE) {
      expectedSignature.append(String.format("%02X ", b & 0xFF));
    }
    log.info(expectedSignature.toString());
  }

  /** Validates that the file has a proper PDF header (%PDF) */
  private static void validatePdfHeader(byte[] fileBytes) {
    boolean signatureMatches = true;
    for (int i = 0; i < PDF_SIGNATURE.length; i++) {
      if (fileBytes[i] != PDF_SIGNATURE[i]) {
        signatureMatches = false;
        break;
      }
    }

    if (!signatureMatches) {
      handleInvalidPdfSignature(fileBytes);
    }
  }

  /** Handles the case when a PDF signature is invalid */
  private static void handleInvalidPdfSignature(byte[] fileBytes) {
    // Check if it's a text file masquerading as a PDF
    if (isProbablyText(fileBytes)) {
      String textPreview = extractTextPreview(fileBytes);
      log.error("File appears to be a text file with .pdf extension. Content: '{}'", textPreview);
      throw new InvalidFileException("File contains text content, not a valid PDF document");
    }

    log.error(
        "PDF signature missing, found bytes: {} {} {} {}",
        String.format("%02X", fileBytes[0] & 0xFF),
        String.format("%02X", fileBytes[1] & 0xFF),
        String.format("%02X", fileBytes[2] & 0xFF),
        String.format("%02X", fileBytes[3] & 0xFF));
    throw new InvalidFileException(INVALID_PDF);
  }

  /** Validates that the PDF file has a reasonable size */
  private static void validatePdfSize(byte[] fileBytes) {
    if (fileBytes.length < 100) {
      log.warn(
          "PDF file is very small: {} bytes. This may not be a complete PDF.", fileBytes.length);
    }
  }

  /** Validates that the PDF has an EOF marker */
  private static void validatePdfEof(byte[] fileBytes) {
    // Look for EOF marker %%EOF
    byte[] eofMarker = new byte[] {0x25, 0x25, 0x45, 0x4F, 0x46}; // %%EOF

    // Check for EOF marker near the end of the file
    int searchStart = Math.max(0, fileBytes.length - 1024); // Search the last 1KB
    boolean foundEOF = findEofMarker(fileBytes, eofMarker, searchStart);

    if (!foundEOF && fileBytes.length > 1024) {
      log.warn("PDF EOF marker (%%EOF) not found. This may not be a complete PDF file.");
    }
  }

  /** Searches for the EOF marker in the PDF */
  private static boolean findEofMarker(byte[] fileBytes, byte[] eofMarker, int searchStart) {
    for (int i = searchStart; i < fileBytes.length - eofMarker.length; i++) {
      boolean match = true;
      for (int j = 0; j < eofMarker.length; j++) {
        if (fileBytes[i + j] != eofMarker[j]) {
          match = false;
          break;
        }
      }
      if (match) {
        return true;
      }
    }
    return false;
  }

  /** Determines if content appears to be readable text */
  private static boolean isProbablyText(byte[] content) {
    if (content == null || content.length == 0) {
      return false;
    }

    // Check a sample of the content
    int checkSize = Math.min(content.length, 100);
    int textCount = 0;

    for (int i = 0; i < checkSize; i++) {
      byte b = content[i];
      // Count printable ASCII characters and common whitespace
      if ((b >= 32 && b <= 126) || b == 9 || b == 10 || b == 13) {
        textCount++;
      }
    }

    // If more than 90% of the checked bytes are text, it's probably a text file
    return (textCount * 100 / checkSize) > 90;
  }

  /** Extracts a text preview from byte content */
  private static String extractTextPreview(byte[] content) {
    if (content == null || content.length == 0) {
      return "(empty)";
    }

    // Convert bytes to string, handling potential encoding issues
    try {
      String text = new String(content, StandardCharsets.UTF_8).trim();

      // Limit to first 50 characters
      if (text.length() > 50) {
        return text.substring(0, 47) + "...";
      }
      return text;
    } catch (Exception e) {
      return "(non-text content)";
    }
  }

  private static Mono<byte[]> readFileContent(FilePart filePart) {
    return filePart
        .content()
        .reduce(new byte[0], FileValidator::mergeDataBuffer)
        .defaultIfEmpty(new byte[0]);
  }

  private static byte[] mergeDataBuffer(byte[] acc, DataBuffer buffer) {
    int bufferSize = buffer.readableByteCount();
    byte[] newData = new byte[acc.length + bufferSize];

    System.arraycopy(acc, 0, newData, 0, acc.length);
    buffer.read(newData, acc.length, bufferSize);
    return newData;
  }
}
