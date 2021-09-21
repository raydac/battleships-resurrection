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

import java.awt.*;
import java.util.Collections;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class FallingObjectSprite extends FieldSprite {

  private final Animation animation;
  private final int maxAllowedY;
  private double drawY;
  private int frame;
  private boolean completed;
  private final double stepY;
  private final Point realSpritePoint;

  public FallingObjectSprite(final Animation animation, final Optional<ShipSprite> shipSprite,
                             final Point targetCell, final Sound sound,
                             final int initialAltitude, final int spriteOffsetY, final int steps) {
    super(Collections.singletonList(targetCell), 1.0d, false);
    this.animation = animation;
    this.maxAllowedY = this.spritePoint.y + spriteOffsetY;
    this.frame = 0;
    this.completed = false;
    this.realSpritePoint = shipSprite.map(s -> s.spritePoint).orElse(this.spritePoint);
    this.drawY = this.realSpritePoint.y - initialAltitude;
    this.stepY = (double) initialAltitude / (double) steps;
    sound.play();
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
      g2d.drawImage(this.animation.getFrame(this.frame), null, this.realSpritePoint.x,
              Math.min(this.maxAllowedY, (int) Math.round(this.drawY)));
    }
  }
}
