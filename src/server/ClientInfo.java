package server;

import java.net.InetAddress;
import java.util.Objects;

public class ClientInfo {
    private final InetAddress address;
    private final int port;
    private final String name;

    public ClientInfo(InetAddress address, int port, String name) {
        this.address = address;
        this.port = port;
        this.name = name;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientInfo that = (ClientInfo) o;
        return port == that.port && address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }
}
