/*
 *    Battleships PC client with GFX multi-player game support
 *    Copyright (C) 2021 Igor Maznitsa
 *
 *    This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 */

package com.igormaznitsa.battleships.sound;

import com.igormaznitsa.battleships.utils.Utils;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused"})
public final class SoundClip implements AutoCloseable {

  private final AtomicBoolean playing = new AtomicBoolean();
  private final Clip clip;

  public SoundClip(final String resource) {
    try {
      final byte[] data = Utils.readResourceAsBytes("/assets/snd/" + resource);
      this.clip = AudioSystem.getClip();
      final AudioInputStream audioStream =
              AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
      this.clip.addLineListener(event -> {
        if (event.getType() == LineEvent.Type.STOP) {
          playing.set(false);
        }
      });
      this.clip.open(audioStream);
    } catch (Exception ex) {
      throw new RuntimeException("Error during load and init sound clip: " + resource, ex);
    }
  }

  @Override
  public synchronized void close() {
    this.stop();
    this.clip.close();
  }

  public synchronized SoundClip play() {
    this.stop();
    if (this.playing.compareAndSet(false, true)) {
      this.clip.setFramePosition(0);
      this.clip.start();
    }
    return this;
  }


  public synchronized SoundClip play(final int loops) {
    this.stop();
    if (this.playing.compareAndSet(false, true)) {
      this.clip.setFramePosition(0);
      this.clip.setLoopPoints(0, -1);
      this.clip.loop(loops);
    }
    return this;
  }

  public synchronized SoundClip stop() {
    if (this.playing.compareAndSet(true, false)
            && this.clip.isActive()) {
        this.clip.stop();
      }
    return this;
  }

  public boolean isPlaying() {
    return this.playing.get();
  }
}
