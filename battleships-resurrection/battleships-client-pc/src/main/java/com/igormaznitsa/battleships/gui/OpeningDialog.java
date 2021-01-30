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

import static com.igormaznitsa.battleships.utils.GfxUtils.loadResImage;
import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createTitledBorder;


import com.igormaznitsa.battleships.utils.GfxUtils;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.text.DecimalFormat;
import java.util.Optional;
import javax.swing.Box;
import javax.swing.Box.Filler;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.text.NumberFormatter;

public class OpeningDialog extends javax.swing.JDialog {

  private JButton buttonExit;
  private JButton buttonGo;
  private JPanel buttonsPanel;
  private JLabel logoLabel;
  private JPanel mainPanel;
  private JPanel modePanel;
  private JPanel networkPanel;
  private JTextField textFieldHostName;
  private JFormattedTextField textFieldPort;
  private Filler filler4;
  private Filler filler5;
  private Filler filler6;
  private JLabel labelServerHostName;
  private JLabel labelServerPort;
  private JRadioButton radioWindow;
  private JRadioButton radioFullScreen;
  private JRadioButton radioSinglePlayer;
  private JRadioButton radioMultiPlayer;
  private JCheckBox checkboxUseOldGfxClient;

  private StartOptions result;

  public OpeningDialog(final StartOptions startOptions) {
    super((Frame) null, startOptions.getGameTitle().orElse("Battleship"), true,
        startOptions.getGraphicsConfiguration().orElse(
            getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration()));
    startOptions.getGameIcon().ifPresent(this::setIconImage);
    initComponents();

    GfxUtils.setApplicationTitle(startOptions.getGameIcon().orElse(null),
        startOptions.getGameTitle().orElse(null));

    this.setAlwaysOnTop(true);

    this.result = null;
    this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

    this.setTitle("Start settings");

    this.radioSinglePlayer.setSelected(!startOptions.isMultiPlayer());
    this.radioMultiPlayer.setSelected(startOptions.isMultiPlayer());
    this.radioWindow.setSelected(!startOptions.isFullScreen());
    this.radioFullScreen.setSelected(startOptions.isFullScreen());
    startOptions.getHostName().ifPresent(x -> this.textFieldHostName.setText(x));
    startOptions.getHostPort().ifPresent(x -> this.textFieldPort.setText(Integer.toString(x)));

    this.checkboxUseOldGfxClient.setSelected(startOptions.isUseOldGfxClient());

    this.radioSinglePlayer.addActionListener(e -> {
      this.updateNetworkPanel();
    });

    this.radioMultiPlayer.addActionListener(e -> {
      if (this.radioMultiPlayer.isSelected()) {
        if (this.textFieldHostName.getText().isBlank()) {
          startOptions.getHostName().ifPresent(t -> {
            this.textFieldHostName.setText(t);
          });
        }
        if (this.textFieldPort.getText().isBlank()) {
          startOptions.getHostPort().ifPresent(p -> {
            this.textFieldPort.setText(Integer.toString(p));
          });
        }
      }
      this.updateNetworkPanel();
    });

    this.buttonGo.addActionListener(e -> {
      int hostPort = -1;
      try {
        hostPort = Integer.parseInt(this.textFieldPort.getText().toString().trim());
      } catch (Exception ex) {
        // ignoring
      }
      this.result = StartOptions.newBuilder()
          .setGameTitle(startOptions.getGameTitle().orElse("Battleships"))
          .setGameIcon(startOptions.getGameIcon().orElse(null))
          .setFullScreen(this.radioFullScreen.isSelected())
          .setMultiPlayer(this.radioMultiPlayer.isSelected())
          .setHostPort(hostPort)
          .setHostName(this.textFieldHostName.getText().trim())
          .build();
      this.dispose();
    });

    this.buttonExit.addActionListener(e -> {
      this.result = null;
      this.dispose();
    });

    this.updateNetworkPanel();
    this.getContentPane().doLayout();
    this.pack();
  }

  private void updateNetworkPanel() {
    final boolean enabled = this.radioMultiPlayer.isSelected();
    this.textFieldHostName.setEnabled(enabled);
    this.textFieldPort.setEnabled(enabled);

    this.checkboxUseOldGfxClient.setEnabled(false); // todo replace when implementd another client
  }

