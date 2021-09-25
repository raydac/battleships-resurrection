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
import com.igormaznitsa.battleships.utils.NetUtils;
import com.igormaznitsa.battleships.utils.Utils;

import javax.swing.*;
import javax.swing.Box.Filler;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.Optional;

import static com.igormaznitsa.battleships.utils.GfxUtils.loadResImage;
import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import static javax.swing.BorderFactory.*;

public class OpeningDialog extends JDialog {

  private final java.util.List<NetUtils.NamedInterfaceAddress> networkInterfaces;
  private JButton buttonExit;
  private JButton buttonGo;
  private JPanel networkPanel;
  private JFormattedTextField textFieldPort;
  private JLabel labelServerHostName;
  private JRadioButton radioWindow;
  private JRadioButton radioFullScreen;
  private JRadioButton radioSinglePlayer;
  private JRadioButton radioMultiPlayer;
  private JComboBox<MultiPlayerMode> comboBoxMultiPlayerMode;
  private JComboBox<String> comboInterfaceName;
  private StartOptions result;

  public OpeningDialog(final StartOptions startOptions) {
    super((Frame) null, startOptions.getGameTitle().orElse("Battleship"), true,
            startOptions.getGraphicsConfiguration().orElse(
                    getLocalGraphicsEnvironment().getDefaultScreenDevice()
                            .getDefaultConfiguration()));
    this.setModalityType(ModalityType.TOOLKIT_MODAL);

    this.networkInterfaces = NetUtils.findAllIp4NetworkInterfacesWithBroadcast();

    startOptions.getGameIcon().ifPresent(this::setIconImage);
    initComponents();

    GfxUtils.setApplicationTaskbarTitle(startOptions.getGameIcon().orElse(null), "Settings");

    this.setAlwaysOnTop(true);

    this.result = null;
    this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

    this.setTitle("Start settings");

    this.radioSinglePlayer.setSelected(!startOptions.isMultiPlayer());
    this.radioMultiPlayer.setSelected(startOptions.isMultiPlayer());
    this.radioWindow.setSelected(!startOptions.isFullScreen());
    this.radioFullScreen.setSelected(startOptions.isFullScreen());

    startOptions.getHostName().ifPresentOrElse(x -> {
      boolean found = false;
      for (int i = 0; i < this.comboInterfaceName.getItemCount(); i++) {
        if (x.equals(this.comboInterfaceName.getItemAt(i))) {
          found = true;
          break;
        }
      }
      if (!found) {
        this.comboInterfaceName.addItem(x);
      }
      this.comboInterfaceName.setSelectedItem(x);
    }, () -> NetUtils.findLanInterface(this.networkInterfaces)
            .ifPresent(y -> this.comboInterfaceName.setSelectedItem(y.getName())));

    startOptions.getHostPort().ifPresent(x -> this.textFieldPort.setText(Integer.toString(x)));

    this.comboBoxMultiPlayerMode.setSelectedItem(startOptions.getMultiPlayerMode());

    this.radioSinglePlayer.addActionListener(e -> networkPanel.setEnabled(this.radioMultiPlayer.isEnabled()));

    this.radioMultiPlayer.addActionListener(e ->
            Utils.setContainerEnabled(this.networkPanel, this.radioMultiPlayer.isEnabled(), JRadioButton.class));

    this.buttonGo.addActionListener(e -> {
      int hostPort = -1;
      try {
        hostPort = Integer.parseInt(this.textFieldPort.getText().trim());
      } catch (Exception ex) {
        // ignoring
      }

      this.result = StartOptions.newBuilder()
              .setGraphicsConfiguration(this.getGraphicsConfiguration())
              .setGameTitle(startOptions.getGameTitle().orElse("Battleships"))
              .setGameIcon(startOptions.getGameIcon().orElse(null))
              .setFullScreen(this.radioFullScreen.isSelected())
              .setMultiPlayer(this.radioMultiPlayer.isSelected())
              .setMultiPlayerMode((MultiPlayerMode) this.comboBoxMultiPlayerMode.getSelectedItem())
              .setHostPort(hostPort)
              .setHostName(String.valueOf(this.comboInterfaceName.getSelectedItem()))
              .build();
      this.dispose();
    });

    this.buttonExit.addActionListener(e -> {
      this.result = null;
      this.dispose();
    });

    Utils.setContainerEnabled(this.networkPanel, this.radioMultiPlayer.isSelected(), JRadioButton.class);
    this.updateComboHostName();

    this.getContentPane().doLayout();
    this.pack();
  }

  public Optional<StartOptions> getResult() {
    return Optional.ofNullable(this.result);
  }

  @Override
  public void setVisible(final boolean flag) {
    // workaround for a swing bug, to update look of disabled elements
    if (flag) {
      SwingUtilities.invokeLater(() -> {
        if (this.radioMultiPlayer.isSelected()) {
          this.radioMultiPlayer.doClick();
        } else {
          this.radioSinglePlayer.doClick();
        }
      });
    }
    super.setVisible(flag);
  }

