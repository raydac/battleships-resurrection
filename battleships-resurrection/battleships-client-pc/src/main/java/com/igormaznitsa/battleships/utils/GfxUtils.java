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

package com.igormaznitsa.battleships.utils;

import static java.util.Objects.requireNonNull;


import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public final class GfxUtils {
  private static final ImageReader PNG_IMAGE_READER =
      ImageIO.getImageReadersByFormatName("png").next();

  private static final BufferedImage EMPTY_128x128_ARGB =
      new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

  private GfxUtils() {
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
      throw new Error("Detected error during load: " + fileName, ex);
    }
  }

  public static boolean hasGfxImage(final String fileName) {
    return GfxUtils.class.getClassLoader().getResource("assets/gfx/" + fileName) != null;
  }

  public static BufferedImage loadGfxImageAsType(final String fileName, final int imageType) {
    return loadGfxImageAsType(fileName, imageType, 1.0d);
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
        final int scaledWidth = (int) Math.round(image.getWidth() * scale);
        final int scaledHeight = (int) Math.round(image.getHeight() * scale);
        final BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, imageType);
        final Graphics2D gfx = scaled.createGraphics();
        try {
          gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          gfx.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
              RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
          gfx.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
              RenderingHints.VALUE_COLOR_RENDER_QUALITY);
          gfx.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
          gfx.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        } finally {
          gfx.dispose();
        }
        return scaled;
      }
    } catch (IOException ex) {
      throw new Error("Detected error during load: " + fileName, ex);
    }
  }
}
