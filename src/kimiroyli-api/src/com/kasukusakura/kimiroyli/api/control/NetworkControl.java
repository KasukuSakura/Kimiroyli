package com.kasukusakura.kimiroyli.api.control;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class NetworkControl {
    public void onTcpConnect(
            InetAddress address, int port
    ) throws IOException {
    }

    public void onTcpBind(
            InetAddress address, int port
    ) throws IOException {
    }

    public void onUdpBind(SocketAddress local) throws IOException {
    }

    public void onUdpConnect(InetSocketAddress address) throws IOException {
    }
}
