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
}