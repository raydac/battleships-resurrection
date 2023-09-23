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

import com.igormaznitsa.battleships.gui.panels.GameField;
import com.igormaznitsa.battleships.utils.Utils;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.igormaznitsa.battleships.utils.Utils.closeQuietly;
import static java.util.Arrays.stream;

public class OldGfxBattleshipSingleSessionBot implements BattleshipsPlayer {

  private static final int PACKET_HEADER = 0xFFCAFE00;

  private static final Logger LOGGER =
          Logger.getLogger(OldGfxBattleshipSingleSessionBot.class.getName());
  private static final int MOVE_MISS = 5;
  private static final int MOVE_HIT = 3;
  private static final int MOVE_KILLED = 4;
  private volatile boolean myFirstTurn;
  private final BlockingQueue<BsGameEvent> queueIn = new ArrayBlockingQueue<>(10);
  private final BlockingQueue<BsGameEvent> queueOut = new ArrayBlockingQueue<>(10);
  private final String id;
  private final URI uriInput;
  private final URI uriOutput;
  private final URI uriTest;
  private final AtomicLong packetCounter = new AtomicLong();
  private final int playerId;
  private final AtomicReference<Optional<String>> sessionId =
          new AtomicReference<>(Optional.empty());
  private final AtomicReference<Thread> threadInput = new AtomicReference<>();
  private final AtomicReference<Thread> threadOutput = new AtomicReference<>();
  private static final Duration DELAY_BETWEEN_RECONNECT_ATTEMPT = Duration.ofSeconds(3);
  private volatile BsGameEvent lastShot = null;
  private volatile boolean readyAlreadySent = false;

  private final GameField gameField = new GameField();

  private final AtomicBoolean sessionReady = new AtomicBoolean(false);

  private final int[] enemyShipNumber = new int[]{4, 3, 2, 1};

  @Override
  public boolean isRemote() {
    return true;
  }

  public OldGfxBattleshipSingleSessionBot(final InetAddress address, final int port) {
    this.id = address.getHostName() + ':' + port;

    final UUID uuid = UUID.randomUUID();
    this.playerId =
            0x7FFFFFFF & (int) (uuid.getLeastSignificantBits() ^ (uuid.getMostSignificantBits() * 31));
    LOGGER.info("Generated player ID: " + this.playerId);
    try {
      final String host = address.getHostAddress();
      LOGGER.info("Game server address: " + host + ':' + port);
      this.uriInput = new URI(String
              .format("http://%s:%d/getoutstream", host, port));
      this.uriOutput = new URI(String
              .format("http://%s:%d/getinstream", host, port));
      this.uriTest = new URI(String
              .format("http://%s:%d/test", host, port));
    } catch (URISyntaxException ex) {
      LOGGER.log(Level.SEVERE, "URI syntax error", ex);
      throw new IllegalArgumentException("Wrong URI format", ex);
    }
  }

