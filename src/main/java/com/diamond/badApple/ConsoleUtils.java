package com.diamond.badApple;

import java.io.BufferedOutputStream;
import java.io.IOException;

public class ConsoleUtils {

  private static final BufferedOutputStream out = new BufferedOutputStream(System.out);

  public static void print(String str) {
    try {
      out.write(str.getBytes());
      out.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
