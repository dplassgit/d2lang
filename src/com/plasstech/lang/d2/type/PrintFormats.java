package com.plasstech.lang.d2.type;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * Mechanisms to format printing primitives.
 */
public class PrintFormats {
  public enum Format {
    INT("%d"),
    BYTE("0y%02x"),
    LONG("%lldL", "%dL"),
    // this skips ".0", and now the Java implementations do too.
    DOUBLE("%.16g"), // 16=# of significant digits (note, 0.0003 is ONE.)
    TRUE("true"),
    FALSE("false"),
    STRING("%s"),
    BOOL("%s"),
    NULL("null");

    // The CRT (C runtime library) format for printing this type
    public final String spec;
    // The Java format for printing this type.
    public final String javaSpec;

    Format(String spec) {
      this(spec, spec);
    }

    Format(String spec, String javaSpec) {
      this.spec = spec;
      this.javaSpec = javaSpec;
    }
  }

  /** Format the given literal as a String. */
  public static String formatLiteral(Object thing) {
    if (thing instanceof Double) {
      double d = (double) thing;
      BigDecimal bd = BigDecimal.valueOf(d).round(MathContext.DECIMAL64).stripTrailingZeros();
      return bd.toPlainString();
    }
    Format format = getFormat(thing);
    if (format == null) {
      return "null";
    }
    return String.format(format.javaSpec, thing);
  }

  /** Get the format for the given boolean. Might not be useful. */
  public static Format getFormat(boolean thing) {
    return thing ? Format.TRUE : Format.FALSE;
  }

  /** Get the format for the given vartype. Returns null if it cannot be formatted. */
  public static Format getFormat(VarType type) {
    return FORMATS_BY_TYPE.get(type);
  }

  /** Get the format for the given object. Returns null if it cannot be formatted. */
  public static Format getFormat(Object thing) {
    if (thing == null) {
      return Format.NULL;
    }
    if (thing instanceof Boolean) {
      return getFormat(thing == Boolean.TRUE);
    }
    return FORMATS.get(thing.getClass());
  }

  // Unusual location of private data because they're implementation details
  private static final Map<Class<?>, Format> FORMATS =
      ImmutableMap.<Class<?>, Format>builder()
          .put(Integer.class, Format.INT)
          .put(Byte.class, Format.BYTE)
          .put(Long.class, Format.LONG)
          .put(Double.class, Format.DOUBLE)
          .put(String.class, Format.STRING)
          .put(Boolean.class, Format.BOOL).build();

  private static final Map<VarType, Format> FORMATS_BY_TYPE =
      ImmutableMap.<VarType, Format>builder()
          .put(VarType.INT, Format.INT)
          .put(VarType.BYTE, Format.BYTE)
          .put(VarType.LONG, Format.LONG)
          .put(VarType.DOUBLE, Format.DOUBLE)
          .put(VarType.STRING, Format.STRING)
          .put(VarType.BOOL, Format.BOOL) // is this right?
          .put(VarType.NULL, Format.NULL).build();
}
