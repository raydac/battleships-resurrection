package com.igormaznitsa.battleships.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class NetUtils {
  private NetUtils() {

  }

  public static List<NamedInterfaceAddress> findAllNetworkInterfaces() {
    try {
      final InetAddress loopback = InetAddress.getLoopbackAddress();

      var allInterfaces = NetworkInterface.networkInterfaces()
              .flatMap(x -> x.getInterfaceAddresses().stream())
              .filter(x -> !loopback.equals(x.getAddress()))
              .filter(x -> x.getBroadcast() != null && x.getAddress() instanceof Inet4Address)
              .map(x -> new NamedInterfaceAddress(x.getAddress().getHostName(), x))
              .sorted()
              .collect(Collectors.toUnmodifiableList());
      return Stream.concat(NetworkInterface.networkInterfaces()
                              .flatMap(x -> x.getInterfaceAddresses().stream())
                              .filter(x -> loopback.equals(x.getAddress()))
                              .findFirst().map(x -> new NamedInterfaceAddress("localhost", x)).stream(),
                      allInterfaces.stream())
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
        throw new Error("Unexpectedly can't find localhost", exx);
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
      return this.address.getAddress().isLoopbackAddress() ? -1 : this.name.compareTo(that.name);
    }
  }


}
