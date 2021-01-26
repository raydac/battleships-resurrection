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

import static java.util.Arrays.stream;


import com.igormaznitsa.battleships.gui.StartOptions;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GexBattleshipSingleSessionBot implements BsPlayer {

  private static final Logger LOGGER =
      Logger.getLogger(GexBattleshipSingleSessionBot.class.getName());

  private final BlockingQueue<BsGameEvent> queueIn = new ArrayBlockingQueue<>(10);
  private final BlockingQueue<BsGameEvent> queueOut = new ArrayBlockingQueue<>(10);

  private final HttpClient httpClient;

  private final URI uriInput;
  private final URI uriOutput;

  private final AtomicLong packetCounter = new AtomicLong();

  private final String playerId;

  private final AtomicReference<Thread> thread = new AtomicReference<>();

  public GexBattleshipSingleSessionBot(final StartOptions startOptions) {
    this.playerId = UUID.randomUUID().toString();
    this.httpClient = HttpClient.newHttpClient();
    try {
      this.uriInput = new URI(String
          .format("http://%s:%d/getoutstream", startOptions.getHostName().orElse("localhost"),
              startOptions.getHostPort().orElse(30000)));
      this.uriOutput = new URI(String
          .format("http://%s:%d/getinstream", startOptions.getHostName().orElse("localhost"),
              startOptions.getHostPort().orElse(30000)));
    } catch (URISyntaxException ex) {
      LOGGER.log(Level.SEVERE, "URI syntax error", ex);
      throw new IllegalArgumentException("Wrong URI format", ex);
    }
  }

  private void doRun() {
  }

  private void sendDataPacketToServer(final String sessionId, final int... packet)
      throws IOException, InterruptedException {
    final ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
    final DataOutputStream dataStream = new DataOutputStream(bufferStream);

    for (final int d : packet) {
      dataStream.writeInt(d);
    }
    dataStream.write(stream(packet).sum());

    final byte[] data = null;
    final HttpRequest request = HttpRequest.newBuilder(this.uriOutput)
        .POST(HttpRequest.BodyPublishers.ofByteArray(data))
        .setHeader("playerID", this.playerId)
        .setHeader("sessionID", sessionId)
        .setHeader("pn", Long.toString(this.packetCounter.get()))
        .build();
    final HttpResponse<Void> result =
        this.httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    this.packetCounter.incrementAndGet();
  }


  @Override
  public Optional<BsGameEvent> pollGameEvent(Duration duration) throws InterruptedException {
    return Optional.empty();
  }

  @Override
  public BsPlayer startBot() {
    final Thread thread = new Thread(this::doRun, "bs-gex-bot-" + this.playerId);
    thread.setDaemon(true);
    if (this.thread.compareAndSet(null, thread)) {
      thread.start();
    } else {
      throw new IllegalStateException("Already started");
    }
    return this;
  }

  @Override
  public void pushGameEvent(BsGameEvent event) {
  }

  @Override
  public void disposeBot() {
    final Thread foundThread = this.thread.getAndSet(null);
    if (foundThread != null) {
      foundThread.interrupt();
      try {
        foundThread.join();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private enum ProtocolEvent {
    UNKNOWN(-1),
    NONE(1),
    JOIN_SESSION(2),
    NEW_SESSION(3),
    EXIT(4),
    OPPONENT_LOST(5),
    SESSION_REMOVE(6),
    GAME_MOVE(7),
    GAME_RESULT(8),
    OPPONENT_JOIN(9),
    PAUSE(10),
    WAIT_MOVE(11),
    OK(12),
    WAIT_PACKET(13),
    LOCK_PACKET(14),
    SERVER_OVERLOADED(15),
    SERVER_GAME(16),
    SERVER_PAUSE(17),
    SERVER_START(18),
    NETWORK_ERROR(19);

    private final int code;

    ProtocolEvent(final int code) {
      this.code = code;
    }

    static ProtocolEvent findForCode(final int code) {
      return stream(ProtocolEvent.values())
          .filter(e -> e.code == code)
          .findFirst().orElse(NONE);
    }

  }
}
