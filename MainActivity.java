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

}