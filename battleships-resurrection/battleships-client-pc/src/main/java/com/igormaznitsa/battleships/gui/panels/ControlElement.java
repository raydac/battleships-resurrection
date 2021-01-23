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

import com.igormaznitsa.battleships.sound.Sound;
import java.awt.Point;
import java.awt.Rectangle;

enum ControlElement {
  NONE(null, Sound.TYPEWRITER, Sound.TYPEWRITER),
  AUTO(new Rectangle(40, 44, 64, 21), Sound.TUNBLER_CRACK_AND_BELL, Sound.WRONG_TUMBLER),
  DONE(new Rectangle(40, 0, 64, 21), Sound.TUNBLER_CRACK_AND_BELL, Sound.WRONG_TUMBLER),
  PAUSE(new Rectangle(652, 474, 105, 36), Sound.TUNBLER_CRACK_AND_BELL, Sound.WRONG_TUMBLER),
  NEUTRAL(new Rectangle(628, 510, 153, 38), Sound.TUNBLER_CRACK_AND_BELL, Sound.WRONG_TUMBLER),
  EXIT(new Rectangle(652, 548, 105, 27), Sound.TUNBLER_CRACK_AND_BELL, Sound.WRONG_TUMBLER),
  VICTORY(null, Sound.VICTORY_HORN, Sound.VICTORY_HORN),
  LOST(null, Sound.DEFEAT_HORN, Sound.DEFEAT_HORN);

  private final Rectangle area;
  private final Sound okSound;
  private final Sound wrongSound;

  ControlElement(final Rectangle area, final Sound okSound, final Sound wrongSound) {
    this.area = area;
    this.okSound = okSound;
    this.wrongSound = wrongSound;
  }

  static ControlElement find(final Point point) {
    for (final ControlElement c : ControlElement.values()) {
      if (c.area != null && c.area.contains(point)) {
        return c;
      }
    }
    return NONE;
  }

  public Sound getOkSound() {
    return this.okSound;
  }

  public Sound getWrongSound() {
    return this.wrongSound;
  }
}
