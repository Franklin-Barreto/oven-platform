package br.com.f2e.ovenplatform.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public final class JsonUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtils.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private JsonUtils() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  public static String toJson(Object obj) {
    try {
      return OBJECT_MAPPER.writeValueAsString(obj);
    } catch (JacksonException ex) {
      LOGGER.error("Something went wrong while trying to process {}", obj, ex);
      throw new RuntimeException("Failed to serialize object to JSON", ex);
    }
  }

  public static <T> T fromJson(String json, Class<T> clazz) {
    try {
      return OBJECT_MAPPER.readValue(json, clazz);
    } catch (JacksonException e) {
      throw new RuntimeException(e);
    }
  }
}
