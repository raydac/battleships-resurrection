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

import static com.igormaznitsa.battleships.gui.Animation.CELL_1_ROK_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_1_ROK_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_1_SP_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_1_SP_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_1_TR_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_1_TR_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_1_V1_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_1_V1_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_2_1_SP_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_2_1_SP_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_2_SP_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_2_SP_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_2_TR_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_2_TR_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_2_V1_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_2_V1_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_3_1SP_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_3_1SP_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_3_2SP_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_3_2SP_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_3_SP_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_3_SP_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_3_TR_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_3_V1_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_3_V1_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_3_V2_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_3_V2_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_4_1SP_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_4_1SP_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_4_1VN_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_4_2SP_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_4_2SP_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_4_2VN_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_4_2VN_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_4_3SP_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_4_3SP_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_4_3VN_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_4_3VN_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_4_SP_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_4_SP_R;
import static com.igormaznitsa.battleships.gui.Animation.CELL_4_TR_L;
import static com.igormaznitsa.battleships.gui.Animation.CELL_4_V1_R;
import static com.igormaznitsa.battleships.utils.Utils.RND;


import com.igormaznitsa.battleships.gui.Animation;
import com.igormaznitsa.battleships.sound.Sound;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;

public final class ShipSprite extends FieldSprite {

  private final ShipType shipType;
  private final boolean toRight;
  private final Animation[] animationNormal;
  private final Animation[] animationFirePrimary;
  private final Animation[] animationFireSecondary;
  private final Animation animationDestroyed;
  private final Animation extendedFireAnimation;
  private final Sound firingSound;
  private int activeCells;
  private Point extendedFireAnimationPoint;
  private Animation animation;
  private int frame;
  private int fireFrame = -1;
  private boolean fire = false;
  private FiringStage firingStage = FiringStage.NONE;
  private int developmentLevel;
  private int stepY;

