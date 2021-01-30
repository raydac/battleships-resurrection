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

import java.util.NoSuchElementException;

public enum ShipType {
  U_BOAT(1),
  GUARD_SHIP(2),
  DREADNOUGHT(3),
  AIR_CARRIER(4);

  private final int cells;

  ShipType(final int cells) {
    this.cells = cells;
  }

  public static ShipType findForCells(final int cells) {
    for (final ShipType s : ShipType.values()) {
      if (s.cells == cells) {
        return s;
      }
    }
    throw new NoSuchElementException("Can't find any ship for cells: " + cells);
  }

  public int getCells() {
    return this.cells;
  }

}
