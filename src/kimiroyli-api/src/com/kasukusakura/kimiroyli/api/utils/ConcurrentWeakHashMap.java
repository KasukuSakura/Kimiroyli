package com.kasukusakura.kimiroyli.api.utils;

import com.kasukusakura.kimiroyli.api.internal.Threads;

import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentWeakHashMap<K, V> {
    private static final Cleaner CLEANER = Cleaner.create(r -> {
        var t = new Thread(Threads.JVM_SECURITY, r, "Concurrent Weak Hash Map Cleaner");
        t.setDaemon(true);
        return t;
    });
    private static final Object PLACEHOLDER_NULL = new Object();

    static class HardKey<K> {
        final K key;
        final int hash;

        private HardKey(K key) {
            this.key = key;
            hash = Objects.hashCode(key);
        }

        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;

            if (o instanceof HardKey) {
                var hk = (HardKey<?>) o;
                if (key == hk.key) return true;
                if (hash != hk.hash) return false;
                return Objects.equals(key, hk.key);
            }
            if (o instanceof WeakKey) {
                var hk = (WeakKey<?>) o;
                var hkk = hk.key.get();
                if (hkk == null) return false;
                return hkk.equals(key);
            }
            return false;
        }

        @Override
        public String toString() {
            return String.valueOf(key);
        }
    }

    static class WeakKey<K> {
        final WeakReference<K> key;
        final int hash;

        public int hashCode() {
            return hash;
        }

        private WeakKey(K key) {
            Objects.requireNonNull(key);

            this.key = new WeakReference<>(key);
            hash = Objects.hashCode(key);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o == null) return false;
            if (o instanceof HardKey) {
                return o.equals(this);
            }
            if (o instanceof WeakKey) {
                var key0 = this.key.get();
                var hk = (WeakKey<?>) o;
                var key1 = hk.key.get();

                if (key0 == null || key1 == null) return false;

                return Objects.equals(key0, key1);
            }
            return false;
        }

        @Override
        public String toString() {
            var k = this.key.get();
            if (k == null) return "<released key@" + hash + ">";
            return String.valueOf(k);
        }
    }

    private final ConcurrentHashMap<Object, V> backend = new ConcurrentHashMap<>();

    public V get(K k) {
        if (k == null) {
            return backend.get(PLACEHOLDER_NULL);
        }
        return backend.get(new HardKey<>(k));
    }

    public V put(K k, V value) {
        var key = k == null ? PLACEHOLDER_NULL : new WeakKey<>(k);
        if (k != null) {
            CLEANER.register(k, () -> backend.remove(key));
        }
        return backend.put(key, value);
    }

    public boolean containsKey(Object key) {
        key = key == null ? PLACEHOLDER_NULL : new HardKey<>(key);
        return backend.containsKey(key);
    }

    public V remove(K key) {
        var k = key == null ? PLACEHOLDER_NULL : new HardKey<>(key);
        return backend.remove(k);
    }

    public boolean remove(K key, V value) {
        var k = key == null ? PLACEHOLDER_NULL : new HardKey<>(key);
        return backend.remove(k, value);
    }

    public void clear() {
        backend.clear();
    }

    @Deprecated
    public ConcurrentHashMap<Object, V> backend() {
        return backend;
    }
}