  private final AtomicReference<HttpURLConnection> openedHttpConnection = new AtomicReference<>();

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private static HttpURLConnection prepareConnection(
          final String method,
          final URI uri,
          final int playerId,
          final Optional<String> sessionId,
          final boolean input,
          final boolean output
  ) throws IOException {
    final HttpURLConnection connection;
    try {
      connection = (HttpURLConnection) uri.toURL().openConnection();
      connection.setConnectTimeout(3600000);
      connection.setInstanceFollowRedirects(false);
      connection.setUseCaches(false);
      connection.setRequestMethod(method);
      connection.setRequestProperty("Content-Type", "application/octet-stream");
      connection.setRequestProperty("User-Agent", "battleships-gex-client");
      connection.setDoInput(input);
      connection.setDoOutput(output);
      connection.setRequestProperty("playerID", Integer.toString(playerId));
      sessionId.ifPresent(s -> connection.setRequestProperty("sessionID", s));
      return connection;
    } catch (IOException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public boolean isReadyForGame() {
    return this.sessionReady.get();
  }

  public boolean doTestCall() {
    try {
      final HttpURLConnection connection =
              prepareConnection("GET", this.uriTest, this.playerId, this.sessionId.get(), true, false);
      connection.connect();
      connection.getInputStream().close();
      connection.disconnect();
      return true;
    } catch (IOException ex) {
      return false;
    }
  }

  @SuppressWarnings("BusyWait")
  private void doRunIn() {
    try {
      LOGGER.info("Processing of incoming network packets started");
      while (!Thread.currentThread().isInterrupted()) {
        final HttpURLConnection httpURLConnection;
        try {
          httpURLConnection =
                  prepareConnection("POST", this.uriInput, this.playerId, this.sessionId.get(), true,
                          false);
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
            Thread.sleep(DELAY_BETWEEN_RECONNECT_ATTEMPT.toMillis());
          } catch (InterruptedException ix) {
            Thread.currentThread().interrupt();
          }
          continue;
        }

        this.openedHttpConnection.set(httpURLConnection);
        try (final DataInputStream inputStream = new DataInputStream(httpURLConnection.getInputStream())) {
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
                  if (packetBuffer[0] == ProtocolEvent.NONE.code) {
                    try {
                      Thread.sleep(10);
                    } catch (InterruptedException ex) {
                      Thread.currentThread().interrupt();
                    }
                  } else {
                    this.onIncomingPacket(packetBuffer);
                  }
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
          LOGGER.log(Level.SEVERE, "io error during loop", ex);
        } finally {
          closeQuietly(this.openedHttpConnection.getAndSet(null));
        }
      }
    } finally {
      LOGGER.info("Processing of incoming packets completed");
    }
  }

  private void onIncomingPacket(final int[] packet) {
    final ProtocolEvent event = ProtocolEvent.findForCode(packet[0]);
    LOGGER.info("Incoming event " + event + " " + Arrays.toString(packet));
    switch (event) {
      case JOIN_SESSION:
      case NEW_SESSION: {
        final String session = Integer.toString(packet[1]);
        this.sessionId.set(Optional.of(session));
        this.sessionReady.set(event == ProtocolEvent.JOIN_SESSION);
        this.myFirstTurn = packet[2] != 0;
        LOGGER.info((event == ProtocolEvent.NEW_SESSION ? "Created session " : "Joined session ") +
                session + ", first turn is " + (myFirstTurn ? "MINE" : "OPPONENT'S"));
      }
      break;
      case OPPONENT_LOST:
      case EXIT:
      case SESSION_REMOVE: {
        this.sessionReady.set(false);
        this.sessionId.set(Optional.empty());
        this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_GAME_ROOM_CLOSED, 0, 0));
      }
      break;
      case GAME_MOVE: {
        final int cellX = packet[1];
        final int cellY = packet[2];
        if (cellX < 0 || cellX > 9 || cellY < 0 || cellY > 9) {
          LOGGER.log(Level.SEVERE, "Unexpected X Y in incoming move: " + cellX + ", " + cellY);
          this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_FAILURE, 0, 0));
        } else {
          final int numberAliveShips = Arrays.stream(this.enemyShipNumber).sum();
          final boolean mainShipPresented = this.enemyShipNumber[3] > 0;

          final GameEventType shot;

          if (mainShipPresented && Utils.RND.nextInt(numberAliveShips) == 0) {
            shot = GameEventType.EVENT_SHOT_MAIN;
          } else {
            shot = GameEventType.EVENT_SHOT_REGULAR;
          }

          this.pushIntoOutput(
                  new BsGameEvent(shot, cellX, cellY));
        }
      }
      break;
      case GAME_RESULT: {
        final BsGameEvent foundLastShot = this.lastShot;
        if (foundLastShot == null) {
          LOGGER.log(Level.SEVERE, "Got result but without shot");
          this.placeEventIntoInQueue(
                  new BsGameEvent(GameEventType.EVENT_CONNECTION_ERROR, -1, -1));
        } else {
          switch (packet[1]) {
            case 3: { // HIT
              this.gameField
                      .setState(foundLastShot.getX(), foundLastShot.getY(), GameField.CellState.HIT);
              this.pushIntoOutput(
                      new BsGameEvent(GameEventType.EVENT_HIT, foundLastShot.getX(),
                              foundLastShot.getY()));
              this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_DO_TURN, 0, 0));
            }
            break;
            case 4: { // KILL
              this.gameField
                      .setState(foundLastShot.getX(), foundLastShot.getY(), GameField.CellState.KILL);
              final List<Point> shipPoints =
                      this.gameField
                              .tryRemoveShipAt(new Point(foundLastShot.getX(), foundLastShot.getY()));
              if (shipPoints.isEmpty() || shipPoints.size() > 4 ||
                      this.enemyShipNumber[shipPoints.size() - 1] == 0) {
                LOGGER.severe("Detected unexpected state of enemy map");
                this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_FAILURE, 0, 0));
              } else {
                LOGGER.info("Detected kill of ship: " + shipPoints.size());
                this.enemyShipNumber[shipPoints.size() - 1]--;
                if (Arrays.stream(this.enemyShipNumber).allMatch(x -> x == 0)) {
                  LOGGER.info("Detected all enemy ship destruction, ending game");
                  this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_LOST,
                          foundLastShot.getX(), foundLastShot.getY()));
                  try {
                    this.sendDataPacketToServer(ProtocolEvent.EXIT, 0, 0, 0);
                  } catch (IOException ex) {
                    LOGGER.severe("Can't send exit signal for error: " + ex.getMessage());
                  } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                  } finally {
                    this.sessionId.set(Optional.empty());
                  }
                } else {
                  this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_KILLED,
                          foundLastShot.getX(), foundLastShot.getY()));
                  this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_DO_TURN, 0, 0));
                }
              }
            }
            break;
            case 5: { // MISS
              this.gameField
                      .setState(foundLastShot.getX(), foundLastShot.getY(), GameField.CellState.MISS);
              this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_MISS,
                      foundLastShot.getX(), foundLastShot.getY()));
            }
            break;
            default: {
              LOGGER.log(Level.SEVERE, "Unexpected turn result: " + packet[1]);
              this.placeEventIntoInQueue(
                      new BsGameEvent(GameEventType.EVENT_CONNECTION_ERROR, 0, 0));
            }
            break;
          }
        }
      }
      break;
      case OPPONENT_JOIN: {
        this.sessionReady.set(true);
      }
      break;
      case IN_GAME: {
        if (this.readyAlreadySent) {
          // player resumed
          this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_RESUME, 0, 0));
        } else {
          // game has started
          this.readyAlreadySent = true;
          LOGGER.info("Ready for game session");
          this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_READY, Utils.RND.nextInt(), Utils.RND.nextInt()));
        }
      }
      break;
      case PAUSE:
      case SERVER_PAUSE: {
        // notification that server paused
        this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_PAUSE, 0, 0));
      }
      break;
      case SERVER_RESUMED: {
        // notification that server work resumed
        this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_RESUME, 0, 0));
      }
      break;
      case NETWORK_ERROR: {
        this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_CONNECTION_ERROR, 0, 0));
      }
      break;
      default: {
        LOGGER.severe("Unexpected incoming event: " + event + " [" + Arrays.toString(packet) + ']');
        this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_CONNECTION_ERROR, 0, 0));
      }
      break;
    }
  }

  private void pushIntoOutput(final BsGameEvent event) {
    if (!this.queueOut.offer(event)) {
      throw new IllegalStateException("Can't queue out-coming event: " + event);
    }
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
        final BsGameEvent event = this.queueIn.take();

        ProtocolEvent sendEvent = ProtocolEvent.NONE;
        int arg1 = 0;
        int arg2 = 0;
        int arg3 = 0;

        switch (event.getType()) {
          case EVENT_OPPONENT_FIRST_TURN: {
            this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_DO_TURN, 0, 0));
          }
          break;
          case EVENT_READY:
          case EVENT_RESUME:
          case EVENT_ARRANGEMENT_COMPLETED:
          case EVENT_DO_TURN: {
            sendEvent = ProtocolEvent.IN_GAME;
          }
          break;
          case EVENT_FAILURE: {
            sendEvent = ProtocolEvent.NETWORK_ERROR;
          }
          break;
          case EVENT_PAUSE: {
            sendEvent = ProtocolEvent.PAUSE;
          }
          break;
          case EVENT_SHOT_MAIN:
          case EVENT_SHOT_REGULAR: {
            this.lastShot = event;
            sendEvent = ProtocolEvent.GAME_MOVE;
            arg1 = event.getX();
            arg2 = event.getY();
          }
          break;
          case EVENT_LOST:
          case EVENT_KILLED:
          case EVENT_MISS:
          case EVENT_HIT: {
            sendEvent = ProtocolEvent.GAME_RESULT;
            switch (event.getType()) {
              case EVENT_LOST:
              case EVENT_KILLED: {
                arg1 = MOVE_KILLED;
              }
              break;
              case EVENT_MISS: {
                this.pushIntoOutput(new BsGameEvent(GameEventType.EVENT_DO_TURN, 0, 0));
                arg1 = MOVE_MISS;
              }
              break;
              case EVENT_HIT:
                arg1 = MOVE_HIT;
                break;
            }
          }
          break;
          default: {
            LOGGER.log(Level.SEVERE, "Unexpected event: " + event);
            sendEvent = ProtocolEvent.EXIT;
            this.placeEventIntoInQueue(new BsGameEvent(GameEventType.EVENT_FAILURE, 0, 0));
          }
          break;
        }
        if (sendEvent != ProtocolEvent.NONE) {
          LOGGER.info("Sending event " + sendEvent + " " + arg1 + "," + arg2 + "," + arg3);
          try {
            this.sendDataPacketToServer(sendEvent, arg1, arg2, arg3);
            LOGGER.info("Event " + sendEvent + " has been sent");
          } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Can't send packet to server", ex);
            this.placeEventIntoInQueue(new BsGameEvent(GameEventType.EVENT_FAILURE, 0, 0));
          }
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void sendDataPacketToServer(final ProtocolEvent event, final int arg1, final int arg2,
                                      final int arg3)
          throws IOException, InterruptedException {

    final ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
    final DataOutputStream dataStream = new DataOutputStream(bufferStream);

    dataStream.writeInt(PACKET_HEADER);
    dataStream.writeInt(event.code);
    dataStream.writeInt(arg1);
    dataStream.writeInt(arg2);
    dataStream.writeInt(arg3);
    dataStream.writeInt(event.code + arg1 + arg2 + arg3);
    dataStream.flush();
    dataStream.close();

    final byte[] data = bufferStream.toByteArray();

    final HttpURLConnection connection =
            prepareConnection("POST", this.uriOutput, this.playerId, this.sessionId.get(), true, true);
    connection.setRequestProperty("pn", Long.toString(this.packetCounter.get()));

    connection.connect();
    try (final OutputStream outputStream = connection.getOutputStream()) {
      outputStream.write(data);
      outputStream.flush();
    }
    final int responseCode = connection.getResponseCode();
    LOGGER.info("Opened connection, got response code: " + responseCode);
    if (responseCode != 200) {
      LOGGER.log(Level.SEVERE,
              "Can't send package to server, response status " + responseCode + ": " +
                      Arrays.toString(data));
      throw new IOException("Error response code: " + responseCode);
    }
    connection.disconnect();
    LOGGER.info("Packet successfully sent");
    this.packetCounter.incrementAndGet();
  }

  @Override
  public Optional<BsGameEvent> pollGameEvent(final Duration duration) throws InterruptedException {
    return Optional.ofNullable(this.queueOut.poll(duration.toMillis(), TimeUnit.MILLISECONDS));
  }

  @Override
  public BattleshipsPlayer startPlayer() {
    this.gameField.reset();
    final Thread threadIn = new Thread(this::doRunIn, "bs-gex-bot-in-" + this.playerId);
    threadIn.setDaemon(true);
    if (!this.threadInput.compareAndSet(null, threadIn)) {
      throw new IllegalStateException("Found already created thread in");
    }

    final Thread threadOut = new Thread(this::doRunOut, "bs-gex-bot-out-" + this.playerId);
    threadIn.setDaemon(true);
    if (!this.threadOutput.compareAndSet(null, threadOut)) {
      throw new IllegalStateException("Found already created thread out");
    }

    this.threadInput.get().start();
    this.threadOutput.get().start();

    return this;
  }

  @Override
  public void pushGameEvent(final BsGameEvent event) {
    if (event != null) {
      try {
        if (!this.queueIn.offer(event, 5, TimeUnit.SECONDS)) {
          this.placeEventIntoInQueue(new BsGameEvent(GameEventType.EVENT_FAILURE, 0, 0));
          throw new IllegalStateException("Can't place event: " + event);
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public boolean isAvailable() {
    return this.doTestCall();
  }


  @Override
  public void disposePlayer() {
    closeQuietly(this.openedHttpConnection.get());
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

  @Override
  public Optional<BattleshipsPlayer> findFirstTurnPlayer(final BattleshipsPlayer playerA, final BattleshipsPlayer playerB) {
    final BattleshipsPlayer result;
    if (this.myFirstTurn) {
      result = this == playerA ? playerA : playerB;
    } else {
      result = this != playerA ? playerA : playerB;
    }
    return Optional.of(result);
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
    IN_GAME(16),
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
