package com.igormaznitsa.battleships.opponent.net;

import com.igormaznitsa.battleships.gui.StartOptions;
import com.igormaznitsa.battleships.utils.Pair;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SelectNetOpponentDialog extends JDialog {

  private static final Logger LOGGER = Logger.getLogger(SelectNetOpponentDialog.class.getSimpleName());
  private static final Duration DELAY_BROADCAST_CHECK = Duration.ofSeconds(1);
  private static final Duration MAX_AGREEMENT_WAIT = Duration.ofSeconds(15);
  private final JList<OpponentRecord> listAllowedPlayers;
  private final JButton buttonSendOffer;
  private final UdpBroadcaster udpBroadcasting;
  private final AtomicReference<TcpOpponentLink> createdLink = new AtomicReference<>();
  private final BlockingQueue<UdpMessage> incomingUdpRecordQueue = new ArrayBlockingQueue<>(256);
  private final Timer timer;
  private final List<OpponentRecord> recordList = new ArrayList<>();
  private final List<ListDataListener> listDataListenerList = new CopyOnWriteArrayList<>();
  private final AtomicReference<Pair<String, Long>> processingOffer = new AtomicReference<>();
  private final InetAddress inetAddress;
  private final int port;

  public SelectNetOpponentDialog(final StartOptions startOptions, final String uid, final InetAddress address, final int port) throws Exception {
    super((JFrame) null, "Choose BattleShips opponent", true, startOptions.getGraphicsConfiguration().orElse(null));
    this.inetAddress = address;
    this.port = port;
    this.setIconImage(startOptions.getGameIcon().orElse(null));
    this.setAlwaysOnTop(true);

    final JPanel contentPanel = new JPanel(new BorderLayout());

    final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    contentPanel.add(buttonPanel, BorderLayout.SOUTH);

    this.listAllowedPlayers = new JList<>(new ListModel<>() {
      @Override
      public int getSize() {
        return recordList.size();
      }

      @Override
      public OpponentRecord getElementAt(final int index) {
        return recordList.get(index);
      }

      @Override
      public void addListDataListener(final ListDataListener l) {
        listDataListenerList.add(l);
      }

      @Override
      public void removeListDataListener(final ListDataListener l) {
        listDataListenerList.remove(l);
      }
    });
    this.listAllowedPlayers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    this.udpBroadcasting = new UdpBroadcaster(uid, DELAY_BROADCAST_CHECK, address, port, this.incomingUdpRecordQueue::offer);

    this.buttonSendOffer = new JButton("Invite for round");
    this.buttonSendOffer.setEnabled(false);
    this.buttonSendOffer.addActionListener(e -> {
      final int selectedOpponentIndex = this.listAllowedPlayers.getSelectedIndex();
      if (selectedOpponentIndex < 0) {
        this.buttonSendOffer.setEnabled(false);
      } else {
        final OpponentRecord opponentRecord = this.recordList.get(selectedOpponentIndex);
        final Pair<String, Long> offerRecord = Pair.of(opponentRecord.getUid(), System.currentTimeMillis());
        if (this.processingOffer.compareAndSet(null, offerRecord)) {
          this.udpBroadcasting.sendEvent(offerRecord.getLeft(), UdpMessage.Event.LETS_PLAY);
          this.listAllowedPlayers.setEnabled(false);
          this.buttonSendOffer.setEnabled(false);
        } else {
          LOGGER.severe("detected still active offer processing: " + this.processingOffer.get());
        }
      }
    });

    JButton buttonCancel = new JButton("Cancel");
    buttonCancel.addActionListener(e -> {
      var link = this.createdLink.getAndSet(null);
      if (link != null) link.dispose();
      closeWindow();
    });

    buttonPanel.add(this.buttonSendOffer);
    buttonPanel.add(buttonCancel);

    this.listAllowedPlayers.addListSelectionListener(e ->
            this.buttonSendOffer.setEnabled(this.listAllowedPlayers.getSelectedIndex() >= 0)
    );

    final JScrollPane listPanel = new JScrollPane(this.listAllowedPlayers);
    listPanel.setBorder(new TitledBorder("List of detected players"));

    contentPanel.add(listPanel, BorderLayout.CENTER);

    this.setContentPane(contentPanel);

    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        closeWindow();
      }
    });

    this.timer = new Timer((int) DELAY_BROADCAST_CHECK.toMillis(), e -> onTimer());
    this.timer.start();

    this.pack();
    this.setLocationRelativeTo(null);
  }

  private void onTimer() {
    boolean changed = this.recordList.removeIf(nextRecord -> (System.currentTimeMillis() - nextRecord.getTimestamp()) >= DELAY_BROADCAST_CHECK.toMillis() * 3);

    boolean linkCompleted = false;

    while (!(Thread.currentThread().isInterrupted() || linkCompleted)) {
      final UdpMessage nextData = this.incomingUdpRecordQueue.poll();
      if (nextData == null) {
        break;
      }
      switch (nextData.getEvent()) {
        case WAITING: {
          final OpponentRecord record = new OpponentRecord(nextData);
          this.recordList.remove(record);
          this.recordList.add(record);
          changed = true;
        }
        break;
        case LETS_PLAY: {
          LOGGER.info("Detected incoming play offer from: " + nextData.getUid());
          final Pair<String, Long> newOffer = Pair.of(nextData.getUid(), System.currentTimeMillis());
          if (this.processingOffer.compareAndSet(null, newOffer)) {
            if (JOptionPane.showConfirmDialog(this, "Let's play! I am " + newOffer.getLeft(), "Opponent request", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
              this.udpBroadcasting.sendEvent(nextData.getUid(), UdpMessage.Event.LETS_PLAY);
            } else {
              this.udpBroadcasting.sendEvent(nextData.getUid(), UdpMessage.Event.NO);
              this.processingOffer.set(null);
            }
          } else {
            if (this.processingOffer.get().getLeft().equals(newOffer.getLeft())) {
              LOGGER.info("player " + newOffer.getLeft() + " sent agreement");
              linkCompleted = true;
              this.startGameSession(nextData);
            } else {
              LOGGER.info("sending auto-reject player " + newOffer.getLeft() + " because already in processing of offer");
              this.udpBroadcasting.sendEvent(nextData.getUid(), UdpMessage.Event.NO);
            }
          }
        }
        break;
        case NO: {
          LOGGER.info("Detected incoming reject play offer from: " + nextData.getUid());
          final Pair<String, Long> offer = this.processingOffer.get();
          if (offer != null && nextData.getUid().equals(offer.getLeft())) {
            LOGGER.info("Player " + nextData.getUid() + " has rejected offer");
            this.processingOffer.set(null);
          } else {
            LOGGER.severe("Incoming event " + nextData.getEvent() + " from unexpected player " + nextData.getUid());
          }
        }
        break;
        default: {
          LOGGER.severe("Unexpected event: " + nextData.getEvent());
        }
      }
    }

    if (changed) {
      Collections.sort(this.recordList);
      this.listDataListenerList.forEach(x -> {
        x.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, 0));
      });
    }

    if (!linkCompleted && this.processingOffer.get() != null && System.currentTimeMillis() - this.processingOffer.get().getRight() > MAX_AGREEMENT_WAIT.toMillis()) {
      LOGGER.info("Too long wait for agreement");
      this.processingOffer.set(null);
      JOptionPane.showMessageDialog(this, "No response from: " + this.processingOffer.get().getLeft(), "No response", JOptionPane.WARNING_MESSAGE);
      this.buttonSendOffer.setEnabled(false);
      this.listAllowedPlayers.setEnabled(true);
      this.listAllowedPlayers.setSelectedIndex(-1);
    }
  }

  private void startGameSession(final UdpMessage message) {
    LOGGER.info("starting game session with: " + message.getUid());
    try {
      final TcpOpponentLink tcpOpponentLink = new TcpOpponentLink(new OpponentRecord(message), this.inetAddress, this.port);
      if (this.createdLink.compareAndSet(null, tcpOpponentLink)) {
        LOGGER.info("created tcp link");
        this.closeWindow();
      } else {
        throw new IOException("Already created link detected, unexpectedly");
      }
    } catch (final IOException ex) {
      LOGGER.log(Level.SEVERE, "Error during create link", ex);
      JOptionPane.showMessageDialog(this, "Can't create link: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      System.exit(99883);
    }
  }

  private void closeWindow() {
    try {
      this.timer.stop();
      this.udpBroadcasting.dispose();
    } finally {
      this.dispose();
    }
  }

  public synchronized SelectNetOpponentDialog start() throws SocketException {
    this.udpBroadcasting.start();
    return this;
  }

  public Optional<TcpOpponentLink> getResult() {
    return Optional.ofNullable(this.createdLink.get());
  }

}
