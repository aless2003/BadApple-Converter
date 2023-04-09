package com.diamond.badApple;

import lombok.Data;

@Data
public class Config {
  private boolean audio;
  private boolean skipImageProcessing;
  private String downloadUrl;
  private boolean color;
  private String name;
  private int width;
}