  private JPanel makeNetworkPanel() {
    final JPanel resultPanel = new JPanel(new GridBagLayout());

    resultPanel.setBorder(
            createCompoundBorder(createTitledBorder("Network"), createEmptyBorder(8, 8, 8, 8)));

    this.radioSinglePlayer = new JRadioButton("Single Player");
    this.radioMultiPlayer = new JRadioButton("Multi-Player");
    this.labelServerHostName = new JShrinkableLabel("");
    JLabel labelServerPort = new JShrinkableLabel("Port:");

    this.textFieldPort = new JFormattedTextField(new NumberFormatter(new DecimalFormat("####")));

    this.comboBoxMultiPlayerMode = new JComboBox<>(new DefaultComboBoxModel<>(MultiPlayerMode.values()));
    this.comboBoxMultiPlayerMode.setToolTipText("Selected network game mode");
    this.comboBoxMultiPlayerMode.addActionListener(x -> this.updateComboHostName());

    this.comboInterfaceName = new JShrinkableComboBox<>(NetUtils.findAllIp4NetworkInterfacesWithBroadcast().stream().map(NetUtils.NamedInterfaceAddress::getName).toArray(String[]::new));

    final GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 4), 0, 0);

    resultPanel.add(this.radioSinglePlayer, gbc);
    this.radioSinglePlayer.addActionListener(e -> {
      this.textFieldPort.setEditable(!this.radioSinglePlayer.isEnabled());
      Utils.setContainerEnabled(resultPanel, !this.radioSinglePlayer.isEnabled(), JRadioButton.class);
    });

    gbc.gridx = 1;
    resultPanel.add(radioMultiPlayer, gbc);

    final ButtonGroup gameTypeGroup = new ButtonGroup();
    gameTypeGroup.add(this.radioSinglePlayer);
    gameTypeGroup.add(this.radioMultiPlayer);

    gbc.gridx = 0;
    gbc.gridy = 1;

    resultPanel.add(this.labelServerHostName, gbc);

    gbc.gridx = 1;

    resultPanel.add(labelServerPort, gbc);

    gbc.gridx = 0;
    gbc.gridy = 2;

    resultPanel.add(this.comboInterfaceName, gbc);

    gbc.gridx = 1;

    resultPanel.add(this.textFieldPort, gbc);

    gbc.gridx = 0;
    gbc.gridy = 3;

    resultPanel.add(Box.createHorizontalGlue(), gbc);

    gbc.gridx = 0;
    gbc.gridwidth = 2;

    final JPanel multiPlayerModePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    multiPlayerModePanel.add(new JLabel("Server mode: "));
    multiPlayerModePanel.add(this.comboBoxMultiPlayerMode);
    resultPanel.add(multiPlayerModePanel, gbc);

    return resultPanel;
  }

  private void initComponents() {
    GridBagConstraints gridBagConstraints;

    JPanel mainPanel = new JPanel(new GridBagLayout());
    JLabel logoLabel = new JLabel();
    JPanel modePanel = new JPanel();
    radioWindow = new JRadioButton();
    radioFullScreen = new JRadioButton();
    networkPanel = this.makeNetworkPanel();

    JPanel buttonsPanel = new JPanel();
    buttonsPanel.setBorder(createEmptyBorder(0, 8, 16, 8));
    buttonGo = new JButton();
    buttonExit = new JButton();
    Filler filler6 = new Filler(new Dimension(32, 0), new Dimension(32, 0),
            new Dimension(32, 32767));
    Filler filler4 = new Filler(new Dimension(0, 16), new Dimension(0, 16),
            new Dimension(32767, 16));
    Filler filler5 = new Filler(new Dimension(0, 16), new Dimension(0, 16),
            new Dimension(32767, 16));

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

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
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.anchor = GridBagConstraints.CENTER;
    mainPanel.add(networkPanel, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    mainPanel.add(filler5, gridBagConstraints);

    this.setContentPane(new JScrollPane(mainPanel));
  }

  private void updateComboHostName() {
    final String selected = (String) this.comboInterfaceName.getSelectedItem();

    final MultiPlayerMode multiPlayerMode = (MultiPlayerMode) Objects.requireNonNull(this.comboBoxMultiPlayerMode.getSelectedItem());
    this.labelServerHostName.setText(multiPlayerMode.getInterfacesFieldTitle() + ": ");
    this.comboInterfaceName.setEditable(multiPlayerMode.isInterfacesEditable());
    this.comboInterfaceName.setToolTipText(multiPlayerMode.getInterfacesFieldTooltip());
    this.comboInterfaceName.removeAllItems();

    this.comboInterfaceName.removeAllItems();
    this.networkInterfaces.forEach(x -> this.comboInterfaceName.addItem(x.getName()));

    this.networkPanel.doLayout();
    this.networkPanel.invalidate();
    this.networkPanel.repaint();

    this.comboInterfaceName.setSelectedItem(selected);

    this.doLayout();
    this.repaint();
  }

  private static class JShrinkableComboBox<T> extends JComboBox<T> {

    public JShrinkableComboBox(T[] items) {
      super(items);
    }

    @Override
    public Dimension getMinimumSize() {
      return new Dimension(10, super.getMinimumSize().height);
    }

    @Override
    public Dimension getPreferredSize() {
      return this.getMinimumSize();
    }
  }

  private static class JShrinkableLabel extends JLabel {

    public JShrinkableLabel(final String text) {
      super(text);
    }

    @Override
    public Dimension getMinimumSize() {
      return new Dimension(10, super.getMinimumSize().height);
    }

    @Override
    public Dimension getPreferredSize() {
      return this.getMinimumSize();
    }
  }
}
