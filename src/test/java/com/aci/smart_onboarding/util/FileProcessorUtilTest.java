package com.aci.smart_onboarding.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.aci.smart_onboarding.exception.BadRequestException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class FileProcessorUtilTest {

  @Mock private Validator validator;

  @InjectMocks private FileProcessorUtil fileProcessorUtil;

  private static final String VALID_CSV_CONTENT =
      "name,email,role\nJohn Doe,john@example.com,USER\nJane Smith,jane@example.com,ADMIN";
  private static final String CSV_WITH_BOM =
      "\uFEFFname,email,role\nJohn Doe,john@example.com,USER";
  private static final String INVALID_CSV_MISSING_HEADERS =
      "fullname,emailaddress,position\nJohn Doe,john@example.com,USER";
  private static final String EMPTY_CSV = "";
  private static final String CSV_HEADERS_ONLY = "name,email,role";

  @BeforeEach
  void setUp() {
    lenient().when(validator.validate(any())).thenReturn(new HashSet<>());
  }

  @Test
  void processCsvFile_ValidCsv_Success() {
    byte[] csvBytes = VALID_CSV_CONTENT.getBytes(StandardCharsets.UTF_8);

    StepVerifier.create(fileProcessorUtil.processCsvFile(csvBytes))
        .assertNext(
            users -> {
              assertEquals(2, users.size());
              assertEquals("John Doe", users.get(0).getName());
              assertEquals("john@example.com", users.get(0).getEmail());
              assertEquals("USER", users.get(0).getRole());
            })
        .verifyComplete();
  }

  @Test
  void processCsvFile_WithBOM_Success() {
    byte[] csvBytes = CSV_WITH_BOM.getBytes(StandardCharsets.UTF_8);

    StepVerifier.create(fileProcessorUtil.processCsvFile(csvBytes))
        .assertNext(
            users -> {
              assertEquals(1, users.size());
              assertEquals("John Doe", users.get(0).getName());
            })
        .verifyComplete();
  }

  @Test
  void processCsvFile_FileTooLarge_ThrowsException() {
    byte[] largeFile = new byte[3 * 1024 * 1024]; // 3MB
    Arrays.fill(largeFile, (byte) 'a');

    StepVerifier.create(fileProcessorUtil.processCsvFile(largeFile))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void processCsvFile_EmptyFile_ThrowsException() {
    byte[] emptyFile = EMPTY_CSV.getBytes(StandardCharsets.UTF_8);

    StepVerifier.create(fileProcessorUtil.processCsvFile(emptyFile))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void processCsvFile_MissingRequiredHeaders_ThrowsException() {
    byte[] csvBytes = INVALID_CSV_MISSING_HEADERS.getBytes(StandardCharsets.UTF_8);

    StepVerifier.create(fileProcessorUtil.processCsvFile(csvBytes))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void processCsvFile_OnlyHeaders_ReturnsEmptyList() {
    byte[] csvBytes = CSV_HEADERS_ONLY.getBytes(StandardCharsets.UTF_8);

    StepVerifier.create(fileProcessorUtil.processCsvFile(csvBytes))
        .assertNext(users -> assertEquals(0, users.size()))
        .verifyComplete();
  }

  @Test
  void processCsvFile_InvalidEmail_SkipsRow() {
    String csvWithInvalidEmail =
        "name,email,role\nJohn Doe,invalid-email,USER\nJane Smith,jane@example.com,ADMIN";
    byte[] csvBytes = csvWithInvalidEmail.getBytes(StandardCharsets.UTF_8);

    StepVerifier.create(fileProcessorUtil.processCsvFile(csvBytes))
        .assertNext(
            users -> {
              assertEquals(1, users.size());
              assertEquals("Jane Smith", users.get(0).getName());
            })
        .verifyComplete();
  }

  @Test
  void processCsvFile_InvalidRole_SkipsRow() {
    String csvWithInvalidRole =
        "name,email,role\nJohn Doe,john@example.com,INVALID_ROLE\nJane Smith,jane@example.com,ADMIN";
    byte[] csvBytes = csvWithInvalidRole.getBytes(StandardCharsets.UTF_8);

    StepVerifier.create(fileProcessorUtil.processCsvFile(csvBytes))
        .assertNext(
            users -> {
              assertEquals(1, users.size());
              assertEquals("Jane Smith", users.get(0).getName());
            })
        .verifyComplete();
  }

  @Test
  void processCsvFile_EmptyFields_SkipsRow() {
    String csvWithEmptyFields =
        "name,email,role\n,john@example.com,USER\nJane Smith,,ADMIN\nBob Wilson,bob@example.com,";
    byte[] csvBytes = csvWithEmptyFields.getBytes(StandardCharsets.UTF_8);

    StepVerifier.create(fileProcessorUtil.processCsvFile(csvBytes))
        .assertNext(users -> assertEquals(0, users.size()))
        .verifyComplete();
  }

  @Test
  void processCsvFile_ValidationErrors_SkipsRow() {
    Set<ConstraintViolation<Object>> violations = new HashSet<>();
    ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
    when(violation.getMessage()).thenReturn("Validation error");
    violations.add(violation);
    when(validator.validate(any())).thenReturn(violations);

    byte[] csvBytes = VALID_CSV_CONTENT.getBytes(StandardCharsets.UTF_8);

    StepVerifier.create(fileProcessorUtil.processCsvFile(csvBytes))
        .assertNext(users -> assertEquals(0, users.size()))
        .verifyComplete();
  }

  @Test
  void processExcelFile_ValidExcel_Success() throws IOException {
    byte[] excelBytes = createValidExcelFile();

    StepVerifier.create(fileProcessorUtil.processExcelFile(excelBytes))
        .assertNext(
            users -> {
              assertEquals(2, users.size());
              assertEquals("John Doe", users.get(0).getName());
              assertEquals("john@example.com", users.get(0).getEmail());
              assertEquals("USER", users.get(0).getRole());
            })
        .verifyComplete();
  }

  @Test
  void processExcelFile_FileTooLarge_ThrowsException() {
    byte[] largeFile = new byte[3 * 1024 * 1024]; // 3MB
    Arrays.fill(largeFile, (byte) 'a');

    StepVerifier.create(fileProcessorUtil.processExcelFile(largeFile))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void processExcelFile_EmptyFile_ThrowsException() throws IOException {
    byte[] emptyExcelBytes = createEmptyExcelFile();

    StepVerifier.create(fileProcessorUtil.processExcelFile(emptyExcelBytes))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void processExcelFile_MissingHeaders_ThrowsException() throws IOException {
    byte[] excelBytes = createExcelWithMissingHeaders();

    StepVerifier.create(fileProcessorUtil.processExcelFile(excelBytes))
        .expectError(BadRequestException.class)
        .verify();
  }

  @Test
  void processExcelFile_InvalidExcelFormat_ThrowsException() {
    byte[] invalidBytes = "Not an Excel file".getBytes(StandardCharsets.UTF_8);

    StepVerifier.create(fileProcessorUtil.processExcelFile(invalidBytes))
        .expectError(Exception.class)
        .verify();
  }

  @Test
  void processExcelFile_NumericCellValues_Success() throws IOException {
    byte[] excelBytes = createExcelWithNumericValues();

    StepVerifier.create(fileProcessorUtil.processExcelFile(excelBytes))
        .assertNext(
            users -> {
              assertEquals(1, users.size());
              assertEquals("123", users.get(0).getName()); // Numeric converted to string
            })
        .verifyComplete();
  }

  @Test
  void processExcelFile_BooleanCellValues_Success() throws IOException {
    byte[] excelBytes = createExcelWithBooleanValues();

    StepVerifier.create(fileProcessorUtil.processExcelFile(excelBytes))
        .assertNext(
            users -> assertEquals(0, users.size())) // Boolean values won't match email pattern
        .verifyComplete();
  }

  @Test
  void processExcelFile_FormulaCellValues_Success() throws IOException {
    byte[] excelBytes = createExcelWithFormulaValues();

    StepVerifier.create(fileProcessorUtil.processExcelFile(excelBytes))
        .assertNext(
            users -> assertEquals(0, users.size())) // Formula values won't match email pattern
        .verifyComplete();
  }

  @Test
  void processExcelFile_EmptyRowsSkipped_Success() throws IOException {
    byte[] excelBytes = createExcelWithEmptyRows();

    StepVerifier.create(fileProcessorUtil.processExcelFile(excelBytes))
        .assertNext(users -> assertEquals(1, users.size()))
        .verifyComplete();
  }

  @Test
  void testFileUserDataConstructors() {
    // Test no-args constructor
    FileProcessorUtil.FileUserData userData1 = new FileProcessorUtil.FileUserData();
    assertNull(userData1.getName());
    assertNull(userData1.getEmail());
    assertNull(userData1.getRole());

    // Test all-args constructor
    FileProcessorUtil.FileUserData userData2 =
        new FileProcessorUtil.FileUserData("John", "john@example.com", "USER");
    assertEquals("John", userData2.getName());
    assertEquals("john@example.com", userData2.getEmail());
    assertEquals("USER", userData2.getRole());

    // Test setters and getters
    userData1.setName("Jane");
    userData1.setEmail("jane@example.com");
    userData1.setRole("ADMIN");
    assertEquals("Jane", userData1.getName());
    assertEquals("jane@example.com", userData1.getEmail());
    assertEquals("ADMIN", userData1.getRole());

    // Test equals and hashCode
    FileProcessorUtil.FileUserData userData3 =
        new FileProcessorUtil.FileUserData("Jane", "jane@example.com", "ADMIN");
    assertEquals(userData1, userData3);
    assertEquals(userData1.hashCode(), userData3.hashCode());

    // Test toString
    assertNotNull(userData1.toString());
  }

  @ParameterizedTest
  @CsvSource({"'\uFEFFHello', 'Hello'", "'\uFFFEHello', 'Hello'", "'Hello', 'Hello'"})
  void testRemoveBOM_WithDifferentBOMTypes(String input, String expected) throws Exception {
    Method removeBOM = FileProcessorUtil.class.getDeclaredMethod("removeBOM", String.class);
    removeBOM.setAccessible(true);

    String result = (String) removeBOM.invoke(fileProcessorUtil, input);
    assertEquals(expected, result);
  }

  @Test
  void testRemoveBOM_NullString() throws Exception {
    Method removeBOM = FileProcessorUtil.class.getDeclaredMethod("removeBOM", String.class);
    removeBOM.setAccessible(true);

    String result = (String) removeBOM.invoke(fileProcessorUtil, (String) null);
    assertNull(result);
  }

  @Test
  void testRemoveBOM_EmptyString() throws Exception {
    Method removeBOM = FileProcessorUtil.class.getDeclaredMethod("removeBOM", String.class);
    removeBOM.setAccessible(true);

    String result = (String) removeBOM.invoke(fileProcessorUtil, "");
    assertEquals("", result);
  }

  @Test
  void testIsBlankOrNull() throws Exception {
    Method isBlankOrNull = FileProcessorUtil.class.getDeclaredMethod("isBlankOrNull", String.class);
    isBlankOrNull.setAccessible(true);

    assertTrue((Boolean) isBlankOrNull.invoke(fileProcessorUtil, (String) null));
    assertTrue((Boolean) isBlankOrNull.invoke(fileProcessorUtil, ""));
    assertTrue((Boolean) isBlankOrNull.invoke(fileProcessorUtil, "   "));
    assertFalse((Boolean) isBlankOrNull.invoke(fileProcessorUtil, "test"));
  }

  @Test
  void testProcessCsvRow_Exception() throws Exception {
    Method processCsvRow =
        FileProcessorUtil.class.getDeclaredMethod(
            "processCsvRow", String[].class, Map.class, int.class, List.class);
    processCsvRow.setAccessible(true);

    String[] row = {"John", "john@example.com", "USER"};
    Map<String, Integer> headerMap = Map.of("name", 0, "email", 1, "role", 2);
    List<FileProcessorUtil.FileUserData> users = new ArrayList<>();

    // This should not throw exception but handle it gracefully
    assertDoesNotThrow(() -> processCsvRow.invoke(fileProcessorUtil, row, headerMap, 1, users));
  }

  @Test
  void testGetValueFromRow_IndexOutOfBounds() throws Exception {
    Method getValueFromRow =
        FileProcessorUtil.class.getDeclaredMethod(
            "getValueFromRow", String[].class, Map.class, String.class);
    getValueFromRow.setAccessible(true);

    String[] row = {"John"};
    Map<String, Integer> headerMap = Map.of("name", 0, "email", 5); // Index 5 is out of bounds

    String result = (String) getValueFromRow.invoke(fileProcessorUtil, row, headerMap, "email");
    assertNull(result);
  }

  @Test
  void testGetValueFromRow_HeaderNotFound() throws Exception {
    Method getValueFromRow =
        FileProcessorUtil.class.getDeclaredMethod(
            "getValueFromRow", String[].class, Map.class, String.class);
    getValueFromRow.setAccessible(true);

    String[] row = {"John"};
    Map<String, Integer> headerMap = Map.of("name", 0);

    String result = (String) getValueFromRow.invoke(fileProcessorUtil, row, headerMap, "email");
    assertNull(result);
  }

  @Test
  void testGetCellValue_NullIndex() throws Exception {
    Method getCellValue =
        FileProcessorUtil.class.getDeclaredMethod(
            "getCellValue", Row.class, Map.class, String.class);
    getCellValue.setAccessible(true);

    Row row = mock(Row.class);
    Map<String, Integer> headerMap = Map.of("name", 0);

    String result = (String) getCellValue.invoke(fileProcessorUtil, row, headerMap, "email");
    assertNull(result);
  }

  @Test
  void testGetCellValueAsString_NullCell() throws Exception {
    Method getCellValueAsString =
        FileProcessorUtil.class.getDeclaredMethod("getCellValueAsString", Cell.class);
    getCellValueAsString.setAccessible(true);

    String result = (String) getCellValueAsString.invoke(fileProcessorUtil, (Cell) null);
    assertNull(result);
  }

  @Test
  void testGetCellValueAsString_BlankCell() throws Exception {
    Method getCellValueAsString =
        FileProcessorUtil.class.getDeclaredMethod("getCellValueAsString", Cell.class);
    getCellValueAsString.setAccessible(true);

    Cell cell = mock(Cell.class);
    when(cell.getCellType()).thenReturn(CellType.BLANK);

    String result = (String) getCellValueAsString.invoke(fileProcessorUtil, cell);
    assertEquals("", result);
  }

  @Test
  void testGetCellValueAsString_DateCell() throws Exception {
    Method getCellValueAsString =
        FileProcessorUtil.class.getDeclaredMethod("getCellValueAsString", Cell.class);
    getCellValueAsString.setAccessible(true);

    Cell cell = mock(Cell.class);
    when(cell.getCellType()).thenReturn(CellType.NUMERIC);
    when(cell.getDateCellValue()).thenReturn(new Date());

    // Mock static method DateUtil.isCellDateFormatted
    try (var mockedDateUtil = mockStatic(DateUtil.class)) {
      mockedDateUtil.when(() -> DateUtil.isCellDateFormatted(cell)).thenReturn(true);

      String result = (String) getCellValueAsString.invoke(fileProcessorUtil, cell);
      assertNotNull(result);
    }
  }

  @Test
  void testGetCellValueAsString_NumericCellWithDecimal() throws Exception {
    Method getCellValueAsString =
        FileProcessorUtil.class.getDeclaredMethod("getCellValueAsString", Cell.class);
    getCellValueAsString.setAccessible(true);

    Cell cell = mock(Cell.class);
    when(cell.getCellType()).thenReturn(CellType.NUMERIC);
    when(cell.getNumericCellValue()).thenReturn(123.45);

    try (var mockedDateUtil = mockStatic(DateUtil.class)) {
      mockedDateUtil.when(() -> DateUtil.isCellDateFormatted(cell)).thenReturn(false);

      String result = (String) getCellValueAsString.invoke(fileProcessorUtil, cell);
      assertEquals("123.45", result);
    }
  }

  private byte[] createValidExcelFile() throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet();

    // Create header row
    Row headerRow = sheet.createRow(0);
    headerRow.createCell(0).setCellValue("name");
    headerRow.createCell(1).setCellValue("email");
    headerRow.createCell(2).setCellValue("role");

    // Create data rows
    Row row1 = sheet.createRow(1);
    row1.createCell(0).setCellValue("John Doe");
    row1.createCell(1).setCellValue("john@example.com");
    row1.createCell(2).setCellValue("USER");

    Row row2 = sheet.createRow(2);
    row2.createCell(0).setCellValue("Jane Smith");
    row2.createCell(1).setCellValue("jane@example.com");
    row2.createCell(2).setCellValue("ADMIN");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    workbook.write(baos);
    workbook.close();
    return baos.toByteArray();
  }

  private byte[] createEmptyExcelFile() throws IOException {
    Workbook workbook = new XSSFWorkbook();
    workbook.createSheet();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    workbook.write(baos);
    workbook.close();
    return baos.toByteArray();
  }

  private byte[] createExcelWithMissingHeaders() throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet();

    // Create header row with wrong headers
    Row headerRow = sheet.createRow(0);
    headerRow.createCell(0).setCellValue("fullname");
    headerRow.createCell(1).setCellValue("emailaddress");
    headerRow.createCell(2).setCellValue("position");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    workbook.write(baos);
    workbook.close();
    return baos.toByteArray();
  }

  private byte[] createExcelWithNumericValues() throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet();

    Row headerRow = sheet.createRow(0);
    headerRow.createCell(0).setCellValue("name");
    headerRow.createCell(1).setCellValue("email");
    headerRow.createCell(2).setCellValue("role");

    Row row1 = sheet.createRow(1);
    row1.createCell(0).setCellValue(123); // Numeric value
    row1.createCell(1).setCellValue("john@example.com");
    row1.createCell(2).setCellValue("USER");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    workbook.write(baos);
    workbook.close();
    return baos.toByteArray();
  }

  private byte[] createExcelWithBooleanValues() throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet();

    Row headerRow = sheet.createRow(0);
    headerRow.createCell(0).setCellValue("name");
    headerRow.createCell(1).setCellValue("email");
    headerRow.createCell(2).setCellValue("role");

    Row row1 = sheet.createRow(1);
    row1.createCell(0).setCellValue("John");
    row1.createCell(1).setCellValue(true); // Boolean value
    row1.createCell(2).setCellValue("USER");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    workbook.write(baos);
    workbook.close();
    return baos.toByteArray();
  }

  private byte[] createExcelWithFormulaValues() throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet();

    Row headerRow = sheet.createRow(0);
    headerRow.createCell(0).setCellValue("name");
    headerRow.createCell(1).setCellValue("email");
    headerRow.createCell(2).setCellValue("role");

    Row row1 = sheet.createRow(1);
    row1.createCell(0).setCellValue("John");
    Cell formulaCell = row1.createCell(1);
    formulaCell.setCellFormula("1+1"); // Formula value
    row1.createCell(2).setCellValue("USER");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    workbook.write(baos);
    workbook.close();
    return baos.toByteArray();
  }

  private byte[] createExcelWithEmptyRows() throws IOException {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet();

    Row headerRow = sheet.createRow(0);
    headerRow.createCell(0).setCellValue("name");
    headerRow.createCell(1).setCellValue("email");
    headerRow.createCell(2).setCellValue("role");

    // Skip row 1 (empty row)

    Row row2 = sheet.createRow(2);
    row2.createCell(0).setCellValue("John Doe");
    row2.createCell(1).setCellValue("john@example.com");
    row2.createCell(2).setCellValue("USER");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    workbook.write(baos);
    workbook.close();
    return baos.toByteArray();
  }
}
