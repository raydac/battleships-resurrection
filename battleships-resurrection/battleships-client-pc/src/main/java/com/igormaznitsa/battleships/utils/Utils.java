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

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Random;

public final class Utils {

  public static final Random RND = new Random(System.currentTimeMillis());

  private Utils() {

  }

  public static void closeQuietly(final Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (IOException ex) {
      // do nothing
    }
  }

  public static void closeQuietly(final ThrowingSupplier<Closeable> supplier) {
    try {
      final Closeable closeable = supplier.get();
      if (closeable != null) {
        closeable.close();
      }
    } catch (Exception ex) {
      // do nothing
    }
  }

  public static void closeQuietly(final HttpURLConnection closeable) {
    if (closeable != null) {
      closeQuietly(closeable::getInputStream);
      closeQuietly(closeable::getOutputStream);
      closeable.disconnect();
    }
  }

  @SafeVarargs
  public static void setContainerEnabled(final Container container, final boolean enabled, Class<? extends Component>... exclude) {
    container.setEnabled(enabled);
    for (final Component component : container.getComponents()) {
      if (Arrays.stream(exclude).noneMatch(x -> x.isAssignableFrom(component.getClass()))) {
        component.setEnabled(enabled);
        if (component instanceof Container) {
          setContainerEnabled((Container) component, enabled, exclude);
        }
        component.revalidate();
        component.repaint();
      }
    }
    container.revalidate();
    container.repaint();
  }

  public static byte[] readResourceAsBytes(final String resource) throws IOException {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(16384);
    final byte[] byteBuffer = new byte[16384];
    try (final InputStream is = Utils.class.getResourceAsStream(resource)) {
      if (is == null) {
        throw new IOException("Can't find resource: " + resource);
      }
      while (!Thread.currentThread().isInterrupted()) {
        final int read = is.read(byteBuffer);
        if (read < 0) {
          break;
        }
        if (read > 0) {
          buffer.write(byteBuffer, 0, read);
        }
      }
      return buffer.toByteArray();
    }
  }

  @FunctionalInterface
  public interface ThrowingSupplier<T> {
    T get() throws Exception;
  }
}
