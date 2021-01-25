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

package com.igormaznitsa.battleships.gui;


import com.igormaznitsa.battleships.gui.panels.BasePanel;
import com.igormaznitsa.battleships.gui.panels.FinalPanel;
import com.igormaznitsa.battleships.gui.panels.GamePanel;
import com.igormaznitsa.battleships.gui.panels.LoadingPanel;
import com.igormaznitsa.battleships.opponent.BsGameEvent;
import com.igormaznitsa.battleships.opponent.BsPlayer;
import com.igormaznitsa.battleships.sound.Sound;
import java.awt.Container;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class BattleshipsFrame extends JFrame implements BasePanel.SignalListener {

  private static final Logger LOGGER = Logger.getLogger(BattleshipsFrame.class.getName());
  private final BsPlayer opponent;

  private final Runnable exitAction;

  private final Optional<ScaleFactor> scaleFactor;


  public BattleshipsFrame(final StartData startData, final BsPlayer opponent,
                          final Runnable exitAction) {
    super(startData.getGameTitle().orElse("Battleship"),
        startData.getGraphicsConfiguration().orElse(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration()));

    if (startData.isFullScreen()) {
      this.setResizable(false);
      this.setAlwaysOnTop(true);
      this.setUndecorated(true);
      final Optional<GraphicsConfiguration> gconfig = startData.getGraphicsConfiguration();
      if (gconfig.isPresent()) {
        final Rectangle screen = gconfig.get().getBounds();
        this.scaleFactor = Optional
            .of(new ScaleFactor(screen.getWidth() / 800.0d, screen.getHeight() / 600.0d));
        LOGGER.info("Calculated scale factor " + scaleFactor + " for screen: " + gconfig.get());
      } else {
        this.scaleFactor = Optional.empty();
      }
    } else {
      this.scaleFactor = Optional.empty();
    }

    this.exitAction = exitAction;
    this.opponent = opponent;

    startData.getGameIcon().ifPresent(this::setIconImage);
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
  }

  public void start() {
    final LoadingPanel loadingPanel = new LoadingPanel(this.scaleFactor);
    this.replaceContentPanel(loadingPanel);
  }

  protected void onExit() {

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
    SwingUtilities.invokeLater(() -> {
      newPanel.start();
    });
    newPanel.requestFocus();
  }

  protected void doLoadingCompleted() {
    final GamePanel gamePanel = new GamePanel(this.scaleFactor);

    final Thread commDaemonThread = new Thread(() -> {
      LOGGER.info("Comm-Daemon started");
      while (Thread.currentThread().isAlive()) {
        try {
          Optional<BsGameEvent> event = this.opponent.pollGameEvent(Duration.ofMillis(100));
          event.ifPresent(e -> {
            LOGGER.info("Message to the game panel: " + e);
            gamePanel.pushGameEvent(e);
          });
          event = gamePanel.pollGameEvent(Duration.ofMillis(100));
          event.ifPresent(e -> {
            LOGGER.info("Message to the opponent: " + e);
            this.opponent.pushGameEvent(e);
          });
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
      LOGGER.info("Comm-Daemon stopped");
    }, "bs-communication-daemon");
    commDaemonThread.setDaemon(true);
    commDaemonThread.start();

    replaceContentPanel(gamePanel);

    gamePanel.start();
  }

  @Override
  public void onSignal(final BasePanel source, final String signal) {
    switch (signal) {
      case LoadingPanel.SIGNAL_LOADING_COMPLETED: {
        this.doLoadingCompleted();
      }
      break;
      case BasePanel.SIGNAL_LOST:
      case BasePanel.SIGNAL_VICTORY: {
        this.onGameEnd(BasePanel.SIGNAL_VICTORY.equalsIgnoreCase(signal));
      }
      break;
      case BasePanel.SIGNAL_RESUME: {
        this.onGameResume();
      }
      break;
      case BasePanel.SIGNAL_ERROR: {
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


  private void onGameEnd(final boolean victory) {
    LOGGER.info("Game session completed, victory flag=" + victory);
    this.replaceContentPanel(new FinalPanel(this.scaleFactor, victory));
  }

  private void onGameError() {
    LOGGER.severe("Detected game error");
  }

  private void onGamePaused() {
    LOGGER.info("Game paused");
  }

  private void onGameResume() {
    LOGGER.info("Game resumed");
  }

  private void doCloseWindow() {
    LOGGER.info("Closing game");
    try {
      final Container contentPane = this.getContentPane();
      if (contentPane instanceof BasePanel) {
        ((BasePanel) contentPane).dispose();
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
