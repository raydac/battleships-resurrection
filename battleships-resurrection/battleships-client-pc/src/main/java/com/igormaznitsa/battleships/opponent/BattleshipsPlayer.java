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

import java.time.Duration;
import java.util.Optional;

public interface BattleshipsPlayer {
  Optional<BsGameEvent> pollGameEvent(final Duration duration) throws InterruptedException;

  void pushGameEvent(BsGameEvent event);

  BattleshipsPlayer startPlayer();

  void disposePlayer();
}