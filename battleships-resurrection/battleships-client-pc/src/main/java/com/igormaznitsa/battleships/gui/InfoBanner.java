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

package com.igormaznitsa.battleships.gui;

import com.igormaznitsa.battleships.utils.GfxUtils;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;

public enum InfoBanner {
  NONE(""),
  ERROR("error"),
  CANT_CONNECT("cantconnect"),
  LOSE("lost"),
  OPPONENTS_MOVE("opponentsmove"),
  PLACEMENT("placement"),
  VICTORY("victory"),
  WAIT_OPPONENT("waitopponent"),
  WAIT_OPPONENT_ARRANGEMENT("waitopponentarrangement"),
  YOUR_MOVE("yourmove");

  private final String resourceName;
  private BufferedImage image;

  InfoBanner(final String resource) {
    this.resourceName = "banner_" + resource + ".png";
  }

  public static void loadAll() {
    for (final InfoBanner banner : InfoBanner.values()) {
      banner.load();
    }
  }

  private void load() {
    this.image = null;
    if (this != NONE) {
      this.image = GfxUtils.loadResImage(resourceName);
    }
  }

  public void render(final Graphics2D gfx, final Point point) {
    if (this.image != null) {
      gfx.drawImage(this.image, null, point.x, point.y);
    }
  }


}
