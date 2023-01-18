package com.diamond.badApple;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.CountDownLatch;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

public class AudioPlayer {

  private final Player player;

  public AudioPlayer(File audioFile) throws FileNotFoundException, JavaLayerException {
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(audioFile));

    player = new Player(in);
  }

  public void play(CountDownLatch latch) {
    try {
      latch.await();
      while (!player.isComplete()) {
        player.play(1);
      }
    } catch (JavaLayerException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
