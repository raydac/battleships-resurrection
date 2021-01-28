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

package com.igormaznitsa.battleships.opponent;


import java.util.Objects;
import java.util.UUID;

public final class BsGameEvent {
  private final UUID uuid = UUID.randomUUID();
  private final GameEventType gameEventType;
  private final int x;
  private final int y;
  private final long timestamp;

  public BsGameEvent(final GameEventType event, final int x, final int y) {
    this.gameEventType = Objects.requireNonNull(event);
    this.x = x;
    this.y = y;
    this.timestamp = System.currentTimeMillis();
  }

  public UUID getUuid() {
    return this.uuid;
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  public GameEventType getType() {
    return this.gameEventType;
  }

  public int getX() {
    return this.x;
  }

  public int getY() {
    return this.y;
  }

  @Override
  public String toString() {
    return String.format("BsGameEvent(type=%s,x=%d,y=%d,time=%d)", this.gameEventType, this.x,
        this.y, this.timestamp);
  }

}
