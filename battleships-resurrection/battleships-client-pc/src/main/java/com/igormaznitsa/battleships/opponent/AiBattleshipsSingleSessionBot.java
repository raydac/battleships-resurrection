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

package com.igormaznitsa.battleships.opponent;

import com.igormaznitsa.battleships.utils.Utils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Collections.shuffle;
import static java.util.List.of;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.IntStream.range;

public final class AiBattleshipsSingleSessionBot implements BattleshipsPlayer {

  private static final Logger LOGGER =
          Logger.getLogger(AiBattleshipsSingleSessionBot.class.getName());

  private static final int FIELD_EDGE = 10;
  private final Random random = new Random(System.currentTimeMillis());
  private final BlockingQueue<BsGameEvent> outQueue = new ArrayBlockingQueue<>(10);
  private final BlockingQueue<BsGameEvent> inQueue = new ArrayBlockingQueue<>(10);
  private final List<MapItem> myMap = new ArrayList<>();
  private final List<MapItem> enemyMap = new ArrayList<>();

  private final int[] counterOfEnemyShips;
  private final int[] counterOfMyShips;

  private final Thread thread;


  public AiBattleshipsSingleSessionBot() {
    super();
    this.placeShipsOnGameField();
    this.counterOfMyShips = new int[]{4, 3, 2, 1};
    this.counterOfEnemyShips = new int[]{4, 3, 2, 1};
    range(0, FIELD_EDGE * FIELD_EDGE).forEach(x -> this.enemyMap.add(MapItem.EMPTY));
    this.thread = new Thread(() -> {
      LOGGER.info("AI player ready for game session");
      this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_READY, Utils.RND.nextInt(), Utils.RND.nextInt()));
      while (!Thread.currentThread().isInterrupted()) {
        try {
          final BsGameEvent nextEvent = this.inQueue.take();
          this.onIncomingGameEvent(nextEvent);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    }, "bs-ai-bot");
    this.thread.setDaemon(true);
  }

  private static int offset(final int x, final int y) {
    return x + y * FIELD_EDGE;
  }

  private static boolean isShipCell(int x, int y, final List<MapItem> gameField) {
    boolean result = false;
    if (isValid(x, y)) {
      final MapItem state = gameField.get(offset(x, y));
      result = state == MapItem.SHIP || state == MapItem.HIT || state == MapItem.KILLED;
    }
    return result;
  }

  private static List<Integer> allShipOffsets(final int shipX, final int shipY,
                                              final List<MapItem> gameField) {
    if (!isValid(shipX, shipY)) {
      return Collections.emptyList();
    }
    if (isShipCell(shipX, shipY, gameField)) {
      final List<Integer> result = new ArrayList<>();
      result.add(offset(shipX, shipY));
      if (isShipCell(shipX - 1, shipY, gameField) || isShipCell(shipX + 1, shipY, gameField)) {
        // horizontal
        for (int px = shipX - 1; px >= 0; px--) {
          if (isShipCell(px, shipY, gameField)) {
            result.add(offset(px, shipY));
          } else {
            break;
          }
        }
        for (int px = shipX + 1; px < FIELD_EDGE; px++) {
          if (isShipCell(px, shipY, gameField)) {
            result.add(offset(px, shipY));
          } else {
            break;
          }
        }
      } else {
        // vertical
        for (int py = shipY - 1; py >= 0; py--) {
          if (isShipCell(shipX, py, gameField)) {
            result.add(offset(shipX, py));
          } else {
            break;
          }
        }
        for (int py = shipY + 1; py < FIELD_EDGE; py++) {
          if (isShipCell(shipX, py, gameField)) {
            result.add(offset(shipX, py));
          } else {
            break;
          }
        }
      }
      return result;
    } else {
      return Collections.emptyList();
    }
  }

  private static boolean isValid(final int x, final int y) {
    return x >= 0 && x < FIELD_EDGE && y >= 0 && y < FIELD_EDGE;
  }

