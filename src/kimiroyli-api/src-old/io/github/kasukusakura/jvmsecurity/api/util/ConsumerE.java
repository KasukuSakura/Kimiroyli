package io.github.kasukusakura.jvmsecurity.api.util;

public interface ConsumerE<T> {
    void accept(T v) throws Exception;
}
