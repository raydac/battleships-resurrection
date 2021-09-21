package com.igormaznitsa.battleships.utils;

import java.awt.*;
import java.util.Objects;

@SuppressWarnings("unused")
public final class ImageCursor {
  private final Image cursorImage;
  private final int hotX;
  private final int hotY;

  public ImageCursor(final Image image, final int hotX, final int hotY) {
    this.cursorImage = Objects.requireNonNull(image);
    this.hotX = hotX;
    this.hotY = hotY;
  }

  public int getHotX() {
    return this.hotX;
  }

  public int getHotY() {
    return this.hotY;
  }

  public Image getImage() {
    return this.cursorImage;
  }

  public void render(final Graphics2D gfx, final Point point) {
    final int x = point.x - this.hotX;
    final int y = point.y - this.hotY;
    gfx.drawImage(this.cursorImage, x, y, null);
  }
}