  private static String asString(final List<MapItem> field) {
    final StringBuilder buffer = new StringBuilder();
    for (int y = 0; y < FIELD_EDGE; y++) {
      if (buffer.length() > 0) {
        buffer.append('\n');
      }
      for (int x = 0; x < FIELD_EDGE; x++) {
        final int offset = x + y * FIELD_EDGE;
        switch (field.get(offset)) {
          case EMPTY:
            buffer.append('.');
            break;
          case SHIP:
            buffer.append('#');
            break;
          case BANNED:
            buffer.append('+');
            break;
          case HIT:
            buffer.append('*');
            break;
          case KILLED:
            buffer.append('X');
            break;
          case MISS:
            buffer.append('-');
            break;
          default:
            buffer.append('?');
            break;
        }
      }
    }
    return buffer.toString();
  }

  public void start() {

  }

  @Override
  public BattleshipsPlayer startPlayer() {
    this.thread.start();
    return this;
  }

  @Override
  public boolean isRemote() {
    return false;
  }

  @Override
  public String getId() {
    return "local-ai-battleships-clients";
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public boolean isReadyForGame() {
    return true;
  }

  @Override
  public void disposePlayer() {
    try {
      this.thread.interrupt();
      this.thread.join();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private boolean canCellContainEnemyShip(final int x, final int y) {
    long horizontalCounter = range(1, 4).takeWhile(i -> {
      final int dx = x - i;
      if (dx < 0) {
        return false;
      }
      return this.enemyMap.get(offset(dx, y)) == MapItem.EMPTY;
    }).count();
    horizontalCounter += range(1, 4).takeWhile(i -> {
      final int dx = x + i;
      if (dx >= FIELD_EDGE) {
        return false;
      }
      return this.enemyMap.get(offset(dx, y)) == MapItem.EMPTY;
    }).count();
    long verticalCounter = range(1, 4).takeWhile(i -> {
      final int dy = y - i;
      if (dy < 0) {
        return false;
      }
      return this.enemyMap.get(offset(x, dy)) == MapItem.EMPTY;
    }).count();
    verticalCounter += range(1, 4).takeWhile(i -> {
      final int dy = y + i;
      if (dy >= FIELD_EDGE) {
        return false;
      }
      return this.enemyMap.get(offset(x, dy)) == MapItem.EMPTY;
    }).count();

    if (this.enemyMap.get(offset(x, y)) == MapItem.EMPTY) {
      horizontalCounter++;
      verticalCounter++;
    }

    final int hMax = (int) horizontalCounter;
    final int vMax = (int) verticalCounter;

    return range(0, 4)
            .filter(i -> this.counterOfEnemyShips[i] > 0)
            .map(i -> i + 1)
            .anyMatch(cells -> cells <= hMax || cells <= vMax);
  }

  private int offerTargetOffset() {
    final List<Integer> hitOffsets = range(0, this.enemyMap.size())
            .filter(x -> this.enemyMap.get(x) == MapItem.HIT).boxed().collect(Collectors.toList());
    if (hitOffsets.isEmpty()) {
      final List<Integer> freeCells = range(0, this.enemyMap.size())
              .filter(i -> this.enemyMap.get(i) == MapItem.EMPTY)
              .boxed()
              .collect(Collectors.toCollection(ArrayList::new));
      shuffle(freeCells, random);
      if (freeCells.isEmpty()) {
        throw new Error("Unexpected state, there is no empty cell but game not completed");
      }
      return freeCells.stream().filter(i ->
              this.canCellContainEnemyShip(i % FIELD_EDGE, i / FIELD_EDGE)
      ).findFirst().orElseThrow(() -> new Error("Can't find any free cell for enemy ship"));
    } else {
      return hitOffsets.stream().map(hitOffset -> {
                final int cx = hitOffset % FIELD_EDGE;
                final int cy = hitOffset / FIELD_EDGE;
                final List<int[]> directions;
                if (isShipCell(cx - 1, cy, this.enemyMap)
                        || isShipCell(cx + 1, cy, this.enemyMap)) {
                  // horizontal
                  directions = new ArrayList<>(of(new int[]{-1, 0}, new int[]{1, 0}));
                } else if (isShipCell(cx, cy - 1, this.enemyMap)
                        || isShipCell(cx, cy + 1, this.enemyMap)) {
                  // vertical
                  directions = new ArrayList<>(of(new int[]{0, -1}, new int[]{0, 1}));
                } else {
                  // any
                  directions = new ArrayList<>(
                          of(new int[]{1, 0}, new int[]{0, 1}, new int[]{-1, 0}, new int[]{0, -1}));
                }
                shuffle(directions, random);
                return directions.stream().filter(d -> {
                  final int px = d[0] + cx;
                  final int py = d[1] + cy;
                  return isValid(px, py)
                          && this.enemyMap.get(offset(px, py)) == MapItem.EMPTY;
                }).map(d -> offset(cx + d[0], cy + d[1])).findFirst();
              }).flatMap(Optional::stream).findFirst()
              .orElseThrow(() -> new Error("Unexpected situation! Can't find empty position to shot!"));
    }
  }

  private void markEnemyShipAsKilled(final int x, final int y) {
    this.enemyMap.set(offset(x, y), MapItem.KILLED);
    final List<Integer> allShipCells = allShipOffsets(x, y, this.enemyMap);
    if (this.counterOfEnemyShips[allShipCells.size() - 1] > 0) {
      this.counterOfEnemyShips[allShipCells.size() - 1]--;
    } else {
      throw new IllegalStateException(
              "Killed a ship which instances already all gone: " + allShipCells);
    }
    allShipCells.forEach(i -> {
      this.enemyMap.set(i, MapItem.KILLED);
      final int cx = i % FIELD_EDGE;
      final int cy = i / FIELD_EDGE;
      for (int dx = -1; dx < 2; dx++) {
        final int px = cx + dx;
        if (px < 0 || px >= FIELD_EDGE) {
          continue;
        }
        for (int dy = -1; dy < 2; dy++) {
          if (dx == 0 || dy == 0) {
            continue;
          }
          final int py = cy + dy;
          if (py < 0 || py >= FIELD_EDGE) {
            continue;
          }
          final int cellOffset = offset(px, py);
          if (this.enemyMap.get(cellOffset) == MapItem.EMPTY) {
            this.enemyMap.set(cellOffset, MapItem.BANNED);
          }
        }
      }
    });
  }

  private void markEnemyCellAsMiss(final int x, final int y) {
    this.enemyMap.set(offset(x, y), MapItem.MISS);
  }

  private void markEnemyShipAsHit(final int x, final int y) {
    this.enemyMap.set(offset(x, y), MapItem.HIT);
    if (isShipCell(x - 1, y, this.enemyMap)
            || isShipCell(x + 1, y, this.enemyMap)) {
      // horizontal
      if (y > 0) {
        this.enemyMap.set(offset(x, y - 1), MapItem.BANNED);
      }
      if (y < FIELD_EDGE - 1) {
        this.enemyMap.set(offset(x, y + 1), MapItem.BANNED);
      }
    } else if (isShipCell(x, y - 1, this.enemyMap)
            || isShipCell(x, y + 1, this.enemyMap)) {
      // vertical
      if (x > 0) {
        this.enemyMap.set(offset(x - 1, y), MapItem.BANNED);
      }
      if (x < FIELD_EDGE - 1) {
        this.enemyMap.set(offset(x + 1, y), MapItem.BANNED);
      }
    }
  }

  private synchronized void onIncomingGameEvent(final BsGameEvent event) {
    switch (requireNonNull(event).getType()) {
      case EVENT_OPPONENT_FIRST_TURN: {
        LOGGER.info("Opponent starts");
        pushIntoOutput(new BsGameEvent(GameEventType.EVENT_DO_TURN, 0, 0));
      }
      break;
      case EVENT_READY: {
        LOGGER.info("incoming ready event: " + event);
      }
      break;
      case EVENT_SHOT_MAIN:
      case EVENT_SHOT_REGULAR: {
        final int cellOffset = offset(event.getX(), event.getY());
        GameEventType result = GameEventType.EVENT_MISS;
        if (this.myMap.get(cellOffset) == MapItem.SHIP) {
          this.myMap.set(cellOffset, MapItem.HIT);
          final List<Integer> shipOffsets =
                  allShipOffsets(event.getX(), event.getY(), this.myMap);
          if (shipOffsets.stream().anyMatch(x -> this.myMap.get(x) == MapItem.SHIP)) {
            result = GameEventType.EVENT_HIT;
          } else {
            shipOffsets.forEach(x -> this.myMap.set(x, MapItem.KILLED));
            this.counterOfMyShips[shipOffsets.size() - 1]--;
            if (this.isThereAnyAliveShip()) {
              result = GameEventType.EVENT_KILLED;
            } else {
              result = GameEventType.EVENT_LOST;
              this.thread.interrupt();
            }
          }
        }
        this.pushIntoOutput(new BsGameEvent(result, event.getX(), event.getY()));
        if (!(result == GameEventType.EVENT_MISS || result == GameEventType.EVENT_LOST)) {
          this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_DO_TURN, 0, 0));
        }
      }
      break;
      case EVENT_DO_TURN: {
        final int targetCell = this.offerTargetOffset();
        if (this.enemyMap.get(targetCell) != MapItem.EMPTY) {
          LOGGER.severe(String.format("Detected choice of non-empty cell (%d,%d)%n%n%s%n%n",
                  (targetCell % FIELD_EDGE), (targetCell / FIELD_EDGE), asString(this.enemyMap)));
          throw new Error(
                  "Detected choice of non-empty cell: x=" + (targetCell % FIELD_EDGE) + ",y="
                          + (targetCell / FIELD_EDGE));
        }
        final int firingShip = this.selectShipToFire();
        this.pushIntoOutput(
                new BsGameEvent(
                        firingShip == 4 ? GameEventType.EVENT_SHOT_MAIN :
                                GameEventType.EVENT_SHOT_REGULAR,
                        targetCell % FIELD_EDGE, targetCell / FIELD_EDGE));
      }
      break;
      case EVENT_KILLED: {
        this.markEnemyShipAsKilled(event.getX(), event.getY());
      }
      break;
      case EVENT_HIT: {
        this.markEnemyShipAsHit(event.getX(), event.getY());
      }
      break;
      case EVENT_MISS: {
        this.markEnemyCellAsMiss(event.getX(), event.getY());
        this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_DO_TURN, 0, 0));
      }
      break;
      case EVENT_LOST:
      case EVENT_CONNECTION_ERROR: {
        this.thread.interrupt();
      }
      break;
      case EVENT_ARRANGEMENT_COMPLETED:
      case EVENT_PAUSE:
      case EVENT_RESUME: {
        // means nothing for AI bot
      }
      break;
      default: {
        LOGGER.severe("Unexpected event: " + event);
        this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_FAILURE, 1, 0));
        throw new Error("Unexpected game event: " + event.getType());
      }
    }
  }

  @Override
  public void pushGameEvent(final BsGameEvent event) {
    if (event != null) {
      try {
        if (!this.inQueue.offer(event, 5, TimeUnit.SECONDS)) {
          this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_FAILURE, 0, 0));
          this.thread.interrupt();
          throw new Error("Can't place event: " + event);
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private int selectShipToFire() {
    final List<Long> foundAliveShipTypes =
            range(0, 4).filter(i -> this.counterOfMyShips[i] > 0)
                    .mapToLong(i -> i + 1)
                    .boxed()
                    .collect(Collectors.toCollection(ArrayList::new));
    if (foundAliveShipTypes.isEmpty()) {
      throw new Error("Unexpected situation, there is no any alive ship!");
    }
    Collections.shuffle(foundAliveShipTypes, this.random);
    return foundAliveShipTypes.remove(0).intValue();
  }

  private boolean isThereAnyAliveShip() {
    return Arrays.stream(this.counterOfMyShips).anyMatch(x -> x > 0);
  }

  private void pushIntoOutput(final BsGameEvent event) {
    if (!this.outQueue.offer(event)) {
      throw new Error("Can't queue out-coming event: " + event);
    }
  }

  @Override
  public Optional<BsGameEvent> pollGameEvent(final Duration duration) throws InterruptedException {
    return Optional.ofNullable(this.outQueue.poll(duration.toMillis(), TimeUnit.MILLISECONDS));
  }

  private void placeShipsOnGameField() {
    synchronized (this.myMap) {
      this.myMap.clear();
      range(0, FIELD_EDGE * FIELD_EDGE).forEach(x -> this.myMap.add(MapItem.EMPTY));
      final int[] counterOfShips = new int[]{4, 3, 2, 1};
      final List<Integer> shipIndexes = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
      shuffle(shipIndexes, random);
      shipIndexes.forEach(i -> {
        final int shipIndex = i;
        final int cellsNumber = i + 1;
        final List<Integer> freePositions = range(0, this.myMap.size())
                .filter(x -> this.myMap.get(x) == MapItem.EMPTY)
                .boxed()
                .collect(toCollection(ArrayList::new));
        shuffle(freePositions, this.random);
        while (counterOfShips[shipIndex] > 0 && !freePositions.isEmpty()) {
          final List<int[]> directions = new ArrayList<>(Arrays
                  .asList(new int[]{1, 0}, new int[]{0, 1}, new int[]{-1, 0}, new int[]{0, -1}));
          shuffle(directions, random);
          freePositions.stream()
                  .filter(offset -> {
                    final int baseX = offset % FIELD_EDGE;
                    final int baseY = offset / FIELD_EDGE;
                    final Optional<int[]> shipCompatibleDirection = directions.stream().filter(d ->
                            // check that free cells in direction meet the ship size
                            range(0, cellsNumber)
                                    .filter(sc -> {
                                      final int px = baseX + sc * d[0];
                                      final int py = baseY + sc * d[1];
                                      return isValid(px, py)
                                              && this.myMap.get(px + py * FIELD_EDGE) == MapItem.EMPTY;
                                    }).count() == cellsNumber).findFirst();

                    return shipCompatibleDirection.map(foundDirection -> {
                      // fill ship cells
                      range(0, cellsNumber).forEach(sc -> {
                        final int px = baseX + sc * foundDirection[0];
                        final int py = baseY + sc * foundDirection[1];
                        this.myMap.set(px + py * FIELD_EDGE, MapItem.SHIP);
                      });
                      // ensure ban around placed ship
                      range(0, this.myMap.size())
                              .filter(x -> this.myMap.get(x) == MapItem.SHIP)
                              .forEach(foundShipOffset -> {
                                final int sx = foundShipOffset % FIELD_EDGE;
                                final int sy = foundShipOffset / FIELD_EDGE;
                                for (int dx = -1; dx < 2; dx++) {
                                  for (int dy = -1; dy < 2; dy++) {
                                    if (dx == 0 && dy == 0) {
                                      continue;
                                    }
                                    final int cx = sx + dx;
                                    final int cy = sy + dy;
                                    if (isValid(cx, cy)) {
                                      final int cOffset = cx + cy * FIELD_EDGE;
                                      if (this.myMap.get(cOffset) == MapItem.EMPTY) {
                                        this.myMap.set(cOffset, MapItem.BANNED);
                                      }
                                    }
                                  }
                                }
                              });
                      return true;
                    }).orElse(false);
                  }).findFirst().ifPresent(x -> counterOfShips[shipIndex]--);
        }
      });
      if (Arrays.stream(counterOfShips).anyMatch(x -> x != 0)) {
        throw new Error("Unexpected situation, can't place ships, may be a bug");
      }
    }
  }

}
