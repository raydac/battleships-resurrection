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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public final class Utils {

  public static final Random RND = new Random(System.currentTimeMillis());

  public static void closeQuietly(final Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (IOException ex) {
      // do nothing
    }
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

  private Utils() {

  }
}
