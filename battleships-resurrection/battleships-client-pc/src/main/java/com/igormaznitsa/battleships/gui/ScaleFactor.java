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

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

public final class ScaleFactor {
  private final double sx;
  private final double sy;

  public ScaleFactor(final GraphicsConfiguration graphicsConfiguration) {
    final Rectangle screen = graphicsConfiguration.getBounds();
    this.sx = screen.getWidth() / 800.0d;
    this.sy = screen.getHeight() / 600.0d;
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

  public Point translateMousePoint(final MouseEvent event) {
    final int descaledX = (int) Math.round(event.getPoint().x / this.sx);
    final int descaledY = (int) Math.round(event.getPoint().y / this.sy);
    return new Point(descaledX, descaledY);
  }

}
