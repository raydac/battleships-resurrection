package com.igormaznitsa.battleships.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public final class NetUtils {
  private NetUtils() {

  }

  public static List<NamedInetAddress> findAllNetworkInterfaces() {
    try {
      return NetworkInterface.networkInterfaces()
              .flatMap(NetworkInterface::inetAddresses)
              .filter(x -> x instanceof Inet4Address)
              .map(x -> new NamedInetAddress(x.getHostName(), x))
              .sorted()
              .collect(Collectors.toUnmodifiableList());
    } catch (Exception ex) {
      try {
        return List.of(new NamedInetAddress("localhost", InetAddress.getLocalHost()));
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

  public static final class NamedInetAddress implements Comparable<NamedInetAddress> {
    private final String name;
    private final InetAddress address;

    private NamedInetAddress(final String name, final InetAddress address) {
      this.name = name;
      this.address = address;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == null) return false;
      if (this == obj) return true;
      if (obj instanceof NamedInetAddress) {
        final NamedInetAddress that = (NamedInetAddress) obj;
        return this.name.equals(that.name) && this.address.equals(that.address);
      }
      return false;
    }

    public String getName() {
      return this.name;
    }

    public InetAddress getAddress() {
      return this.address;
    }

    @Override
    public String toString() {
      return String.format("%s (%s)", this.name, this.address);
    }

    @Override
    public int compareTo(final NamedInetAddress that) {
      return this.address.isLoopbackAddress() ? -1 : this.name.compareTo(that.name);
    }
  }


}
