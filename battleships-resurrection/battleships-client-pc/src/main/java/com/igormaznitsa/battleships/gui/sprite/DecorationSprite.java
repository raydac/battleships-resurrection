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

package com.igormaznitsa.battleships.gui.sprite;

import com.igormaznitsa.battleships.gui.Animation;
import com.igormaznitsa.battleships.sound.Sound;
import java.awt.Graphics2D;
import java.awt.Point;

public final class DecorationSprite {
  private final Animation animation;
  private final Point renderPoint;
  private int frame;

  public DecorationSprite(final Point renderPoint, final Animation animation, final Sound sound) {
    this.renderPoint = renderPoint;
    this.animation = animation;
    this.frame = 0;
    sound.play();
  }

  public void nextFrame() {
    if (this.frame < this.animation.getLength()) {
      this.frame++;
    }
  }

  public boolean isCompleted() {
    return this.frame >= this.animation.getLength();
  }

  public void render(final Graphics2D gfx) {
    if (!this.isCompleted()) {
      gfx.drawImage(this.animation.getFrame(this.frame), null, this.renderPoint.x,
          this.renderPoint.y);
    }
  }
}
