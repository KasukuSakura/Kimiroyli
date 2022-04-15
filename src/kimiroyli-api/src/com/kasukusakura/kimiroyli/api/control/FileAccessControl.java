package com.kasukusakura.kimiroyli.api.control;

import java.io.IOException;

public abstract class FileAccessControl {
    public void onFileRead(Object file) throws IOException {
    }

    public void onFileWrite(Object file) throws IOException {
    }

    public void onNewRandomAccessFile(Object file, String mode) throws IOException {
    }
}
