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

package com.igormaznitsa.battleships.gui.panels;

import static com.igormaznitsa.battleships.utils.Utils.RND;
import static java.lang.Math.min;
import static java.util.Arrays.stream;
import static java.util.Collections.shuffle;
import static java.util.Objects.requireNonNull;


import com.igormaznitsa.battleships.gui.sprite.FieldSprite;
import com.igormaznitsa.battleships.gui.sprite.ShipSprite;
import com.igormaznitsa.battleships.gui.sprite.ShipType;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SuppressWarnings("checkstyle:MissingJavadocMethod")
public final class GameField {
  public static final int FIELD_EDGE = 10;
  private final CellState[] gameField = new CellState[FIELD_EDGE * FIELD_EDGE];
  private final int[] freeShipsCount = new int[4];

  public GameField() {

  }

  private static boolean isValidCoord(final int x, final int y) {
    return x >= 0 && x < FIELD_EDGE && y >= 0 && y < FIELD_EDGE;
  }

  private static Point offset2xy(final int offset) {
    return new Point(offset % FIELD_EDGE, offset / FIELD_EDGE);
  }

  private static int xy2offset(final int x, final int y) {
    if (isValidCoord(x, y)) {
      return x + y * FIELD_EDGE;
    } else {
      throw new IllegalArgumentException("Unexpected coords :" + x + ", " + y);
    }
  }

  public List<FieldSprite> moveFieldToShipSprites() {
    final List<FieldSprite> result = IntStream.range(0, this.gameField.length)
        .filter(x -> this.gameField[x] == CellState.SHIP)
        .mapToObj(x -> tryRemoveShipAt(offset2xy(x)))
        .filter(x -> !x.isEmpty())
        .map(ShipSprite::new)
        .sorted()
        .collect(Collectors.toCollection(ArrayList::new));

    if (stream(this.gameField).anyMatch(x -> x != CellState.EMPTY)) {
      throw new IllegalStateException(
          "Detected wrong state of field, detected non-empty cells after ship remove");
    }
    return result;
  }

  public List<Point> tryRemoveShipAt(final Point cell) {
    List<Point> foundCells = Collections.emptyList();
    int offset = xy2offset(cell.x, cell.y);

    if (this.gameField[offset].isShip()) {
      foundCells = new ArrayList<>();
      this.gameField[offset] = CellState.EMPTY;
      this.changeCellStateAround(offset, CellState.BANNED, CellState.EMPTY);
      foundCells.add(cell);

      for (int dx = -1; dx < 2; dx++) {
        for (int dy = -1; dy < 2; dy++) {
          if (dx == 0 && dy == 0) {
            continue;
          }
          final int cx = cell.x + dx;
          final int cy = cell.y + dy;
          if (isValidCoord(cx, cy)) {
            offset = xy2offset(cx, cy);
            if (this.gameField[offset].isShip()) {
              foundCells.addAll(tryRemoveShipAt(offset2xy(offset)));
            }
          }
        }
      }
    }

    return foundCells;
  }

  public void increaseFreeShips(final int cells) {
    final ShipType shipType = ShipType.findForCells(cells);
    this.freeShipsCount[shipType.ordinal()]++;
  }

  public void ensureBanAroundShips() {
    IntStream.range(0, this.gameField.length)
        .filter(x -> this.gameField[x] == CellState.SHIP || this.gameField[x] == CellState.KILL)
        .forEach(x -> changeCellStateAround(x, CellState.EMPTY, CellState.BANNED));
  }

