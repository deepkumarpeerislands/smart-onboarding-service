package com.aci.smart_onboarding.serializer;

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CustomDateSerializerTest {

  private CustomDateSerializer serializer;

  @Mock private JsonGenerator jsonGenerator;

  @Mock private SerializerProvider serializerProvider;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    serializer = new CustomDateSerializer();
  }

  @Test
  @DisplayName("Should serialize non-null date to correct format")
  void serialize_WithNonNullDate_ShouldFormatCorrectly() throws IOException {
    // Given
    LocalDateTime date = LocalDateTime.of(2024, 3, 15, 10, 30);
    String expectedFormat = "Mar 15, 2024";

    // When
    serializer.serialize(date, jsonGenerator, serializerProvider);

    // Then
    verify(jsonGenerator).writeString(expectedFormat);
    verify(jsonGenerator, never()).writeNull();
  }

  @Test
  @DisplayName("Should handle null date by writing null")
  void serialize_WithNullDate_ShouldWriteNull() throws IOException {
    // When
    serializer.serialize(null, jsonGenerator, serializerProvider);

    // Then
    verify(jsonGenerator).writeNull();
    verify(jsonGenerator, never()).writeString(anyString());
  }

  @Test
  @DisplayName("Should handle different months correctly")
  void serialize_WithDifferentMonths_ShouldFormatCorrectly() throws IOException {
    // Given
    LocalDateTime[] dates = {
      LocalDateTime.of(2024, 1, 1, 0, 0), // Jan
      LocalDateTime.of(2024, 6, 15, 0, 0), // Jun
      LocalDateTime.of(2024, 12, 31, 0, 0) // Dec
    };
    String[] expectedFormats = {"Jan 01, 2024", "Jun 15, 2024", "Dec 31, 2024"};

    // When & Then
    for (int i = 0; i < dates.length; i++) {
      serializer.serialize(dates[i], jsonGenerator, serializerProvider);
      verify(jsonGenerator).writeString(expectedFormats[i]);
    }
  }

  @Test
  @DisplayName("Should handle different years correctly")
  void serialize_WithDifferentYears_ShouldFormatCorrectly() throws IOException {
    // Given
    LocalDateTime[] dates = {
      LocalDateTime.of(2020, 3, 15, 0, 0),
      LocalDateTime.of(2024, 3, 15, 0, 0),
      LocalDateTime.of(2030, 3, 15, 0, 0)
    };
    String[] expectedFormats = {"Mar 15, 2020", "Mar 15, 2024", "Mar 15, 2030"};

    // When & Then
    for (int i = 0; i < dates.length; i++) {
      serializer.serialize(dates[i], jsonGenerator, serializerProvider);
      verify(jsonGenerator).writeString(expectedFormats[i]);
    }
  }
}
