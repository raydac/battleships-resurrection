/*
 *    Battleships PC client for GEX server
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
import java.util.Collections;

public abstract class FallingObjectSprite extends FieldSprite {

  private final Animation animation;
  private final int maxAllowedY;
  private double drawY;
  private int steps;
  private int frame;
  private boolean completed;
  private double stepY;

  public FallingObjectSprite(final Animation animation, final Point targetCell, final Sound sound,
                             final int initialAltitude, final int spriteOffsetY, final int steps) {
    super(Collections.singletonList(targetCell), targetCell, targetCell, false);
    this.animation = animation;
    this.maxAllowedY = this.renderPoint.y + spriteOffsetY;
    this.frame = 0;
    this.completed = false;
    this.drawY = this.getRenderPoint().y - initialAltitude;
    this.stepY = (double) initialAltitude / (double) steps;
    sound.getClip().play();
  }

  @Override
  public void nextFrame() {
    if (!this.completed) {
      this.frame++;
      if (this.frame >= this.animation.getLength()) {
        this.frame = 0;
      }
      this.drawY += this.stepY;
      if (this.drawY >= this.maxAllowedY) {
        this.completed = true;
      }
    }
  }

  public boolean isCompleted() {
    return this.completed;
  }

  @Override
  public void render(final Graphics2D g2d) {
    if (!this.completed) {
      g2d.drawImage(this.animation.getFrame(this.frame), null, this.renderPoint.x,
          Math.min(this.maxAllowedY, (int) Math.round(this.drawY)));
    }
  }
}
