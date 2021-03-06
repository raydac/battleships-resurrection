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
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.util.Optional;
import java.util.OptionalInt;

public class StartOptions {
  private final Optional<GraphicsConfiguration> graphicsConfiguration;
  private final Optional<String> gameTitle;
  private final Optional<Image> gameIcon;
  private final boolean multiPlayer;
  private final boolean fullScreen;
  private final boolean withSound;
  private final boolean useOldGfxClient;
  private final Optional<String> hostName;
  private final OptionalInt hostPort;

  private StartOptions(final Optional<GraphicsConfiguration> graphicsConfiguration,
                       final Optional<String> gameTitle,
                       final Optional<Image> gameIcon,
                       final boolean multiPlayer,
                       final boolean fullScreen,
                       final boolean withSound,
                       final Optional<String> hostName,
                       final OptionalInt hostPort,
                       final boolean useOldGfxClient
  ) {
    this.graphicsConfiguration = graphicsConfiguration;
    this.useOldGfxClient = useOldGfxClient;
    this.gameTitle = gameTitle;
    this.withSound = withSound;
    this.gameIcon = gameIcon;
    this.multiPlayer = multiPlayer;
    this.fullScreen = fullScreen;
    this.hostName = hostName;
    this.hostPort = hostPort;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public boolean isWithSound() {
    return this.withSound;
  }

  public Optional<GraphicsConfiguration> getGraphicsConfiguration() {
    return this.graphicsConfiguration;
  }

  public Optional<String> getGameTitle() {
    return this.gameTitle;
  }

  public Optional<Image> getGameIcon() {
    return this.gameIcon;
  }

  public boolean isMultiPlayer() {
    return this.multiPlayer;
  }

  public boolean isFullScreen() {
    return this.fullScreen;
  }

  public Optional<String> getHostName() {
    return this.hostName;
  }

  public OptionalInt getHostPort() {
    return this.hostPort;
  }

  public boolean isUseOldGfxClient() {
    return this.useOldGfxClient;
  }

  public static class Builder {
    private Optional<GraphicsConfiguration> graphicsConfiguration =
        Optional.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
            .getDefaultConfiguration());
    private Optional<String> gameTitle = Optional.of("BattleShips");
    private Optional<Image> gameIcon =
        Optional.of(GfxUtils.loadResImage("icon3.png"));
    private boolean multiPlayer = false;
    private boolean fullScreen = false;
    private boolean withSound = true;
    private boolean useOldGfxClient = true;
    private Optional<String> hostName = Optional.of("localhost");
    private OptionalInt hostPort = OptionalInt.of(30000);

    public Builder setWithSound(final boolean value) {
      this.withSound = value;
      return this;
    }

    public Builder setHostPort(final int port) {
      this.hostPort = port < 0 ? OptionalInt.empty() : OptionalInt.of(port);
      return this;
    }

    public Builder setUseOldGfxClient(final boolean value) {
      this.useOldGfxClient = value;
      return this;
    }

    public Builder setHostName(final String hostName) {
      this.hostName = Optional.ofNullable(hostName);
      return this;
    }

    public Builder setFullScreen(final boolean value) {
      this.fullScreen = value;
      return this;
    }

    public Builder setMultiPlayer(final boolean value) {
      this.multiPlayer = value;
      return this;
    }

    public Builder setGameIcon(final Image value) {
      this.gameIcon = Optional.ofNullable(value);
      return this;
    }

    public Builder setGameTitle(final String value) {
      this.gameTitle = Optional.ofNullable(value);
      return this;
    }

    public Builder setGraphicsConfiguration(final GraphicsConfiguration value) {
      this.graphicsConfiguration = Optional.of(value);
      return this;
    }

    public StartOptions build() {
      return new StartOptions(this.graphicsConfiguration,
          this.gameTitle,
          this.gameIcon,
          this.multiPlayer,
          this.fullScreen,
          this.withSound,
          this.hostName,
          this.hostPort,
          this.useOldGfxClient);
    }

  }

}
