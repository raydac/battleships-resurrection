/*
 *    Battleships PC client for GEX server
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

package com.igormaznitsa.battleships.gui.panels;

import static com.igormaznitsa.battleships.gui.panels.ControlElement.NONE;
import static com.igormaznitsa.battleships.gui.panels.ControlElement.PAUSE;


import com.igormaznitsa.battleships.gui.Animation;
import com.igormaznitsa.battleships.gui.InfoBanner;
import com.igormaznitsa.battleships.sound.Sound;
import com.igormaznitsa.battleships.utils.GfxUtils;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.Timer;

public class FinalPanel extends BasePanel {

  private final Sound sound;
  private final BufferedImage image;
  private ControlElement selectedControl = ControlElement.NONE;
  private final InfoBanner infoBanner;

  public FinalPanel(final boolean victory) {
    super();
    final String imageResource;
    if (victory) {
      imageResource = "victory.png";
      this.sound = Sound.VICTORY_HORN;
      this.infoBanner = InfoBanner.VICTORY;
    } else {
      imageResource = "LOSE.png";
      this.sound = Sound.DEFEAT_HORN;
      this.infoBanner = InfoBanner.LOST;
    }
    this.image = GfxUtils.loadGfxImageAsType(imageResource, BufferedImage.TYPE_INT_RGB);
    final Dimension size = new Dimension(this.image.getWidth(), this.image.getHeight());
    this.setSize(size);
    this.setPreferredSize(size);
    this.setMinimumSize(size);
    this.setMaximumSize(size);

    this.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent e) {
        final ControlElement detectedControl = ControlElement.find(e.getPoint());
        Sound sound = null;
        switch (detectedControl) {
          case EXIT: {
            sound = detectedControl.getOkSound();
            selectedControl = detectedControl;
            final Timer timer = new Timer(1500, d -> {
              fireSignal(SIGNAL_EXIT);
            });
            timer.setRepeats(false);
            timer.start();
            refreshUi();
          }
          break;
          case PAUSE: {
            sound = detectedControl.getOkSound();
            if (selectedControl != PAUSE) {
              fireSignal(BasePanel.SIGNAL_PAUSED);
            }
            selectedControl = detectedControl;
            refreshUi();
          }
          break;
          case NEUTRAL: {
            sound = detectedControl.getOkSound();
            if (selectedControl == PAUSE) {
              fireSignal(BasePanel.SIGNAL_RESUME);
            }
            selectedControl = detectedControl;
            refreshUi();
          }
          break;
          default: {
            if (detectedControl != NONE) {
              sound = detectedControl.getWrongSound();
            }
          }
          break;
        }
        if (sound != null) {
          sound.getClip().play();
        }
      }
    });
  }

  @Override
  protected void doStart() {
    this.sound.getClip().play();
  }

  @Override
  protected void doDispose() {
    this.sound.getClip().stop();
  }

  @Override
  protected void doPaint(final Graphics2D g2d) {
    g2d.drawImage(this.image, null, 0, 0);
    g2d.drawImage(Animation.E1_NEW.getFirst(), null, 0, 0);
    g2d.drawImage(Animation.E2_NEW.getFirst(), null, 512, 0);
    switch (this.selectedControl) {
      case PAUSE: {
        g2d.drawImage(Animation.DONE_AUTO.getFrame(1), null, 8, 0);
        g2d.drawImage(Animation.PAUSE_EXIT.getFirst(), null, 544, 344);
      }
      break;
      case EXIT: {
        g2d.drawImage(Animation.DONE_AUTO.getFrame(1), null, 8, 0);
        g2d.drawImage(Animation.PAUSE_EXIT.getLast(), null, 544, 344);
      }
      break;
      default: {
        g2d.drawImage(Animation.DONE_AUTO.getFrame(1), null, 8, 0);
        g2d.drawImage(Animation.PAUSE_EXIT.getFrame(1), null, 544, 344);
      }
      break;
    }
    this.infoBanner.render(g2d, BANNER_COORD);
  }
}
