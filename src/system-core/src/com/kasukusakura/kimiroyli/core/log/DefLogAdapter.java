package com.kasukusakura.kimiroyli.core.log;

import com.kasukusakura.kimiroyli.api.log.LogAdapter;
import com.kasukusakura.kimiroyli.api.log.Logger;
import com.kasukusakura.kimiroyli.api.utils.StringFormatable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DefLogAdapter extends LogAdapter {
    protected static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm:ss"
    );

    static {
        FORMATTER.format(ZonedDateTime.now());
        FORMATTER.formatTo(ZonedDateTime.now(), new StringBuilder());
    }


    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    protected void log(String level, Logger logger, Object msg, Throwable err, Object[] args) {
        final var out = System.out;
        synchronized (out) {
            FORMATTER.formatTo(
                    ZonedDateTime.now(), out
            );
            out.append(" ");
            out.append("[").append(logger.getName()).append("] ").append(level).append(" ");
            if (msg != null) {
                var oot = new StringBuilder();
                if (args != null && args.length > 0) {
                    StringFormatable.formatTo(
                            StringFormatable.toStringAs(msg),
                            oot, args
                    );
                } else {
                    StringFormatable.toStringTo(msg, oot);
                }
                out.append(oot);
            }
            if (err != null) {
                out.println();
                err.printStackTrace(out);
            }
            out.println();
        }
    }

    @Override
    protected void info(Logger logger, Object msg, Throwable err, Object[] args) {
        log("INFO ", logger, msg, err, args);
    }

    @Override
    protected void error(Logger logger, Object msg, Throwable err, Object[] args) {
        log("ERROR", logger, msg, err, args);
    }

    @Override
    protected void debug(Logger logger, Object msg, Throwable err, Object[] args) {
        log("DEBUG", logger, msg, err, args);
    }

    @Override
    protected void warn(Logger logger, Object msg, Throwable err, Object[] args) {
        log("WARN ", logger, msg, err, args);
    }
}