  public void autoPlacingFreeShips() {
    final List<ShipType> sortedShipTypesFromDescend = Arrays.stream(ShipType.values())
        .sorted((o1, o2) -> Integer.compare(o2.getCells(), o1.getCells()))
        .collect(Collectors.toList());
    for (final ShipType shipType : sortedShipTypesFromDescend) {
      while (this.freeShipsCount[shipType.ordinal()] > 0) {
        final List<Integer> freeCells = IntStream.range(0, this.gameField.length)
            .filter(x -> this.gameField[x] == CellState.EMPTY)
            .boxed()
            .collect(Collectors.toCollection(ArrayList::new));
        if (freeCells.isEmpty()) {
          throw new IllegalStateException("Can't find any free position for ship: " + shipType);
        }

        shuffle(freeCells, RND);
        while (!freeCells.isEmpty()) {
          final Point nextPosition = offset2xy(freeCells.remove(0));
          final List<Direction> directions = Arrays.asList(Direction.values());
          shuffle(directions, RND);
          final Optional<Direction> foundPosition = directions.stream()
              .filter(d -> this.tryInjectShip(nextPosition, shipType, d))
              .findFirst();
          if (foundPosition.isEmpty()) {
            if (freeCells.isEmpty()) {
              throw new IllegalStateException("Can't auto-place ship: " + shipType);
            }
          } else {
            break;
          }
        }
        this.freeShipsCount[shipType.ordinal()]--;
      }
    }
  }

  private boolean tryInjectShip(final Point position, final ShipType shipType,
                                final Direction direction) {
    final int x = position.x;
    final int y = position.y;
    for (int i = 0; i < shipType.getCells(); i++) {
      final int px = x + direction.getDx() * i;
      final int py = y + direction.getDy() * i;
      if (!isValidCoord(px, py) || this.getState(px, py) != CellState.EMPTY) {
        return false;
      }
    }

    for (int i = 0; i < shipType.getCells(); i++) {
      final int px = x + direction.getDx() * i;
      final int py = y + direction.getDy() * i;
      this.gameField[xy2offset(px, py)] = CellState.SHIP;
    }

    this.ensureBanAroundShips();

    return true;
  }

  public boolean hasAnyFreeShip() {
    for (int n : this.freeShipsCount) {
      if (n != 0) {
        return true;
      }
    }
    return false;
  }

  public void setState(int x, int y, CellState ship) {
    this.gameField[xy2offset(x, y)] = requireNonNull(ship);
  }

  public CellState getState(final int x, final int y) {
    return this.gameField[xy2offset(x, y)];
  }

  public Optional<ShipType> findMaxAllowedShipForLen(final int length) {
    return Stream.of(ShipType.values())
        .filter(x -> this.freeShipsCount[x.ordinal()] > 0 && x.getCells() <= length)
        .max(Comparator.comparingInt(ShipType::getCells));
  }

  public void reset() {
    IntStream.range(0, this.gameField.length).forEach(x -> this.gameField[x] = CellState.EMPTY);
    this.freeShipsCount[ShipType.U_BOAT.ordinal()] = 4;
    this.freeShipsCount[ShipType.GUARD_SHIP.ordinal()] = 3;
    this.freeShipsCount[ShipType.DREADNOUGHT.ordinal()] = 2;
    this.freeShipsCount[ShipType.AIR_CARRIER.ordinal()] = 1;
  }

  public int getShipsCount(final ShipType shipType) {
    return this.freeShipsCount[shipType.ordinal()];
  }

  public Optional<Point> removeTarget() {
    int lastFoundOffset = -1;
    for (int i = 0; i < this.gameField.length; i++) {
      if (this.gameField[i] == CellState.TARGET) {
        this.gameField[i] = CellState.EMPTY;
        lastFoundOffset = i;
      }
    }
    return lastFoundOffset < 0 ? Optional.empty() : Optional.of(offset2xy(lastFoundOffset));
  }

  public boolean fixPlaceholder() {
    boolean ok = false;
    final int reservedCells = (int) IntStream.range(0, this.gameField.length)
        .filter(x -> this.gameField[x] == CellState.PLACEHOLDER)
        .count();
    if (reservedCells > 4) {
      throw new IllegalStateException(
          "Detected too long placeholder on the game field: " + reservedCells);
    }
    if (reservedCells != 0) {
      IntStream.range(0, this.gameField.length)
          .filter(x -> this.gameField[x] == CellState.PLACEHOLDER)
          .forEach(x -> {
            this.gameField[x] = CellState.SHIP;
            changeCellStateAround(x, CellState.EMPTY, CellState.BANNED);
          });
      final ShipType shipType = ShipType.findForCells(reservedCells);
      if (this.freeShipsCount[shipType.ordinal()] > 0) {
        this.freeShipsCount[shipType.ordinal()]--;
      } else {
        throw new IllegalStateException("There are no free ships for cells: " + reservedCells);
      }
      ok = true;
    }
    return ok;
  }

