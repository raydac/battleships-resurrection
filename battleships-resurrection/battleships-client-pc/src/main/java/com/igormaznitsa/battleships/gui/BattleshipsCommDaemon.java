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

package com.igormaznitsa.battleships.gui;

import com.igormaznitsa.battleships.opponent.BattleshipsPlayer;
import com.igormaznitsa.battleships.opponent.BsGameEvent;
import com.igormaznitsa.battleships.opponent.GameEventType;
import com.igormaznitsa.battleships.utils.Utils;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Logger;

final class BattleshipsCommDaemon {
  private final Logger LOGGER = Logger.getLogger(BattleshipsCommDaemon.class.getName());

  private final Thread thread;

  private final BattleshipsPlayer playerA;
  private final BattleshipsPlayer playerB;

  private BsGameEvent readyEventFromPlayerA;
  private BsGameEvent readyEventFromPlayerB;

  BattleshipsCommDaemon(final BattleshipsPlayer playerA, final BattleshipsPlayer playerB) {
    this.playerA = playerA;
    this.playerB = playerB;
    this.thread = new Thread(this::doRun, "battleships-comm-daemon");
    this.thread.setDaemon(true);
  }

  public void start() {
    this.thread.start();
  }

  public void dispose() {
    this.thread.interrupt();
    try {
      this.thread.join();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private void doRun() {
    LOGGER.info("Comm-Daemon started");
    while (!Thread.currentThread().isInterrupted()) {
      try {
        Optional<BsGameEvent> event = this.playerA.pollGameEvent(Duration.ofMillis(100));
        event.ifPresent(e -> {
          LOGGER.info("Message from A: " + e);
          if (e.getType() == GameEventType.EVENT_READY) {
            this.readyEventFromPlayerA = e;
            this.onReadyEvent();
          } else {
            this.playerB.pushGameEvent(e);
          }
        });
        event = this.playerB.pollGameEvent(Duration.ofMillis(100));
        event.ifPresent(e -> {
          LOGGER.info("Message from B: " + e);
          if (e.getType() == GameEventType.EVENT_READY) {
            this.readyEventFromPlayerB = e;
            this.onReadyEvent();
          } else {
            this.playerA.pushGameEvent(e);
          }
        });
      } catch (InterruptedException ex) {
        LOGGER.info("Comm-daemon has detected interruption");
        Thread.currentThread().interrupt();
      }
    }
    LOGGER.info("Comm-Daemon stopped");
  }

  private void onReadyEvent() {
    if (this.readyEventFromPlayerA != null && this.readyEventFromPlayerB != null) {
      final boolean firstTurnA = Utils.RND.nextBoolean();
      LOGGER.info("Both ready signal presented, first turn " + (firstTurnA ? "A" : "B"));
      if (firstTurnA) {
        this.playerB.pushGameEvent(new BsGameEvent(GameEventType.EVENT_OPPONENT_STARTS, 0, 0));
      } else {
        this.playerA.pushGameEvent(new BsGameEvent(GameEventType.EVENT_OPPONENT_STARTS, 0, 0));
      }
    }
  }

}