  public Optional<StartOptions> getResult() {
    return Optional.ofNullable(this.result);
  }

  private void initComponents() {
    GridBagConstraints gridBagConstraints;

    mainPanel = new JPanel();
    logoLabel = new JLabel();
    modePanel = new JPanel();
    radioWindow = new JRadioButton();
    radioFullScreen = new JRadioButton();
    checkboxUseOldGfxClient = new JCheckBox();
    networkPanel = new JPanel();
    radioSinglePlayer = new JRadioButton();
    radioMultiPlayer = new JRadioButton();
    labelServerHostName = new JLabel();
    labelServerPort = new JLabel();
    textFieldHostName = new JTextField();
    textFieldPort = new JFormattedTextField(new NumberFormatter(new DecimalFormat("####")));
    buttonsPanel = new JPanel();
    buttonsPanel.setBorder(createEmptyBorder(0, 8, 16, 8));
    buttonGo = new JButton();
    buttonExit = new JButton();
    filler6 =
        new Filler(new Dimension(32, 0), new Dimension(32, 0),
            new Dimension(32, 32767));
    filler4 =
        new Filler(new Dimension(0, 16), new Dimension(0, 16),
            new Dimension(32767, 16));
    filler5 =
        new Filler(new Dimension(0, 16), new Dimension(0, 16),
            new Dimension(32767, 16));

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    mainPanel.setLayout(new GridBagLayout());

    logoLabel.setIcon(
        new ImageIcon(loadResImage("rusoft.png").getScaledInstance(400, 258, Image.SCALE_SMOOTH)));
    mainPanel.add(logoLabel, new GridBagConstraints());

    modePanel
        .setBorder(createCompoundBorder(createTitledBorder("Mode"), createEmptyBorder(8, 8, 8, 8)));
    modePanel.setLayout(new GridLayout(1, 0, 16, 0));

    radioWindow.setText("Window");
    radioWindow.setHorizontalAlignment(SwingConstants.LEFT);
    modePanel.add(radioWindow);

    radioFullScreen.setText("FullScreen");
    modePanel.add(radioFullScreen);

    final ButtonGroup screenTypeGroup = new ButtonGroup();
    screenTypeGroup.add(radioFullScreen);
    screenTypeGroup.add(radioWindow);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    mainPanel.add(modePanel, gridBagConstraints);

    networkPanel.setBorder(
        createCompoundBorder(createTitledBorder("Network"), createEmptyBorder(8, 8, 8, 8)));
    networkPanel.setLayout(new GridLayout(6, 2, 16, 0));

    radioSinglePlayer.setText("Single Player");
    networkPanel.add(radioSinglePlayer);

    radioMultiPlayer.setText("Multi-Player");
    networkPanel.add(radioMultiPlayer);

    final ButtonGroup gameTypeGroup = new ButtonGroup();
    gameTypeGroup.add(radioSinglePlayer);
    gameTypeGroup.add(radioMultiPlayer);

    labelServerHostName.setText("Server host name:");
    networkPanel.add(labelServerHostName);

    labelServerPort.setText("Port:");
    networkPanel.add(labelServerPort);
    networkPanel.add(textFieldHostName);
    networkPanel.add(textFieldPort);

    networkPanel.add(Box.createHorizontalGlue());
    checkboxUseOldGfxClient.setText("Use old GFX client");
    checkboxUseOldGfxClient.setHorizontalAlignment(JCheckBox.LEFT);
    networkPanel.add(checkboxUseOldGfxClient);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    mainPanel.add(networkPanel, gridBagConstraints);

    buttonsPanel.setLayout(new GridBagLayout());

    buttonGo.setText("    Go!    ");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    buttonsPanel.add(buttonGo, gridBagConstraints);

    buttonExit.setText("   Exit   ");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    buttonsPanel.add(buttonExit, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    buttonsPanel.add(filler6, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    mainPanel.add(buttonsPanel, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    mainPanel.add(filler4, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    mainPanel.add(filler5, gridBagConstraints);

    this.setContentPane(mainPanel);
  }
}
