package com.igormaznitsa.battleships.opponent.net;

import com.igormaznitsa.battleships.opponent.BsGameEvent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.igormaznitsa.battleships.utils.Utils.closeQuietly;

public class TcpOpponentLink {
  private static final Logger LOGGER = Logger.getLogger(TcpOpponentLink.class.getSimpleName());
  private static final int RETRIES_TO_SEND_PACKET = 3;
  private static final Duration DELAY_BETWEEN_RETRY = Duration.ofMillis(100);
  private final ServerSocketChannel serverSocket;
  private final AtomicReference<Thread> serverThread = new AtomicReference<>();
  private final String name;
  private final OpponentRecord opponentRecord;
  private final BlockingQueue<BsGameEvent> incomingRecords = new PriorityBlockingQueue<>(256);
  private final BlockingQueue<BsGameEvent> outgoingRecords = new ArrayBlockingQueue<>(256);
  private final AtomicReference<String> globalError = new AtomicReference<>();

  public TcpOpponentLink(final OpponentRecord opponent, final InetAddress inetAddress, final int port) throws IOException {
    this.opponentRecord = opponent;
    this.serverSocket = new ServerSocket(port, 3, inetAddress).getChannel();
    this.serverSocket.configureBlocking(false);
    this.name = this.serverSocket.toString();
  }

  public TcpOpponentLink start() {
    final Thread newThread = new Thread(this::mainLoop, "tcp-socket-server");
    newThread.setDaemon(true);
    if (this.serverThread.compareAndSet(null, newThread)) {
      LOGGER.info("Starting server thread");
    } else {
      throw new IllegalStateException("Already started server");
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

  private void raiseGlobalError(final String message) {
    if (this.globalError.compareAndSet(null, message == null ? "..." : message)) {
      LOGGER.severe("Raised global error: " + this.globalError.get());
      final Thread thread = this.serverThread.get();
      if (thread != null) thread.interrupt();
      closeQuietly(this.serverSocket);
    }
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

  private void sendBsGameEvent(final BsGameEvent gameEvent) {
    int retry = RETRIES_TO_SEND_PACKET;
    do {
      Socket socket = null;
      try {
        socket = new Socket(this.opponentRecord.getAddress(), this.opponentRecord.getPort());
        socket.setKeepAlive(false);
        socket.setReuseAddress(true);

        LOGGER.info("sending game packet: " + gameEvent);
        final OutputStream outputStream = socket.getOutputStream();
        outputStream.write(gameEvent.asArray());

        outputStream.flush();
        closeQuietly(outputStream);

        LOGGER.info("game packet " + gameEvent.getUuid() + " has been sent");
        retry = 0;
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, "can't send game event: " + gameEvent, ex);
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
      this.raiseGlobalError("can't send message packet, all retries failed");
    }
  }

  private void mainLoop() {
    try {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          final SocketChannel incomingConnection = this.serverSocket.accept();
          if (incomingConnection != null) {
            try {
              final ByteBuffer buffer = ByteBuffer.allocate(1024);
              incomingConnection.read(buffer);
              closeQuietly(incomingConnection);

              final BsGameEvent incomingEvent = new BsGameEvent(new ByteArrayInputStream(buffer.array()));
              LOGGER.info("Incoming game event: " + incomingEvent);
              if (!this.incomingRecords.offer(incomingEvent)) {
                this.raiseGlobalError("Can't place incoming event into queue: " + incomingEvent);
              }
            } catch (Exception ex) {
              LOGGER.log(Level.SEVERE, "Error during packet read", ex);
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
            LOGGER.info("sending game packet: " + nextEvent);
            this.sendBsGameEvent(nextEvent);
          }
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
      this.serverSocket.close();
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

  public boolean isActive() {
    boolean result = true;
    try {
      this.assertWorking();
    } catch (Exception ex) {
      result = false;
    }
    return result;
  }

  public Optional<BsGameEvent> pollIncomingEvent() {
    return Optional.ofNullable(this.incomingRecords.poll());
  }

  public OpponentRecord getOpponentRecord() {
    return this.opponentRecord;
  }
}
