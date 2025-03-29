package com.example.testing;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;

import com.example.testing.Sdk.OnSubProgressListener;
import com.example.testing.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("sdk");
    }

    private Sdk sdk = new Sdk();

    private ActivityMainBinding binding;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressBar = findViewById(R.id.progressBar);

        sdk.download(
            "xx.png",
                new OnSubProgressListener() {
                    @Override
                    public void onProgressChange(Sdk.Point point) {
                        Log.d("MainActivity", "dowload progress: " + point.x + "," + point.y);
                        progressBar.setProgress((int) point.x);
                    }
                }
        );

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
            for (int y = 0; y < height / 2; y++) {
                for (int x = 0; x < width / 2; x++) {
                    int newPos = ((width - x - 1) * height + y * 2) + size;
                    rotated[newPos] = data[size + y * width + x * 2];
                    rotated[newPos + 1] = data[size + y * width + x * 2 + 1];
                }
            }
        }
        return rotated;
    }
}