  public ShipSprite(final List<Point> cells) {
    super(cells, findBaseRenderCell(cells), findBaseRenderCell(cells), true);
    switch (this.cells.size()) {
      case 1:
        this.shipType = ShipType.U_BOAT;
        this.firingSound = Sound.SUBMARINE_FIRE;
        break;
      case 2:
        this.shipType = ShipType.GUARD_SHIP;
        this.firingSound = Sound.GUARD_BOAT_FIRE;
        break;
      case 3:
        this.shipType = ShipType.DREADNOUGHT;
        this.firingSound = Sound.DRENDOUT_FIRE;
        break;
      case 4:
        this.shipType = ShipType.AIR_CARRIER;
        this.firingSound = Sound.AIRPLANE_OUT;
        break;
      default:
        throw new Error("Unexpected ship: " + cells.size());
    }

    if (this.shipType == ShipType.U_BOAT) {
      this.toRight = RND.nextBoolean();
    } else {
      this.toRight = this.cells.get(0).y == this.cells.get(1).y;
    }

    this.activeCells = this.shipType.getCells();

    switch (this.shipType) {
      case U_BOAT: {
        if (toRight) {
          this.animationNormal = new Animation[] {CELL_1_SP_L};
          this.animationFirePrimary = new Animation[] {CELL_1_V1_L};
          this.animationFireSecondary = null;
          this.animationDestroyed = CELL_1_TR_L;
          this.extendedFireAnimation = CELL_1_ROK_L;
        } else {
          this.animationNormal = new Animation[] {CELL_1_SP_R};
          this.animationFirePrimary = new Animation[] {CELL_1_V1_R};
          this.animationFireSecondary = null;
          this.animationDestroyed = CELL_1_TR_R;
          this.extendedFireAnimation = CELL_1_ROK_R;
        }
      }
      break;
      case GUARD_SHIP: {
        if (this.toRight) {
          this.animationNormal = new Animation[] {CELL_2_1_SP_L, CELL_2_SP_L};
          this.animationFirePrimary =
              new Animation[] {CELL_2_V1_L, CELL_2_V1_L};
          this.animationFireSecondary = null;
          this.animationDestroyed = CELL_2_TR_L;
        } else {
          this.animationNormal = new Animation[] {CELL_2_1_SP_R, CELL_2_SP_R};
          this.animationFirePrimary =
              new Animation[] {CELL_2_V1_R, CELL_2_V1_R};
          this.animationFireSecondary = null;
          this.animationDestroyed = CELL_2_TR_R;
        }
        this.extendedFireAnimation = null;
      }
      break;
      case DREADNOUGHT: {
        if (this.toRight) {
          this.animationNormal = new Animation[] {CELL_3_2SP_L, CELL_3_1SP_L, CELL_3_SP_L};
          this.animationFirePrimary = new Animation[] {CELL_3_V2_L, CELL_3_V2_L, CELL_3_V2_L};
          this.animationFireSecondary = new Animation[] {CELL_3_V1_L, CELL_3_V1_L, CELL_3_V1_L};
          this.animationDestroyed = Animation.CELL_3_TR_L;
        } else {
          this.animationNormal = new Animation[] {CELL_3_2SP_R, CELL_3_1SP_R, CELL_3_SP_R};
          this.animationFirePrimary = new Animation[] {CELL_3_V2_R, CELL_3_V2_R, CELL_3_V2_R};
          this.animationFireSecondary = new Animation[] {CELL_3_V1_R, CELL_3_V1_R, CELL_3_V1_R};
          this.animationDestroyed = CELL_3_TR_R;
        }
        this.extendedFireAnimation = null;
      }
      break;
      case AIR_CARRIER: {
        if (this.toRight) {
          this.animationNormal = new Animation[] {CELL_4_3SP_L, CELL_4_2SP_L,
              CELL_4_1SP_L, CELL_4_SP_L};
          this.animationFirePrimary =
              new Animation[] {CELL_4_1VN_L, CELL_4_1VN_L, CELL_4_1VN_L, CELL_4_1VN_L};
          this.animationFireSecondary =
              new Animation[] {CELL_4_3VN_L, CELL_4_3VN_L, CELL_4_3VN_L, CELL_4_3VN_L};
          this.animationDestroyed = CELL_4_TR_L;
          this.extendedFireAnimation = CELL_4_2VN_L;
        } else {
          this.animationNormal =
              new Animation[] {CELL_4_3SP_R, CELL_4_2SP_R, CELL_4_1SP_R, CELL_4_SP_R};
          this.animationFirePrimary =
              new Animation[] {CELL_4_V1_R, CELL_4_V1_R, CELL_4_V1_R, CELL_4_V1_R};
          this.animationFireSecondary =
              new Animation[] {CELL_4_3VN_R, CELL_4_3VN_R, CELL_4_3VN_R, CELL_4_3VN_R};
          this.animationDestroyed = CELL_4_TR_L;
          this.extendedFireAnimation = CELL_4_2VN_R;
        }
      }
      break;
      default:
        throw new Error("Unknown ship: " + this.shipType);
    }
    this.fire = false;
    this.animation = this.animationNormal[this.activeCells - 1];
    this.frame = RND.nextInt(this.animation.getLength());
    this.developmentLevel = this.developmentOnStart ? DEVELOPMENT_LEVELS : 0;
  }

  private static Point findBaseRenderCell(final List<Point> cells) {
    final int x;
    final int y;
    if (cells.size() == 1) {
      x = cells.get(0).x;
      y = cells.get(0).y;
    } else {
      if (cells.get(0).x == cells.get(1).x) {
        final int minY = cells.stream().mapToInt(c -> c.y).min().getAsInt();
        final int maxY = cells.stream().mapToInt(c -> c.y).max().getAsInt();
        x = cells.get(0).x;
        y = minY + (maxY - minY) / 2;
      } else {
        final int minX = cells.stream().mapToInt(c -> c.x).min().getAsInt();
        final int maxX = cells.stream().mapToInt(c -> c.x).max().getAsInt();
        x = minX + (maxX - minX) / 2;
        y = cells.get(0).y;
      }
    }

    return new Point(x, y);
  }

  public ShipType getShipType() {
    return this.shipType;
  }

  public int getActiveCells() {
    return this.activeCells;
  }

  public boolean isVertical() {
    return this.toRight;
  }

  public void fire() {
    if (!this.isDestroyed()) {
      this.fire = true;
    }
  }

