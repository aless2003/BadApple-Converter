package com.diamond.badApple;

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
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameGrabber.Exception;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Frame.Type;
import org.bytedeco.javacv.Java2DFrameConverter;

public class FrameExtractor {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(
      FrameExtractor.class);

  private final File videoFile;
  private final File outDir;

  public FrameExtractor(File videoFile, File outDir) {
    this.videoFile = videoFile;
    this.outDir = outDir;
  }

  private BufferedImage convertFrameToImage(Frame frame) {
    BufferedImage image;
    try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
      image = converter.convert(frame);
    }
    return image;
  }

  public void extractFrames() {
    try (FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoFile)) {

      frameGrabber.start();

      double curMax = frameGrabber.getLengthInVideoFrames();

      ProgressBarBuilder pbb = new ProgressBarBuilder()
          .setTaskName("Extracting frames")
          .setInitialMax((long) curMax)
          .setStyle(ProgressBarStyle.ASCII);

      try (var pb = pbb.build()) {

        ExecutorService executor = Executors.newFixedThreadPool(16);
        List<Future<?>> futures = new ArrayList<>();

        int numPerRound = 100;
        int numRounds = (int) Math.ceil(curMax / numPerRound);

        for (int i = 0; i < numRounds; i++) {
          for (int j = 0; j < numPerRound; j++) {
            Frame nextFrame = getNextFrame(frameGrabber);

            if (nextFrame == null) {
              break;
            }

            Frame convFrame = nextFrame.clone();

            int frameNumber = numPerRound * i + j;
            var future = executor.submit(() -> convertFrameToImage(convFrame, frameNumber));

            futures.add(future);
          }

          while (!futures.isEmpty()) {
            int size = futures.size();
            futures.removeIf(Future::isDone);
            pb.stepBy(size - futures.size());
          }

        }

      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void convertFrameToImage(Frame nextFrame, int finalI) {
    try {
      BufferedImage image = convertFrameToImage(nextFrame);
      nextFrame.close();
      String name = String.format("frame-%d.png", finalI);
      ImageIO.write(image, "png", new File(outDir, name));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Frame getNextFrame(FFmpegFrameGrabber grabber) throws Exception {
    Frame frame = new Frame();
    frame.type = Type.SUBTITLE;
    while (frame != null && frame.type != Type.VIDEO) {
      frame = grabber.grab();
    }
    return frame;
  }
}
