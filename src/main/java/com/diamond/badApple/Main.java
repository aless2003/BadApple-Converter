package com.diamond.badApple;

import com.diamond.badApple.ascii.FrameAsciiProcessor;
import java.io.File;
import java.io.IOException;
import javazoom.jl.decoder.JavaLayerException;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static String VIDEO_NAME = "bofuri";
  private static String VIDEO_FILE_PATH = "input/" + VIDEO_NAME + ".mp4";
  private static String AUDIO_FILE_PATH = "input/" + VIDEO_NAME + ".mp3";
  private static int RESIZED_WIDTH = 100;

  public static void main(String[] args) throws IOException, JavaLayerException {

    var parser = initArgsParser();

    boolean audio;
    boolean skipImageProcessing;

    try {
      Namespace parsedArgs = parser.parseArgs(args);

      String temp = parsedArgs.getString("name");

      if (temp != null) {
        VIDEO_NAME = temp;
        VIDEO_FILE_PATH = "input/" + VIDEO_NAME + ".mp4";
        AUDIO_FILE_PATH = "input/" + VIDEO_NAME + ".mp3";
      }

      Integer width = parsedArgs.getInt("width");

      if (width != null) {
        RESIZED_WIDTH = width;
      }

      audio = parsedArgs.getBoolean("audio");

      skipImageProcessing = parsedArgs.getBoolean("skip");

    } catch (ArgumentParserException e) {
      parser.handleError(e);
      return;
    }

    File outDir = new File("out");
    File audioFile = new File(AUDIO_FILE_PATH);
    logger.info("test");

    File videoFile = new File(VIDEO_FILE_PATH);

    FrameExtractor frameExtractor = new FrameExtractor(videoFile, outDir);
    if (!skipImageProcessing) {
      cleanUp();
      vidToFrames(outDir, frameExtractor);
      resizeFrames(outDir);
    }
    System.out.print(Ansi.ansi().eraseScreen());
    AudioPlayer player = new AudioPlayer(audioFile);
    double fps = frameExtractor.getFrameRate();
    framesToStr(outDir, player, audio, fps);
  }

  private static @NotNull ArgumentParser initArgsParser() {
    ArgumentParser parser = ArgumentParsers.newFor("Video to ASCII converter").build();

    parser.addArgument("-n", "--name").help("Name of the video and audio file").required(true);

    parser
        .addArgument("-w", "--width")
        .help("Width of the resized video")
        .type(Integer.class)
        .setDefault(RESIZED_WIDTH);

    parser
        .addArgument("-a", "--audio")
        .help("Whether to play music or not")
        .type(Boolean.class)
        .setDefault(true);

    parser
        .addArgument("-s", "--skip")
        .help("Whether it should just play the last video")
        .type(Boolean.class)
        .setDefault(false);

    return parser;
  }

  private static void resizeFrames(File outDir) {
    FrameResizer resizer = new FrameResizer(outDir, RESIZED_WIDTH);
    resizer.resizeFrames();
  }

  private static void framesToStr(File outDir, AudioPlayer player, boolean audio, double fps) {
    FrameAsciiProcessor processor = new FrameAsciiProcessor(player, audio, fps);
    processor.convertAndPrint(outDir);
  }

  private static void vidToFrames(File outDir, FrameExtractor extractor) {
    if (!outDir.exists() && !outDir.mkdirs()) {
      logger.error("Could not create output directory");
    }
    extractor.extractFrames();
  }

  private static void cleanUp() {
    File outDir = new File("out");

    // delete all files in outDir
    ProgressBarBuilder pbb =
        new ProgressBarBuilder()
            .setTaskName("Cleaning up")
            .setStyle(ProgressBarStyle.ASCII)
            .setInitialMax(1);

    File[] files = outDir.listFiles();
    if (files != null) {
      ProgressBar.wrap(files, pbb)
          .parallel()
          .forEach(
              f -> {
                if (!f.delete()) {
                  logger.error("Could not delete file: " + f.getName());
                }
              });
    }
  }
}
