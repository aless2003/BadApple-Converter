package com.diamond.badApple.video;

import lombok.Data;
import org.bytedeco.javacv.Frame;

@Data
public class FrameEntry {
  private Frame frame;
  private int frameNumber;

  public FrameEntry(Frame frame, int frameNumber) {
    this.frame = frame;
    this.frameNumber = frameNumber;
  }
}
