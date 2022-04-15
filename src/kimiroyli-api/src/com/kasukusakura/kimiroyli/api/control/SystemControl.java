package com.kasukusakura.kimiroyli.api.control;

public abstract class SystemControl {
    public void onNewThread() {
    }

    public void onNewClassLoader() {
    }

    public void onThreadGroup_checkAccess(ThreadGroup thiz) {
    }

    public void onNativeLink(Class<?> caller, boolean isLoadLibrary, String lib) {
    }

    public void onInitiativeShutdown(int code, boolean isHalt) {
    }
}
