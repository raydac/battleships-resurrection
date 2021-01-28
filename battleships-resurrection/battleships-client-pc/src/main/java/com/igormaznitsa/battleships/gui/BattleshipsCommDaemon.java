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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

final class BattleshipsCommDaemon {
  private final Logger LOGGER = Logger.getLogger(BattleshipsCommDaemon.class.getName());

  private final Thread thread;

  private final BattleshipsPlayer playerA;
  private final BattleshipsPlayer playerB;

  private final Map<String, Map<String, String>> playerSessionRecords = new HashMap<>();

  BattleshipsCommDaemon(final BattleshipsPlayer playerA, final BattleshipsPlayer playerB) {
    this.playerSessionRecords.put(playerA.getId(), new HashMap<>());
    this.playerSessionRecords.put(playerB.getId(), new HashMap<>());
    this.playerA = playerA;
    this.playerB = playerB;
    this.thread = new Thread(this::doRun, "battleships-comm-daemon");
    this.thread.setDaemon(true);
  }

  public void start() {
    this.thread.start();
  }

  public void dispose() {
    this.playerA.pushGameEvent(new BsGameEvent(GameEventType.CLOSING_GAME, 0, 0));
    this.thread.interrupt();
    try {
      this.thread.join();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private void onServiceEvent(final BattleshipsPlayer source, final BsGameEvent event) {
    final BattleshipsPlayer opponent = source == this.playerA ? this.playerB : this.playerA;
    switch (event.getType()) {
      case EVENT_READY: {
        this.playerSessionRecords.get(source.getId()).put("ready", "ok");
        this.checkPlayersReady();
      }
      break;
      case EVENT_FAILURE:
      case EVENT_CONNECTION_ERROR: {
        opponent.pushGameEvent(new BsGameEvent(GameEventType.EVENT_FAILURE, 0, 0));
      }
      break;
      case CLOSING_GAME: {
        LOGGER.info("Player '" + source.getId() + "' is leaving game room");
        opponent.pushGameEvent(event);
      }
      break;
      case EVENT_RESUME:
      case EVENT_PAUSE: {
        opponent.pushGameEvent(event);
      }
      break;
      default: {
        LOGGER.severe(String.format("Got unexpected event '%s' from '%s'", event, source.getId()));
        opponent.pushGameEvent(new BsGameEvent(GameEventType.EVENT_FAILURE, 0, 0));
        source.pushGameEvent(new BsGameEvent(GameEventType.EVENT_FAILURE, 0, 0));
      }
      break;
    }
  }

  private void doRun() {
    LOGGER.info("Comm-Daemon started");
    while (!Thread.currentThread().isInterrupted()) {
      try {
        Optional<BsGameEvent> event = this.playerA.pollGameEvent(Duration.ofMillis(100));
        event.ifPresent(e -> {
          LOGGER.info("Message from A: " + e);
          if (e.getType().isServiceEvent()) {
            this.onServiceEvent(this.playerA, e);
          } else {
            this.playerB.pushGameEvent(e);
          }
        });
        event = this.playerB.pollGameEvent(Duration.ofMillis(100));
        event.ifPresent(e -> {
          LOGGER.info("Message from B: " + e);
          if (e.getType().isServiceEvent()) {
            this.onServiceEvent(this.playerB, e);
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

  private void checkPlayersReady() {
    if ("ok".equals(this.playerSessionRecords.get(this.playerA.getId()).getOrDefault("ready", "no"))
        && "ok".equals(
        this.playerSessionRecords.get(this.playerB.getId()).getOrDefault("ready", "no"))) {
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
