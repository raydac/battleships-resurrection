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

import com.igormaznitsa.battleships.gui.*;
import com.igormaznitsa.battleships.opponent.AiBattleshipsSingleSessionBot;
import com.igormaznitsa.battleships.opponent.BattleshipsPlayer;
import com.igormaznitsa.battleships.opponent.NewNetSingleSessionOpponent;
import com.igormaznitsa.battleships.opponent.OldGfxBattleshipSingleSessionBot;
import com.igormaznitsa.battleships.utils.GfxUtils;
import com.igormaznitsa.battleships.utils.NetUtils;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BattleShips {

  private static final Logger LOGGER = Logger.getLogger(BattleShips.class.getName());

  public static void main(final String... args) {
    System.setProperty("apple.awt.fullscreenhidecursor", "true");

    SwingUtilities.invokeLater(() -> {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Exception ex) {
        // ignoring
      }

      final StartOptions startOptions = StartOptions.newBuilder().loadPreferences().build();
      final OpeningDialog openingDialog = new OpeningDialog(startOptions);
      GfxUtils.toScreenCenter(openingDialog);
      openingDialog.setVisible(true);

      final StartOptions selectedData = openingDialog.getResult().orElse(null);
      if (selectedData == null) {
        LOGGER.info("Exit for player request");
        System.exit(0);
      } else {
        LOGGER.info("Saving preferences");
        selectedData.savePreferences();
      }

      final int chosenPort = selectedData.getHostPort().orElse(30000);
      final BattleshipsPlayer selectedOpponent;
      if (selectedData.isMultiPlayer()) {
        GfxUtils.setApplicationTaskbarTitle(startOptions.getGameIcon().orElse(null), "Network");
        switch (selectedData.getMultiPlayerMode()) {
          case GFX_PLAYROOM: {
            InetAddress address = null;
            try {
              address = InetAddress.getByName(NetUtils.removeInterfaceNameIfFound(selectedData.getHostName().orElseThrow(NullPointerException::new)));
            } catch (Exception ex) {
              LOGGER.severe("Can't find host address: " + selectedData.getHostName());
              JOptionPane.showMessageDialog(null, "Can't resolve address of host: " + selectedData.getHostName().orElse("<not provided>"));
              System.exit(556677);
            }
            LOGGER.info("Creating old client for: " + address + " : " + chosenPort);
            selectedOpponent = new OldGfxBattleshipSingleSessionBot(address, chosenPort).startPlayer();
          }
          break;
          case LAN_P2P: {
            final String interfaceName = selectedData.getHostName().orElse("localhost");
            final NetUtils.NamedInterfaceAddress selectedInterface = NetUtils.findAllIp4NetworkInterfacesWithBroadcast().stream()
                    .filter(x -> x.getName().equals(interfaceName))
                    .findFirst().orElse(null);
            if (selectedInterface == null) {
              LOGGER.severe("Can't find interface: " + selectedData.getHostName());
              JOptionPane.showMessageDialog(null, "Can't find interface: " + selectedData.getHostName().orElse(""));
              System.exit(556677);
            }
            LOGGER.info("Creating new client for: " + selectedInterface + " : " + chosenPort);
            selectedOpponent = new NewNetSingleSessionOpponent(selectedData, selectedInterface, chosenPort).startPlayer();
          }
          break;
          default:
            throw new IllegalArgumentException("Unexpected multi-player mode: " + selectedData.getMultiPlayerMode());
        }
      } else {
        GfxUtils.setApplicationTaskbarTitle(startOptions.getGameIcon().orElse(null), null);
        selectedOpponent = new AiBattleshipsSingleSessionBot().startPlayer();
      }

      if (selectedOpponent == null) {
        LOGGER.info("Opponent not selected");
        System.exit(112233);
      }

      final WaitOpponentDialog waitOpponentDialog =
              new WaitOpponentDialog(
                      Duration.ofSeconds(30),
                      selectedData.getGraphicsConfiguration().orElse(null),
                      selectedData.getGameTitle().orElse("BattleShips"),
                      selectedData.getGameIcon().orElse(null), selectedOpponent);
      try {
        if (waitOpponentDialog.start()) {
          LOGGER.info("Waiting is completed");
        } else {
          LOGGER.warning("Wait was interrupted by user");
          System.exit(1);
        }
      } catch (TimeoutException ex) {
        LOGGER.log(Level.SEVERE, "Timeout during wait of game server or opponent!", ex);
        JOptionPane.showMessageDialog(null, "Timeout for opponent and server wait!", "Timeout", JOptionPane.WARNING_MESSAGE);
        System.exit(2233);
      }

      if (!selectedOpponent.isAvailable()) {
        LOGGER.severe(selectedOpponent.getId() + " is unavailable");
        JOptionPane.showMessageDialog(null, selectedOpponent.getId() + " is unavailable!",
                "Error", JOptionPane.ERROR_MESSAGE);
        System.exit(11);
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

            GfxUtils.tryMacOsFullScreen(mainFrameRef.get());

            final DisplayMode displayMode = new DisplayMode(800, 600, DisplayMode.BIT_DEPTH_MULTI,
                    DisplayMode.REFRESH_RATE_UNKNOWN);
            try {
              d.setFullScreenWindow(mainFrameRef.get());
              scaleFactorRef
                      .set(Optional.of(new ScaleFactor(selectedData.getGraphicsConfiguration().get())));
              LOGGER.info("Set full screen window");
            } catch (Exception ex) {
              LOGGER.log(Level.SEVERE, "Can't set full screen window", ex);
              selectedOpponent.disposePlayer();
              System.exit(4);
            }
            if (d.isDisplayChangeSupported()) {
              LOGGER.info("Detected support of display change: " + d);
              try {
                d.setDisplayMode(displayMode);
                scaleFactorRef.set(Optional.empty());
                LOGGER.info("Display mode is set: " + displayMode);
              } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Error during display change", ex);
              }
            } else {
              LOGGER.info("Display change is not allowed by device: " + d);
            }
          } else {
            LOGGER.severe("Full screen is not supported by device: " + d);
            JOptionPane.showMessageDialog(null, "Full-screen is not supported for the device!",
                    "Can't full screen", JOptionPane.ERROR_MESSAGE);
            selectedOpponent.disposePlayer();
            System.exit(3);
          }
        }, () -> {
          JOptionPane.showMessageDialog(null, "Can't find device for full screen",
                  "Can't start full-screen", JOptionPane.ERROR_MESSAGE);
          LOGGER.severe("Can't find device for full screen");
          selectedOpponent.disposePlayer();
          System.exit(5);
        });
      } else {
        mainFrameRef
                .set(new BattleshipsFrame(selectedData, selectedOpponent,
                        selectedOpponent::disposePlayer));
      }

      final BattleshipsFrame mainFrame = mainFrameRef.get();
      if (mainFrame != null) {
        if (!selectedData.isFullScreen()) {
          GfxUtils.toScreenCenter(mainFrame);
        }
        mainFrame.setVisible(true);
        mainFrame.start(scaleFactorRef.get());
      }
    });

  }
}
