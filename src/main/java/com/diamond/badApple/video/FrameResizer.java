package com.diamond.badApple.video;

import static java.awt.Image.SCALE_SMOOTH;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrameResizer {

  private static final Logger logger = LoggerFactory.getLogger(FrameResizer.class);
  private final File frameDir;
  private final int resizedWidth;

  public FrameResizer(File frameDir, int resizedWidth) {
    this.frameDir = frameDir;
    this.resizedWidth = resizedWidth;
  }

  public void resizeFrames() {
    File[] frames = frameDir.listFiles();

    if (frames == null) {
      logger.error("No frames found in {}", frameDir.getAbsolutePath());
      return;
    }

    ExecutorService executor = Executors.newFixedThreadPool(16);

    ProgressBarBuilder pbb =
        new ProgressBarBuilder()
            .setTaskName("Resizing frames")
            .setInitialMax(frames.length)
            .setStyle(ProgressBarStyle.ASCII);

    try (var pb = pbb.build()) {

      List<Future<?>> futures = new ArrayList<>();

      for (File frame : frames) {
        var future = executor.submit(() -> resizeFrame(frame));
        futures.add(future);
      }

      while (!futures.isEmpty()) {
        var iterator = futures.iterator();

        while (iterator.hasNext()) {
          var future = iterator.next();
          if (future.isDone()) {
            iterator.remove();
            pb.step();
          }
        }
      }
    }

    executor.shutdown();
  }

  private void resizeFrame(File frame) {
    try {
      BufferedImage image = ImageIO.read(frame);
      float aspectRatio = (float) image.getWidth() / image.getHeight();
      int newWidth = resizedWidth;
      int newHeight = (int) (newWidth / aspectRatio);

      Image resized = image.getScaledInstance(newWidth, newHeight, SCALE_SMOOTH);

      BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, TYPE_INT_RGB);

      Graphics2D graphics = scaledImage.createGraphics();
      graphics.drawImage(resized, 0, 0, null);
      graphics.dispose();

      ImageIO.write(scaledImage, "png", frame);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
