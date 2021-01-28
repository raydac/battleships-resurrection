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

import static com.igormaznitsa.battleships.utils.Utils.closeQuietly;
import static java.util.Arrays.stream;


import com.igormaznitsa.battleships.gui.StartOptions;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OldGexBattleshipSingleSessionBot implements BattleshipsPlayer {

  private static final Logger LOGGER =
      Logger.getLogger(OldGexBattleshipSingleSessionBot.class.getName());

  private final BlockingQueue<BsGameEvent> queueIn = new ArrayBlockingQueue<>(10);
  private final BlockingQueue<BsGameEvent> queueOut = new ArrayBlockingQueue<>(10);

  private final HttpClient httpClient;

  private final URI uriInput;
  private final URI uriOutput;

  private final AtomicLong packetCounter = new AtomicLong();

  private static final int PACKET_HEADER = 0xFFCAFE00;
  private final int playerId;
  private final AtomicReference<Optional<String>> sessionId =
      new AtomicReference<>(Optional.empty());
  private final AtomicReference<Thread> threadInput = new AtomicReference<>();
  private final AtomicReference<Thread> threadOutput = new AtomicReference<>();
  private final AtomicReference<InputStream> openedInputStream = new AtomicReference<>();

  public OldGexBattleshipSingleSessionBot(final StartOptions startOptions) {
    final UUID uuid = UUID.randomUUID();
    this.playerId =
        (int) (0x7FFFFFFFL & uuid.getLeastSignificantBits() ^ (uuid.getMostSignificantBits() * 31));
    LOGGER.info("Generated player ID: " + this.playerId);
    this.httpClient = HttpClient.newHttpClient();
    try {
      //todo replace by values from config
      final String host = "localhost";
      final int port = 30000;
      LOGGER.info("Game server address: " + host + ':' + port);
      this.uriInput = new URI(String
          .format("http://%s:%d/getoutstream", host, port));
      this.uriOutput = new URI(String
          .format("http://%s:%d/getinstream", host, port));
    } catch (URISyntaxException ex) {
      LOGGER.log(Level.SEVERE, "URI syntax error", ex);
      throw new IllegalArgumentException("Wrong URI format", ex);
    }
  }

  private void doRunIn() {
    try {
      LOGGER.info("Processing of incoming packets started");
      while (!Thread.currentThread().isInterrupted()) {
        final HttpURLConnection httpURLConnection;
        try {
          httpURLConnection = (HttpURLConnection) this.uriInput.toURL().openConnection();
          httpURLConnection.setConnectTimeout(3600000);
          httpURLConnection.setInstanceFollowRedirects(false);
          httpURLConnection.setUseCaches(false);
          httpURLConnection.setRequestMethod("POST");
          httpURLConnection.setRequestProperty("Content-Type", "application/octet-stream");
          httpURLConnection.setRequestProperty("User-Agent", "battleships-gex-client");
          httpURLConnection.setDoInput(true);
          httpURLConnection.setRequestProperty("playerID", Integer.toString(this.playerId));
          this.sessionId.get().ifPresent(s -> httpURLConnection.setRequestProperty("sessionID", s));
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, "Can't prepare listening connection", ex);
          placeEventIntoInQueue(new BsGameEvent(GameEventType.EVENT_FAILURE, 0, 0));
          return;
        }

        try {
          httpURLConnection.connect();
        } catch (IOException ex) {
          LOGGER.warning("Can't open connection: " + ex.getMessage());
          try {
            Thread.sleep(3000);
          } catch (InterruptedException ix) {
            Thread.currentThread().interrupt();
          }
          continue;
        }

        DataInputStream inputStream = null;
        try {
          inputStream = new DataInputStream(httpURLConnection.getInputStream());
          this.openedInputStream.set(inputStream);

          final int[] packetBuffer = new int[5];
          int bufferPointer = -10;

          while (!Thread.currentThread().isInterrupted()) {
            try {
              final int data = inputStream.readInt();
              if (bufferPointer >= 0) {
                packetBuffer[bufferPointer++] = data;
                if (bufferPointer == packetBuffer.length) {
                  final int checkSum = Arrays.stream(packetBuffer, 0, 4).sum();
                  if (checkSum != packetBuffer[4]) {
                    packetBuffer[0] = ProtocolEvent.NETWORK_ERROR.code;
                  }
                  this.onIncomingPacket(packetBuffer);
                  bufferPointer = -1;
                }
              } else {
                if (data == PACKET_HEADER) {
                  bufferPointer = 0;
                } else {
                  bufferPointer = -1;
                }
              }
            } catch (IOException ex) {
              break;
            }
          }
        } catch (IOException ex) {
          closeQuietly(inputStream);
          this.openedInputStream.set(null);
        }
      }
    } finally {
      LOGGER.info("Processing of incoming packets completed");
    }
  }

  private void onIncomingPacket(final int[] packet) {
    final ProtocolEvent event = ProtocolEvent.findForCode(packet[0]);
    LOGGER.info("Detected incoming event " + event + ": " + Arrays.toString(packet));
    switch (event) {
      case JOIN_SESSION:
      case NEW_SESSION: {
        final String session = Integer.toString(packet[1]);
        this.sessionId.set(Optional.of(session));
        final boolean myFirstTurn = packet[2] != 0;
        LOGGER.info((event == ProtocolEvent.NEW_SESSION ? "Created session " : "Joined session ") +
            session + ", first turn is " + (myFirstTurn ? "MINE" : "OPPONENT"));
      }
      break;
      case EXIT:
      case OPPONENT_LOST: {
        // ??? do nothing like in the mobile version ???
      }
      break;
      case SESSION_REMOVE: {
        // game session has been removed
      }
      case GAME_MOVE: {
        // fire X,Y,0
      }
      break;
      case GAME_RESULT: {
        // fire result A,0,0
        // A is HIT=3, KILL=4, MISS=5
      }
      break;
      case OPPONENT_JOIN:
      case PAUSE: {
        // opponent has turned on pause mode
      }
      break;
      case SERVER_GAME: {
        // opponent in game
      }
      break;
      case SERVER_PAUSE: {
        // notification that server paused
      }
      break;
      case SERVER_RESUMED: {
        // notification that server work resumed
      }
      break;
      case NETWORK_ERROR: {

      }
      break;
    }
    LOGGER.info("Incoming packet: " + Arrays.toString(packet));
  }

  private void placeEventIntoInQueue(final BsGameEvent event) {
    try {
      if (!this.queueIn.offer(event, 10, TimeUnit.SECONDS)) {
        LOGGER.severe("Can't place event into internal queue for long time: " + event);
        System.exit(14);
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private void doRunOut() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        final BsGameEvent event = this.queueOut.take();

      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
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
        .setHeader("playerID", Integer.toString(this.playerId))
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
  public BattleshipsPlayer startPlayer() {
    final Thread threadIn = new Thread(this::doRunIn, "bs-gex-bot-in-" + this.playerId);
    threadIn.setDaemon(true);
    if (!this.threadInput.compareAndSet(null, threadIn)) {
      throw new IllegalStateException("Found already created thread in");
    }

    final Thread threadOut = new Thread(this::doRunOut, "bs-gex-bot-out-" + this.playerId);
    threadIn.setDaemon(true);
    if (!this.threadOutput.compareAndSet(null, threadOut)) {
      throw new IllegalStateException("Found alreadt created thread out");
    }

    this.threadInput.get().start();
    this.threadOutput.get().start();

    return this;
  }

  @Override
  public void pushGameEvent(final BsGameEvent event) {

  }

  @Override
  public String getId() {
    return "battleships-old-gex-network-client";
  }

  @Override
  public void disposePlayer() {
    closeQuietly(this.openedInputStream.get());
    Thread foundThread = this.threadInput.getAndSet(null);
    if (foundThread != null) {
      foundThread.interrupt();
      try {
        foundThread.join();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
    foundThread = this.threadOutput.getAndSet(null);
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
    SERVER_RESUMED(18),
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
