package com.igormaznitsa.battleships.opponent;

import com.igormaznitsa.battleships.gui.StartOptions;
import com.igormaznitsa.battleships.opponent.net.SelectNetOpponentDialog;
import com.igormaznitsa.battleships.opponent.net.TcpGameLink;
import com.igormaznitsa.battleships.utils.NetUtils;

import javax.swing.*;
import java.net.InterfaceAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class NewNetSingleSessionOpponent implements BattleshipsPlayer {
  private static final Logger LOGGER = Logger.getLogger(NewNetSingleSessionOpponent.class.getSimpleName());

  private final String uid;
  private final InterfaceAddress interfaceAddress;
  private final int port;
  private final StartOptions startOptions;
  private final AtomicReference<TcpGameLink> opponentLink = new AtomicReference<>();

  public NewNetSingleSessionOpponent(final StartOptions startOptions, final NetUtils.NamedInterfaceAddress namedInetAddress, final int port) {
    this.startOptions = Objects.requireNonNull(startOptions);
    this.interfaceAddress = Objects.requireNonNull(namedInetAddress).getInterfaceAddress();
    this.port = port;
    this.uid = NetUtils.makeNetworkUid();
    LOGGER.info("Network UID: " + this.uid);
  }

  @Override
  public Optional<BsGameEvent> pollGameEvent(Duration duration) {
    final TcpGameLink link = this.opponentLink.get();
    return link == null ? Optional.empty() : link.pollIncomingEvent();
  }

  @Override
  public void pushGameEvent(final BsGameEvent event) {
    this.opponentLink.get().sendEvent(event);
  }

  @Override
  public BattleshipsPlayer startPlayer() {
    try {
      final SelectNetOpponentDialog dialog = new SelectNetOpponentDialog(this.startOptions, this.uid, this.interfaceAddress, this.port).start();
      dialog.setVisible(true);
      final TcpGameLink opponentLink = dialog.getResult().orElse(null);
      if (opponentLink != null) {
        LOGGER.info("created link with " + opponentLink.getOpponentRecord().getUid());
        if (!this.opponentLink.compareAndSet(null, opponentLink)) {
          throw new IllegalStateException("Unexpected already existing opponent link");
        }
        opponentLink.start();
        return this;
      }
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      return null;
    }
    return null;
  }

  @Override
  public void disposePlayer() {
    final TcpGameLink opponentLink = this.opponentLink.getAndSet(null);
    if (opponentLink != null) {
      opponentLink.dispose();
    }
  }

  @Override
  public boolean isAvailable() {
    final TcpGameLink opponentLink = this.opponentLink.get();
    return opponentLink != null && opponentLink.isActive();
  }

  @Override
  public boolean isReadyForGame() {
    return true;
  }

  @Override
  public boolean isRemote() {
    return true;
  }

  @Override
  public String getId() {
    return this.uid;
  }
}
