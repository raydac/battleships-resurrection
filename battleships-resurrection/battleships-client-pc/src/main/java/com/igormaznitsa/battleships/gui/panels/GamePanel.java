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

package com.igormaznitsa.battleships.gui.panels;

import static com.igormaznitsa.battleships.gui.Animation.DONE_AUTO;
import static com.igormaznitsa.battleships.gui.Animation.E1_NEW;
import static com.igormaznitsa.battleships.gui.Animation.E2_NEW;
import static com.igormaznitsa.battleships.gui.Animation.FIRE;
import static com.igormaznitsa.battleships.gui.Animation.PANEL;
import static com.igormaznitsa.battleships.gui.Animation.PAUSE_EXIT;
import static com.igormaznitsa.battleships.gui.Animation.SPLASH;
import static com.igormaznitsa.battleships.gui.Animation.SPLASH_GOR;
import static com.igormaznitsa.battleships.opponent.GameEventType.EVENT_DO_TURN;
import static com.igormaznitsa.battleships.opponent.GameEventType.EVENT_HIT;
import static com.igormaznitsa.battleships.opponent.GameEventType.EVENT_KILLED;
import static com.igormaznitsa.battleships.opponent.GameEventType.EVENT_LOST;
import static com.igormaznitsa.battleships.opponent.GameEventType.EVENT_MISS;
import static com.igormaznitsa.battleships.opponent.GameEventType.EVENT_SHOT_AIRCARRIER;
import static com.igormaznitsa.battleships.opponent.GameEventType.EVENT_SHOT_REGULAR;
import static com.igormaznitsa.battleships.utils.Utils.RND;
import static java.lang.Math.round;


import com.igormaznitsa.battleships.gui.Animation;
import com.igormaznitsa.battleships.gui.InfoBanner;
import com.igormaznitsa.battleships.gui.ScaleFactor;
import com.igormaznitsa.battleships.gui.sprite.DecorationSprite;
import com.igormaznitsa.battleships.gui.sprite.FallingAirplaneSprite;
import com.igormaznitsa.battleships.gui.sprite.FallingObjectSprite;
import com.igormaznitsa.battleships.gui.sprite.FallingRocketSprite;
import com.igormaznitsa.battleships.gui.sprite.FieldSprite;
import com.igormaznitsa.battleships.gui.sprite.FishSprite;
import com.igormaznitsa.battleships.gui.sprite.OneTimeWaterEffectSprite;
import com.igormaznitsa.battleships.gui.sprite.ShipSprite;
import com.igormaznitsa.battleships.gui.sprite.ShipType;
import com.igormaznitsa.battleships.opponent.BsGameEvent;
import com.igormaznitsa.battleships.opponent.BsPlayer;
import com.igormaznitsa.battleships.opponent.GameEventType;
import com.igormaznitsa.battleships.sound.Sound;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.Timer;

public class GamePanel extends BasePanel implements BsPlayer {

  public static final Point PLAYER_POSITION = new Point(395, 683);
  public static final Duration INTER_FRAME_DELAY = Duration.ofMillis(70);
  private static final int TICKS_BEFORE_CONTROL_ACTION = 3;
  private static final int GAME_FIELD_CELL_WIDTH = 23;
  private static final int GAME_FIELD_CELL_HEIGHT = 23;
  private static final Ellipse2D FIRE_BUTTON_AREA = new Ellipse2D.Float(184, 16, 43, 43);
  private static final Rectangle ACTION_PANEL_AREA =
      new Rectangle(287, 119, GAME_FIELD_CELL_WIDTH * GameField.FIELD_EDGE,
          GAME_FIELD_CELL_HEIGHT * GameField.FIELD_EDGE);
  private static final Point HORIZONS_SPLASH_COORDS = new Point(561, 32);
  private static final Point HORIZONS_EXPLOSION_COORDS = new Point(585, 36);
  private static final long ENV_SOUNDS_TICKS_BIRD_SOUND = 60;
  private static final long ENV_SOUNDS_TICKS_OTHER_SOUND = 100;
  private final BufferedImage background;
  private final Timer timer;
  private final GameField gameField;
  private final BlockingQueue<BsGameEvent> incomingQueue = new ArrayBlockingQueue<>(10);
  private final BlockingQueue<BsGameEvent> outgoingQueue = new ArrayBlockingQueue<>(10);
  private final BsGameEvent myReadyGameEvent;
  private Stage currentStage;
  private int stageStep;
  private ControlElement selectedControl;
  private int controlTicksCounter;
  private ControlElement prevControl;
  private Point lastPressedEmptyCell = null;
  private boolean pressedPlaceShipMouseButton = false;
  private DecorationSprite activeDecorationSprite = null;
  private FallingObjectSprite activeFallingObjectSprite = null;
  private OneTimeWaterEffectSprite fieldWaterEffect = null;
  private long envTicksBeforeBirdSound = ENV_SOUNDS_TICKS_BIRD_SOUND;
  private long envTicksBeforeOtherSound = ENV_SOUNDS_TICKS_OTHER_SOUND;
  private final AtomicReference<Optional<BsGameEvent>> savedGameEvent =
      new AtomicReference<>(Optional.empty());
  private List<FieldSprite> animatedSpriteField = Collections.emptyList();

