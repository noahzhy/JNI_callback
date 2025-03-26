package com.example.testing;

public class Sdk {

    public Sdk() {
    }

    public static class Point {
        public int x;
        public int y;
    }

    private static class SdkHodler {
        static Sdk instance = new Sdk();
    }

    public static Sdk getInstance() {
        return SdkHodler.instance;
    }

    public interface OnSubProgressListener {
        void onProgressChange(Point point);
    };

    protected native void download(String downloadPath, OnSubProgressListener listener);

}