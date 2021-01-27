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
import com.igormaznitsa.battleships.gui.ScaleFactor;
import com.igormaznitsa.battleships.gui.StartOptions;
import com.igormaznitsa.battleships.sound.Sound;
import com.igormaznitsa.battleships.utils.GfxUtils;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Optional;
import javax.swing.Timer;

public class FinalPanel extends BasePanel {

  private final Sound sound;
  private final BufferedImage image;
  private ControlElement selectedControl = ControlElement.NONE;
  private final InfoBanner infoBanner;
  private final Optional<Color> fillColor;

  public FinalPanel(final StartOptions startOptions, final Optional<ScaleFactor> scaleFactor,
                    final FinalState state) {
    super(startOptions, scaleFactor);

    this.image =
        GfxUtils.loadGfxImageAsType(state.getImageResourceName(), BufferedImage.TYPE_INT_RGB);
    this.fillColor = state.getFillColor();
    this.sound = state.getSound();
    this.sound.load(startOptions.isWithSound(), true);
    this.infoBanner = state.getBanner();

    this.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent mouseEvent) {
        final Point translatedMousePoint =
            scaleFactor.map(sf -> sf.translateMousePoint(mouseEvent)).orElse(mouseEvent.getPoint());
        final ControlElement detectedControl = ControlElement.find(translatedMousePoint);
        Sound sound = null;
        switch (detectedControl) {
          case EXIT: {
            sound = detectedControl.getOkSound();
            selectedControl = detectedControl;
            final Timer timer = new Timer(1500, d -> fireSignal(SIGNAL_EXIT));
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
          sound.play();
        }
      }
    });
  }

  @Override
  protected void doStart() {
    this.sound.play();
  }

  @Override
  protected void doDispose() {
    this.sound.stop();
  }

  @Override
  protected void doPaint(final Graphics2D g2d) {
    this.fillColor.ifPresent(c -> {
      g2d.setColor(c);
      g2d.fillRect(0, 0, GAMEFIELD_WIDTH, GAMEFIELD_HEIGHT);
    });

    if (this.image.getWidth() == GAMEFIELD_WIDTH && this.image.getHeight() == GAMEFIELD_HEIGHT) {
      g2d.drawImage(this.image, null, 0, 0);
    } else {
      final int imageX = (GAMEFIELD_WIDTH - this.image.getWidth()) / 2;
      final int imageY = (GAMEFIELD_HEIGHT - this.image.getHeight()) / 2;
      g2d.drawImage(this.image, null, imageX, imageY);
    }

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
