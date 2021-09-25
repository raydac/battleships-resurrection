package com.igormaznitsa.battleships.gui;

import java.util.Arrays;

public enum MultiPlayerMode {
  GFX_PLAYROOM("GFX play-room", "Server host name", "Select or enter address of GFX play-room server", true),
  LAN_P2P("LAN Serverless", "Network interface", "Select LAN interface, both UDP and TCP ports should be open", false);

  private final String text;
  private final boolean interfacesEditable;
  private final String interfacesFieldTitle;
  private final String interfacesFieldTooltip;

  MultiPlayerMode(
          final String text,
          final String interfacesFieldTitle,
          final String interfacesFieldTooltip,
          final boolean interfacesEditable
  ) {
    this.text = text;
    this.interfacesFieldTooltip = interfacesFieldTooltip;
    this.interfacesEditable = interfacesEditable;
    this.interfacesFieldTitle = interfacesFieldTitle;
  }

  public static MultiPlayerMode safeValueOf(final String name, final MultiPlayerMode defaultValue) {
    return Arrays.stream(MultiPlayerMode.values())
            .filter(x -> x.name().equalsIgnoreCase(name))
            .findFirst()
            .orElse(defaultValue);
  }

  public String getInterfacesFieldTooltip() {
    return this.interfacesFieldTooltip;
  }

  public boolean isInterfacesEditable() {
    return this.interfacesEditable;
  }

  public String getInterfacesFieldTitle() {
    return this.interfacesFieldTitle;
  }

  @Override
  public String toString() {
    return this.text;
  }
}
