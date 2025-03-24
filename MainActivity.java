package com.example.testing;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.testing.Sdk.OnSubProgressListener;
import com.example.testing.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("sdk");
    }

    private Sdk sdk;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());

        Sdk.getInstance().download("xx.png", new OnSubProgressListener() {
            @Override
            public int onProgressChange(long already, long total) {
                Log.d("MainActivity", "dowload progress: " + already + "/" + total);
                return 0;
            }
        });

    }

    public native String stringFromJNI();
    public static byte[] BitmapToI420(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int ySize = width * height;
        int uvSize = ySize / 4;
        byte[] yuv = new byte[ySize + 2 * uvSize];

        int[] argb = new int[width * height];
        bitmap.getPixels(argb, 0, width, 0, 0, width, height);

        int yIndex = 0;
        int uIndex = ySize;
        int vIndex = ySize + uvSize;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int argbColor = argb[j * width + i];
                int r = (argbColor >> 16) & 0xFF;
                int g = (argbColor >> 8) & 0xFF;
                int b = (argbColor) & 0xFF;

                // RGB 转 YUV
                int y = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                int u = (int) (-0.169 * r - 0.331 * g + 0.5 * b + 128);
                int v = (int) (0.5 * r - 0.419 * g - 0.081 * b + 128);

                // 限制范围
                y = Math.max(0, Math.min(255, y));
                u = Math.max(0, Math.min(255, u));
                v = Math.max(0, Math.min(255, v));

                // 存储 Y 分量
                yuv[yIndex++] = (byte) y;

                // 存储 U 和 V 分量 (只存储 2x2 块的左上角像素)
                if (j % 2 == 0 && i % 2 == 0) {
                    yuv[uIndex++] = (byte) u;
                    yuv[vIndex++] = (byte) v;
                }
            }
        }
        return yuv;
    }

}
