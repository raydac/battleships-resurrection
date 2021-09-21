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


import com.igormaznitsa.battleships.gui.panels.*;
import com.igormaznitsa.battleships.opponent.BattleshipsPlayer;
import com.igormaznitsa.battleships.sound.Sound;
import com.igormaznitsa.battleships.utils.GfxUtils;
import com.igormaznitsa.battleships.utils.ImageCursor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.igormaznitsa.battleships.utils.GfxUtils.loadResImage;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class BattleshipsFrame extends JFrame implements BasePanel.SignalListener {

  private static final Logger LOGGER = Logger.getLogger(BattleshipsFrame.class.getName());
  private final BattleshipsPlayer opponent;

  private final Runnable exitAction;
  private final StartOptions startOptions;
  private final AtomicReference<BattleshipsCommDaemon> commDaemonThreadRef =
          new AtomicReference<>();
  private Optional<ScaleFactor> scaleFactor;

  private ImageCursor gameCursor = null;

  private final Cursor emptyCursor = GfxUtils.makeEmptyAwtCursor();

  public BattleshipsFrame(final StartOptions startOptions,
                          final BattleshipsPlayer opponent,
                          final Runnable exitAction) {
    super(startOptions.getGameTitle().orElse("Battleship"),
            startOptions.getGraphicsConfiguration().orElse(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                            .getDefaultConfiguration()));

    this.startOptions = startOptions;
    GfxUtils.setApplicationTaskbarTitle(this.startOptions.getGameIcon().orElse(null), null);

    this.scaleFactor = Optional.empty();
    if (startOptions.isFullScreen()) {
      this.setResizable(false);
      this.setAlwaysOnTop(true);
      this.setUndecorated(true);
    }
    this.exitAction = exitAction;
    this.opponent = opponent;

    startOptions.getGameIcon().ifPresent(this::setIconImage);
    this.setResizable(false);
    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        if (exitAction != null) {
          try {
            exitAction.run();
          } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "Error during close action", ex);
          }
        }
        doCloseWindow();
      }

      @Override
      public void windowActivated(WindowEvent e) {
        GfxUtils.setCursorForAll(BattleshipsFrame.this, emptyCursor);
      }
    });

    final KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventPostProcessor(e -> {
      boolean processed = false;
      if (!e.isConsumed()) {
        switch (e.getKeyCode()) {
          case KeyEvent.VK_ESCAPE: {
            this.onExit();
            e.consume();
            processed = true;
          }
          break;
          case KeyEvent.VK_X: {
            switch (e.getID()) {
              case KeyEvent.KEY_PRESSED: {
                BasePanel.setCreditsVisible(true);
                this.getContentPane().repaint();
              }
              break;
              case KeyEvent.KEY_RELEASED: {
                BasePanel.setCreditsVisible(false);
                this.getContentPane().repaint();
              }
              break;
            }
            e.consume();
            processed = true;
          }
          break;
          case KeyEvent.VK_SPACE: {
            final Container container = this.getContentPane();
            if (container instanceof BasePanel) {
              ((BasePanel) container).onGameKeyEvent(e);
            }
            e.consume();
            processed = true;
          }
          break;
        }
      }
      return processed;
    });

    final double scaleX;
    final double scaleY;
    if (startOptions.getGraphicsConfiguration().isPresent()) {
      final AffineTransform transforms =
              startOptions.getGraphicsConfiguration().get().getDefaultTransform();
      scaleX = transforms.getScaleX();
      scaleY = transforms.getScaleY();
    } else {
      scaleX = 1.0d;
      scaleY = 1.0d;
    }

    final Dimension defaultFrameSize =
            new Dimension((int) Math.round(scaleX * BasePanel.GAMEFIELD_WIDTH),
                    (int) Math.round(scaleY * BasePanel.GAMEFIELD_HEIGHT));


    final JPanel panel = new JPanel();
    panel.setSize(defaultFrameSize);
    panel.setMaximumSize(defaultFrameSize);
    panel.setMinimumSize(defaultFrameSize);
    panel.setPreferredSize(defaultFrameSize);

    this.setContentPane(panel);

    GfxUtils.setCursorForAll(this, this.emptyCursor);
    GfxUtils.setCursorForAll(this.getContentPane(), this.emptyCursor);
    GfxUtils.setCursorForAll(this.getGlassPane(), this.emptyCursor);

    this.pack();
  }

  public void start(final Optional<ScaleFactor> scaleFactor) {
    this.scaleFactor = requireNonNull(scaleFactor);
    this.gameCursor = new ImageCursor(loadResImage("cursor.png"), 8, 8);

    final LoadingPanel loadingPanel = new LoadingPanel(this.startOptions, this.scaleFactor, this.gameCursor);
    this.replaceContentPanel(loadingPanel);

    if (this.startOptions.isFullScreen()) {
      this.moveMouseIntoCenterOfForm();
    }
  }

  private void moveMouseIntoCenterOfForm() {
    try {
      Robot robot;
      if (this.startOptions.getGraphicsConfiguration().isPresent()) {
        robot = new Robot(this.startOptions.getGraphicsConfiguration().get().getDevice());
      } else {
        robot = new Robot();
      }
      final Rectangle formBounds = this.getBounds();
      LOGGER.info("Main form bounds: " + formBounds);
      final Point point = new Point(formBounds.width / 2, formBounds.height / 2);
      this.scaleFactor.ifPresent(s -> s.translatePoint(point));
      SwingUtilities.convertPointToScreen(point, this);
      robot.mouseMove(point.x, point.y);
      this.repaint();
    } catch (Exception ex) {
      LOGGER.warning("Can't move mouse into form center: " + ex.getMessage());
    }
  }

  private void onExit() {
    this.doCloseWindow();
  }

  private void replaceContentPanel(final BasePanel newPanel) {
    final Container oldPanel = this.getContentPane();
    if (oldPanel instanceof BasePanel) {
      ((BasePanel) oldPanel).removeSignalListener(this);
      ((BasePanel) oldPanel).dispose();
    }
    this.setContentPane(newPanel);
    this.revalidate();
    this.repaint();
    this.pack();
    newPanel.addSignalListener(this);
    SwingUtilities.invokeLater(newPanel::start);
    newPanel.requestFocus();
    GfxUtils.setCursorForAll(this, this.emptyCursor);
    newPanel.addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseEntered(final MouseEvent e) {
        GfxUtils.setCursorForAll(newPanel, emptyCursor);
      }
    });

    GfxUtils.setApplicationTaskbarTitle(this.startOptions.getGameIcon().orElse(null),
            newPanel.getApplicationBadgeTitle());
  }

  private void doLoadingCompleted() {
    final GamePanel gamePanel = new GamePanel(this.startOptions, this.scaleFactor, this.gameCursor);

    final BattleshipsCommDaemon newCommDaemon = new BattleshipsCommDaemon(gamePanel, this.opponent);
    if (this.commDaemonThreadRef.compareAndSet(null, newCommDaemon)) {
      newCommDaemon.start();
    }
    replaceContentPanel(gamePanel);

    gamePanel.start();
  }

  @Override
  public void onSignal(final BasePanel source, final String signal) {
    LOGGER.info("internal signal : " + signal);
    switch (signal) {
      case LoadingPanel.SIGNAL_LOADING_COMPLETED: {
        this.doLoadingCompleted();
      }
      break;
      case BasePanel.SIGNAL_LOST:
      case BasePanel.SIGNAL_VICTORY: {
        this.onGameEnd(BasePanel.SIGNAL_VICTORY.equalsIgnoreCase(signal) ? FinalState.VICTORY :
                FinalState.LOST);
      }
      break;
      case BasePanel.SIGNAL_PLAYER_IS_OUT: {
        this.onPlayerIsOut();
      }
      break;
      case BasePanel.SIGNAL_RESUME: {
        this.onGameResume();
      }
      break;
      case BasePanel.SIGNAL_SYSTEM_FAILURE: {
        this.onGameError();
      }
      break;
      case BasePanel.SIGNAL_EXIT: {
        this.onExit();
      }
      break;
      case BasePanel.SIGNAL_PAUSED: {
        this.onGamePaused();
      }
      break;
    }
  }

  private void onGameEnd(final FinalState state) {
    LOGGER.info("Game session ended, final state " + state);
    this.replaceContentPanel(new FinalPanel(this.startOptions, scaleFactor, state, this.gameCursor));
  }

  private void onGameError() {
    LOGGER.severe("Detected game error");
    this.replaceContentPanel(
            new FinalPanel(this.startOptions, this.scaleFactor, FinalState.SYSTEM_FAILURE, this.gameCursor));
  }

  private void onPlayerIsOut() {
    LOGGER.severe("Detected that opponent has left the game room");
    this.replaceContentPanel(
            new FinalPanel(this.startOptions, this.scaleFactor, FinalState.OPPONENT_OFF, this.gameCursor));
  }

  private void onGamePaused() {
    LOGGER.info("Game paused");
  }

  private void onGameResume() {
    LOGGER.info("Game resumed");
  }

  private void doCloseWindow() {
    LOGGER.info("Closing game");

    final BattleshipsCommDaemon currentDaemon = this.commDaemonThreadRef.getAndSet(null);
    if (currentDaemon != null) {
      LOGGER.info("Disposing comm daemon");
      currentDaemon.dispose();
    }

    try {
      final Container contentPane = this.getContentPane();
      if (contentPane instanceof BasePanel) {
        ((BasePanel) contentPane).dispose();
      }
    } finally {
      try {
        if (this.exitAction != null) {
          this.exitAction.run();
        }
      } finally {
        try {
          for (Sound s : Sound.values()) {
            s.dispose();
          }
          for (Animation a : Animation.values()) {
            a.dispose();
          }
        } finally {
          LOGGER.info("Disposing main window");
          this.dispose();
        }
      }
    }
  }
}
