package com.igormaznitsa.battleships.opponent.net;

public class OpponentRecord implements Comparable<OpponentRecord> {
  private final String uid;
  private final UdpMessage incomingData;

  OpponentRecord(final UdpMessage incomingData) {
    this.uid = incomingData.getPlayerUid();
    this.incomingData = incomingData;
  }

  public String getUid() {
    return uid;
  }

  public String getAddress() {
    return this.incomingData.getAddress();
  }

  public int getPort() {
    return this.incomingData.getPort();
  }

  public long getTimestamp() {
    return this.incomingData.getTimestamp();
  }

  @Override
  public String toString() {
    return String.format("%s (%s)", this.uid, this.getAddress());
  }

  @Override
  public int hashCode() {
    return this.uid.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (obj instanceof OpponentRecord) {
      final OpponentRecord that = (OpponentRecord) obj;
      return this.uid.equals(that.uid) && this.incomingData.getAddress().equals(that.incomingData.getAddress());
    }
    return false;
  }

  @Override
  public int compareTo(final OpponentRecord o) {
    return this.uid.compareTo(o.uid);
  }
}
