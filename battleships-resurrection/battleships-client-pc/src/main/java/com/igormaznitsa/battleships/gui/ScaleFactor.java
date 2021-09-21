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

package com.igormaznitsa.battleships.gui;

import com.igormaznitsa.battleships.gui.panels.BasePanel;

import java.awt.*;
import java.awt.event.MouseEvent;

public final class ScaleFactor {
  private final double sx;
  private final double sy;

  public ScaleFactor(final GraphicsConfiguration graphicsConfiguration) {
    final Rectangle screen = graphicsConfiguration.getBounds();
    this.sx = screen.getWidth() / (double) BasePanel.GAMEFIELD_WIDTH;
    this.sy = screen.getHeight() / (double) BasePanel.GAMEFIELD_HEIGHT;
  }

  public double getScaleX() {
    return this.sx;
  }

  public double getScaleY() {
    return this.sy;
  }

  public void apply(final Graphics2D gfx) {
    gfx.scale(this.sx, this.sy);
  }

  @Override
  public String toString() {
    return "ScaleFactor[sx=" + this.sx + ", sy=" + this.sy + ']';
  }

  public Point translatePoint(final MouseEvent event) {
    translatePoint(event.getPoint());
    return event.getPoint();
  }

  public void translatePoint(final Point point) {
    final int descaledX = (int) Math.round(point.x / this.sx);
    final int descaledY = (int) Math.round(point.y / this.sy);
    point.move(descaledX, descaledY);
  }

}
