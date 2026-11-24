package com.pixelforge.web;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Convierte mapas, listas, textos y números a texto JSON. Se escribe a mano para no depender de
 * librerías externas.
 *
 * <p>GRASP Fabricación pura y Alta cohesión: su único trabajo es el formato JSON.
 */
public final class Json {
  private Json() {}

  public static String write(Object value) {
    StringBuilder out = new StringBuilder();
    append(out, value);
    return out.toString();
  }

  private static void append(StringBuilder out, Object value) {
    if (value == null) out.append("null");
    else if (value instanceof String text) string(out, text);
    else if (value instanceof Double number) out.append(number.isNaN() || number.isInfinite() ? "0" : trim(number));
    else if (value instanceof Number || value instanceof Boolean) out.append(value);
    else if (value instanceof Map<?, ?> map) {
      out.append('{');
      Iterator<? extends Map.Entry<?, ?>> entries = map.entrySet().iterator();
      while (entries.hasNext()) {
        Map.Entry<?, ?> entry = entries.next();
        string(out, String.valueOf(entry.getKey()));
        out.append(':');
        append(out, entry.getValue());
        if (entries.hasNext()) out.append(',');
      }
      out.append('}');
    } else if (value instanceof List<?> list) {
      out.append('[');
      for (int i = 0; i < list.size(); i++) {
        if (i > 0) out.append(',');
        append(out, list.get(i));
      }
      out.append(']');
    } else string(out, value.toString());
  }

  /** 12.0 se escribe 12 y 12.5 queda igual, para que el JSON sea más corto. */
  private static String trim(double number) {
    double rounded = Math.round(number * 100) / 100.0;
    return rounded == Math.rint(rounded) ? String.valueOf((long) rounded) : String.valueOf(rounded);
  }

  private static void string(StringBuilder out, String text) {
    out.append('"');
    for (char c : text.toCharArray()) {
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
          else out.append(c);
        }
      }
    }
    out.append('"');
  }
}
