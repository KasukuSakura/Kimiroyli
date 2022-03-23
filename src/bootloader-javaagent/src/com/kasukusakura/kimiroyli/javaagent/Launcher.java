package com.kasukusakura.kimiroyli.javaagent;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Launcher {

    public static void premain(String opts, Instrumentation instrumentation) throws Throwable {
        var ccl = Launcher.class.getClassLoader();
        var bootloaderClasses = new HashMap<String, byte[]>();
        try (var bootloader = new ZipInputStream(new BufferedInputStream(
                Objects.requireNonNull(ccl.getResourceAsStream("com/kasukusakura/kimiroyli/boot/core/bootloader.jar"))
        ))) {
            ZipEntry entry;
            while ((entry = bootloader.getNextEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    var cpn = entry.getName();
                    cpn = cpn.substring(0, cpn.length() - 6);
                    if (cpn.startsWith("/")) cpn = cpn.substring(1);
                    bootloaderClasses.put(cpn, bootloader.readAllBytes());
                }
            }
        }

        var classloader = new ClassLoader(ClassLoader.getPlatformClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                var cb = bootloaderClasses.get(name.replace('.', '/'));
                if (cb != null) return defineClass(name, cb, 0, cb.length, null);
                return super.findClass(name);
            }
        };
        var moduleDescriptorBuilder = ModuleDescriptor.newModule("kimiroyli.boot");

        moduleDescriptorBuilder.packages(bootloaderClasses.keySet().stream().map(it -> {
            var idx = it.lastIndexOf('/');
            return it.substring(0, idx).replace('/', '.');
        }).collect(Collectors.toSet()));
        moduleDescriptorBuilder.exports("com.kasukusakura.kimiroyli.boot");
        moduleDescriptorBuilder.requires("java.instrument");

        var ref = new ModuleReference(moduleDescriptorBuilder.build(), null) {
            @Override
            public ModuleReader open() throws IOException {
                throw new IOException();
            }
        };
        var cusModuleFinder = new ModuleFinder() {
            @Override
            public Optional<ModuleReference> find(String name) {
                if ("kimiroyli.boot".equals(name)) return Optional.of(ref);
                return Optional.empty();
            }

            @Override
            public Set<ModuleReference> findAll() {
                return null;
            }
        };
        var bootLayer = ModuleLayer.boot();
        var cusConf = bootLayer.configuration()
                .resolve(cusModuleFinder, ModuleFinder.ofSystem(), List.of("kimiroyli.boot"));
        bootLayer.defineModules(cusConf, $ -> classloader);

        var lookup = MethodHandles.lookup();
        lookup.findStatic(
                Class.forName("com.kasukusakura.kimiroyli.boot.BootLoader", true, classloader),
                "premain", MethodType.methodType(void.class, String.class, Instrumentation.class)
        ).invokeExact(opts, instrumentation);
    }
}
