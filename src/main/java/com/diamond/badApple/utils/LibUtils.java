package com.diamond.badApple.utils;

import com.diamond.badApple.constants.OSType;
import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibUtils {

  private static final Logger logger = LoggerFactory.getLogger(LibUtils.class);


  private LibUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static void install() {
    File binDir = new File("bin");
    if (!binDir.exists()) {
      if (!binDir.mkdir()) {
        logger.error("Failed to create bin directory");
        System.exit(-1);
      }
    }

    installYouTubeDL(binDir);
  }

  private static void installYouTubeDL(File binDir) {
    File youtubeDL = new File(binDir, getYouTubeDLName());

    if (youtubeDL.exists()) {
      return;
    }

    URL url = getYouTubeDLURL();
    try {
      ReadableByteChannel channel = Channels.newChannel(url.openStream());

      try (FileOutputStream fileStream = new FileOutputStream(youtubeDL)) {
        FileChannel fileChannel = fileStream.getChannel();
        fileChannel.transferFrom(channel, 0, Long.MAX_VALUE);
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public static String getYouTubeDLName() {
    if (OSUtils.getOS() == OSType.WINDOWS) {
      return "youtube-dl.exe";
    } else {
      return "youtube-dl";
    }
  }

  private static URL getYouTubeDLURL() {
    String base = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/";

    OSType os = OSUtils.getOS();

    try {
      if (OSType.WINDOWS == os) {
        return new URL(base + "yt-dlp.exe");
      } else if (OSType.LINUX == os) {
        return new URL(base + "yt-dlp_linux");
      } else if (OSType.MACOS == os) {
        return new URL(base + "yt-dlp_macos");
      } else {
        throw new IllegalStateException("Unsupported OS");
      }
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

}
