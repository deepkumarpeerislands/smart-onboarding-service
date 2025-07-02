package com.aci.smart_onboarding.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Custom serializer to format Instant dates as 'MMM dd, yyyy' (e.g., "Jan 20, 2024") */
public class InstantToFormattedStringSerializer extends JsonSerializer<Instant> {
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault());

  @Override
  public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    if (value == null) {
      gen.writeNull();
    } else {
      gen.writeString(FORMATTER.format(value));
    }
  }
}
