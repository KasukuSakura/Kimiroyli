package com.kasukusakura.kimiroyli.core;

import java.net.Inet6Address;
import java.net.InetAddress;

public class MiscKit {
    public static Object ipv6format(InetAddress address) {
        if (address instanceof Inet6Address) {
            var str = address.toString();
            var split = str.indexOf('/');
            return str.substring(0, split + 1) + "[" + str.substring(split + 1) + "]";
        }
        return address;
    }
}
