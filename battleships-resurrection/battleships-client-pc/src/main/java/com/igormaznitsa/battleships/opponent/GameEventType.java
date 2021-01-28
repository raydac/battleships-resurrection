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

public enum GameEventType {
  EVENT_READY(true),
  EVENT_OPPONENT_STARTS(true),
  EVENT_PAUSE(true),
  EVENT_CONNECTION_ERROR(true),
  EVENT_RESUME(true),
  EVENT_SHOT_REGULAR(false),
  EVENT_SHOT_MAIN(false),
  EVENT_DO_TURN(false),
  EVENT_HIT(false),
  EVENT_KILLED(false),
  EVENT_MISS(false),
  EVENT_LOST(false),
  EVENT_FAILURE(true),
  CLOSING_GAME(true);

  private final boolean serviceEvent;

  GameEventType(final boolean specialEvent) {
    this.serviceEvent = specialEvent;
  }

  public boolean isServiceEvent() {
    return this.serviceEvent;
  }
}
