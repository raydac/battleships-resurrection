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

import static com.igormaznitsa.battleships.utils.Utils.RND;


import com.igormaznitsa.battleships.gui.Animation;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Collections;

public class FishSprite extends FieldSprite {

  private final Animation animation = Animation.KILKA;
  private int frame;
  private int developmentLevel;

  public FishSprite(final Point cell) {
    super(Collections.singletonList(cell), 1.0d, true);
    this.frame = RND.nextInt(this.animation.getLength());
    this.developmentLevel = DEVELOPMENT_LEVELS;
  }

  @Override
  public void nextFrame() {
    if (this.developmentLevel > 0) {
      this.developmentLevel--;
    }
    this.frame++;
    if (this.frame >= animation.getLength()) {
      this.frame = 0;
    }
  }

  @Override
  public void render(final Graphics2D g2d) {
    final Composite oldComposite = g2d.getComposite();
    if (this.developmentLevel > 0) {
      final AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
          (1.0f / DEVELOPMENT_LEVELS) * (DEVELOPMENT_LEVELS - this.developmentLevel));
      g2d.setComposite(alphaComposite);
    }
    g2d.drawImage(this.animation.getFrame(this.frame), null, this.spritePoint.x,
        this.spritePoint.y);
    if (this.developmentLevel > 0) {
      g2d.setComposite(oldComposite);
    }
  }

}