  private void changeCellStateAround(final int centerOffset, final CellState from,
                                     final CellState to) {
    final Point point = offset2xy(centerOffset);
    for (int dx = -1; dx < 2; dx++) {
      final int px = point.x + dx;
      if (px < 0 || px >= FIELD_EDGE) {
        continue;
      }
      for (int dy = -1; dy < 2; dy++) {
        final int py = point.y + dy;
        if ((dx == 0 && dy == 0) || py < 0 || py >= FIELD_EDGE) {
          continue;
        }
        final int poffset = xy2offset(px, py);
        if (this.gameField[poffset] == from) {
          this.gameField[poffset] = to;
        }
      }
    }
  }

  public void clearPlaceholder() {
    IntStream.range(0, this.gameField.length)
        .filter(x -> this.gameField[x] == CellState.PLACEHOLDER)
        .forEach(x -> this.gameField[x] = CellState.EMPTY);
  }

  private boolean tryPlaceShip(final Point start, final int length, final Direction direction) {
    int x = start.x;
    int y = start.y;

    int foundFreeCells = 0;
    do {
      foundFreeCells++;
      x += direction.getDx();
      y += direction.getDy();
    } while (x >= 0 && y >= 0 && x < FIELD_EDGE && y < FIELD_EDGE
        && this.gameField[xy2offset(x, y)] == CellState.EMPTY);

    return this.findMaxAllowedShipForLen(min(length, foundFreeCells)).map(shipType -> {
      int sx = start.x;
      int sy = start.y;
      for (int i = 0; i < shipType.getCells(); i++) {
        this.gameField[xy2offset(sx, sy)] = CellState.PLACEHOLDER;
        sx += direction.getDx();
        sy += direction.getDy();
      }
      return true;
    }).orElse(false);
  }


  public boolean tryMakePlaceholder(final Point start, final Point end) {
    final int dx = end.x - start.x;
    final int dy = end.y - start.y;
    if (Math.abs(dx) > Math.abs(dy)) {
      // horizontal one
      if (dx > 0) {
        // on right
        return tryPlaceShip(start, dx + 1, Direction.RIGHT);
      } else {
        // on left
        return tryPlaceShip(start, -dx + 1, Direction.LEFT);
      }
    } else {
      // vertical one
      if (dy > 0) {
        // down
        return tryPlaceShip(start, dy + 1, Direction.DOWN);
      } else {
        // up
        return tryPlaceShip(start, -dy + 1, Direction.UP);
      }
    }
  }

  public boolean tryMarkAsTarget(final Point cell) {
    boolean success = false;
    final int offset = xy2offset(cell.x, cell.y);
    this.removeTarget();
    if (this.gameField[offset] == CellState.EMPTY) {
      this.gameField[offset] = CellState.TARGET;
      success = true;
    }
    return success;
  }

  public boolean hasTarget() {
    return stream(this.gameField).anyMatch(x -> x == CellState.TARGET);
  }

  private enum Direction {
    LEFT(-1, 0), RIGHT(1, 0), UP(0, -1), DOWN(0, 1);
    private final int dx;
    private final int dy;

    Direction(final int dx, final int dy) {
      this.dx = dx;
      this.dy = dy;
    }

    public int getDx() {
      return this.dx;
    }

    public int getDy() {
      return this.dy;
    }
  }

  public enum CellState {
    EMPTY(false),
    PLACEHOLDER(true),
    SHIP(true),
    HIT(true),
    MISS(false),
    KILL(true),
    BANNED(false),
    TARGET(false);

    private final boolean ship;

    CellState(final boolean ship) {
      this.ship = ship;
    }

    public boolean isShip() {
      return this.ship;
    }
  }

}