  private void initAnimation(final Animation animation) {
    this.animation = animation;
    this.frame = 0;
  }

  @Override
  public void nextFrame() {
    this.frame++;
    if (this.developmentLevel > 0) {
      this.developmentLevel--;
    }
    if (this.frame >= this.animation.getLength()) {
      this.frame = 0;
      if (this.fire && this.firingStage == FiringStage.NONE) {
        this.fire = false;
        this.firingStage = FiringStage.MAIN;
        this.fireFrame = -1;
        this.firingSound.getClip().play();
      }
    }

    if (this.activeCells == 0) {
      this.fire = false;
      this.firingStage = FiringStage.NONE;
    }

    final int airplaneStepOffsetX = 25;
    final int airplaneStepOffsetY = 11;

    switch (this.firingStage) {
      case MAIN: {
        this.fireFrame++;
        if (this.shipType == ShipType.AIR_CARRIER) {
          final int startExtendedAnimationFrame =
              this.animationFirePrimary[this.activeCells - 1].getLength() / 2;
          if (this.fireFrame >= startExtendedAnimationFrame) {
            this.extendedFireAnimationPoint = new Point(this.renderPoint.x, this.renderPoint.y);
            this.firingStage = FiringStage.MAIN_AND_EXTENDED;
          }
        } else {
          if (this.fireFrame >= this.animationFirePrimary[this.activeCells - 1].getLength()) {
            if (this.extendedFireAnimation == null) {
              this.firingStage = FiringStage.NONE;
              this.fireFrame = -1;
            } else {
              this.firingStage = FiringStage.EXTENDED;
              if (this.shipType == ShipType.U_BOAT) {
                this.stepY = -12;
                this.extendedFireAnimationPoint =
                    new Point(this.renderPoint.x, this.renderPoint.y + this.stepY);
              } else {
                this.extendedFireAnimationPoint = new Point(this.renderPoint.x, this.renderPoint.y);
              }
              this.fireFrame = 0;
            }
          }
        }
      }
      break;
      case MAIN_AND_EXTENDED: {
        if (this.shipType == ShipType.AIR_CARRIER) {
          this.fireFrame++;
          if (this.fireFrame >= this.animationFirePrimary[this.activeCells - 1].getLength()) {
            this.firingStage = FiringStage.EXTENDED;
            this.fireFrame = this.fireFrame % this.extendedFireAnimation.getLength();
          } else {
            if (this.toRight) {
              this.extendedFireAnimationPoint.move(this.extendedFireAnimationPoint.x
                      + airplaneStepOffsetX,
                  this.extendedFireAnimationPoint.y
                      - airplaneStepOffsetY);
            } else {
              this.extendedFireAnimationPoint.move(this.extendedFireAnimationPoint.x
                      - airplaneStepOffsetX,
                  this.extendedFireAnimationPoint.y
                      - airplaneStepOffsetY);
            }
          }
        } else {
          throw new Error("Unexpected ship spriteType: " + this.shipType);
        }
      }
      break;
      case EXTENDED: {
        this.fireFrame++;
        if (this.fireFrame >= this.extendedFireAnimation.getLength()) {
          this.fireFrame = 0;
        }
        switch (this.shipType) {
          case U_BOAT: {
            this.extendedFireAnimationPoint.move(this.extendedFireAnimationPoint.x,
                this.extendedFireAnimationPoint.y + this.stepY);
            this.stepY <<= 1;
            if (this.extendedFireAnimationPoint.y < 0) {
              this.firingStage = FiringStage.NONE;
            }
          }
          break;
          case AIR_CARRIER: {
            if (this.toRight) {
              this.extendedFireAnimationPoint.move(this.extendedFireAnimationPoint.x
                      + airplaneStepOffsetX,
                  this.extendedFireAnimationPoint.y
                      - airplaneStepOffsetY);
            } else {
              this.extendedFireAnimationPoint.move(this.extendedFireAnimationPoint.x
                      - airplaneStepOffsetX,
                  this.extendedFireAnimationPoint.y
                      - airplaneStepOffsetY);
            }
            if (this.extendedFireAnimationPoint.x < 0 || this.extendedFireAnimationPoint.y < 0) {
              this.firingStage = FiringStage.NONE;
            }
          }
          break;
          default:
            throw new Error("Unexpected ship for extended animation: " + this.shipType);
        }
      }
      break;
    }
  }

