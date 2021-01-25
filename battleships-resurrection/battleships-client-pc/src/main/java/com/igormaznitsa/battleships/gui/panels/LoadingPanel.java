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

import com.igormaznitsa.battleships.gui.Animation;
import com.igormaznitsa.battleships.gui.InfoBanner;
import com.igormaznitsa.battleships.sound.Sound;
import com.igormaznitsa.battleships.sound.SoundClip;
import com.igormaznitsa.battleships.utils.GfxUtils;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

public class LoadingPanel extends BasePanel {

  private static final Logger LOGGER = Logger.getLogger(LoadingPanel.class.getName());

  private final BufferedImage background;
  private final SoundClip soundClip;

  public LoadingPanel() {
    super();
    this.soundClip = new SoundClip("fullpart.wav");
    this.background = GfxUtils.loadGfxImageAsType("splash.png", BufferedImage.TYPE_INT_RGB, 1.0d);
    final Dimension size = new Dimension(this.background.getWidth(), this.background.getHeight());
    this.setMaximumSize(size);
    this.setMinimumSize(size);
    this.setSize(size);
    this.setPreferredSize(size);
  }

  protected void doLoading() {
    LOGGER.info("Loading animation");
    for (final Animation a : Animation.values()) {
      a.load();
    }
    LOGGER.info("Loading sounds");
    for (final Sound s : Sound.values()) {
      s.load();
    }
    LOGGER.info("Loading banners");
    InfoBanner.loadAll();
  }

  @Override
  protected void doStart() {
    this.soundClip.play(Integer.MAX_VALUE);

    final Thread loadingThread = new Thread(() -> {
      final long startTime = System.currentTimeMillis();
      try {
        this.doLoading();
        this.fireSignal(SIGNAL_LOADING_COMPLETED);
      } catch (Throwable ex) {
        ex.printStackTrace();
        this.fireSignal(SIGNAL_ERROR);
      }
      final long endTime = System.currentTimeMillis();
      LOGGER.info("Spent time for loading: " + (endTime - startTime) + " ms");

    }, "BattleShip-loading");
    loadingThread.setDaemon(true);
    loadingThread.start();
  }

  @Override
  protected void doDispose() {
    this.soundClip.close();
  }

  @Override
  protected void doPaint(final Graphics2D g) {
    g.drawImage(this.background, null, 0, 0);
  }
}
