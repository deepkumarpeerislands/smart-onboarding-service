package com.aci.smart_onboarding.util;

import com.aci.smart_onboarding.dto.BrdRules;
import com.aci.smart_onboarding.dto.GuidanceData;
import com.aci.smart_onboarding.exception.FileReadException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FileReader {

  /**
   * Parse file content from a byte array based on the specified file type
   *
   * @param fileContent byte array containing the file content
   * @param fileType the type of file (STANDARD_DATA or USER_RULES)
   * @param clazz the class of objects to return
   * @return a list of objects of the specified type
   */
  public <T> List<T> parseByteArrayContent(byte[] fileContent, FileType fileType, Class<T> clazz) {
    // Detect BOM and encoding
    Charset charset = detectCharset(fileContent);

    // Skip BOM if present
    int offset = 0;
    if (hasBOM(fileContent)) {
      offset = getBOMLength(fileContent);
    }

    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                new ByteArrayInputStream(fileContent, offset, fileContent.length - offset),
                charset))) {

      List<T> resultList = new ArrayList<>();
      String line;

      while ((line = reader.readLine()) != null) {
        if (!line.trim().isEmpty()) {
          Object parsedObject = parseLineBasedOnType(line, fileType);
          if (clazz.isInstance(parsedObject)) {
            resultList.add(clazz.cast(parsedObject));
          }
        }
      }

      return resultList;
    } catch (IOException e) {
      throw new FileReadException("Error reading from byte array", e);
    }
  }

  /** Detect charset from file content by looking at BOM */
  private Charset detectCharset(byte[] content) {
    if (content.length >= 3
        && content[0] == (byte) 0xEF
        && content[1] == (byte) 0xBB
        && content[2] == (byte) 0xBF) {
      return StandardCharsets.UTF_8;
    }

    if (content.length >= 2) {
      // UTF-16LE BOM: FF FE
      if (content[0] == (byte) 0xFF && content[1] == (byte) 0xFE) {
        return StandardCharsets.UTF_16LE;
      }
      // UTF-16BE BOM: FE FF
      if (content[0] == (byte) 0xFE && content[1] == (byte) 0xFF) {
        return StandardCharsets.UTF_16BE;
      }
    }

    // Default to UTF-8 if no BOM found
    return StandardCharsets.UTF_8;
  }

  /** Check if the byte array starts with a BOM */
  private boolean hasBOM(byte[] content) {
    if (content.length >= 3
        && content[0] == (byte) 0xEF
        && content[1] == (byte) 0xBB
        && content[2] == (byte) 0xBF) {
      return true; // UTF-8 BOM
    }

    return content.length >= 2
        && ((content[0] == (byte) 0xFF && content[1] == (byte) 0xFE)
            || // UTF-16LE BOM
            (content[0] == (byte) 0xFE && content[1] == (byte) 0xFF)); // UTF-16BE BOM
  }

  /** Get the length of the BOM in bytes */
  private int getBOMLength(byte[] content) {
    if (content.length >= 3
        && content[0] == (byte) 0xEF
        && content[1] == (byte) 0xBB
        && content[2] == (byte) 0xBF) {
      return 3; // UTF-8 BOM
    }

    if (content.length >= 2
        && ((content[0] == (byte) 0xFF && content[1] == (byte) 0xFE)
            || // UTF-16LE BOM
            (content[0] == (byte) 0xFE && content[1] == (byte) 0xFF))) { // UTF-16BE BOM
      return 2;
    }

    return 0;
  }

  /** Parses a line based on the specified file type */
  private Object parseLineBasedOnType(String line, FileType fileType) {
    switch (fileType) {
      case STANDARD_DATA:
        return parseStandardDataLine(line);
      case USER_RULES:
        return parseUserRulesLine(line);
      default:
        throw new IllegalArgumentException("Unsupported file type: " + fileType);
    }
  }

  /** Parses a line from the franklin_rules.txt file into a StandardData object */
  private GuidanceData parseStandardDataLine(String line) {
    String[] fields = line.split("\\|");

    GuidanceData data = new GuidanceData();

    // Extract rule name (first field)
    if (fields.length > 0) {
      data.setRuleName(fields[0].trim());
    }

    // Extract mapping key (second field)
    if (fields.length > 1) {
      String mappingKey = fields[1].trim();
      data.setMappingKey(mappingKey);
    }

    // Extract match percentage (third field)
    if (fields.length > 2 && !fields[2].trim().isEmpty()) {
      try {
        data.setSimilarity(fields[2].trim());
      } catch (NumberFormatException e) {
        // If not a valid number, leave it as null
      }
    }

    // Extract description (fourth field)
    if (fields.length > 3) {
      data.setExplanation(fields[3].trim());
    }

    return data;
  }

  /** Parses a line from the user_rules.txt file into a UserRules object */
  private BrdRules parseUserRulesLine(String line) {
    String[] fields = line.split("\\|");

    BrdRules rules = new BrdRules();

    // Parse fields based on the format of Frankline.txt
    // BRDId = first field (client ID)
    if (fields.length > 0) {
      rules.setBrdId(fields[0].trim());
    } else {
      rules.setBrdId(null);
    }

    // BRDName = second field (client name)
    if (fields.length > 1) {
      rules.setBrdName(fields[1].trim());
    } else {
      rules.setBrdName(null);
    }

    // ruleId = third field (rule ID)
    if (fields.length > 2) {
      rules.setRuleId(fields[2].trim());
    } else {
      rules.setRuleId(null);
    }

    // ruleName = fourth field (rule name)
    if (fields.length > 3) {
      rules.setRuleName(fields[3].trim());
    } else {
      rules.setRuleName(null);
    }

    // value = fifth field (rule value)
    if (fields.length > 4) {
      rules.setValue(fields[4].trim());
    } else {
      rules.setValue(null);
    }

    // order = sixth field (order number)
    if (fields.length > 5) {
      rules.setOrder(fields[5].trim());
    } else {
      rules.setOrder(null);
    }

    return rules;
  }

  /** Enum representing the types of files that can be parsed */
  public enum FileType {
    STANDARD_DATA,
    USER_RULES
  }
}
