package com.diamond.badApple.video;

import com.sapher.youtubedl.YoutubeDL;
import com.sapher.youtubedl.YoutubeDLException;
import com.sapher.youtubedl.YoutubeDLRequest;
import com.sapher.youtubedl.YoutubeDLResponse;
import java.io.File;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.progress.EncoderProgressListener;

public class YouTubeDownloader {

  private static final Logger logger = LoggerFactory.getLogger(YouTubeDownloader.class);

  public void download(String url, String output, File videoDir, String audioFile) {
    YoutubeDL.setExecutablePath("bin/youtube-dl");
    YoutubeDLRequest request = new YoutubeDLRequest(url);
    output = videoDir.getPath() + "/" + output + ".mp4";

    request.setOption("output", output);
    // format to 720p mp4
    request.setOption("format", "22");

    File file = new File(output);

    if (!file.exists()) {
      try {
        logger.info("Downloading video...");
        YoutubeDLResponse response = YoutubeDL.execute(request);
        logger.info("Download complete!");
        if (response.getExitCode() != 0) {
          throw new RuntimeException("Failed to download video");
        }
      } catch (YoutubeDLException e) {
        logger.error("Couldn't download Video", e);
      }
    } else {
      logger.info("Video {} already exists, skipping download", output);
    }

    File audio = new File(audioFile);

    if (audio.exists()) {
      logger.info("Audio {} already exists, skipping conversion", audioFile);
      return;
    }

    convertMp4toMp3(output, audioFile);
  }

  private void convertMp4toMp3(String videoPath, String audioPath) {

    ProgressBarBuilder pbb =
        new ProgressBarBuilder()
            .setTaskName("Converting video to audio")
            .setStyle(ProgressBarStyle.ASCII)
            .setInitialMax(1000);

    try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath);
        ProgressBar pb = pbb.build()) {
      File source = new File(videoPath);
      File target = new File(audioPath);

      grabber.start();

      AudioAttributes audio = new AudioAttributes();
      audio.setCodec("libmp3lame");
      audio.setBitRate(grabber.getAudioBitrate());
      audio.setChannels(grabber.getAudioChannels());
      audio.setSamplingRate(grabber.getSampleRate());

      EncodingAttributes encodingAttributes = new EncodingAttributes();
      encodingAttributes.setInputFormat("mp4");
      encodingAttributes.setOutputFormat("mp3");
      encodingAttributes.setAudioAttributes(audio);

      ConvertProgressListener listener = new ConvertProgressListener(pb);

      Encoder encoder = new Encoder();
      encoder.encode(new MultimediaObject(source), target, encodingAttributes, listener);

    } catch (Exception | EncoderException e) {
      throw new RuntimeException(e);
    }
  }

  private static class ConvertProgressListener implements EncoderProgressListener {

    ProgressBar bar;

    public ConvertProgressListener(ProgressBar bar) {
      this.bar = bar;
    }

    @Override
    public void sourceInfo(MultimediaInfo info) {
      logger.debug("Source info: {}", info);
    }

    @Override
    public void progress(int permil) {
      bar.stepTo(permil);
    }

    @Override
    public void message(String message) {
      logger.debug("{}", message);
    }
  }
}
