package com.kasukusakura.kimiroyli.cg;

import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CPX {
    public static void run() throws Throwable {
        var classes = System.getenv("TST_CP");
        if (classes == null) throw new NoSuchElementException("Class path not found: env[TST_CP]");
        var directory = new File(classes);
        try (var stream = Files.walk(directory.toPath())) {
            File path;
            //noinspection ConstantConditions
            for (Iterator<Path> iterator = stream.iterator(); iterator.hasNext() && (path = iterator.next().toFile()) != null; ) {
                if (!path.isFile()) continue;
                if (path.getName().endsWith(".class")) {
                    String name;
                    try (var fi = new FileInputStream(path)) {
                        name = new ClassReader(fi).getClassName();
                    }
                    var klass = Class.forName(name.replace('/', '.'));
                    for (var func : klass.getDeclaredMethods()) {
                        if (func.isSynthetic() || func.isBridge()) continue;

                        var st = func.getDeclaredAnnotation(SubTask.class);
                        if (st == null) continue;
                        var tskName = st.name();
                        if (tskName.isBlank()) {
                            tskName = klass.getSimpleName() + " -> " + func.getName();
                        }
                        System.out.println("=====================================================");
                        System.out.println("Task: " + tskName);
                        System.out.println();

                        @SuppressWarnings("deprecation")
                        var instance = Modifier.isStatic(func.getModifiers()) ? null : klass.newInstance();

                        if (!Modifier.isPublic(func.getModifiers())) {
                            func.setAccessible(true);
                        }
                        try {
                            func.invoke(instance);
                        } catch (InvocationTargetException e) {
                            throw e.getTargetException();
                        }
                        System.out.println();
                    }
                }
            }
        }
    }
}
