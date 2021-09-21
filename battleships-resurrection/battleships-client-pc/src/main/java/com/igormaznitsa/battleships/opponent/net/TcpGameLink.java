package com.igormaznitsa.battleships.opponent.net;

import com.igormaznitsa.battleships.opponent.BsGameEvent;
import com.igormaznitsa.battleships.opponent.GameEventType;
import com.igormaznitsa.battleships.utils.NetUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.igormaznitsa.battleships.utils.Utils.closeQuietly;

@SuppressWarnings("unused")
public class TcpGameLink {
  private static final Logger LOGGER = Logger.getLogger(TcpGameLink.class.getSimpleName());
  private static final int RETRIES_TO_SEND_PACKET = 3;
  private static final Duration DELAY_BETWEEN_RETRY = Duration.ofMillis(200);
  private static final Duration ALIVE_SEND_DELAY = Duration.ofSeconds(5);
  private static final Duration MAX_ALIVE_DELAY = Duration.ofSeconds(15);
  private final AtomicReference<Thread> serverThread = new AtomicReference<>();
  private final String name;
  private final OpponentRecord opponentRecord;
  private final BlockingQueue<BsGameEvent> incomingRecords = new PriorityBlockingQueue<>(256);
  private final BlockingQueue<BsGameEvent> outgoingRecords = new ArrayBlockingQueue<>(256);
  private final AtomicReference<String> globalError = new AtomicReference<>();
  private final ServerSocketChannel serverSocketChannel;
  private final AtomicBoolean opponentActive = new AtomicBoolean();
  private final InetAddress opponentAddress;

  public TcpGameLink(final OpponentRecord opponent, final InterfaceAddress interfaceAddress, final int port) throws IOException {
    this.opponentRecord = opponent;
    this.opponentAddress = InetAddress.getByName(opponent.getAddress());
    this.serverSocketChannel = ServerSocketChannel.open();
    this.serverSocketChannel.bind(new InetSocketAddress(interfaceAddress.getAddress(), port));
    this.serverSocketChannel.configureBlocking(false);

    this.name = this.serverSocketChannel.toString();
  }

  public TcpGameLink start() {
    final Thread newThread = new Thread(this::mainLoop, "tcp-socket-server");
    newThread.setDaemon(true);
    if (this.serverThread.compareAndSet(null, newThread)) {
      LOGGER.info("starting server thread");
      newThread.start();
    } else {
      throw new IllegalStateException("detected already started server");
    }
    return this;
  }

