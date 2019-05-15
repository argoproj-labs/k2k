package com.intuit.dev.patterns.ks2kust;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

class StreamGobbler extends Thread {
    InputStream is;
    List<String> buffer;

    StreamGobbler(InputStream is) {
        this.is = is;
        this.buffer = new LinkedList<>();
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                buffer.add(line);
                System.out.println(line);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public List<String> getBuffer() throws InterruptedException {
        this.join(0);
        return buffer;
    }
}