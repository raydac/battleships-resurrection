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

import static com.igormaznitsa.battleships.gui.panels.GamePanel.findShipRenderPositionForCell;
import static java.util.List.copyOf;
import static java.util.Objects.requireNonNull;


import com.igormaznitsa.battleships.gui.panels.GamePanel;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;

public abstract class FieldSprite implements Comparable<FieldSprite> {
  protected static final int DEVELOPMENT_LEVELS = 25;

  protected final List<Point> cells;
  protected final Point actionCell;
  protected final Point renderPoint;
  protected final double distanceFromPlayer;
  protected final boolean developmentOnStart;

  public FieldSprite(final List<Point> allCells, final Point renderCell, final Point actionCell,
                     final boolean developmentOnStart) {
    this.cells = copyOf(allCells);
    this.developmentOnStart = developmentOnStart;
    this.renderPoint = findShipRenderPositionForCell(renderCell.x, renderCell.y);
    this.actionCell = requireNonNull(actionCell);

    final int delta = Math.abs(actionCell.y - (9 - actionCell.x));

    this.distanceFromPlayer = this.renderPoint.distance(GamePanel.PLAYER_VIEW_POSITION) - delta;
  }

  public Point getActionCell() {
    return this.actionCell;
  }

  public abstract void nextFrame();

  @Override
  public final int hashCode() {
    return renderPoint.hashCode();
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
      return this.renderPoint.equals(((FieldSprite) object).renderPoint);
    }
    return false;
  }

  @Override
  public int compareTo(final FieldSprite that) {
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

  public Point getRenderPoint() {
    return this.renderPoint;
  }

  public boolean containsCell(final Point cell) {
    return cell != null && this.cells.contains(cell);
  }

}
