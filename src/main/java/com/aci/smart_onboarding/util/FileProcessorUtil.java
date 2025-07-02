package com.aci.smart_onboarding.util;

import com.aci.smart_onboarding.exception.BadRequestException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessorUtil {

  private final Validator validator;

  private static final String NAME_HEADER = "name";
  private static final String EMAIL_HEADER = "email";
  private static final String ROLE_HEADER = "role";

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

  private static final long MAX_FILE_SIZE = 2L * 1024 * 1024; // 2MB

  // Valid roles for WalletronUsers
  private static final Set<String> VALID_ROLES =
      Set.of("USER", "ADMIN", "MANAGER", "PM", "BA", "BILLER");

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FileUserData {

    @NotNull(message = "Name cannot be null")
    private String name;

    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be valid")
    private String email;

    @NotNull(message = "Role cannot be null")
    private String role;
  }

  public Mono<List<FileUserData>> processCsvFile(byte[] fileContent) {
    return Mono.fromCallable(
            () -> {
              validateFileSize(fileContent);

              String csvContent = new String(fileContent, StandardCharsets.UTF_8);
              List<FileUserData> users = new ArrayList<>();

              try (CSVReader csvReader = new CSVReader(new StringReader(csvContent))) {
                String[] headers = csvReader.readNext();
                if (headers == null) {
                  throw new BadRequestException("CSV file is empty");
                }

                Map<String, Integer> headerMap = createHeaderMap(headers);
                validateRequiredHeaders(headerMap);

                String[] line;
                int rowNumber = 1; // Start from 1 since 0 is header

                while ((line = csvReader.readNext()) != null) {
                  rowNumber++;
                  processCsvRow(line, headerMap, rowNumber, users);
                }

              } catch (IOException | CsvValidationException e) {
                throw new BadRequestException("Failed to parse CSV file: " + e.getMessage());
              }

              log.info("Successfully processed {} valid users from CSV file", users.size());
              return users;
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<List<FileUserData>> processExcelFile(byte[] fileContent) {
    return Mono.fromCallable(
            () -> {
              validateFileSize(fileContent);

              List<FileUserData> users = new ArrayList<>();

              try (ByteArrayInputStream bis = new ByteArrayInputStream(fileContent);
                  Workbook workbook = new XSSFWorkbook(bis)) {

                Sheet sheet = workbook.getSheetAt(0);
                validateExcelSheet(sheet);

                Map<String, Integer> headerMap = processExcelHeaders(sheet);
                users = processExcelRows(sheet, headerMap);

              } catch (IOException e) {
                throw new BadRequestException("Failed to parse Excel file: " + e.getMessage());
              }

              log.info("Successfully processed {} valid users from Excel file", users.size());
              return users;
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  private void validateFileSize(byte[] fileContent) {
    if (fileContent.length > MAX_FILE_SIZE) {
      throw new BadRequestException(
          String.format(
              "File size %d bytes exceeds maximum allowed size of %d bytes (2MB)",
              fileContent.length, MAX_FILE_SIZE));
    }
  }

  private Map<String, Integer> createHeaderMap(String[] headers) {
    Map<String, Integer> headerMap = new HashMap<>();
    log.debug("Processing CSV headers:");
    for (int i = 0; i < headers.length; i++) {
      String originalHeader = headers[i];
      String normalizedHeader = removeBOM(headers[i]).trim().toLowerCase();
      headerMap.put(normalizedHeader, i);
      log.debug("  Header[{}]: '{}' -> '{}'", i, originalHeader, normalizedHeader);
    }
    log.debug("Final header map: {}", headerMap);
    return headerMap;
  }

  private Map<String, Integer> createHeaderMapFromRow(Row headerRow) {
    Map<String, Integer> headerMap = new HashMap<>();
    log.debug("Processing Excel headers:");
    for (Cell cell : headerRow) {
      String originalHeader = getCellValueAsString(cell);
      String normalizedHeader = removeBOM(originalHeader).trim().toLowerCase();
      headerMap.put(normalizedHeader, cell.getColumnIndex());
      log.debug(
          "  Header[{}]: '{}' -> '{}'", cell.getColumnIndex(), originalHeader, normalizedHeader);
    }
    log.debug("Final header map: {}", headerMap);
    return headerMap;
  }

  /**
   * Removes BOM (Byte Order Mark) characters from the beginning of a string. Common BOM characters:
   * UTF-8 BOM (EF BB BF), UTF-16 BOM, etc.
   */
  private String removeBOM(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }

    // Remove UTF-8 BOM character (U+FEFF)
    if (str.charAt(0) == '\uFEFF') {
      return str.substring(1);
    }

    // Remove other common BOM characters
    if (str.startsWith("\uFEFF") || str.startsWith("\uFFFE")) {
      return str.substring(1);
    }

    return str;
  }

  private void validateRequiredHeaders(Map<String, Integer> headerMap) {
    List<String> requiredHeaders = Arrays.asList(NAME_HEADER, EMAIL_HEADER, ROLE_HEADER);
    List<String> missingHeaders = new ArrayList<>();

    for (String required : requiredHeaders) {
      if (!headerMap.containsKey(required)) {
        missingHeaders.add(required);
      }
    }

    if (!missingHeaders.isEmpty()) {
      String foundHeaders = String.join(", ", headerMap.keySet());
      String expectedHeaders = String.join(", ", requiredHeaders);
      throw new BadRequestException(
          String.format(
              "Missing required headers. Expected: [%s], Found: [%s], Missing: [%s]",
              expectedHeaders, foundHeaders, String.join(", ", missingHeaders)));
    }
  }

  private void processCsvRow(
      String[] line, Map<String, Integer> headerMap, int rowNumber, List<FileUserData> users) {
    try {
      FileUserData userData = processRow(line, headerMap, rowNumber);
      if (userData != null) {
        users.add(userData);
      }
    } catch (Exception e) {
      log.warn("Skipping invalid row {}: {}", rowNumber, e.getMessage());
    }
  }

  private FileUserData processRow(String[] row, Map<String, Integer> headerMap, int rowNumber) {
    String name = getValueFromRow(row, headerMap, NAME_HEADER);
    String email = getValueFromRow(row, headerMap, EMAIL_HEADER);
    String role = getValueFromRow(row, headerMap, ROLE_HEADER);

    return validateAndCreateUserData(name, email, role, rowNumber);
  }

  private FileUserData processExcelRow(Row row, Map<String, Integer> headerMap, int rowNumber) {
    String name = getCellValue(row, headerMap, NAME_HEADER);
    String email = getCellValue(row, headerMap, EMAIL_HEADER);
    String role = getCellValue(row, headerMap, ROLE_HEADER);

    return validateAndCreateUserData(name, email, role, rowNumber);
  }

  private FileUserData validateAndCreateUserData(
      String name, String email, String role, int rowNumber) {
    // Check if all required fields are present and not empty
    if (isBlankOrNull(name) || isBlankOrNull(email) || isBlankOrNull(role)) {
      log.debug("Row {} skipped: missing required fields", rowNumber);
      return null;
    }

    // Validate email format
    if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
      log.warn("Row {} skipped: invalid email format: {}", rowNumber, email);
      return null;
    }

    // Validate role
    if (!VALID_ROLES.contains(role.trim().toUpperCase())) {
      log.warn("Row {} skipped: invalid role format: {}", rowNumber, role);
      return null;
    }

    FileUserData userData = new FileUserData(name.trim(), email.trim().toLowerCase(), role.trim());

    // Validate using Jakarta validation
    Set<ConstraintViolation<FileUserData>> violations = validator.validate(userData);
    if (!violations.isEmpty()) {
      String errors =
          violations.stream()
              .map(ConstraintViolation::getMessage)
              .reduce((a, b) -> a + ", " + b)
              .orElse("Unknown validation error");
      log.warn("Row {} skipped: validation errors: {}", rowNumber, errors);
      return null;
    }

    return userData;
  }

  private String getValueFromRow(String[] row, Map<String, Integer> headerMap, String header) {
    Integer index = headerMap.get(header);
    if (index == null || index >= row.length) {
      return null;
    }
    return row[index];
  }

  private String getCellValue(Row row, Map<String, Integer> headerMap, String header) {
    Integer index = headerMap.get(header);
    if (index == null) {
      return null;
    }
    Cell cell = row.getCell(index);
    return getCellValueAsString(cell);
  }

  private String getCellValueAsString(Cell cell) {
    if (cell == null) {
      return null;
    }

    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> {
        if (DateUtil.isCellDateFormatted(cell)) {
          yield cell.getDateCellValue().toString();
        } else {
          // Handle numeric values that should be strings (like IDs)
          double numericValue = cell.getNumericCellValue();
          if (numericValue == Math.floor(numericValue)) {
            yield String.valueOf((long) numericValue);
          } else {
            yield String.valueOf(numericValue);
          }
        }
      }
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> cell.getCellFormula();
      default -> "";
    };
  }

  private boolean isBlankOrNull(String value) {
    return value == null || value.trim().isEmpty();
  }

  private void validateExcelSheet(Sheet sheet) {
    if (sheet.getPhysicalNumberOfRows() == 0) {
      throw new BadRequestException("Excel file is empty");
    }

    Row headerRow = sheet.getRow(0);
    if (headerRow == null) {
      throw new BadRequestException("Excel file header row is missing");
    }
  }

  private Map<String, Integer> processExcelHeaders(Sheet sheet) {
    Row headerRow = sheet.getRow(0);
    Map<String, Integer> headerMap = createHeaderMapFromRow(headerRow);
    validateRequiredHeaders(headerMap);
    return headerMap;
  }

  private List<FileUserData> processExcelRows(Sheet sheet, Map<String, Integer> headerMap) {
    List<FileUserData> users = new ArrayList<>();

    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
      Row row = sheet.getRow(i);
      if (row == null) continue;

      try {
        FileUserData userData = processExcelRow(row, headerMap, i + 1);
        if (userData != null) {
          users.add(userData);
        }
      } catch (Exception e) {
        log.warn("Skipping invalid Excel row {}: {}", i + 1, e.getMessage());
      }
    }

    return users;
  }
}
