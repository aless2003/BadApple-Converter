package com.diamond.badApple.ascii;

import com.diamond.badApple.AudioPlayer;
import com.diamond.badApple.ConsoleUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.lang3.time.StopWatch;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrameAsciiProcessor {

  private static final Logger logger = LoggerFactory.getLogger(FrameAsciiProcessor.class);
  private final Ascii ascii = new Ascii(true);
  private final AudioPlayer audioPlayer;
  private String[] asciiFrames;
  private int counter = 0;
  private final boolean audio;
  ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
  private final long frameTime;

  public FrameAsciiProcessor(AudioPlayer audioPlayer, boolean audio, double fps) {
    this.audioPlayer = audioPlayer;
    this.audio = audio;
    this.frameTime = (long) (1000000 / fps);
  }

  public void convertAndPrint(File dir) {
    convert(dir);

    CountDownLatch latch = new CountDownLatch(1);

    StopWatch stopWatch = StopWatch.createStarted();
    Thread printThread = new Thread(() -> print(latch));

    printThread.start();

    if (audio) {
      Thread audioThread = new Thread(() -> audioPlayer.play(latch));
      audioThread.start();
    }


    latch.countDown();

    try {
      printThread.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    ConsoleUtils.clearScreen();
    logger.info("Finished in {} time", stopWatch.formatTime());
  }

  private void print(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    executor.scheduleAtFixedRate(this::printNext, 0, frameTime, TimeUnit.MICROSECONDS);
    try {

      boolean terminate = executor.awaitTermination(1, TimeUnit.DAYS);

      if (!terminate) {
        throw new RuntimeException("Failed to terminate");
      }

    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void printNext() {
    // clearScreen();
    if (counter >= asciiFrames.length) {
      executor.shutdown();
      return;
    }
    if (counter == 0) {
      ConsoleUtils.print(asciiFrames[counter]);
    } else {
      String prevFrame = asciiFrames[counter - 1];
      String nextFrame = asciiFrames[counter];
      printDiff(prevFrame, nextFrame);
    }
    counter++;
  }

  private void printDiff(String prevFrame, String nextFrame) {
    int rows = prevFrame.split("\\n").length;
    int cols = prevFrame.split("\\n")[0].length();

    for (int currentRow = 0; currentRow < rows; currentRow++) {

      for (int currentCol = 0; currentCol < cols - 1; currentCol++) {

        int currentIndex = currentRow * (cols + 1) + currentCol;

        char prevChar = prevFrame.charAt(currentIndex);
        char nextChar = nextFrame.charAt(currentIndex);

        if (prevChar != nextChar) {
          Ansi ansi = Ansi.ansi().cursor(currentRow, currentCol).a(nextChar);

          System.out.print(ansi);
        }
      }
    }
  }

  private void convert(File dir) {
    File[] unsortedFrames = dir.listFiles();

    if (unsortedFrames == null) {
      logger.error("No frames found in {}", dir.getAbsolutePath());
      throw new RuntimeException("No frames found");
    }

    // sort frames so it's in numerical order instead of frames 38 coming after 3700
    Comparator<File> comparator =
        Comparator.comparingInt(o -> Integer.parseInt(o.getName().split("-")[1].split("\\.")[0]));

    List<File> frames = Arrays.stream(unsortedFrames).sorted(comparator).toList();

    asciiFrames = new String[frames.size()];

    var builderPB =
        new ProgressBarBuilder()
            .setTaskName("Converting frames")
            .setInitialMax(frames.size())
            .setStyle(ProgressBarStyle.ASCII);

    try (ProgressBar pb = builderPB.build()) {

      for (File frame : frames) {
        BufferedImage image = ImageIO.read(frame);
        String output = ascii.convert(image);
        pb.step();
        asciiFrames[Integer.parseInt(frame.getName().split("-")[1].split("\\.")[0])] = output;
      }
    } catch (IOException e) {
      logger.error("Couldn't Process frames", e);
      throw new RuntimeException(e);
    }

    ConsoleUtils.clearScreen();
  }

}
