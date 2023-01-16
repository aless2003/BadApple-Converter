package com.diamond.badApple;

import com.diamond.badApple.ascii.FrameAsciiProcessor;
import java.io.File;
import java.io.IOException;
import javazoom.jl.decoder.JavaLayerException;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static final String VIDEO_NAME = "amatsuki";
  private static final String VIDEO_FILE = "input/" + VIDEO_NAME + ".mp4";
  private static final String AUDIO_FILE = "input/" + VIDEO_NAME + ".mp3";
  public static final int RESIZED_WIDTH = 300;

  public static void main(String[] args) throws IOException, JavaLayerException {
    File outDir = new File("out");
    File audioFile = new File(AUDIO_FILE);
    logger.info("test");
    cleanUp();
    vidToFrames(outDir);
    resizeFrames(outDir);
    System.out.print(Ansi.ansi().eraseScreen());
    AudioPlayer player = new AudioPlayer(audioFile);
    framesToStr(outDir, player);
  }

  private static void resizeFrames(File outDir) {
    FrameResizer resizer = new FrameResizer(outDir, RESIZED_WIDTH);
    resizer.resizeFrames();
  }

  private static void framesToStr(File outDir, AudioPlayer player) {
    FrameAsciiProcessor processor = new FrameAsciiProcessor(player);
    processor.convertAndPrint(outDir);
  }

  private static void vidToFrames(File outDir) {
    File badApple = new File(VIDEO_FILE);
    if (!outDir.exists() && !outDir.mkdirs()) {
      logger.error("Could not create output directory");
    }
    var extractor = new FrameExtractor(badApple, outDir);
    extractor.extractFrames();
  }

  private static void cleanUp() {
    File outDir = new File("out");

    // delete all files in outDir
    ProgressBarBuilder pbb = new ProgressBarBuilder()
        .setTaskName("Cleaning up")
        .setStyle(ProgressBarStyle.ASCII)
        .setInitialMax(1);

    File[] files = outDir.listFiles();
    if (files != null) {
      ProgressBar.wrap(files, pbb)
          .parallel()
          .forEach(File::delete);
    }
  }
}
