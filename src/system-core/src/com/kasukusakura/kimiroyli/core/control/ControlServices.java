package com.kasukusakura.kimiroyli.core.control;

import com.kasukusakura.kimiroyli.api.control.FileAccessControl;
import com.kasukusakura.kimiroyli.api.control.SystemControl;
import com.kasukusakura.kimiroyli.api.control.NetworkControl;

public class ControlServices {
    public static FileAccessControl FILE_ACCESS_CONTROL;
    public static SystemControl SYSTEM_CONTROL;
    public static NetworkControl NETWORK_CONTROL;


    public static void reg(Class<?> klass, Object instance) {
        if (instance == null) throw new NullPointerException("instance is null");
        if (klass == null) throw new NullPointerException("Can't register a null service.");
        klass.cast(instance);
        if (klass == FileAccessControl.class) {
            FILE_ACCESS_CONTROL = (FileAccessControl) instance;
            return;
        }
        if (klass == SystemControl.class) {
            SYSTEM_CONTROL = (SystemControl) instance;
            return;
        }
        if (klass == NetworkControl.class) {
            NETWORK_CONTROL = (NetworkControl) instance;
            return;
        }
        throw new IllegalArgumentException("No matched service can be register: " + klass);
    }

    public static Object get(Class<?> klass) {
        if (klass == FileAccessControl.class) {
            return FILE_ACCESS_CONTROL;
        }
        if (klass == SystemControl.class) {
            return SYSTEM_CONTROL;
        }
        if (klass == NetworkControl.class) {
            return NETWORK_CONTROL;
        }
        throw new IllegalArgumentException("No matched service can be found: " + klass);
    }

}