  public GamePanel(final Optional<ScaleFactor> scaleFactor) {
    super(scaleFactor);

    this.gameField = new GameField();
    this.background = Animation.FON.getFrame(0);

    this.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseDragged(final MouseEvent mouseEvent) {
        if (currentStage == Stage.PLACING && pressedPlaceShipMouseButton) {
          final Point preparedMousePoint = scaleFactor.map(sf -> sf.translateMousePoint(mouseEvent))
              .orElse(mouseEvent.getPoint());
          if (lastPressedEmptyCell == null) {
            lastPressedEmptyCell = mouse2game(preparedMousePoint);
            final GameField.CellState cellState =
                gameField.getState(lastPressedEmptyCell.x, lastPressedEmptyCell.y);
            if (cellState == GameField.CellState.EMPTY) {
              gameField.tryMakePlaceholder(lastPressedEmptyCell, lastPressedEmptyCell);
            } else {
              lastPressedEmptyCell = null;
            }
          } else {
            final Point cellUnderMouse = mouse2game(preparedMousePoint);
            gameField.clearPlaceholder();
            gameField.tryMakePlaceholder(lastPressedEmptyCell, cellUnderMouse);
            refreshUi();
          }
        }
      }
    });

    this.addMouseListener(new MouseAdapter() {

      @Override
      public void mouseReleased(final MouseEvent mouseEvent) {
        if (currentStage == Stage.PLACING) {
          if (lastPressedEmptyCell != null) {
            Sound.MOUSE_FREE.getClip().play();
          }
          gameField.fixPlaceholder();
          refreshUi();
        }
      }

      @Override
      public void mousePressed(final MouseEvent mouseEvent) {
        final Point preparedMousePoint =
            scaleFactor.map(sf -> sf.translateMousePoint(mouseEvent)).orElse(mouseEvent.getPoint());
        final ControlElement detectedControl = ControlElement.find(preparedMousePoint);
        switch (currentStage) {
          case PLACING: {
            if (detectedControl == ControlElement.AUTO || detectedControl == ControlElement.DONE) {
              lastPressedEmptyCell = null;
              if (detectedControl == ControlElement.AUTO && gameField.hasAnyFreeShip()) {
                doSelectControl(detectedControl);
              } else if (detectedControl == ControlElement.DONE && !gameField.hasAnyFreeShip()) {
                doSelectControl(detectedControl);
              } else {
                detectedControl.getWrongSound().getClip().play();
              }
            } else if (ACTION_PANEL_AREA.contains(preparedMousePoint)) {
              lastPressedEmptyCell = mouse2game(preparedMousePoint);
              final GameField.CellState cellState =
                  gameField.getState(lastPressedEmptyCell.x, lastPressedEmptyCell.y);
              if (mouseEvent.isPopupTrigger()) {
                // remove
                pressedPlaceShipMouseButton = false;
                if (cellState == GameField.CellState.SHIP) {
                  final List<Point> removedShipCells =
                      gameField.tryRemoveShipAt(
                          new Point(lastPressedEmptyCell.x, lastPressedEmptyCell.y));
                  if (!removedShipCells.isEmpty()) {
                    gameField.ensureBanAroundShips();
                    gameField.increaseFreeShips(removedShipCells.size());
                  }
                }
                lastPressedEmptyCell = null;
              } else {
                lastPressedEmptyCell = mouse2game(preparedMousePoint);
                // place
                pressedPlaceShipMouseButton = true;
                if (cellState == GameField.CellState.EMPTY) {
                  gameField.tryMakePlaceholder(lastPressedEmptyCell, lastPressedEmptyCell);
                } else {
                  lastPressedEmptyCell = null;
                }
                Sound.MOUSE_CLICK.getClip().play();
              }
              refreshUi();
            }
          }
          break;
          case TARGET_SELECT: {
            if (ACTION_PANEL_AREA.contains(preparedMousePoint)) {
              final Point cell = mouse2game(preparedMousePoint);
              if (gameField.tryMarkAsTarget(cell)) {
                refreshUi();
              }
              Sound.MOUSE_CLICK.getClip().play();
            } else if (FIRE_BUTTON_AREA.contains(preparedMousePoint)) {
              Sound.ATTACK_HORN.getClip().play();
              if (gameField.hasTarget()) {
                initStage(Stage.PANEL_EXIT);
              }
            }
          }
          break;
          default: {
            // do nothing
          }
          break;
        }

        if (detectedControl == ControlElement.PAUSE || detectedControl == ControlElement.EXIT
            || detectedControl == ControlElement.NEUTRAL) {
          doSelectControl(detectedControl);
        }
      }
    });

    this.timer = new Timer((int) INTER_FRAME_DELAY.toMillis(), e -> {
      this.processEnvironmentSounds();
      this.onTimer();
    });
    this.timer.setRepeats(true);

    this.myReadyGameEvent =
        new BsGameEvent(GameEventType.EVENT_READY, RND.nextInt(GameField.FIELD_EDGE),
            RND.nextInt(GameField.FIELD_EDGE));
  }

  public static Point findShipRenderPositionForCell(final int cellX, final int cellY) {
    final int deltaX = 34;
    final int deltaY = 19;
    final int leftX = -16;
    final int middleY = 257;

    final double baseX = leftX + cellX * deltaX;
    final double baseY = middleY - cellX * deltaY;

    return new Point((int) Math.round(baseX + cellY * deltaX),
        (int) Math.round(baseY + cellY * deltaY));
  }

  private void processEnvironmentSounds() {
    this.envTicksBeforeBirdSound--;
    this.envTicksBeforeOtherSound--;
    if (this.envTicksBeforeBirdSound <= 0) {
      Sound sound = null;
      switch (RND.nextInt(8)) {
        case 1: {
          sound = Sound.SEAGULL_01;
        }
        break;
        case 3: {
          sound = Sound.SEAGULL_02;
        }
        break;
        case 5: {
          sound = Sound.SEAGULL_03;
        }
        break;
        case 4:
        default:
          break;
      }
      if (sound != null && !sound.getClip().isPlaying()) {
        sound.getClip().play();
      }
      this.envTicksBeforeBirdSound = ENV_SOUNDS_TICKS_BIRD_SOUND;
    }
    if (this.envTicksBeforeOtherSound <= 0) {
      Sound sound = null;
      switch (RND.nextInt(40)) {
        case 12:
          if (this.currentStage == Stage.TARGET_SELECT ||
              this.currentStage == Stage.ENEMY_FIRING_RESULT) {
            sound = Sound.MORSE2;
          }
          break;
        case 14:
          if (this.currentStage == Stage.FIRING_RESULT) {
            sound = Sound.MORSE;
          }
          break;
        default: {
          if (RND.nextInt(20) > 17) {
            sound = Sound.DECK_CREAK;
          }
        }
        break;
      }
      if (sound != null && !sound.getClip().isPlaying()) {
        sound.getClip().play();
      }
      this.envTicksBeforeOtherSound = ENV_SOUNDS_TICKS_OTHER_SOUND;
    }
  }

  // auxiliary method for test purposes
  protected void fillEmptyCellsByFish() {
    IntStream.range(0, GameField.FIELD_EDGE * GameField.FIELD_EDGE)
        .mapToObj(c -> new Point(c % GameField.FIELD_EDGE, c / GameField.FIELD_EDGE))
        .filter(p -> this.findShipForCell(p.x, p.y).isEmpty())
        .forEach(p -> {
          this.animatedSpriteField.add(new FishSprite(p));
        });
    Collections.sort(this.animatedSpriteField);
  }

  @Override
  public void pushGameEvent(final BsGameEvent event) {
    if (event != null && !this.incomingQueue.offer(event)) {
      throw new Error("Can't place event into queue for long time: " + event + " queue.size="
          + this.incomingQueue.size());
    }
  }

  @Override
  public Optional<BsGameEvent> pollGameEvent(final Duration duration) throws InterruptedException {
    return Optional
        .ofNullable(this.outgoingQueue.poll(duration.toMillis(), TimeUnit.MILLISECONDS));
  }

  private void renderActionPanel(final Graphics2D g, final int offsetX, final int offsetY,
                                 final GameField field, final boolean placementMode) {
    for (int x = 0; x < GameField.FIELD_EDGE; x++) {
      final int gx = offsetX + x * GAME_FIELD_CELL_WIDTH + 3;
      for (int y = 0; y < GameField.FIELD_EDGE; y++) {
        final int gy = offsetY + y * GAME_FIELD_CELL_HEIGHT + 3;
        if (placementMode) {
          switch (field.getState(x, y)) {
            case EMPTY: {
              // none
            }
            break;
            case PLACEHOLDER: {
              g.drawImage(Animation.ACT_MAP.getFrame(6), null, gx, gy);
            }
            break;
            case BANNED: {
              g.drawImage(Animation.ACT_MAP.getFrame(2), null, gx, gy);
            }
            break;
            case SHIP: {
              g.drawImage(Animation.ACT_MAP.getFrame(3), null, gx, gy);
            }
            break;
            default: {
              throw new Error("Unexpected cell state in placement mode: " + field.getState(x, y));
            }
          }
        } else {
          switch (field.getState(x, y)) {
            case EMPTY: {
              // none
            }
            break;
            case TARGET: {
              g.drawImage(Animation.ACT_MAP.getFrame(8), null, gx, gy);
            }
            break;
            case BANNED: {
              g.drawImage(Animation.ACT_MAP.getFrame(2), null, gx, gy);
            }
            break;
            case HIT: {
              g.drawImage(Animation.ACT_MAP.getFrame(0), null, gx, gy);
            }
            break;
            case KILL: {
              g.drawImage(Animation.ACT_MAP.getFrame(5), null, gx, gy);
            }
            break;
            case MISS: {
              g.drawImage(Animation.ACT_MAP.getFrame(7), null, gx, gy);
            }
            break;
            default: {
              throw new Error("Unexpected cell state in placement mode: " + field.getState(x, y));
            }
          }
        }
      }
    }
  }

  private Point mouse2game(final Point point) {
    final int x = point.x - ACTION_PANEL_AREA.x;
    final int y = point.y - ACTION_PANEL_AREA.y;
    return new Point(x / GAME_FIELD_CELL_WIDTH, y / GAME_FIELD_CELL_HEIGHT);
  }

  private void doSelectControl(final ControlElement control) {
    if (control != this.selectedControl) {
      this.prevControl = this.selectedControl;
      this.selectedControl = control;
      this.controlTicksCounter = TICKS_BEFORE_CONTROL_ACTION;
      if (control != ControlElement.NONE) {
        control.getOkSound().getClip().play();
      }
      this.refreshUi();
    }
  }

  private void fireEventToOpponent(final BsGameEvent event) {
    if (!this.outgoingQueue.offer(Objects.requireNonNull(event))) {
      throw new Error(
          "Can't queue output game event: " + event + " (size=" + this.outgoingQueue.size() + ')');
    }
  }

  private Optional<BsGameEvent> findGameEventInQueue(final Set<GameEventType> expected) {
    BsGameEvent result = null;
    final Set<UUID> alreadyMet = new HashSet<>();
    while (!Thread.currentThread().isInterrupted()) {
      result = this.incomingQueue.poll();
      if (result == null) {
        break;
      } else if (expected.contains(result.getType())) {
        break;
      } else {
        if (!this.incomingQueue.offer(result)) {
          throw new Error("Can't return event back into queue: " + result);
        }
        if (alreadyMet.contains(result.getUuid())) {
          result = null;
          break;
        } else {
          alreadyMet.add(result.getUuid());
          result = null;
        }
      }
    }
    return Optional.ofNullable(result);
  }

  private void initStage(final Stage stage) {
    this.currentStage = stage;
    this.stageStep = 0;
    this.refreshUi();
    this.startSoundForStage(stage);
  }

  private void startSoundForStage(final Stage stage) {
    switch (stage) {
      case PLACEMENT_START: {
        Sound.MENU_SCREEN_IN.getClip().play();
      }
      break;
      case PANEL_EXIT:
      case PLACEMENT_END: {
        Sound.MENU_SCREEN_OUT.getClip().play();
      }
      break;
      case PANEL_ENTER: {
        Sound.MENU_IN.getClip().play();
      }
      break;
    }
  }

  @Override
  protected void doStart() {
    this.selectedControl = ControlElement.NONE;
    this.initStage(Stage.PLACEMENT_START);
    this.gameField.reset();
    Sound.WAVES_LOOP.getClip().play(-1);
    this.timer.start();
  }

  private ShipType activateShipFire() {
    final List<ShipSprite> foundAliveShips = this.animatedSpriteField.stream()
        .filter(x -> x instanceof ShipSprite)
        .map(x -> (ShipSprite) x)
        .filter(x -> !x.isDestroyed())
        .collect(Collectors.toCollection(ArrayList::new));
    Collections.shuffle(foundAliveShips, RND);
    if (foundAliveShips.isEmpty()) {
      throw new Error("Unexpected fire request without alive ships");
    } else {
      final ShipSprite firingShip = foundAliveShips.remove(0);
      firingShip.fire();
      return firingShip.getShipType();
    }
  }

  private void onTimer() {
    this.animatedSpriteField.forEach(FieldSprite::nextFrame);
    if (this.activeDecorationSprite != null) {
      this.activeDecorationSprite.nextFrame();
    }

    if (this.fieldWaterEffect != null) {
      this.fieldWaterEffect.nextFrame();
    }

    if (this.prevControl != this.selectedControl) {
      this.controlTicksCounter--;
      if (this.controlTicksCounter <= 0) {
        this.prevControl = this.selectedControl;
        this.doProcessGameControl(this.selectedControl);
      }
    }

    switch (this.currentStage) {
      case PLACEMENT_START: {
        if (this.stageStep < E1_NEW.getLength() - 1) {
          this.stageStep++;
        } else {
          this.initStage(Stage.PLACING);
        }
      }
      break;
      case PLACEMENT_END: {
        if (this.stageStep < E1_NEW.getLength() - 1) {
          this.stageStep++;
        } else {
          this.findGameEventInQueue(EnumSet.of(GameEventType.EVENT_READY))
              .ifPresent(e -> {
                if (BsGameEvent.isFirstMoveLeft(this.myReadyGameEvent, e)) {
                  this.initStage(Stage.WAIT_FOR_TURN);
                } else {
                  this.fireEventToOpponent(new BsGameEvent(GameEventType.EVENT_DO_TURN, 0, 0));
                  this.initStage(Stage.ENEMY_TURN);
                }
              });
        }
      }
      break;
      case WAIT_FOR_TURN: {
        this.findGameEventInQueue(EnumSet.of(GameEventType.EVENT_DO_TURN))
            .ifPresent(e -> {
              this.initStage(Stage.PANEL_ENTER);
            });
      }
      break;
      case PANEL_ENTER: {
        if (this.stageStep < E1_NEW.getLength() - 1) {
          this.stageStep++;
        } else {
          this.initStage(Stage.TARGET_SELECT);
        }
      }
      break;
      case PLACING:
      case TARGET_SELECT: {
        // do nothing
      }
      break;
      case PANEL_EXIT: {
        if (this.stageStep < E1_NEW.getLength() - 1) {
          this.stageStep++;
        } else {
          final Point target = this.gameField.removeTarget()
              .orElseThrow(() -> new Error("Target must be presented"));
          final ShipType firingShip = this.activateShipFire();
          this.fireEventToOpponent(new BsGameEvent(
              firingShip == ShipType.AIR_CARRIER ? EVENT_SHOT_AIRCARRIER : EVENT_SHOT_REGULAR,
              target.x, target.y));
          this.initStage(Stage.FIRING);
        }
      }
      break;
      case FIRING: {
        this.activeDecorationSprite = null;
        if (this.noAnyFiringShip()) {
          this.initStage(Stage.FIRING_RESULT);
        }
      }
      break;
      case FIRING_RESULT: {
        if (this.activeDecorationSprite == null) {
          this.findGameEventInQueue(EnumSet
              .of(GameEventType.EVENT_KILLED, EVENT_MISS, EVENT_HIT,
                  EVENT_LOST)).ifPresent(e -> {
            this.savedGameEvent.set(Optional.of(e));
            switch (e.getType()) {
              case EVENT_LOST:
              case EVENT_KILLED:
              case EVENT_HIT: {
                this.activeDecorationSprite =
                    new DecorationSprite(HORIZONS_EXPLOSION_COORDS, Animation.EXPLO_GOR,
                        Sound.OUR_EXPLODE_ONLY);
                if (e.getType() != EVENT_HIT) {
                  Sound.BUBBLES.getClip().play();
                }
              }
              break;
              case EVENT_MISS: {
                this.activeDecorationSprite =
                    new DecorationSprite(HORIZONS_SPLASH_COORDS, SPLASH_GOR,
                        Sound.OUR_WATER_SPLASH_ONLY);
              }
              break;
            }
          });
        } else if (this.activeDecorationSprite.isCompleted()) {
          this.activeDecorationSprite = null;
          this.savedGameEvent.getAndSet(Optional.empty()).ifPresent(e -> {
            switch (e.getType()) {
              case EVENT_MISS: {
                this.gameField.setState(e.getX(), e.getY(), GameField.CellState.MISS);
                this.fireEventToOpponent(new BsGameEvent(GameEventType.EVENT_DO_TURN, 0, 0));
                this.initStage(Stage.ENEMY_TURN);
              }
              break;
              case EVENT_LOST: {
                this.fireSignal(SIGNAL_VICTORY);
              }
              break;
              default: {
                if (e.getType() == EVENT_KILLED) {
                  this.gameField.setState(e.getX(), e.getY(), GameField.CellState.KILL);
                  // kill
                  final List<Point> removedShip =
                      this.gameField.tryRemoveShipAt(new Point(e.getX(), e.getY()));
                  if (removedShip.isEmpty()) {
                    this.fireEventToOpponent(
                        new BsGameEvent(GameEventType.EVENT_SYSTEM_ERROR, 0, 0));
                    throw new Error("Can't remove killed enemy ship from map: " + e);
                  } else {
                    removedShip
                        .forEach(c -> this.gameField.setState(c.x, c.y, GameField.CellState.KILL));
                    this.gameField.ensureBanAroundShips();
                  }
                } else {
                  // hit
                  this.gameField.setState(e.getX(), e.getY(), GameField.CellState.HIT);
                }
                this.initStage(Stage.WAIT_FOR_TURN);
              }
              break;
            }
          });
        }
      }
      break;
      case ENEMY_TURN: {
        if (this.activeFallingObjectSprite == null) {
          this.findGameEventInQueue(
              EnumSet.of(GameEventType.EVENT_SHOT_AIRCARRIER, GameEventType.EVENT_SHOT_REGULAR))
              .ifPresent(e -> {
                this.savedGameEvent.set(Optional.of(e));
                final Optional<ShipSprite> hitShip = this.findShipForCell(e.getX(), e.getY());
                final Point targetCell = hitShip.map(
                    FieldSprite::getActionCell).orElse(new Point(e.getX(), e.getY()));

                if (e.getType() == EVENT_SHOT_AIRCARRIER) {
                  this.activeFallingObjectSprite =
                      new FallingAirplaneSprite(hitShip, targetCell);
                } else {
                  this.activeFallingObjectSprite =
                      new FallingRocketSprite(hitShip, targetCell);
                }
                this.animatedSpriteField.add(this.activeFallingObjectSprite);
                Collections.sort(this.animatedSpriteField);
              });
        } else if (this.activeFallingObjectSprite.isCompleted()) {
          this.animatedSpriteField.remove(this.activeFallingObjectSprite);
          Collections.sort(this.animatedSpriteField);
          this.activeFallingObjectSprite = null;
          this.initStage(Stage.ENEMY_FIRING_RESULT);
        }
      }
      break;
      case ENEMY_FIRING_RESULT: {
        if (this.fieldWaterEffect == null) {
          this.savedGameEvent.getAndSet(Optional.empty()).ifPresent(enemyShoot -> {
            boolean enemyMayTurn = false;
            final BsGameEvent enemyTurnResultEvent;
            final ShipSprite hitShip = this.processEnemyShot(enemyShoot).orElse(null);
            if (hitShip == null) {
              enemyTurnResultEvent = new BsGameEvent(EVENT_MISS, enemyShoot.getX(),
                  enemyShoot.getY());
              this.fieldWaterEffect = new OneTimeWaterEffectSprite(
                  new Point(enemyShoot.getX(), enemyShoot.getY()), Optional.empty(),
                  Animation.SPLASH);
              Sound.WATER_SPLASH01.getClip().play();
              this.animatedSpriteField.add(this.fieldWaterEffect);
              Collections.sort(this.animatedSpriteField);
            } else {
              enemyMayTurn = true;
              if (hitShip.isDestroyed()) {
                final GameEventType resultType;
                if (this.isThereAnyAliveShip()) {
                  resultType = EVENT_KILLED;
                } else {
                  enemyMayTurn = false;
                  resultType = EVENT_LOST;
                }
                enemyTurnResultEvent = new BsGameEvent(resultType,
                    enemyShoot.getX(),
                    enemyShoot.getY());
                Sound.BUBBLES.getClip().play();
              } else {
                enemyTurnResultEvent = new BsGameEvent(EVENT_HIT, enemyShoot.getX(),
                    enemyShoot.getY());
              }
              this.fieldWaterEffect = new OneTimeWaterEffectSprite(
                  hitShip.getActionCell(), Optional.of(hitShip), Animation.EXPLODE);
              Sound.WATER_SPLASH01.EXPLODE01.getClip().play();
              this.animatedSpriteField.add(this.fieldWaterEffect);
              Collections.sort(this.animatedSpriteField);
            }
            this.fireEventToOpponent(enemyTurnResultEvent);
            if (enemyMayTurn) {
              this.fireEventToOpponent(new BsGameEvent(EVENT_DO_TURN, 0, 0));
            }
            this.savedGameEvent.set(Optional.of(enemyTurnResultEvent));
          });
        } else {
          if (this.fieldWaterEffect.isCompleted()) {
            this.animatedSpriteField.remove(this.fieldWaterEffect);
            if (this.fieldWaterEffect.getAnimation() == SPLASH) {
              final FishSprite fishSprite = new FishSprite(this.fieldWaterEffect.getCell());
              this.animatedSpriteField.add(fishSprite);
              Collections.sort(this.animatedSpriteField);
            }
            this.fieldWaterEffect = null;
            this.savedGameEvent.getAndSet(Optional.empty()).ifPresent(event -> {
              if (event.getType() == EVENT_LOST) {
                this.fireSignal(SIGNAL_LOST);
              } else {
                this.initStage(event.getType() == EVENT_MISS ? Stage.WAIT_FOR_TURN :
                    Stage.ENEMY_TURN);
              }
            });
          }
        }
      }
      break;
      default: {
        throw new Error("Unexpected stage: " + this.currentStage);
      }
    }
    this.refreshUi();
  }

  private boolean noAnyFiringShip() {
    return this.animatedSpriteField.stream()
        .noneMatch(x -> x instanceof ShipSprite && ((ShipSprite) x).isFiring());
  }

  private boolean isThereAnyAliveShip() {
    return this.animatedSpriteField.stream()
        .anyMatch(s -> s instanceof ShipSprite && !((ShipSprite) s).isDestroyed());
  }

  private Optional<ShipSprite> findShipForCell(final int x, final int y) {
    final Point cell = new Point(x, y);
    return this.animatedSpriteField.stream()
        .filter(s -> s instanceof ShipSprite && s.containsCell(cell))
        .map(s -> (ShipSprite) s)
        .findFirst();
  }

  private Optional<ShipSprite> processEnemyShot(final BsGameEvent e) {
    final Optional<ShipSprite> hitShip = this.findShipForCell(e.getX(), e.getY());
    hitShip.ifPresent(ShipSprite::processHit);
    return hitShip;
  }

  @Override
  protected void doDispose() {
    Sound.stopAll();
    this.timer.stop();
  }

  private void drawNumberOfShipsOnPanel(final Graphics2D g2d,
                                        final int cell4,
                                        final int cell3,
                                        final int cell2,
                                        final int cell1,
                                        final int panelY) {
    g2d.drawImage(Animation.DIGIT.getFrame(cell4), null, 8, panelY + 97);
    g2d.drawImage(Animation.DIGIT.getFrame(cell3), null, 8, panelY + 202);
    g2d.drawImage(Animation.DIGIT.getFrame(cell2), null, 8, panelY + 299);
    g2d.drawImage(Animation.DIGIT.getFrame(cell1), null, 8, panelY + 394);
  }

  private void drawFish(final Graphics2D g) {
    this.animatedSpriteField.stream().filter(x -> x instanceof FieldSprite)
        .forEach(x -> x.render(g));
  }

  private void drawAllExcludeFish(final Graphics2D g) {
    this.animatedSpriteField.stream().filter(x -> !(x instanceof FishSprite))
        .forEach(x -> x.render(g));
  }

  @Override
  protected void doPaint(final Graphics2D g2d) {
    g2d.drawImage(this.background, null, 0, 0);
    if (this.activeDecorationSprite != null) {
      this.activeDecorationSprite.render(g2d);
    }
    this.drawFish(g2d);
    this.drawAllExcludeFish(g2d);

    switch (this.currentStage) {
      case PLACEMENT_START: {
        final int dx = round((PANEL.getWidth() / (float) E1_NEW.getLength()) * this.stageStep);
        g2d.drawImage(PANEL.getLast(), null, dx - PANEL.getWidth(), 100);
        g2d.drawImage(E1_NEW.getFrame(this.stageStep), null, 0, 0);
        g2d.drawImage(E2_NEW.getFrame(this.stageStep), null, 512, 0);
      }
      break;
      case PLACING: {
        g2d.drawImage(PANEL.getLast(), null, 0, 100);
        g2d.drawImage(E1_NEW.getLast(), null, 0, 0);
        g2d.drawImage(E2_NEW.getLast(), null, 512, 0);
        this.drawNumberOfShipsOnPanel(g2d, this.gameField.getShipsCount(ShipType.AIR_CARRIER),
            this.gameField.getShipsCount(ShipType.DREADNOUGHT),
            this.gameField.getShipsCount(ShipType.GUARD_SHIP),
            this.gameField.getShipsCount(ShipType.U_BOAT), 100);
        this.renderActionPanel(g2d, ACTION_PANEL_AREA.x, ACTION_PANEL_AREA.y, this.gameField, true);
      }
      break;
      case PLACEMENT_END: {
        final int dx = round((PANEL.getWidth() / (float) E1_NEW.getLength()) * this.stageStep);
        g2d.drawImage(PANEL.getLast(), null, -dx, 100);
        g2d.drawImage(E1_NEW.getFrame(E1_NEW.getLength() - this.stageStep - 1),
            null, 0, 0);
        g2d.drawImage(E2_NEW.getFrame(E1_NEW.getLength() - this.stageStep - 1),
            null, 512, 0);
      }
      break;
      case TARGET_SELECT: {
        g2d.drawImage(E1_NEW.getLast(), null, 0, 0);
        g2d.drawImage(E2_NEW.getLast(), null, 512, 0);
        g2d.drawImage(FIRE.getFirst(), null, 136, 0);
        this.renderActionPanel(g2d, 287, 119, this.gameField, false);
      }
      break;
      case PANEL_ENTER: {
        g2d.drawImage(E1_NEW.getFrame(this.stageStep), null, 0, 0);
        g2d.drawImage(E2_NEW.getFrame(this.stageStep), null, 512, 0);
      }
      break;
      case FIRING:
      case WAIT_FOR_TURN:
      case ENEMY_TURN:
      case ENEMY_FIRING_RESULT:
      case FIRING_RESULT: {
        g2d.drawImage(E1_NEW.getFirst(), null, 0, 0);
        g2d.drawImage(E2_NEW.getFirst(), null, 512, 0);
      }
      break;
      case PANEL_EXIT: {
        g2d.drawImage(E1_NEW.getFrame(E1_NEW.getLength() - this.stageStep - 1),
            null, 0, 0);
        g2d.drawImage(E2_NEW.getFrame(E1_NEW.getLength() - this.stageStep - 1),
            null, 512, 0);
      }
      break;
      default: {
        throw new Error("Unexpected stage: " + this.currentStage);
      }
    }

    switch (this.selectedControl) {
      case PAUSE: {
        g2d.drawImage(DONE_AUTO.getFrame(1), null, 8, 0);
        g2d.drawImage(PAUSE_EXIT.getFirst(), null, 544, 344);
      }
      break;
      case EXIT: {
        g2d.drawImage(DONE_AUTO.getFrame(1), null, 8, 0);
        g2d.drawImage(PAUSE_EXIT.getLast(), null, 544, 344);
      }
      break;
      case AUTO:
      case DONE: {
        final BufferedImage controlImage =
            this.selectedControl == ControlElement.DONE ? DONE_AUTO.getFirst() :
                DONE_AUTO.getLast();
        g2d.drawImage(controlImage, null, 8, 0);
        g2d.drawImage(PAUSE_EXIT.getFrame(1), null, 544, 344);
      }
      break;
      default: {
        g2d.drawImage(DONE_AUTO.getFrame(1), null, 8, 0);
        g2d.drawImage(PAUSE_EXIT.getFrame(1), null, 544, 344);
      }
      break;
    }

    this.currentStage.getBanner().render(g2d, BANNER_COORD);
  }

  private void doProcessGameControl(final ControlElement control) {
    switch (control) {
      case AUTO: {
        this.gameField.autoPlacingFreeShips();
        this.doSelectControl(ControlElement.NONE);
      }
      break;
      case DONE: {
        this.doSelectControl(ControlElement.NONE);
        this.animatedSpriteField = this.gameField.moveFieldToShipSprites();
        //this.fillEmptyCellsByFish();
        this.gameField.reset();
        this.fireEventToOpponent(this.myReadyGameEvent);
        this.initStage(Stage.PLACEMENT_END);
      }
      break;
      case NEUTRAL: {
        this.doSelectControl(ControlElement.NONE);
      }
      break;
      case PAUSE: {
        this.fireSignal(SIGNAL_PAUSED);
      }
      break;
      case EXIT: {
        final Timer timer = new Timer(1500, e -> this.fireSignal(SIGNAL_EXIT));
        timer.setRepeats(false);
        timer.start();
      }
      break;
      case VICTORY: {
        this.fireSignal(SIGNAL_VICTORY);
      }
      break;
      case LOST: {
        this.fireSignal(SIGNAL_LOST);
      }
      break;
    }
  }

  @Override
  public void onGameKeyEvent(final KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_SPACE: {
        if (this.currentStage == Stage.PLACING) {
          this.gameField.reset();
          refreshUi();
        }
      }
      break;
    }
  }

  private enum Stage {
    PLACEMENT_START(InfoBanner.NONE),
    PLACING(InfoBanner.PLACEMENT),
    PLACEMENT_END(InfoBanner.NONE),
    WAIT_FOR_TURN(InfoBanner.NONE),
    PANEL_ENTER(InfoBanner.NONE),
    TARGET_SELECT(InfoBanner.YOUR_MOVE),
    PANEL_EXIT(InfoBanner.NONE),
    FIRING(InfoBanner.NONE),
    FIRING_RESULT(InfoBanner.NONE),
    ENEMY_TURN(InfoBanner.OPPONENTS_MOVE),
    ENEMY_FIRING_RESULT(InfoBanner.OPPONENTS_MOVE),
    VICTORY(InfoBanner.VICTORY),
    LOST(InfoBanner.LOST);
    private final InfoBanner banner;

    Stage(final InfoBanner banner) {
      this.banner = banner;
    }

    public InfoBanner getBanner() {
      return this.banner;
    }
  }


}
