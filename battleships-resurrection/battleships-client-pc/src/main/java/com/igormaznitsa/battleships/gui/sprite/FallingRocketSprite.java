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

import com.igormaznitsa.battleships.gui.Animation;
import com.igormaznitsa.battleships.sound.Sound;

import java.awt.*;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class FallingRocketSprite extends FallingObjectSprite {
  public FallingRocketSprite(final Optional<ShipSprite> hitShip, final Point targetCell) {
    super(Animation.ROKET_FALL, hitShip, targetCell, Sound.ROCKET_IN, 500, -16, 17);
  }
}
