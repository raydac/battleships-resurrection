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

package com.igormaznitsa.battleships.opponent;

public enum GameEventType {
  EVENT_READY(true),
  EVENT_ARRANGEMENT_COMPLETED(true),
  EVENT_OPPONENT_FIRST_TURN(true),
  EVENT_PAUSE(true),
  EVENT_CONNECTION_ERROR(true, true),
  EVENT_RESUME(true),
  EVENT_SHOT_REGULAR(false),
  EVENT_SHOT_MAIN(false),
  EVENT_DO_TURN(false),
  EVENT_HIT(false),
  EVENT_KILLED(false),
  EVENT_MISS(false),
  EVENT_LOST(false),
  EVENT_FAILURE(true, true),
  EVENT_GAME_ROOM_CLOSED(true, true);

  private final boolean notification;
  private final boolean force;

  GameEventType(final boolean notification) {
    this(notification, false);
  }

  GameEventType(final boolean notification, final boolean force) {
    this.notification = notification;
    this.force = force;
  }

  public boolean isForced() {
    return this.force;
  }

  public boolean isNotification() {
    return this.notification;
  }
}
