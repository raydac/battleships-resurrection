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

import java.awt.*;
import java.util.List;

import static com.igormaznitsa.battleships.gui.panels.GamePanel.findShipRenderPositionForCell;
import static java.util.List.copyOf;

public abstract class FieldSprite implements Comparable<FieldSprite> {
  protected static final int DEVELOPMENT_LEVELS = 25;

  protected final List<Point> cells;
  protected final Point actionCell;
  protected final Point spritePoint;
  protected final double distanceFromPlayer;
  protected final boolean developmentOnStart;

  public FieldSprite(final List<Point> cells,
                     final double visibilityWeight,
                     final boolean developmentOnStart) {
    this.cells = copyOf(cells);
    this.developmentOnStart = developmentOnStart;

    this.actionCell = findMiddleCell(cells);
    this.spritePoint = findRenderPointInMiddleOfPosition(cells);

    this.distanceFromPlayer = -this.spritePoint.y - visibilityWeight;
  }

  private static boolean isHoriz(final List<Point> cells) {
    return cells.size() == 1 || cells.get(0).x == cells.get(1).x;
  }

  private static Point findRenderPointInMiddleOfPosition(final List<Point> cells) {
    final int x1 = cells.stream().mapToInt(c -> c.x).min().orElse(-1);
    final int y1 = cells.stream().mapToInt(c -> c.y).min().orElse(-1);
    final int x2 = cells.stream().mapToInt(c -> c.x).max().orElse(-1);
    final int y2 = cells.stream().mapToInt(c -> c.y).max().orElse(-1);

    final Point renderA = findShipRenderPositionForCell(x1, y1);
    final Point renderB = findShipRenderPositionForCell(x2, y2);

    return new Point(renderA.x + (renderB.x - renderA.x) / 2,
            renderA.y + (renderB.y - renderA.y) / 2);
  }

  private static Point findMiddleCell(final List<Point> cells) {
    final Point nearestPlayerCell = findMinCell(cells);
    if (isHoriz(cells)) {
      nearestPlayerCell.y -= cells.size() / 2;
    } else {
      nearestPlayerCell.x += cells.size() / 2;
    }
    return nearestPlayerCell;
  }

  private static Point findMinCell(final List<Point> cells) {
    int x = cells.stream().mapToInt(c -> c.x).min().orElse(-1);
    int y = cells.stream().mapToInt(c -> c.y).min().orElse(-1);
    return new Point(x, y);
  }

  public Point getActionCell() {
    return this.actionCell;
  }

  public abstract void nextFrame();

  @Override
  public final int hashCode() {
    return spritePoint.hashCode();
  }

  @Override
  public final boolean equals(final Object object) {
    if (object == null) {
      return false;
    }
    if (object == this) {
      return true;
    }
    if (object.getClass() == this.getClass()) {
      return this.spritePoint.equals(((FieldSprite) object).spritePoint);
    }
    return false;
  }

  @Override
  public final int compareTo(final FieldSprite that) {
    if (that == this) {
      return 0;
    }

    if (this.actionCell.equals(that.actionCell)) {
      if (this instanceof FallingObjectSprite) {
        return 1;
      } else if (that instanceof FallingObjectSprite) {
        return -1;
      }
    }

    return Double.compare(that.distanceFromPlayer, this.distanceFromPlayer);
  }

  public abstract void render(final Graphics2D g2d);

  public boolean containsCell(final Point cell) {
    return cell != null && this.cells.contains(cell);
  }

}
