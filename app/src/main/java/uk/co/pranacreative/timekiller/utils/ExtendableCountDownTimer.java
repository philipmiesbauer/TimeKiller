package uk.co.pranacreative.timekiller.utils;

import android.os.CountDownTimer;
import android.util.Log;

/**
 * Created by Philip Miesbauer on 11/12/2017.
 */

public abstract class ExtendableCountDownTimer {

    private static final String TAG = ExtendableCountDownTimer.class.getSimpleName();

    private long remainingTime;
    private final long countDownInterval;
    private CountDownTimer timer;

    public ExtendableCountDownTimer(long millisInFuture, long countDownInterval) {
        timer = new CountDownTimer(millisInFuture, countDownInterval) {
            @Override
            public void onTick(long l) {
                remainingTime = l;
                onTimerTick(l);
                Log.d(TAG, ("Ticked: " + l + "ms left"));
            }

            @Override
            public void onFinish() {
                onTimerFinish();
                Log.d(TAG, ("Timer finished"));
            }
        };
        remainingTime = millisInFuture;
        this.countDownInterval = countDownInterval;
    }

    public void addMillis(long millisToAdd) {
        timer.cancel();
        timer = new CountDownTimer(remainingTime + millisToAdd, countDownInterval) {
            @Override
            public void onTick(long l) {
                remainingTime = l;
                onTimerTick(l);
                Log.d(TAG, ("Ticked: " + l + "ms left"));
            }

            @Override
            public void onFinish() {
                onTimerFinish();
                Log.d(TAG, ("Timer finished"));
            }
        }.start();
    }

    public void start() {
        timer.start();
        Log.d(TAG, "Timer started");
    }

    public abstract void onTimerTick(long l);

    public abstract void onTimerFinish();
}
