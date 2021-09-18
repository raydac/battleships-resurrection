package com.igormaznitsa.battleships.opponent;

import com.igormaznitsa.battleships.utils.NetUtils;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public class NetworkSingleSessionOpponent implements BattleshipsPlayer {
  private static final Logger LOGGER = Logger.getLogger(NetworkSingleSessionOpponent.class.getSimpleName());

  private final String uid;
  private final NetUtils.NamedInetAddress address;
  private final int port;

  public NetworkSingleSessionOpponent(final NetUtils.NamedInetAddress namedInetAddress, final int port) {
    this.address = Objects.requireNonNull(namedInetAddress);
    this.port = port;
    this.uid = NetUtils.makeNetworkUid();
    LOGGER.info("Network UID: " + this.uid);
  }

  @Override
  public Optional<BsGameEvent> pollGameEvent(Duration duration) throws InterruptedException {
    return Optional.empty();
  }

  @Override
  public void pushGameEvent(final BsGameEvent event) {

  }

  @Override
  public BattleshipsPlayer startPlayer() {
    return null;
  }

  @Override
  public void disposePlayer() {

  }

  @Override
  public boolean isAvailable() {
    return false;
  }

  @Override
  public boolean isReadyForGame() {
    return false;
  }

  @Override
  public String getId() {
    return null;
  }
}
