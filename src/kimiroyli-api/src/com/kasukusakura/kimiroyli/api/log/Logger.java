package com.kasukusakura.kimiroyli.api.log;

public class Logger {
    protected String name;
    protected LogAdapter adapter;

    public static Logger getLogger(String name) {
        return LogAdapter.system().getLogger(name);
    }

    public Logger(String name, LogAdapter adapter) {
        this.name = name;
        this.adapter = adapter;
    }

    public String getName() {
        return name;
    }

    public void info(Object msg) {
        adapter.info(this, msg, null, null);
    }

    public void info(Object msg, Throwable err) {
        adapter.info(this, msg, err, null);
    }

    public void info(Object msg, Object... args) {
        adapter.info(this, msg, null, args);
    }


    public void error(Object msg) {
        adapter.error(this, msg, null, null);
    }

    public void error(Object msg, Throwable err) {
        adapter.error(this, msg, err, null);
    }

    public void error(Object msg, Object... args) {
        adapter.error(this, msg, null, args);
    }


    public void debug(Object msg) {
        adapter.debug(this, msg, null, null);
    }

    public void debug(Object msg, Throwable err) {
        adapter.debug(this, msg, err, null);
    }

    public void debug(Object msg, Object... args) {
        adapter.debug(this, msg, null, args);
    }


    public void warn(Object msg) {
        adapter.warn(this, msg, null, null);
    }

    public void warn(Object msg, Throwable err) {
        adapter.warn(this, msg, err, null);
    }

    public void warn(Object msg, Object... args) {
        adapter.warn(this, msg, null, args);
    }

    public boolean isInfoEnabled() {
        return adapter.isInfoEnabled(this);
    }

    public boolean isErrorEnabled() {
        return adapter.isErrorEnabled(this);
    }

    public boolean isWarnEnabled() {
        return adapter.isWarnEnabled(this);
    }

    public boolean isDebugEnabled() {
        return adapter.isDebugEnabled(this);
    }
}
