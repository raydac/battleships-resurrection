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

import com.igormaznitsa.battleships.gui.InfoBanner;
import com.igormaznitsa.battleships.sound.Sound;
import java.awt.Color;
import java.util.Optional;

public enum FinalState {
  SYSTEM_FAILURE(Color.BLUE, "failure.png", Sound.SOS, InfoBanner.ERROR),
  OPPONENT_OFF(Color.RED, "opponentout.png", Sound.SOS, InfoBanner.ERROR),
  LOST(null, "LOSE.png", Sound.DEFEAT_HORN, InfoBanner.LOSE),
  VICTORY(null, "victory.png", Sound.VICTORY_HORN, InfoBanner.VICTORY);

  private final InfoBanner banner;
  private final Sound sound;
  private final String imageResourceName;
  private final Color fillColor;

  FinalState(
      final Color fillColor,
      final String imageResource,
      final Sound sound,
      final InfoBanner banner
  ) {
    this.fillColor = fillColor;
    this.banner = banner;
    this.sound = sound;
    this.imageResourceName = imageResource;
  }

  public InfoBanner getBanner() {
    return this.banner;
  }

  public Sound getSound() {
    return this.sound;
  }

  public String getImageResourceName() {
    return this.imageResourceName;
  }

  public Optional<Color> getFillColor() {
    return Optional.ofNullable(this.fillColor);
  }
}
