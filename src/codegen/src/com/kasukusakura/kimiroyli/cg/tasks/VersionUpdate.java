package com.kasukusakura.kimiroyli.cg.tasks;

import com.kasukusakura.kimiroyli.cg.SubTask;

import java.io.File;
import java.nio.file.Files;
import java.util.regex.Pattern;

@SuppressWarnings("RegExpRedundantEscape")
public class VersionUpdate {
    @SubTask
    public void run() throws Throwable {
        System.out.println("!");
        var file = new File("src/kimiroyli-api/src/com/kasukusakura/kimiroyli/api/Kimiroyli.java");
        System.out.println(file.isFile());
        var ptr = Pattern.compile("\\/\\*\\s*Kimiroyli:VERSION\\s*\\*\\/.+?;");
        Files.writeString(
                file.toPath(),
                ptr.matcher(Files.readString(file.toPath()))
                        .replaceAll("/*Kimiroyli:VERSION*/ \"" + System.getenv("PROJ_VER") + "\";")
        );
    }
}
