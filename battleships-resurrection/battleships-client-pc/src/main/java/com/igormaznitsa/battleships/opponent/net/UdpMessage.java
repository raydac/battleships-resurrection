package com.igormaznitsa.battleships.opponent.net;

import java.io.*;
import java.util.Objects;

public final class UdpMessage {

  private final int version;
  private final String uid;
  private final Event event;
  private final String address;
  private final int port;
  private final long timestamp;

  public UdpMessage(
          final int version,
          final String uid,
          final Event event,
          final String address,
          final int port,
          final long timestamp
  ) {
    this.version = version;
    this.uid = Objects.requireNonNull(uid);
    this.event = Objects.requireNonNull(event);
    this.address = address;
    this.port = port;
    this.timestamp = timestamp;
  }

  public UdpMessage(final byte[] data) throws IOException {
    final DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
    this.version = in.readInt();
    this.uid = in.readUTF();
    this.event = Event.valueOf(in.readUTF());
    this.address = in.readUTF();
    this.port = in.readInt();
    this.timestamp = in.readLong();
  }

  public Event getEvent() {
    return this.event;
  }

  public byte[] asArray() throws IOException {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(256);
    try (DataOutputStream out = new DataOutputStream(buffer)) {
      out.writeInt(this.version);
      out.writeUTF(this.uid);
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

  public String getUid() {
    return uid;
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
    NO;
  }
}
