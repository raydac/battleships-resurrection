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
  EVENT_READY,
  EVENT_OPPONENT_STARTS,
  EVENT_PAUSE,
  EVENT_CONNECTION_ERROR,
  EVENT_RESUME,
  EVENT_SHOT_REGULARSHIP,
  EVENT_SHOT_MAINSHIP,
  EVENT_DO_TURN,
  EVENT_HIT,
  EVENT_KILLED,
  EVENT_MISS,
  EVENT_LOST,
  EVENT_FAILURE
}
