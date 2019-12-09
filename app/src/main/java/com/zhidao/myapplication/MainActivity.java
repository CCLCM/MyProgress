package com.zhidao.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;

import com.zhidao.myapplication.view.UpdateProgressBar;
import com.zhidao.myapplication.view.WaveProgress;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    Handler mhandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == 0) {
                System.out.println("chencl_  213131");
                wave_progress_bar.showRightMarkAnimator();
                wave_progress_bar.setDownloadComplete(true);
            }

        }
    };

    private Animation operatingAnim;

    private WaveProgress wave_progress_bar;
    private UpdateProgressBar loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wave_progress_bar = findViewById(R.id.wave_progress_bar);
        loading = findViewById(R.id.loading);


        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);

        loading.startRingAnimator();
        loading.startAnimation(operatingAnim);











        final Random random = new Random();

        findViewById(R.id.wave_progress_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wave_progress_bar.setValue(100);
                mhandler.sendEmptyMessageDelayed(0,3000);
            }
        });

        wave_progress_bar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wave_progress_bar.setValue( (random.nextInt(100 )/100f) * 100);
            }
        });
    }
}