  public boolean isFiring() {
    return this.fire || this.firingStage != FiringStage.NONE;
  }

  public boolean processHit() {
    if (this.activeCells > 0) {
      this.activeCells--;
      this.initAnimation(this.activeCells == 0 ? this.animationDestroyed :
          this.animationNormal[this.activeCells - 1]);
    }
    return this.activeCells == 0;
  }

  public boolean isDestroyed() {
    return this.activeCells == 0;
  }

  public void render(final Graphics2D g2d) {
    final int x = this.renderPoint.x;
    final int y = this.renderPoint.y;

    final Composite oldComposite = g2d.getComposite();
    if (this.developmentLevel > 0) {
      final AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
          (1.0f / DEVELOPMENT_LEVELS) * (DEVELOPMENT_LEVELS - this.developmentLevel));
      g2d.setComposite(alphaComposite);
    }
    switch (this.shipType) {
      case U_BOAT: {
        g2d.drawImage(this.animation.getFrame(this.frame), null, x, y);
        switch (this.firingStage) {
          case MAIN: {
            g2d.drawImage(this.animationFirePrimary[this.activeCells - 1].getFrame(this.fireFrame),
                null,
                x, y);
          }
          break;
          case EXTENDED: {
            g2d.drawImage(this.extendedFireAnimation.getFrame(this.fireFrame),
                null,
                this.extendedFireAnimationPoint.x, this.extendedFireAnimationPoint.y);
          }
          break;
          default: {
            // none
          }
          break;
        }
      }
      break;
      case GUARD_SHIP: {
        g2d.drawImage(this.animation.getFrame(this.frame), null, x, y);
        if (this.firingStage == FiringStage.MAIN) {
          g2d.drawImage(this.animationFirePrimary[this.activeCells - 1].getFrame(this.fireFrame),
              null,
              x, y);
        }
      }
      break;
      case DREADNOUGHT: {
        if (this.firingStage == FiringStage.MAIN) {
          g2d.drawImage(this.animationFirePrimary[this.activeCells - 1].getFrame(this.fireFrame),
              null,
              x, y);
          g2d.drawImage(this.animation.getFrame(this.frame), null, x, y);
          g2d.drawImage(this.animationFireSecondary[this.activeCells - 1].getFrame(this.fireFrame),
              null,
              x, y);
        } else {
          g2d.drawImage(this.animation.getFrame(this.frame), null, x, y);
        }
      }
      break;
      case AIR_CARRIER: {
        g2d.drawImage(this.animation.getFrame(this.frame), null, x, y);
        switch (this.firingStage) {
          case MAIN: {
            g2d.drawImage(this.animationFirePrimary[this.activeCells - 1].getFrame(this.fireFrame),
                null,
                x, y);
          }
          break;
          case MAIN_AND_EXTENDED: {
            g2d.drawImage(this.animationFirePrimary[this.activeCells - 1].getFrame(this.fireFrame),
                null,
                x, y);
            g2d.drawImage(this.extendedFireAnimation
                    .getFrame(this.fireFrame % this.extendedFireAnimation.getLength()),
                null,
                this.extendedFireAnimationPoint.x, this.extendedFireAnimationPoint.y);
          }
          break;
          case EXTENDED: {
            g2d.drawImage(this.extendedFireAnimation.getFrame(this.fireFrame),
                null,
                this.extendedFireAnimationPoint.x, this.extendedFireAnimationPoint.y);
            g2d.drawImage(this.animationFireSecondary[this.activeCells - 1].getFrame(this.frame),
                null,
                x, y);
          }
          break;
          default: {
            if (this.activeCells > 0) {
              g2d.drawImage(this.animationFireSecondary[this.activeCells - 1].getFrame(this.frame),
                  null,
                  x, y);
            }
          }
          break;
        }
      }
      break;
    }
    if (this.developmentLevel > 0) {
      g2d.setComposite(oldComposite);
    }
  }

  private enum FiringStage {
    NONE,
    MAIN,
    EXTENDED,
    MAIN_AND_EXTENDED
  }
}
