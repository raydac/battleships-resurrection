package com.igormaznitsa.battleships.opponent.net;

import com.igormaznitsa.battleships.utils.Utils;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UdpBroadcastingServer {
  private static final Logger LOGGER = Logger.getLogger(UdpBroadcastingServer.class.getSimpleName());

  private static final int BUFFER_SIZE = 256;
  private static final int VERSION = 201;
  private final MulticastSocket udpSocket;
  private final Thread threadReceiving;
  private final Thread threadSending;
  private final Duration delay;
  private final String uid;
  private final InterfaceAddress interfaceAddress;
  private final int port;
  private final Consumer<UdpMessage> incomingDataConsumer;
  private final Map<String, UdpMessage> lastMessagesMap = new ConcurrentHashMap<>();
  private final Map<String, UdpMessage.Event> mapEventsToSend = new ConcurrentHashMap<>();

  public UdpBroadcastingServer(final String uid, final Duration delay, final InterfaceAddress interfaceAddress, final int port, final Consumer<UdpMessage> incomingDataConsumer) throws IOException {
    this.incomingDataConsumer = incomingDataConsumer;
    this.uid = uid;
    this.delay = delay;
    this.interfaceAddress = interfaceAddress;
    this.port = port;

    this.udpSocket = new MulticastSocket(port);
    this.udpSocket.setInterface(interfaceAddress.getAddress());
    this.udpSocket.setNetworkInterface(NetworkInterface.getByInetAddress(interfaceAddress.getAddress()));
    this.udpSocket.setBroadcast(true);
    this.udpSocket.setLoopbackMode(false);
    this.udpSocket.setReceiveBufferSize(BUFFER_SIZE);
    this.udpSocket.setSendBufferSize(BUFFER_SIZE);
    this.udpSocket.setReuseAddress(true);

    this.threadReceiving = new Thread(this::receivingLoop, "udp-broadcast-server-read");
    this.threadReceiving.setDaemon(true);

    this.threadSending = new Thread(this::sendingLoop, "udp-broadcast-server-send");
    this.threadSending.setDaemon(true);
  }

  public void sendEvent(final String uid, final UdpMessage.Event event) {
    synchronized (mapEventsToSend) {
      if (event == null) {
        this.mapEventsToSend.remove(uid);
      } else {
        this.mapEventsToSend.put(uid, event);
      }
    }
  }

  private void receivingLoop() {
    LOGGER.info("receiving loop started");
    try {
      final byte[] buffer = new byte[BUFFER_SIZE];
      final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
      while (!Thread.currentThread().isInterrupted() && !this.udpSocket.isClosed()) {
        try {
          this.udpSocket.receive(packet);
          final byte[] incomingData = Arrays.copyOf(packet.getData(), packet.getLength());
          if (!packet.getAddress().equals(this.interfaceAddress.getAddress())) {
            LOGGER.info("incoming packet from " + packet.getAddress());
            final UdpMessage data;
            try {
              data = new UdpMessage(incomingData);
            } catch (Exception ex) {
              LOGGER.log(Level.SEVERE, "Can't parse udp packet", ex);
              continue;
            }
            if (data.getVersion() == VERSION && !this.uid.equals(data.getPlayerUid())) {
              this.lastMessagesMap.put(data.getPlayerUid(), data);
              LOGGER.info("Incoming udp packet: " + data);
              if (this.incomingDataConsumer != null) {
                try {
                  this.incomingDataConsumer.accept(data);
                } catch (Exception ex) {
                  LOGGER.log(Level.SEVERE, "Error during consumer processing", ex);
                }
              }
            }
          }
        } catch (SocketTimeoutException ex) {
          // do nothing
        } catch (IOException e) {
          if (e.getMessage() != null && !e.getMessage().toLowerCase(Locale.ENGLISH).contains("closed")) {
            LOGGER.log(Level.SEVERE, "IO error during receiving", e);
          }
        }
      }
    } finally {
      LOGGER.info("receiving loop completed");
    }
  }

  @SuppressWarnings("BusyWait")
  public void flush() {
    LOGGER.info("flush events");
    while (!Thread.currentThread().isInterrupted() && this.threadSending.isAlive()) {
      synchronized (this.mapEventsToSend) {
        if (this.mapEventsToSend.isEmpty()) break;
      }
      try {
        Thread.sleep(100L);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  @SuppressWarnings("BusyWait")
  private void sendingLoop() {
    LOGGER.info("sending loop started");
    try {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          final byte[] body = new UdpMessage(VERSION, this.uid, UdpMessage.Event.WAITING, this.interfaceAddress.getAddress().getHostAddress(), this.port, System.currentTimeMillis()).asArray();
          this.udpSocket.send(new DatagramPacket(body, body.length, this.interfaceAddress.getBroadcast(), this.port));
          LOGGER.info("broadcast message sent");
        } catch (IOException ex) {
          LOGGER.log(Level.SEVERE, "io exception during broadcast send", ex);
        }

        if (!this.mapEventsToSend.isEmpty()) {
          final Map<String, UdpMessage.Event> copy = Map.copyOf(this.mapEventsToSend);
          this.mapEventsToSend.keySet().removeIf(x -> this.lastMessagesMap.containsKey(x) && this.mapEventsToSend.get(x) == copy.get(x));

          copy.forEach((uid, event) -> {
            try {
              final UdpMessage lastMessage = Objects.requireNonNull(this.lastMessagesMap.get(uid));
              final byte[] messageBody = new UdpMessage(VERSION, this.uid, event, this.interfaceAddress.getAddress().getHostAddress(), this.port, System.currentTimeMillis()).asArray();
              this.udpSocket.send(new DatagramPacket(messageBody, messageBody.length, InetAddress.getByName(lastMessage.getAddress()), lastMessage.getPort()));
              LOGGER.info("event " + event + " has been sent to " + uid);
            } catch (Exception ex) {
              LOGGER.log(Level.SEVERE, "can't send event " + event + " to " + uid, ex);
            }
          });
        }
        Thread.sleep(this.delay.toMillis());
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    } finally {
      LOGGER.info("sending loop completed");
    }
  }

  public synchronized void start() {
    LOGGER.info("Starting");
    this.threadSending.start();
    this.threadReceiving.start();
  }

  public synchronized void dispose() {
    LOGGER.info("disposing");
    Utils.closeQuietly(this.udpSocket);
    this.threadReceiving.interrupt();
    this.threadSending.interrupt();
    try {
      this.threadReceiving.join();
      this.threadSending.join();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
