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

package com.igormaznitsa.battleships.utils;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import static java.util.Objects.requireNonNull;

public final class GfxUtils {
  private static final ImageReader PNG_IMAGE_READER =
          ImageIO.getImageReadersByFormatName("png").next();

  private static final BufferedImage EMPTY_128x128_ARGB =
          new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

  private GfxUtils() {
  }

  public static void tryMacOsFullScreen(final Window frame) {
    try {
      final Class<?> fullScreenUtilitiesClass = Class.forName("com.apple.eawt.FullScreenUtilities");
      fullScreenUtilitiesClass.getMethod("setWindowCanFullScreen", Window.class, Boolean.TYPE).invoke(null, frame, true);
      final Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
      final Object getApplicationResult = applicationClass.getMethod("getApplication").invoke(null);
      getApplicationResult.getClass().getMethod("requestToggleFullScreen", Window.class).invoke(getApplicationResult, frame);
    } catch (Exception e) {
      // do nothing
    }
  }

  public static void setApplicationTaskbarTitle(final Image taskBarIcon, final String taskBarBadgeTitle) {
    if (Taskbar.isTaskbarSupported()) {
      final Taskbar taskbar = Taskbar.getTaskbar();
      try {
        taskbar.setIconImage(taskBarIcon);
      } catch (Exception ex) {
        // do nothing
      }
      try {
        taskbar.setIconBadge(taskBarBadgeTitle);
      } catch (Exception ex) {
        // do nothing
      }
    }

    final Toolkit toolkit = Toolkit.getDefaultToolkit();
    try {
      final Field awtAppClassNameField =
              toolkit.getClass().getDeclaredField("awtAppClassName");
      awtAppClassNameField.setAccessible(true);
      awtAppClassNameField.set(toolkit, taskBarBadgeTitle);
    } catch (Exception ex) {
      // just ignoring
    }
  }

  public static void doInSwingThread(final Runnable action) {
    if (SwingUtilities.isEventDispatchThread()) {
      action.run();
    } else {
      SwingUtilities.invokeLater(action);
    }
  }

  public static void toScreenCenter(final Window window) {
    doInSwingThread(() -> {
      final GraphicsConfiguration config = window.getGraphicsConfiguration();
      if (config != null) {
        final Rectangle screenBounds = config.getBounds();
        final Rectangle windowBounds = window.getBounds();
        final int guessedX = screenBounds.x + (screenBounds.width - windowBounds.width) / 2;
        final int guessedY = screenBounds.y + (screenBounds.height - windowBounds.height) / 2;
        window.setLocation(guessedX, Math.max(guessedY, 64));
      }
    });
  }

  private static BufferedImage readPngStream(final InputStream stream) throws IOException {
    synchronized (PNG_IMAGE_READER) {
      try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(stream)) {
        PNG_IMAGE_READER.setInput(imageInputStream, true, true);
        try {
          return PNG_IMAGE_READER.read(0);
        } catch (IIOException ex) {
          if (ex.getCause() instanceof ArrayIndexOutOfBoundsException) {
            // looks like that no frames in image
            if (PNG_IMAGE_READER.getNumImages(false) > 0) {
              final int width = PNG_IMAGE_READER.getWidth(0);
              final int height = PNG_IMAGE_READER.getHeight(0);
              if (width == 128 && height == 128) {
                return EMPTY_128x128_ARGB;
              } else {
                return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
              }
            }
          }
          throw ex;
        }
      }
    }
  }

  public static BufferedImage loadResImage(final String fileName) {
    try (final InputStream stream = requireNonNull(
            GfxUtils.class.getClassLoader().getResourceAsStream("assets/res/" + fileName),
            "Can't find resource: " + fileName)) {
      return readPngStream(stream);
    } catch (IOException ex) {
      throw new RuntimeException("Detected error during load: " + fileName, ex);
    }
  }

  public static boolean hasGfxImage(final String fileName) {
    return GfxUtils.class.getClassLoader().getResource("assets/gfx/" + fileName) != null;
  }

  public static BufferedImage loadGfxImageAsType(final String fileName, final int imageType) {
    return loadGfxImageAsType(fileName, imageType, 1.0d);
  }

  public static BufferedImage scaleImage(final BufferedImage source, final int targetImageType,
                                         final double scaleX, final double scaleY, final boolean quality) {
    final int scaledWidth = (int) Math.round(source.getWidth() * scaleX);
    final int scaledHeight = (int) Math.round(source.getHeight() * scaleY);
    final BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, targetImageType);
    final Graphics2D gfx = scaled.createGraphics();
    try {
      gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
              quality ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
      gfx.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
              quality ? RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY : RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
      gfx.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
              quality ? RenderingHints.VALUE_COLOR_RENDER_QUALITY : RenderingHints.VALUE_COLOR_RENDER_SPEED);
      gfx.setRenderingHint(RenderingHints.KEY_RENDERING,
              quality ? RenderingHints.VALUE_RENDER_QUALITY : RenderingHints.VALUE_RENDER_SPEED);
      gfx.drawImage(source, 0, 0, scaledWidth, scaledHeight, null);
    } finally {
      gfx.dispose();
    }
    return scaled;
  }

  public static Cursor makeEmptyAwtCursor() {
    final Dimension size = Toolkit.getDefaultToolkit().getBestCursorSize(32, 32);
    final BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
    return Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "hidden-cursor");
  }

  public static void setCursorForAll(final Component component, final Cursor cursor) {
    if (component != null) {
      component.setCursor(cursor);
      if (component instanceof Container) {
        final Container container = (Container) component;
        for (int i = 0; i < container.getComponentCount(); i++) {
          final Component child = container.getComponent(i);
          if (child instanceof Container) {
            setCursorForAll(child, cursor);
          } else {
            child.setCursor(cursor);
          }
        }
      }
    }
  }

  public static BufferedImage loadGfxImageAsType(final String fileName, final int imageType,
                                                 final double scale) {
    try (final InputStream stream = requireNonNull(
            GfxUtils.class.getClassLoader().getResourceAsStream("assets/gfx/" + fileName),
            "Can't find resource: " + fileName)) {
      final BufferedImage image = readPngStream(stream);
      if (image.getType() != imageType) {
        final BufferedImage newImage =
                new BufferedImage(image.getWidth(), image.getHeight(), imageType);
        final Graphics2D gfx = newImage.createGraphics();
        try {
          gfx.drawImage(image, null, 0, 0);
        } finally {
          gfx.dispose();
        }
      }

      if (Math.abs(1.0d - scale) <= 0.000000000001d) {
        return image;
      } else {
        return scaleImage(image, imageType, scale, scale, true);
      }
    } catch (IOException ex) {
      throw new RuntimeException("Detected error during load: " + fileName, ex);
    }
  }
}
