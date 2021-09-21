package com.igormaznitsa.battleships.opponent.net;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("unused")
public final class UdpMessage implements Comparable<UdpMessage> {

  private static final AtomicLong UID_COUNTER = new AtomicLong();
  private final int version;
  private final long uid;
  private final String playerUid;
  private final Event event;
  private final String address;
  private final int port;
  private final long timestamp;

  public UdpMessage(
          final int version,
          final String playerUid,
          final Event event,
          final String address,
          final int port,
          final long timestamp
  ) {
    this.uid = UID_COUNTER.incrementAndGet();
    this.version = version;
    this.playerUid = Objects.requireNonNull(playerUid);
    this.event = Objects.requireNonNull(event);
    this.address = address;
    this.port = port;
    this.timestamp = timestamp;
  }

  public UdpMessage(final byte[] data) throws IOException {
    final DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
    this.version = in.readInt();
    this.uid = in.readLong();
    this.playerUid = in.readUTF();
    this.event = Event.valueOf(in.readUTF());
    this.address = in.readUTF();
    this.port = in.readInt();
    this.timestamp = in.readLong();
  }

  @Override
  public int compareTo(final UdpMessage that) {
    if (this.timestamp == that.timestamp) {
      return Long.compare(this.uid, that.uid);
    } else {
      return Long.compare(this.timestamp, that.timestamp);
    }
  }

  public long getUid() {
    return this.uid;
  }

  public Event getEvent() {
    return this.event;
  }

  public byte[] asArray() throws IOException {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(256);
    try (DataOutputStream out = new DataOutputStream(buffer)) {
      out.writeInt(this.version);
      out.writeLong(this.uid);
      out.writeUTF(this.playerUid);
      out.writeUTF(this.event.name());
      out.writeUTF(this.address);
      out.writeInt(this.port);
      out.writeLong(this.timestamp);
      out.flush();
    }
    return buffer.toByteArray();
  }

  public int getVersion() {
    return this.version;
  }

  public String getPlayerUid() {
    return playerUid;
  }

  public String getAddress() {
    return address;
  }

  public int getPort() {
    return port;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public enum Event {
    WAITING,
    LETS_PLAY,
    NO
  }
}