  private void assertWorking() {
    final Thread thread = this.serverThread.get();
    if (thread == null || !thread.isAlive())
      throw new IllegalStateException("Main thread non-started or already stopped");
    if (this.globalError.get() != null) {
      throw new IllegalStateException("Stopped work for global error: " + this.globalError);
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void raiseGlobalError(final String message) {
    if (this.globalError.compareAndSet(null, message == null ? "..." : message)) {
      LOGGER.severe("Raised global error: " + this.globalError.get());
      final Thread thread = this.serverThread.get();
      if (thread != null) thread.interrupt();
      closeQuietly(this.serverSocketChannel);
      this.incomingRecords.offer(new BsGameEvent(GameEventType.EVENT_FAILURE, 0, 0));
    }
  }

  public boolean isActive() {
    return this.opponentActive.get();
  }

  public String getGlobalErrorText() {
    return this.globalError.get();
  }

  public void sendEvent(final BsGameEvent gameEvent) {
    this.assertWorking();
    if (!this.outgoingRecords.offer(gameEvent)) {
      this.raiseGlobalError("Can't place packet into output queue");
    }
  }

  @SuppressWarnings("BusyWait")
  private boolean sendData(final byte[] data) {
    int retry = RETRIES_TO_SEND_PACKET;
    do {
      Socket socket = null;
      try {
        final InetSocketAddress endPoint = new InetSocketAddress(this.opponentRecord.getAddress(), this.opponentRecord.getPort());
        socket = new Socket();
        socket.setSoTimeout(200);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(false);
        socket.setReuseAddress(true);

        socket.connect(endPoint, 1000);

        final OutputStream outputStream = socket.getOutputStream();
        outputStream.write(data);

        outputStream.flush();
        closeQuietly(outputStream);

        retry = 0;
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, "can't send game event", ex);
        try {
          Thread.sleep(DELAY_BETWEEN_RETRY.toMillis());
        } catch (InterruptedException exx) {
          Thread.currentThread().interrupt();
        }
      } finally {
        closeQuietly(socket);
        retry--;
      }
    } while (!Thread.currentThread().isInterrupted() && retry > 0);
    if (!Thread.currentThread().isInterrupted() && retry == 0) {
      this.incomingRecords.offer(new BsGameEvent(GameEventType.EVENT_CONNECTION_ERROR, 0, 0));
      return false;
    } else {
      return true;
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void mainLoop() {
    try {
      final byte[] alivePacket = "alive_event".getBytes(StandardCharsets.UTF_8);

      long lastPacketInTime = System.currentTimeMillis();
      long lastPacketOutTime = System.currentTimeMillis();

      while (!Thread.currentThread().isInterrupted() && this.serverSocketChannel.isOpen()) {
        if (System.currentTimeMillis() - lastPacketInTime > MAX_ALIVE_DELAY.toMillis()) {
          LOGGER.severe("no events from opponent for long time, stopping work");
          this.opponentActive.compareAndSet(true, false);
          this.incomingRecords.offer(new BsGameEvent(GameEventType.EVENT_CONNECTION_ERROR, 0, 0));
          break;
        }

        try {
          final byte[] dataBuffer = new byte[4096];
          final SocketChannel incomingConnection = this.serverSocketChannel.accept();
          if (incomingConnection != null) {
            final InetAddress address = ((InetSocketAddress) incomingConnection.getRemoteAddress()).getAddress();
            if (address.equals(this.opponentAddress)) {
              LOGGER.info("incoming connection from opponent");
              lastPacketInTime = System.currentTimeMillis();
              this.opponentActive.compareAndSet(false, true);
              try {
                final int readLength = NetUtils.readData(incomingConnection, dataBuffer);
                closeQuietly(incomingConnection);

                final byte[] readDataPacket = Arrays.copyOf(dataBuffer, readLength);

                if (Arrays.equals(alivePacket, readDataPacket)) {
                  LOGGER.info("incoming alive packet from opponent");
                } else {
                  try {
                    final BsGameEvent incomingEvent = new BsGameEvent(new ByteArrayInputStream(readDataPacket));
                    LOGGER.info("incoming game event: " + incomingEvent);
                    if (!this.incomingRecords.offer(incomingEvent)) {
                      this.raiseGlobalError("Can't place incoming event into queue: " + incomingEvent);
                    }
                  } catch (IllegalArgumentException ex) {
                    LOGGER.severe("incoming packet not for battleship game: " + readDataPacket.length + " byte(s)");
                  }
                }
              } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error during packet read", ex);
              }
            } else {
              LOGGER.severe("incoming request from unexpected client, address: " + address);
              closeQuietly(incomingConnection);
            }
          }
        } catch (ClosedByInterruptException ex) {
          Thread.currentThread().interrupt();
        } catch (IOException ex) {
          LOGGER.log(Level.SEVERE, "Error in accept", ex);
        }

        try {
          final BsGameEvent nextEvent = this.outgoingRecords.poll(100, TimeUnit.MILLISECONDS);
          if (nextEvent != null) {
            LOGGER.info("sending game event: " + nextEvent);
            if (this.sendData(nextEvent.asArray())) {
              lastPacketOutTime = System.currentTimeMillis();
              LOGGER.info("game event has been sent: " + nextEvent);
            } else {
              this.incomingRecords.offer(new BsGameEvent(GameEventType.EVENT_CONNECTION_ERROR, 0, 0));
              break;
            }
          } else if ((System.currentTimeMillis() - lastPacketOutTime) >= ALIVE_SEND_DELAY.toMillis()) {
            LOGGER.info("sending ALIVE packet");
            if (this.sendData(alivePacket)) {
              lastPacketOutTime = System.currentTimeMillis();
              LOGGER.info("ALIVE packet has been sent");
            } else {
              this.incomingRecords.offer(new BsGameEvent(GameEventType.EVENT_CONNECTION_ERROR, 0, 0));
              break;
            }
          }
        } catch (IOException ex) {
          LOGGER.log(Level.SEVERE, "IOException in send game event", ex);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    } finally {
      LOGGER.info("main loop closed");
    }
  }

  @Override
  public String toString() {
    return this.name;
  }

  public void dispose() {
    try {
      this.serverSocketChannel.close();
    } catch (Exception ex) {
      // do nothing
    }
    final Thread thread = this.serverThread.getAndSet(null);
    if (thread != null) {
      thread.interrupt();
      try {
        thread.join();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public Optional<BsGameEvent> pollIncomingEvent() {
    return Optional.ofNullable(this.incomingRecords.poll());
  }

  public OpponentRecord getOpponentRecord() {
    return this.opponentRecord;
  }
}
