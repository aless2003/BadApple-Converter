package com.diamond.badApple;

import static java.awt.Image.SCALE_SMOOTH;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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

    ProgressBarBuilder pbb =
        new ProgressBarBuilder()
            .setTaskName("Resizing frames")
            .setInitialMax(frames.length)
            .setStyle(ProgressBarStyle.ASCII);

    try (var pb = pbb.build()) {
      for (File frame : frames) {
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
        pb.step();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
