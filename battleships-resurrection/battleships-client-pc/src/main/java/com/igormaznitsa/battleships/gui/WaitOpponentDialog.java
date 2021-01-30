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

import com.igormaznitsa.battleships.opponent.BattleshipsPlayer;
import com.igormaznitsa.battleships.utils.GfxUtils;
import java.awt.GraphicsConfiguration;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;

public class WaitOpponentDialog extends JDialog {
  private final BattleshipsPlayer player;
  private final JProgressBar progressBar;
  private final Timer timer;
  private boolean completed;

  public WaitOpponentDialog(final GraphicsConfiguration configuration, final Image icon,
                            final BattleshipsPlayer player) {
    super(null, "Battleship", ModalityType.APPLICATION_MODAL, configuration);
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
    if (!this.player.isAvailable()) {
      this.completed = false;
      this.progressBar.setString("Waiting connection");
    } else if (!this.player.isReadyForGame()) {
      this.completed = false;
      this.progressBar.setString("Waiting opponent");
    } else {
      this.completed = true;
      this.progressBar.setString("");
      this.disposeWindow();
    }
  }

  private void disposeWindow() {
    this.timer.stop();
    this.dispose();
  }

  public boolean start() {
    this.completed = false;
    if (this.player.isAvailable() && this.player.isReadyForGame()) {
      this.completed = true;
    } else {
      this.timer.start();
      GfxUtils.toScreenCenter(this);
      this.setVisible(true);
    }
    this.disposeWindow();
    return this.completed;
  }
}
