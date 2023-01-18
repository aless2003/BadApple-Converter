package com.diamond.badApple.utils;

import com.diamond.badApple.constants.OSType;

public class OSUtils {

  public static OSType getOS() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) {
      return OSType.WINDOWS;
    } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
      return OSType.LINUX;
    } else if (os.contains("mac")) {
      return OSType.MACOS;
    } else {
      return OSType.UNKNOWN;
    }
  }
}
