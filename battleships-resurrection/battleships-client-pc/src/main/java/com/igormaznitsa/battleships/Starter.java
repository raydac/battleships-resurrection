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

package com.igormaznitsa.battleships;

import com.igormaznitsa.battleships.gui.BattleshipsFrame;
import com.igormaznitsa.battleships.gui.OpeningDialog;
import com.igormaznitsa.battleships.gui.ScaleFactor;
import com.igormaznitsa.battleships.gui.StartOptions;
import com.igormaznitsa.battleships.opponent.AiBattleshipsSingleSessionBot;
import com.igormaznitsa.battleships.opponent.BattleshipsPlayer;
import com.igormaznitsa.battleships.opponent.OldGexBattleshipSingleSessionBot;
import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Starter {

  private static final Logger LOGGER = Logger.getLogger(Starter.class.getName());

  public static void main(final String... args) {
    SwingUtilities.invokeLater(() -> {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Exception ex) {
        // ignoring
      }

      final StartOptions startOptions = StartOptions.newBuilder().build();
      final OpeningDialog openingDialog = new OpeningDialog(startOptions);
      openingDialog.setVisible(true);

      final StartOptions selectedData = openingDialog.getResult().orElse(null);
      if (selectedData == null) {
        LOGGER.info("Exit for player request");
        System.exit(0);
      }

      final BattleshipsPlayer selectedOpponent;
      if (selectedData.isMultiPlayer()) {
        selectedOpponent = new OldGexBattleshipSingleSessionBot(selectedData).startPlayer();
      } else {
        selectedOpponent = new AiBattleshipsSingleSessionBot().startPlayer();
      }
      final Optional<GraphicsDevice> device = selectedData.getGraphicsConfiguration().map(
          GraphicsConfiguration::getDevice);

      final AtomicReference<BattleshipsFrame> mainFrameRef = new AtomicReference<>();

      final AtomicReference<Optional<ScaleFactor>> scaleFactorRef =
          new AtomicReference<>(Optional.empty());
      if (selectedData.isFullScreen()) {
        device.ifPresentOrElse(d -> {
          if (d.isFullScreenSupported()) {
            LOGGER.info("Detected support of full screen: " + d);
            mainFrameRef.set(new BattleshipsFrame(selectedData, selectedOpponent, () -> {
              try {
                d.setFullScreenWindow(null);
              } finally {
                selectedOpponent.disposePlayer();
              }
            }));
            final DisplayMode displayMode = new DisplayMode(800, 600, DisplayMode.BIT_DEPTH_MULTI,
                DisplayMode.REFRESH_RATE_UNKNOWN);
            try {
              d.setFullScreenWindow(mainFrameRef.get());
              scaleFactorRef
                  .set(Optional.of(new ScaleFactor(selectedData.getGraphicsConfiguration().get())));
            } catch (Exception ex) {
              LOGGER.log(Level.SEVERE, "Can't set full screen window", ex);
              System.exit(4);
            }
            if (d.isDisplayChangeSupported()) {
              LOGGER.info("Detected support of display change: " + d);
              try {
                d.setDisplayMode(displayMode);
                scaleFactorRef.set(Optional.empty());
              } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Error during display change", ex);
              }
            } else {
              d.setFullScreenWindow(null);
              LOGGER.log(Level.FINE, "Display change is not allowed by device: " + d);
            }
          } else {
            LOGGER.severe("Full screen is not supported by device: " + d);
            JOptionPane.showMessageDialog(null, "Full-screen is not supported for the device!",
                "Can't full screen", JOptionPane.ERROR_MESSAGE);
            System.exit(3);
          }
        }, () -> {
          JOptionPane.showMessageDialog(null, "Can't find device for full screen",
              "Can't start full-screen", JOptionPane.ERROR_MESSAGE);
          LOGGER.severe("Can't find device for full screen");
          System.exit(5);
        });
      } else {
        mainFrameRef
            .set(new BattleshipsFrame(selectedData, selectedOpponent,
                selectedOpponent::disposePlayer));
      }

      final BattleshipsFrame mainFrame = mainFrameRef.get();
      if (mainFrame != null) {
        mainFrame.setVisible(true);
        mainFrame.start(scaleFactorRef.get());
      }
    });

  }
}
