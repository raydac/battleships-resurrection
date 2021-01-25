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

import static com.igormaznitsa.battleships.utils.GfxUtils.loadResImage;
import static java.awt.Toolkit.getDefaultToolkit;


import com.igormaznitsa.battleships.gui.ScaleFactor;
import com.igormaznitsa.battleships.utils.GfxUtils;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public abstract class BasePanel extends JComponent {
  public static final int GAMEFIELD_WIDTH = 800;
  public static final int GAMEFIELD_HEIGHT = 600;
  public static final Point BANNER_COORD = new Point(423, 10);

  public static final String SIGNAL_LOADING_COMPLETED = "LOADING_COMPLETED";
  public static final String SIGNAL_ERROR = "ERROR";
  public static final String SIGNAL_EXIT = "EXIT";
  public static final String SIGNAL_PAUSED = "PAUSED";
  public static final String SIGNAL_RESUME = "RESUME";
  public static final String SIGNAL_VICTORY = "VICTORY";
  public static final String SIGNAL_LOST = "LOST";
  private static final Cursor CURSOR = getDefaultToolkit()
      .createCustomCursor(loadResImage("cursor.png"), new Point(8, 8), "BattleShips");
  private final AtomicBoolean disposed = new AtomicBoolean();
  private final List<SignalListener> signalListenerList = new CopyOnWriteArrayList<>();

  private static BufferedImage creditsImage;
  protected final Optional<ScaleFactor> scaleFactor;

  private final Dimension size;

  public BasePanel(final Optional<ScaleFactor> scaleFactor) {
    super();
    this.scaleFactor = scaleFactor;
    this.setCursor(CURSOR);
    this.size =
        scaleFactor.map(sf -> new Dimension((int) Math.round(GAMEFIELD_WIDTH * sf.getScaleX()),
            (int) Math.round(GAMEFIELD_HEIGHT * sf.getScaleY())))
            .orElse(new Dimension(GAMEFIELD_WIDTH, GAMEFIELD_HEIGHT));
  }

  @Override
  public final Dimension getMinimumSize() {
    return this.size;
  }

  @Override
  public final Dimension getMaximumSize() {
    return this.size;
  }

  @Override
  public final Dimension getPreferredSize() {
    return this.size;
  }

  @Override
  public Dimension getSize() {
    return this.size;
  }

  @Override
  public final int getWidth() {
    return this.size.width;
  }

  @Override
  public final int getHeight() {
    return this.size.height;
  }

  public static void setCreditsVisible(final boolean show) {
    if (show) {
      if (creditsImage == null) {
        creditsImage =
            GfxUtils.loadGfxImageAsType("credits.png", BufferedImage.TYPE_INT_ARGB);
      }
    } else {
      creditsImage = null;
    }
  }


  public void onGameKeyEvent(final KeyEvent e) {

  }

  @Override
  public boolean isDoubleBuffered() {
    return false;
  }

  public void addSignalListener(final SignalListener listener) {
    if (listener != null) {
      this.signalListenerList.add(listener);
    }
  }

  public void removeSignalListener(final SignalListener listener) {
    if (listener != null) {
      this.signalListenerList.remove(listener);
    }
  }

  protected void fireSignal(final String signal) {
    final Runnable runnable = () -> {
      this.signalListenerList.forEach(x -> x.onSignal(this, signal));
    };
    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    } else {
      SwingUtilities.invokeLater(runnable);
    }
  }

  public final void paint(final Graphics g) {
    if (this.isDisposed()) {
      return;
    }
    final Graphics2D gfx = (Graphics2D) g;

    this.scaleFactor.ifPresent(sf -> sf.apply(gfx));

    gfx.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
        RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);
    gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
    gfx.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
        RenderingHints.VALUE_COLOR_RENDER_DEFAULT);

    this.doPaint(gfx);

    if (creditsImage != null) {
      gfx.drawImage(creditsImage, null, 0, 0);
    }
  }

  protected void doPaint(final Graphics2D g2d) {

  }

  protected void refreshUi() {
    this.revalidate();
    this.repaint(0L);
  }

  public final void start() {
    if (!this.isDisposed()) {
      this.doStart();
    }
  }

  protected void doStart() {

  }

  protected void doDispose() {

  }

  public final boolean isDisposed() {
    return this.disposed.get();
  }

  public final void dispose() {
    if (this.disposed.compareAndSet(false, true)) {
      this.signalListenerList.clear();
      this.doDispose();
    }
  }

  @Override
  public final boolean isOpaque() {
    return true;
  }

  @FunctionalInterface
  public interface SignalListener {
    void onSignal(BasePanel source, String signal);
  }
}
