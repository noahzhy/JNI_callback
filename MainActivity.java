package com.example.testing;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.example.testing.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'testing' library on application startup.
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

        sdk = new Sdk();

        sdk.nativeDownload(
                "xxx.jpg",
                new Sdk.OnSubProgressListener() {
                    @Override
                    public int onProgressChange(long total, long already) {
                        return 0;
                    }
                }
        );

    }

    /**
     * A native method that is implemented by the 'testing' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
//    public native void callbackFromJNI();

}