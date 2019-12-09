package com.limelion.nggyu;

import java.io.OutputStream;

public class NullOutputStream extends OutputStream {

    @Override
    public void write(int b) {
        return;
    }

    @Override
    public void write(byte[] b) {
        return;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        return;
    }
}
