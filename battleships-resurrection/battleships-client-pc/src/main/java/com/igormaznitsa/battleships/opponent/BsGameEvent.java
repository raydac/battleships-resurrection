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


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("unused")
public final class BsGameEvent implements Comparable<BsGameEvent> {
  private static final byte[] PREFIX = "BATTLESHIPS_2.0\n".getBytes(StandardCharsets.UTF_8);
  private final UUID uuid;
  private final GameEventType gameEventType;
  private final int x;
  private final int y;
  private final long timestamp;

  public BsGameEvent(final GameEventType event, final int x, final int y) {
    this.uuid = UUID.randomUUID();
    this.gameEventType = Objects.requireNonNull(event);
    this.x = x;
    this.y = y;
    this.timestamp = System.currentTimeMillis();
  }

  public BsGameEvent(final InputStream inputStream) throws IOException {
    final DataInputStream in = new DataInputStream(inputStream);

    final byte[] prefix = new byte[PREFIX.length];
    in.readFully(prefix);
    if (!Arrays.equals(PREFIX, prefix))
      throw new IllegalArgumentException("No battleships game packet");

    this.uuid = UUID.fromString(in.readUTF());
    this.gameEventType = GameEventType.valueOf(in.readUTF());
    this.x = in.readInt();
    this.y = in.readInt();
    this.timestamp = in.readLong();
  }

  @Override
  public int compareTo(final BsGameEvent that) {
    return Long.compare(this.timestamp, that.timestamp);
  }

  public byte[] asArray() throws IOException {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(256);
    final DataOutputStream out = new DataOutputStream(buffer);

    out.write(PREFIX);

    out.writeUTF(this.uuid.toString());
    out.writeUTF(this.gameEventType.name());
    out.writeInt(this.x);
    out.writeInt(this.y);
    out.writeLong(this.timestamp);

    out.flush();
    out.close();

    return buffer.toByteArray();
  }

  public UUID getUuid() {
    return this.uuid;
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  public GameEventType getType() {
    return this.gameEventType;
  }

  public int getX() {
    return this.x;
  }

  public int getY() {
    return this.y;
  }

  @Override
  public String toString() {
    return String.format("BsGameEvent(type=%s,x=%d,y=%d,time=%d)", this.gameEventType, this.x,
            this.y, this.timestamp);
  }

}
