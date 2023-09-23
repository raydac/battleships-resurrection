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

package com.igormaznitsa.battleships.gui;

import com.igormaznitsa.battleships.utils.GfxUtils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.igormaznitsa.battleships.utils.GfxUtils.loadGfxImageAsType;

@SuppressWarnings("unused")
public enum Animation {
  CELL_1_ROK_L("1_CELL_ROK_L$$$$", 100, 1.5d),
  CELL_1_ROK_R("1_CELL_ROK_R$$$$", 100, 1.5d),
  CELL_1_SP_L("1_CELL_SP_L$$$$", 40, 1.5d),
  CELL_1_SP_R("1_CELL_SP_R$$$$", 40, 1.5d),
  CELL_1_TR_L("1_CELL_TR_L$$$$", 20, 1.5d),
  CELL_1_TR_R("1_CELL_TR_R$$$$", 20, 1.5d),
  CELL_1_V1_L("1_CELL_V1_L$$$$", 30, 1.5d),
  CELL_1_V1_R("1_CELL_V1_R$$$$", 30, 1.5d),

  CELL_2_1_SP_L("2_1CELL_SP_L$$$$", 80, 1.5d),
  CELL_2_1_SP_R("2_1CELL_SP_R$$$$", 80, 1.5d),
  CELL_2_SP_L("2_CELL_SP_L$$$$", 80, 1.5d),
  CELL_2_SP_R("2_CELL_SP_R$$$$", 80, 1.5d),

  CELL_2_TR_L("2_CELL_TR_L$$$$", 20, 1.5d),
  CELL_2_TR_R("2_CELL_TR_R$$$$", 20, 1.5d),

  CELL_2_V1_L("2_CELL_V1_L$$$$", 80, 1.5d),
  CELL_2_V1_R("2_CELL_V1_R$$$$", 80, 1.5d),

  CELL_3_1SP_L("3_CELL_1SP_L$$$$", 80, 1.5d),
  CELL_3_1SP_R("3_CELL_1SP_R$$$$", 80, 1.5d),

  CELL_3_2SP_L("3_CELL_2SP_L$$$$", 80, 1.5d),
  CELL_3_2SP_R("3_CELL_2SP_R$$$$", 80, 1.5d),

  CELL_3_SP_L("3_CELL_SP_L$$$$", 80, 1.5d),
  CELL_3_SP_R("3_CELL_SP_R$$$$", 80, 1.5d),

  CELL_3_TR_L("3_CELL_TR_L$$$$", 20, 1.5d),
  CELL_3_TR_R("3_CELL_TR_R$$$$", 20, 1.5d),

  CELL_3_V1_L("3_CELL_V1_L$$$$", 35, 1.5d),
  CELL_3_V1_R("3_CELL_V1_R$$$$", 35, 1.5d),

  CELL_3_V2_L("3_CELL_V2_L$$$$", 35, 1.5d),
  CELL_3_V2_R("3_CELL_V2_R$$$$", 35, 1.5d),

  CELL_4_1SP_L("4_CELL_1SP_L$$$$", 60, 1.5d),
  CELL_4_1SP_R("4_CELL_1SP_R$$$$", 60, 1.5d),

  CELL_4_1VN_L("4_CELL_1VN_L$$$$", 120, 1.5d),

  CELL_4_2SP_L("4_CELL_2SP_L$$$$", 60, 1.5d),
  CELL_4_2SP_R("4_CELL_2SP_R$$$$", 60, 1.5d),

  CELL_4_2VN_L("4_CELL_2VN_L$$$$", 60, 1.5d),
  CELL_4_2VN_R("4_CELL_2VN_R$$$$", 60, 1.5d),

  CELL_4_3SP_L("4_CELL_3SP_L$$$$", 60, 1.5d),
  CELL_4_3SP_R("4_CELL_3SP_R$$$$", 60, 1.5d),

  CELL_4_3VN_L("4_CELL_3VN_L$$$$", 60, 1.5d),
  CELL_4_3VN_R("4_CELL_3VN_R$$$$", 60, 1.5d),

  CELL_4_SP_L("4_CELL_SP_L$$$$", 60, 1.5d),
  CELL_4_SP_R("4_CELL_SP_R$$$$", 60, 1.5d),

