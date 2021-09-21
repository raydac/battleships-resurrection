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

import java.awt.*;
import java.util.Collections;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class OneTimeWaterEffectSprite extends FieldSprite {
  private final Animation animation;
  private int frame;
  private final Point realSpritePoint;

  public OneTimeWaterEffectSprite(final Point cell, final Optional<ShipSprite> hitShip,
                                  final Animation animation) {
    super(Collections.singletonList(cell), animation == Animation.EXPLODE ? 1000.0d : 1.0d, false);
    this.realSpritePoint =
            animation == Animation.EXPLODE ? hitShip.map(s -> s.spritePoint).orElse(this.spritePoint) :
                    this.spritePoint;
    this.animation = animation;
    this.frame = 0;
  }

  public boolean isCompleted() {
    return this.frame >= this.animation.getLength();
  }

  public Point getCell() {
    return this.cells.get(0);
  }

  public Animation getAnimation() {
    return this.animation;
  }

  @Override
  public void nextFrame() {
    if (!this.isCompleted()) {
      this.frame++;
    }
  }

  @Override
  public void render(final Graphics2D g2d) {
    if (!this.isCompleted()) {
      g2d.drawImage(this.animation.getFrame(this.frame), null, this.realSpritePoint.x,
              this.realSpritePoint.y);
    }
  }

}
