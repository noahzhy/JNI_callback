package com.example.testing;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.testing.Sdk.OnSubProgressListener;
import com.example.testing.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("sdk");
    }

    private Sdk sdk = new Sdk();
    
    private ActivityMainBinding binding;
    private ProgressBar progressBar;
    private ImageView iv_img;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressBar = findViewById(R.id.progressBar);

        sdk.download(
            "xx.png",
                point -> {
                    Log.d("MainActivity", "dowload progress: " + point.x + "," + point.y);
                    progressBar.setProgress((int) point.x);
                }
        );

        iv_img = findViewById(R.id.iv_img);
        BitmapDrawable bd = (BitmapDrawable) iv_img.getDrawable();
        Bitmap b = bd.getBitmap();
        byte[] nv21 = BitmapToNV21(b);

        int width = b.getWidth();
        int height = b.getHeight();
        int rotation = 270;
        byte[] bytes = rotateNV21(nv21, width, height, rotation);

        FileOutputStream fos = null;
        try {
            File file = new File(getExternalFilesDir(null), "test.jpg");
            String filePath = file.getAbsolutePath();
            Log.d("test_path", filePath);
            fos = new FileOutputStream(file);
            YuvImage yuvImage = new YuvImage(bytes, ImageFormat.NV21, height, width, null);
            yuvImage.compressToJpeg(new Rect(0, 0, height, width), 100, fos);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                assert fos != null;
                fos.close();
            } catch (IOException e) {
                Log.e("error", e.toString());
            }
        }
    }

    public static byte[] BitmapToNV21(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] argb = new int[width * height];
        bitmap.getPixels(argb, 0, width, 0, 0, width, height);
        byte[] yuv = new byte[width * height * 3 / 2];
        encodeYUV420SP(yuv, argb, width, height);
//        bitmap.recycle();
        return yuv;
    }

    private static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        int R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff);
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;
                yuv420sp[yIndex++] = (byte) Math.min(Y, 255);
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) Math.min(V, 255);
                    yuv420sp[uvIndex++] = (byte) Math.min(U, 255);
                }
                index++;
            }
        }
    }

    public static byte[] YUV420_888ToNV21(Image image, int rotation) {
        assert (image.getFormat() == ImageFormat.YUV_420_888);

        final int width = image.getWidth();
        final int height = image.getHeight();
        final int size = width * height;
        byte[] nv21 = new byte[size + size / 2];

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        int rowStride = planes[0].getRowStride();
        int pixelStride = planes[1].getPixelStride();

        // Copy Y data
        for (int i = 0; i < height; i++) {
            yBuffer.position(i * rowStride);
            yBuffer.get(nv21, i * width, width);
        }

        // Copy UV data
        int uvHeight = height / 2;
        int offset = size;
        for (int i = 0; i < uvHeight; i++) {
            for (int j = 0; j < width / 2; j++) {
                int index = i * rowStride / 2 + j * pixelStride;
                nv21[offset++] = vBuffer.get(index);
                nv21[offset++] = uBuffer.get(index);
            }
        }

        return rotateNV21(nv21, width, height, rotation);
    }

    private static byte[] rotateNV21(byte[] data, int width, int height, int rotation) {
        if (rotation == 0) return data;

        byte[] rotated = new byte[data.length];
        int size = width * height;
        int uvSize = size / 2;

        if (rotation == 90) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    rotated[x * height + (height - y - 1)] = data[y * width + x];
                }
            }
            for (int y = 0; y < height / 2; y++) {
                for (int x = 0; x < width / 2; x++) {
                    int newPos = (x * height + (height - y * 2 - 2)) + size;
                    rotated[newPos] = data[size + y * width + x * 2];
                    rotated[newPos + 1] = data[size + y * width + x * 2 + 1];
                }
            }
        } else if (rotation == 180) {
            for (int i = 0; i < size; i++) {
                rotated[size - 1 - i] = data[i];
            }
            for (int i = 0; i < uvSize; i += 2) {
                rotated[size + uvSize - 2 - i] = data[size + i];
                rotated[size + uvSize - 1 - i] = data[size + i + 1];
            }
        } else if (rotation == 270) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    rotated[(width - x - 1) * height + y] = data[y * width + x];
                }
            }
            int i = size;
            for (int x = width - 1; x >= 0; x -= 2) {
                for (int y = 0; y < height / 2; y++) {
                    int newPos = size + y * width + x;
                    rotated[i] = data[newPos - 1];
                    rotated[i + 1] = data[newPos];
                    i += 2;
                }
            }
        }
        return rotated;
    }
}
