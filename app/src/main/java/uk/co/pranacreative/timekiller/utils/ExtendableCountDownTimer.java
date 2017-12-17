package uk.co.pranacreative.timekiller.utils;

import android.os.CountDownTimer;

/**
 * Created by Philip Miesbauer on 11/12/2017.
 */

public abstract class ExtendableCountDownTimer {

    private long remainingTime;
    private final long countDownInterval;
    private CountDownTimer timer;

    public ExtendableCountDownTimer(long millisInFuture, long countDownInterval) {
        timer = new CountDownTimer(millisInFuture, countDownInterval) {
            @Override
            public void onTick(long l) {
                remainingTime = l;
                onTimerTick(l);
            }

            @Override
            public void onFinish() {
                onTimerFinish();
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
            }

            @Override
            public void onFinish() {
                onTimerFinish();
            }
        };
    }

    public void start() {
        timer.start();
    }

    public abstract void onTimerTick(long l);

    public abstract void onTimerFinish();
}
