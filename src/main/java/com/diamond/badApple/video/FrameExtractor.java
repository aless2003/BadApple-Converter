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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameGrabber.Exception;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Frame.Type;
import org.bytedeco.javacv.Java2DFrameConverter;

public class FrameExtractor {

  private final File videoFile;
  private final File outDir;
  private final int resizedWidth;
  private final AtomicInteger frameNumber = new AtomicInteger(0);

  public FrameExtractor(File videoFile, File outDir, int resizedWidth) {
    this.videoFile = videoFile;
    this.outDir = outDir;
    this.resizedWidth = resizedWidth;
  }

  private BufferedImage convertFrameToImage(Frame frame) {
    BufferedImage image;
    try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
      image = converter.convert(frame);
    } catch (java.lang.Exception e) {
      throw new RuntimeException(e);
    }
    return image;
  }

  public void extractFrames() {
    try (FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoFile)) {

      frameGrabber.start();

      double curMax = frameGrabber.getLengthInVideoFrames();

      ProgressBarBuilder pbb =
          new ProgressBarBuilder()
              .setTaskName("Extracting frames")
              .setInitialMax((long) curMax)
              .setStyle(ProgressBarStyle.ASCII);

      ProgressBarBuilder resizeBar = new ProgressBarBuilder()
          .setTaskName("Resizing frames")
          .setInitialMax((long) curMax)
          .setStyle(ProgressBarStyle.ASCII);

      try (var pb = pbb.build(); var resizePb = resizeBar.build()) {

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(16);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < executor.getMaximumPoolSize(); i++) {
          var future = executor.submit(() -> {
            FrameEntry entry = getNextFrameFromVideo(frameGrabber);
            while (entry != null) {
              var extractedFrameImage = convertFrameToImage(entry);

              if (extractedFrameImage == null) {
                return;
              }
              pb.step();

              resizeFrame(extractedFrameImage, entry.getFrameNumber());
              resizePb.step();
              entry = getNextFrameFromVideo(frameGrabber);
            }
          });
          futures.add(future);
        }


        while (futures.stream().anyMatch(future -> !future.isDone())) {
          Thread.sleep(100);
        }

        executor.shutdown();

        futures.forEach(future -> {
          try {
            future.get();
          } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
          }
        });

      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public double getFrameRate() {
    try (FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoFile)) {
      frameGrabber.start();
      return frameGrabber.getFrameRate();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private BufferedImage convertFrameToImage(FrameEntry entry) {
    BufferedImage image = convertFrameToImage(entry.getFrame());
    entry.getFrame().close();
    return image;
  }

  private FrameEntry getNextFrameFromVideo(FFmpegFrameGrabber grabber) {
    try {
      Frame videoFrame = new Frame();
      do {
        videoFrame.close();
        videoFrame = grabber.grabImage();

        if (videoFrame != null) {
          videoFrame = videoFrame.clone();
        }

        if (videoFrame == null) {
          return null;
        }
      } while (videoFrame.type != Type.VIDEO);

      int number = frameNumber.getAndIncrement();

      return new FrameEntry(videoFrame, number);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void resizeFrame(BufferedImage frame, int frameNumber) {
    try {
      float aspectRatio = (float) frame.getWidth() / frame.getHeight();
      int newWidth = resizedWidth;
      int newHeight = (int) (newWidth / aspectRatio);

      Image resized = frame.getScaledInstance(newWidth, newHeight, SCALE_SMOOTH);

      BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, TYPE_INT_RGB);

      Graphics2D graphics = scaledImage.createGraphics();
      graphics.drawImage(resized, 0, 0, null);
      graphics.dispose();

      File out = new File(outDir, String.format("frame-%d.png", frameNumber));

      ImageIO.write(scaledImage, "png", out);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
