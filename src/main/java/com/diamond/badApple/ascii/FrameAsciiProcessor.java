package com.diamond.badApple.ascii;

import com.diamond.badApple.Config;
import com.diamond.badApple.audio.AudioPlayer;
import com.diamond.badApple.utils.ConsoleUtils;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
  private List<List<List<Color>>> framePixels;
  private int counter = 0;
  private final boolean audio;
  private final boolean color;
  ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
  private final long frameTime;

  public FrameAsciiProcessor(AudioPlayer audioPlayer, double fps, Config config) {
    this.audioPlayer = audioPlayer;
    this.audio = config.isAudio();
    this.color = config.isColor();
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
      // logger.info("Frame: {}", counter);
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

        boolean isSameColor;
        Color nextColor;
        if (color) {
          Color prevColor = framePixels.get(counter - 1).get(currentRow).get(currentCol);
          nextColor = framePixels.get(counter).get(currentRow).get(currentCol);

          isSameColor = prevColor.equals(nextColor);
        } else {
          isSameColor = true;
          nextColor = null;
        }

        if (prevChar != nextChar || !isSameColor) {
          Ansi ansi = Ansi.ansi().cursor(currentRow, currentCol);

          if (color) {
            ansi = ansi.fgRgb(nextColor.getRed(), nextColor.getGreen(), nextColor.getBlue());
          }

          ansi = ansi.a(nextChar);

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

    int stepSize = color ? frames.size() * 2 : frames.size();

    var builderPB =
        new ProgressBarBuilder()
            .setTaskName("Converting frames")
            .setInitialMax(stepSize)
            .setStyle(ProgressBarStyle.ASCII);

    framePixels = new ArrayList<>(frames.size());
    // fill with empty lists
    for (int i = 0; i < frames.size(); i++) {
      framePixels.add(new ArrayList<>());
    }

    ExecutorService converter = new ScheduledThreadPoolExecutor(16);
    ExecutorService colorMapper = new ScheduledThreadPoolExecutor(16);

    List<Future<?>> futures = new ArrayList<>(frames.size() * 2);

    try (ProgressBar pb = builderPB.build()) {

      for (int i = 0, framesSize = frames.size(); i < framesSize; i++) {
        File frame = frames.get(i);
        BufferedImage image = ImageIO.read(frame);

        final String imageName = frame.getName();
        var convertFuture =
            converter.submit(
                () -> {
                  String output = ascii.convert(image);
                  asciiFrames[Integer.parseInt(imageName.split("-")[1].split("\\.")[0])] = output;
                  pb.step();
                });
        futures.add(convertFuture);

        if (color) {
          final int frameIndex = i;
          var mapFuture =
              colorMapper.submit(
                  () -> {
                    addColorFrame(image, frameIndex);
                    pb.step();
                  });
          futures.add(mapFuture);
        }
      }

      while (!futures.isEmpty()) {
        var iter = futures.iterator();
        while (iter.hasNext()) {
          var future = iter.next();
          checkFuture(iter, future);
        }
      }

    } catch (IOException e) {
      logger.error("Couldn't Process frames", e);
      throw new RuntimeException(e);
    }

    converter.shutdown();
    colorMapper.shutdown();

    ConsoleUtils.clearScreen();
  }

  private static void checkFuture(Iterator<Future<?>> iter, Future<?> future) {
    if (future.isDone()) {
      try {
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        logger.error("Couldn't convert frames", e);
        throw new RuntimeException(e);
      }
      iter.remove();
    }
  }

  private void addColorFrame(BufferedImage image, int index) {
    List<List<Color>> pixelList = new ArrayList<>();
    for (int y = 0; y < image.getHeight(); y++) {
      List<Color> row = new ArrayList<>();
      for (int x = 0; x < image.getWidth(); x++) {
        Color color = new Color(image.getRGB(x, y));
        color = ColorMapper.getNearestConstant(color);
        row.add(color);
      }
      pixelList.add(row);
    }
    framePixels.set(index, pixelList);
  }
}
