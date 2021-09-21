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
import java.net.InterfaceAddress;
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
  private static final Duration DELAY_BROADCAST_CHECK = Duration.ofSeconds(3);
  private static final Duration MAX_AGREEMENT_WAIT = Duration.ofSeconds(10);
  private final JList<OpponentRecord> listAllowedPlayers;
  private final JButton buttonSendOffer;
  private final UdpBroadcastingServer udpBroadcasting;
  private final AtomicReference<TcpGameLink> createdLink = new AtomicReference<>();
  private final BlockingQueue<UdpMessage> incomingUdpRecordQueue = new ArrayBlockingQueue<>(4096);
  private final Timer timer;
  private final List<OpponentRecord> recordList = new ArrayList<>();
  private final List<ListDataListener> listDataListenerList = new CopyOnWriteArrayList<>();
  private final AtomicReference<Pair<String, Long>> processingOffer = new AtomicReference<>();
  private final InterfaceAddress interfaceAddress;
  private final int port;
  private final JPanel panelWaitForOffer;
  private final JProgressBar progressBarOfferWait;
  private final JPanel contentPanel;

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public SelectNetOpponentDialog(final StartOptions startOptions, final String uid, final InterfaceAddress address, final int port) throws Exception {
    super((JFrame) null, "Choose BattleShips opponent", true, startOptions.getGraphicsConfiguration().orElse(null));
    this.interfaceAddress = address;
    this.port = port;
    this.setIconImage(startOptions.getGameIcon().orElse(null));
    this.setAlwaysOnTop(true);

    this.panelWaitForOffer = new JPanel(new BorderLayout());

    this.progressBarOfferWait = new JProgressBar();
    this.progressBarOfferWait.setOrientation(JProgressBar.HORIZONTAL);
    this.progressBarOfferWait.setStringPainted(true);
    this.progressBarOfferWait.setIndeterminate(true);

    this.panelWaitForOffer.add(this.progressBarOfferWait, BorderLayout.CENTER);

    JButton buttonCancelOffer = new JButton("Cancel offer");
    buttonCancelOffer.addActionListener(e -> this.onButtonCancelOffer());
    this.panelWaitForOffer.add(buttonCancelOffer, BorderLayout.EAST);

    this.contentPanel = new JPanel(new BorderLayout());

    final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    this.contentPanel.add(buttonPanel, BorderLayout.SOUTH);

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

    this.udpBroadcasting = new UdpBroadcastingServer(uid, DELAY_BROADCAST_CHECK, address, port, this.incomingUdpRecordQueue::offer);

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
          this.showOfferProgressPanel(offerRecord.getLeft());
          this.udpBroadcasting.sendEvent(offerRecord.getLeft(), UdpMessage.Event.LETS_PLAY);
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

  private void showOfferProgressPanel(final String uid) {
    this.progressBarOfferWait.setString(uid);
    this.contentPanel.add(this.panelWaitForOffer, BorderLayout.NORTH);
    this.contentPanel.doLayout();
    this.contentPanel.revalidate();
    this.contentPanel.repaint();

    this.listAllowedPlayers.setEnabled(false);
    this.buttonSendOffer.setEnabled(false);
  }

  private void hideOfferProgressPanel() {
    this.contentPanel.remove(this.panelWaitForOffer);
    this.contentPanel.doLayout();
    this.contentPanel.revalidate();
    this.contentPanel.repaint();

    this.listAllowedPlayers.setEnabled(true);
    this.listAllowedPlayers.setSelectedIndex(-1);
    this.buttonSendOffer.setEnabled(false);
  }

  private void onButtonCancelOffer() {
    final Pair<String, Long> currentOffer = this.processingOffer.getAndSet(null);
    this.hideOfferProgressPanel();
    if (currentOffer != null) {
      LOGGER.info("Canceling offer for Player " + currentOffer.getLeft());
      this.udpBroadcasting.sendEvent(currentOffer.getLeft(), UdpMessage.Event.NO);
    }
  }

  private boolean checkForIncomingNo(final String uid) {
    return this.incomingUdpRecordQueue.removeIf(x -> x.getEvent() == UdpMessage.Event.NO && x.getPlayerUid().equals(uid));
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
          LOGGER.info("Detected incoming play offer from: " + nextData.getPlayerUid());
          final Pair<String, Long> newOffer = Pair.of(nextData.getPlayerUid(), System.currentTimeMillis());
          if (this.processingOffer.compareAndSet(null, newOffer)) {
            if (JOptionPane.showConfirmDialog(this, "Let's play! I am " + newOffer.getLeft(), "Opponent request", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
              if (this.checkForIncomingNo(newOffer.getLeft())) {
                this.processingOffer.set(null);
                JOptionPane.showMessageDialog(this, String.format("Player %s has canceled offer!", newOffer.getLeft()), "Offer canceled", JOptionPane.WARNING_MESSAGE);
              } else {
                this.udpBroadcasting.sendEvent(nextData.getPlayerUid(), UdpMessage.Event.LETS_PLAY);
                this.udpBroadcasting.flush();
                LOGGER.info("Starting session with " + newOffer.getLeft());
                linkCompleted = true;
                this.beginGame(nextData);
              }
            } else {
              this.udpBroadcasting.sendEvent(nextData.getPlayerUid(), UdpMessage.Event.NO);
              this.processingOffer.set(null);
            }
          } else {
            if (this.processingOffer.get().getLeft().equals(newOffer.getLeft())) {
              LOGGER.info("player " + newOffer.getLeft() + " sent agreement");
              linkCompleted = true;
              this.beginGame(nextData);
              this.udpBroadcasting.flush();
            } else {
              LOGGER.info("sending auto-reject player " + newOffer.getLeft() + " because already in processing of offer");
              this.udpBroadcasting.sendEvent(nextData.getPlayerUid(), UdpMessage.Event.NO);
            }
          }
        }
        break;
        case NO: {
          LOGGER.info("Detected incoming reject play offer from: " + nextData.getPlayerUid());
          final Pair<String, Long> offer = this.processingOffer.get();
          if (offer != null && nextData.getPlayerUid().equals(offer.getLeft())) {
            LOGGER.info("Player " + nextData.getPlayerUid() + " has rejected offer");
            this.hideOfferProgressPanel();
            this.processingOffer.set(null);
          } else {
            LOGGER.severe("Incoming event " + nextData.getEvent() + " from unexpected player " + nextData.getPlayerUid());
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
      this.listDataListenerList.forEach(x -> x.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, 0)));
    }

    var currentOffer = this.processingOffer.get();

    if (!linkCompleted && currentOffer != null && System.currentTimeMillis() - this.processingOffer.get().getRight() > MAX_AGREEMENT_WAIT.toMillis()) {
      LOGGER.info("Too long wait for agreement");
      this.processingOffer.set(null);
      JOptionPane.showMessageDialog(this, "No response from: " + currentOffer.getLeft(), "No response", JOptionPane.WARNING_MESSAGE);
      this.hideOfferProgressPanel();
      this.listAllowedPlayers.setSelectedIndex(-1);
    }
  }

  private void beginGame(final UdpMessage message) {
    LOGGER.info("starting game session with: " + message.getPlayerUid());
    try {
      this.udpBroadcasting.dispose();
      final TcpGameLink tcpOpponentLink = new TcpGameLink(new OpponentRecord(message), this.interfaceAddress, this.port);
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

  public Optional<TcpGameLink> getResult() {
    return Optional.ofNullable(this.createdLink.get());
  }

}
