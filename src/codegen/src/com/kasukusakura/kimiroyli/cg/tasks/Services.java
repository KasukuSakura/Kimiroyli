package com.kasukusakura.kimiroyli.cg.tasks;

import com.kasukusakura.kimiroyli.cg.StrKit;
import com.kasukusakura.kimiroyli.cg.SubTask;

import java.nio.file.Files;
import java.nio.file.Path;

public class Services {
    @SubTask
    public void run() throws Throwable {
        var output = Path.of("src/system-core/src/com/kasukusakura/kimiroyli/core/control/ControlServices.java");
        var services = new String[]{
                "FileAccessControl",
                "SystemControl",
                "NetworkControl",
        };
        try (var writer = Files.newBufferedWriter(output)) {
            writer.write("package com.kasukusakura.kimiroyli.core.control;\n\n");
            for (var s : services) {
                writer.write("import com.kasukusakura.kimiroyli.api.control.");
                writer.write(s);
                writer.write(";\n");
            }
            writer.write("\npublic class ControlServices {\n");
            for (var s : services) {
                writer.write("    public static ");
                writer.write(s);
                writer.write(" ");
                writer.write(StrKit.spscn(s));
                writer.write(";\n");
            }
            writer.write("\n\n");

            writer.write("    public static void reg(Class<?> klass, Object instance) {\n");
            writer.write("        if (instance == null) throw new NullPointerException(\"instance is null\");\n");
            writer.write("        if (klass == null) throw new NullPointerException(\"Can't register a null service.\");\n");
            writer.write("        klass.cast(instance);\n");
            for (var s : services) {
                writer.write("        if (klass == ");
                writer.write(s);
                writer.write(".class) {\n");
                writer.write("            ");
                writer.write(StrKit.spscn(s));
                writer.write(" = (");
                writer.write(s);
                writer.write(") instance;\n");
                writer.write("            return;\n");
                writer.write("        }\n");
            }
            writer.write("        throw new IllegalArgumentException(\"No matched service can be register: \" + klass);\n");
            writer.write("    }\n\n");

            writer.write("    public static Object get(Class<?> klass) {\n");
            for (var s : services) {
                writer.write("        if (klass == ");
                writer.write(s);
                writer.write(".class) {\n");
                writer.write("            return ");
                writer.write(StrKit.spscn(s));
                writer.write(";\n");
                writer.write("        }\n");
            }
            writer.write("        throw new IllegalArgumentException(\"No matched service can be found: \" + klass);\n");
            writer.write("    }\n\n");

            writer.write("}\n");
        }
    }
}
