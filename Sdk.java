package com.example.testing;

public class Sdk {

    public Sdk() {
    }

    // 单例
    private static class SdkHodler {
        static Sdk instance = new Sdk();
    }

    public static Sdk getInstance() {
        return SdkHodler.instance;
    }

    // 回调到各个线程
    public interface OnSubProgressListener {
        public int onProgressChange(long already, long total);
    };

    // 调到C层的方法
    protected native void download(String downloadPath, OnSubProgressListener listener);

}