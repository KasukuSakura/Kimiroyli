package io.github.kasukusakura.jvmsecurity.api.util;

import java.util.function.Function;
import java.util.function.IntFunction;

public class ArrayHelper {
    public static <T, R> R[] remap(T[] src, Function<T, R> remapper, IntFunction<R[]> alloc) {
        var rsp = alloc.apply(src.length);
        for (var i = 0; i < src.length; i++) {
            rsp[i] = remapper.apply(src[i]);
        }
        return rsp;
    }
}
