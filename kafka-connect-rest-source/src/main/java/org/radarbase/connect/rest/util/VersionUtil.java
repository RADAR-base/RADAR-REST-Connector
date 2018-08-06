package org.radarbase.connect.rest.util;

public final class VersionUtil {
  private VersionUtil() {
    // utility class
  }

  public static String getVersion() {
    try {
      return VersionUtil.class.getPackage().getImplementationVersion();
    } catch (Exception ex) {
      return "0.0.0.0";
    }
  }
}
