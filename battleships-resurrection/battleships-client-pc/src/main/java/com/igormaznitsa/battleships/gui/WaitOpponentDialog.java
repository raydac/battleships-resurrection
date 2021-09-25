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

import com.igormaznitsa.battleships.opponent.BattleshipsPlayer;
import com.igormaznitsa.battleships.utils.GfxUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public class WaitOpponentDialog extends JDialog {
  private final BattleshipsPlayer player;
  private final JProgressBar progressBar;
  private final Timer timer;
  private boolean completed;
  private final Duration maxWaitTime;
  private boolean timeOut;
  private long startTime;

  public WaitOpponentDialog(final Duration maxWaitTime,
                            final GraphicsConfiguration configuration,
                            final String title,
                            final Image icon,
                            final BattleshipsPlayer player) {
    super(null, title, ModalityType.APPLICATION_MODAL, configuration);

    GfxUtils.setApplicationTaskbarTitle(icon, title);

    this.maxWaitTime = maxWaitTime;

    this.setAlwaysOnTop(true);
    this.player = Objects.requireNonNull(player);
    this.setIconImage(icon);
    this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    this.completed = false;

    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        completed = false;
        disposeWindow();
      }
    });

    this.timer = new Timer(1000, this::check);
    this.timer.setRepeats(true);

    final JPanel panel = new JPanel(new GridLayout(2, 1, 8, 8));
    panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    final JLabel label = new JLabel("Wait...                                                     ");
    label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
    panel.add(label);
    this.progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
    this.progressBar.setIndeterminate(true);
    this.progressBar.setStringPainted(true);
    this.progressBar.setString("");

    panel.add(this.progressBar);

    this.setContentPane(panel);
    this.pack();
  }

  private void check(final ActionEvent action) {
    if (System.currentTimeMillis() - this.startTime > maxWaitTime.toMillis()) {
      this.timeOut = true;
      this.disposeWindow();
    } else {
      if (!this.player.isAvailable()) {
        this.completed = false;
        this.progressBar.setString("Waiting for availability...");
      } else if (!this.player.isReadyForGame()) {
        this.completed = false;
        this.progressBar.setString("Waiting for opponent ready...");
      } else {
        this.completed = true;
        this.progressBar.setString("Completed");
        this.disposeWindow();
      }
    }
  }

  private void disposeWindow() {
    this.timer.stop();
    this.dispose();
  }

  public boolean start() throws TimeoutException {
    this.completed = false;
    this.startTime = System.currentTimeMillis();
    if (this.player.isAvailable() && this.player.isReadyForGame()) {
      this.completed = true;
    } else {
      this.timer.start();
      GfxUtils.toScreenCenter(this);
      this.setVisible(true);
      if (this.timeOut) {
        throw new TimeoutException();
      }
    }
    this.disposeWindow();
    return this.completed;
  }
}
