package com.igormaznitsa.battleships.utils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public final class NetUtils {
  private NetUtils() {

  }

  public static int readData(final SocketChannel socketChannel, final byte[] buffer) throws IOException {
    final ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
    byteBuffer.clear();
    int offset = 0;
    while (offset < buffer.length) {
      int read = socketChannel.read(byteBuffer);
      if (read < 0) break;
      offset += read;
    }
    byteBuffer.flip();
    return byteBuffer.limit();
  }

  public static Optional<NamedInterfaceAddress> findLanInterface(final Collection<NamedInterfaceAddress> interfaceSet) {
    NamedInterfaceAddress result = null;

    for (final NamedInterfaceAddress i : interfaceSet) {
      final String name = i.getName().toLowerCase(Locale.ENGLISH).trim();
      if (name.contains("(e") || name.contains("(w") || name.contains("(b")) {
        result = i;
      }
    }

    return Optional.ofNullable(result);
  }

  public static String removeInterfaceNameIfFound(final String hostName) {
    if (hostName.startsWith("(")) {
      int closing = hostName.indexOf(") ", 1);
      if (closing >= 0) {
        return hostName.substring(closing + 1).trim();
      }
    }
    return hostName;
  }

  public static List<NamedInterfaceAddress> findAllIp4NetworkInterfacesWithBroadcast() {
    try {
      return NetworkInterface.networkInterfaces()
              .flatMap(x -> x.getInterfaceAddresses().stream().map(p -> Pair.of(x, p)))
              .filter(x -> (x.getRight().getAddress().isLoopbackAddress() && x.getRight().getAddress() instanceof Inet4Address) || (x.getRight().getBroadcast() != null && x.getRight().getAddress() instanceof Inet4Address))
              .map(x -> new NamedInterfaceAddress(String.format("(%s) %s", x.getLeft().getName(), x.getRight().getAddress().getHostName()), x.getRight()))
              .sorted()
              .collect(Collectors.toUnmodifiableList());
    } catch (Exception ex) {
      try {
        final InterfaceAddress loopback = NetworkInterface.networkInterfaces()
                .filter(x -> {
                  try {
                    return x.isLoopback();
                  } catch (Exception xxx) {
                    return false;
                  }
                })
                .flatMap(x -> x.getInterfaceAddresses().stream())
                .filter(x -> x.getAddress() instanceof Inet4Address)
                .findFirst().orElseThrow(() -> new Error("Can't find loopback interface"));
        return List.of(new NamedInterfaceAddress(loopback.getAddress().getHostName(), loopback));
      } catch (Exception exx) {
        throw new RuntimeException("Unexpectedly can't find localhost", exx);
      }
    }
  }

  public static String makeNetworkUid() {
    Optional<String> userName = Optional.ofNullable(System.getProperty("user.name", null));
    Optional<String> hostName = Optional.empty();
    try {
      hostName = Optional.ofNullable(InetAddress.getLocalHost().getHostName());
    } catch (Exception ex) {
      // do nothing
    }
    return String.format("%s::%s", userName.orElseGet(() -> "UnknownName." + Long.toHexString(System.currentTimeMillis()).toUpperCase(Locale.ENGLISH)),
            hostName.orElseGet(() -> "UnknownHost" + Long.toHexString(System.currentTimeMillis()).toUpperCase(Locale.ENGLISH)));
  }

  public static final class NamedInterfaceAddress implements Comparable<NamedInterfaceAddress> {
    private final String name;
    private final InterfaceAddress address;

    private NamedInterfaceAddress(final String name, final InterfaceAddress address) {
      this.name = name;
      this.address = address;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == null) return false;
      if (this == obj) return true;
      if (obj instanceof NamedInterfaceAddress) {
        final NamedInterfaceAddress that = (NamedInterfaceAddress) obj;
        return this.name.equals(that.name) && this.address.equals(that.address);
      }
      return false;
    }

    public String getName() {
      return this.name;
    }

    public InterfaceAddress getInterfaceAddress() {
      return this.address;
    }

    @Override
    public String toString() {
      return this.getName();
    }

    @Override
    public int compareTo(final NamedInterfaceAddress that) {
      if (this.name.contains("127.0.0.1") || this.name.contains("localhost")) return -1;
      if (that.name.contains("127.0.0.1") || that.name.contains("localhost")) return 1;
      return this.name.compareTo(that.name);
    }
  }


}