  CELL_4_TR_L("4_CELL_TR_L$$$$", 20, 1.5d),
  CELL_4_TR_R("4_CELL_TR_R$$$$", 20, 1.5d),

  CELL_4_V1_R("4_CELL_V1_R$$$$", 120, 1.5d),

  ACT_MAP("act_map$$$", 8, 1.0d),
  DIGIT("digit$$$", 4, 1.0d),
  DONE_AUTO("DoneAuto$$$", 3, 1.0d),
  E1_NEW("E1_new$$$", 12, 1.0d),
  E2_NEW("E2_new$$$", 12, 1.0d),
  EXPLO_GOR("EXPLO_GOR$$$$", 53, 1.5d),
  EXPLODE("EXPLODE$$$$", 53, 1.5d),
  KILKA("KILKA$$$$", 40, 1.5d),
  PAUSE_EXIT("PauseExit$$$", 3, 1.0d),
  ROKET_FALL("roket_fall$$$$", 29, 1.5d),
  SAMOL_FALL("samol_fall$$$$", 29, 1.5d),
  SPLASH("SPLASH$$$$", 65, 1.5d),
  SPLASH_GOR("SPLASH_GOR$$$$", 65, 1.5d),
  PANEL("panel", -1, 1.0d),
  MAP_EMPTY("map_empty", -1, 1.0d),
  MAP_HIT("map_hit", -1, 1.0d),
  MAP_SHIP("map_ship", -1, 1.0d),
  LOSE("LOSE", -1, 1.0d),
  VICTORY("victory", -1, 1.0d),
  FIRE("FIRE", -1, 1.0d),
  FON("Fon", -1, 1.0d),
  ;

  private final String animationFileNameTemplate;
  private final int maxFrameIndex;
  private final double scale;
  private List<BufferedImage> frames = Collections.emptyList();

  Animation(final String fileNameTemplate, final int maxFrameIndex, final double scale) {
    this.animationFileNameTemplate = fileNameTemplate;
    this.maxFrameIndex = maxFrameIndex;
    this.scale = scale;
  }

  private static String makeFileName(final String prefix, final int index) {
    final int postfixIndex = prefix.indexOf('$');
    if (postfixIndex < 0) {
      throw new IllegalArgumentException("Unexpected prefix: " + prefix);
    }
    final int numOfPostfixDigits = prefix.length() - postfixIndex;

    final StringBuilder buffer = new StringBuilder(prefix.substring(0, postfixIndex));
    final String indexStr = Integer.toString(index);
    buffer.append("0".repeat(Math.max(0, numOfPostfixDigits - indexStr.length())));
    buffer.append(indexStr).append(".png");
    return buffer.toString();
  }

  public synchronized BufferedImage getFirst() {
    return this.frames.get(0);
  }

  public synchronized BufferedImage getLast() {
    return this.frames.get(this.frames.size() - 1);
  }

  public synchronized int getWidth() {
    return this.frames.isEmpty() ? -1 : this.frames.get(0).getWidth();
  }

  public synchronized int getHeight() {
    return this.frames.isEmpty() ? -1 : this.frames.get(0).getHeight();
  }

  public synchronized void load() {
    if (this.maxFrameIndex < 0) {
      this.frames = Collections.singletonList(
              loadGfxImageAsType(this.animationFileNameTemplate + ".png", BufferedImage.TYPE_INT_ARGB,
                      this.scale));
    } else {
      final ArrayList<BufferedImage> frameList = new ArrayList<>();
      for (int i = 0; i <= this.maxFrameIndex; i++) {
        final String fileName = makeFileName(this.animationFileNameTemplate, i);
        if (GfxUtils.hasGfxImage(fileName)) {
          final BufferedImage frame =
                  loadGfxImageAsType(fileName, BufferedImage.TYPE_INT_ARGB, this.scale);
          frameList.add(frame);
        }
      }
      frameList.trimToSize();
      if (frameList.isEmpty()) {
        throw new IllegalArgumentException("Can't find any frame prefixed: " + this.animationFileNameTemplate);
      } else {
        this.frames = Collections.unmodifiableList(frameList);
      }
    }
  }

  public synchronized void dispose() {
    this.frames = Collections.emptyList();
  }

  public synchronized int getLength() {
    return this.frames.size();
  }

  public synchronized BufferedImage getFrame(final int index) {
    return this.frames.get(index);
  }
}
