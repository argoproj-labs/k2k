package com.intuit.dev.patterns.ks2kust;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

class FilePrintStream extends PrintStream {
    public FilePrintStream(File file) throws IOException {
        super(new FileOutputStream(file));
    }
}