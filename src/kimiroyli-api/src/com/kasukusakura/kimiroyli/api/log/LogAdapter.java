package com.kasukusakura.kimiroyli.api.log;

public abstract class LogAdapter {
    private static LogAdapter SYSTEM;

    static LogAdapter system() {
        var rsp = SYSTEM;
        if (rsp == null) throw new IllegalStateException("System LogAdapter not initialized.");
        return rsp;
    }

    public static void setAdapter(LogAdapter adapter) {
        synchronized (LogAdapter.class) {
            var sys = SYSTEM;
            if (sys != null) {
                sys.canOverride(adapter);
            }
            SYSTEM = adapter;
        }
    }

    protected Logger getLogger(String name) {
        return new Logger(name, this);
    }

    protected boolean isInfoEnabled(Logger logger) {
        return true;
    }

    protected boolean isErrorEnabled(Logger logger) {
        return true;
    }

    protected boolean isDebugEnabled(Logger logger) {
        return true;
    }

    protected boolean isWarnEnabled(Logger logger) {
        return true;
    }

    protected abstract void info(Logger logger, Object msg, Throwable err, Object[] args);

    protected abstract void error(Logger logger, Object msg, Throwable err, Object[] args);

    protected abstract void debug(Logger logger, Object msg, Throwable err, Object[] args);

    protected abstract void warn(Logger logger, Object msg, Throwable err, Object[] args);

    protected void canOverride(LogAdapter newAdapter) {
    }

    protected final void setName(Logger logger, String name) {
        if (logger.adapter == this) {
            logger.name = name;
        }
    }

    protected final void setAdapter(Logger logger, LogAdapter adapter) {
        if (adapter == null) throw new NullPointerException("adapter");
        if (logger.adapter == this) {
            logger.adapter = adapter;
        }
    }
}
