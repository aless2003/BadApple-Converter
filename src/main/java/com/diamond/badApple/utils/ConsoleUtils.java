package com.diamond.badApple.utils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import org.fusesource.jansi.Ansi;

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

  public static void clearScreen() {
    System.out.print(Ansi.ansi().eraseScreen());
    System.out.print(Ansi.ansi().cursor(0, 0));
  }
}
