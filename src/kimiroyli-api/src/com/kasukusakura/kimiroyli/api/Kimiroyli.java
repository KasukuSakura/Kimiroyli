package com.kasukusakura.kimiroyli.api;

import com.kasukusakura.kimiroyli.api.internal.ImplBridge;

public class Kimiroyli {
    public static final String VERSION_CONSTANT = /*Kimiroyli:VERSION*/ "0.0.1";

    public static String getVersion() {
        return VERSION_CONSTANT;
    }

    public static <T> void registerControlService(Class<T> type, T instance) {
        ImplBridge.INSTANCE.regCtrService(type, instance);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getControlService(Class<T> type) {
        return (T) ImplBridge.INSTANCE.getService(type);
    }
}
