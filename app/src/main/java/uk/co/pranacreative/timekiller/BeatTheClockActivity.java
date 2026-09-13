package uk.co.pranacreative.timekiller;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;

import com.google.android.gms.games.PlayGames;

import uk.co.pranacreative.timekiller.utils.ExtendableCountDownTimer;


/**
 * Beat The Clock game mode activity.
 */
public class BeatTheClockActivity extends TimeKillerActivity {

    private static final String TAG = BeatTheClockActivity.class.getSimpleName();

    // Preferences
    private static final String HIGHSCORE_BEAT_THE_CLOCK = "HS_BTC";

    private long countBeatTheClock;
    private long highscoreBeatTheClock;
    private TextView tvTimeLeft;
    private ObjectAnimator animPulseTimeLeft;
    private TextView tvHighScore;
    private TextView tvAddedTime;
    private long START_TIME_LEFT = 5000;
    private long START_MILLIS_TO_ADD = 500;
    private ExtendableCountDownTimer timerTimeLeft;

    private float tvCountX;
    private float tvCountY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_beat_the_clock);

        context = this;
        activity = this;
        tvCount = findViewById(R.id.tv_count);
        rlActivity = findViewById(R.id.rl_activity);
        tvTimeLeft = findViewById(R.id.tv_time_left);
        tvHighScore = findViewById(R.id.tv_highscore);
        tvAddedTime = findViewById(R.id.tv_added_time);

        tvCountX = tvCount.getX();
        tvCountY = tvCount.getY();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        highscoreBeatTheClock = prefs.getLong(HIGHSCORE_BEAT_THE_CLOCK, 0);
        updateHighScore(highscoreBeatTheClock);

        tvAddedTime.setText(String.format("+ %d ms", START_MILLIS_TO_ADD));

        tvCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (countBeatTheClock == 0) {
                    timerTimeLeft.start();
                } else {
                    addTime();
                }
                countUp();
                relocateView(view);
                changeBackgroundColour();
                resetCurrentNumberTimer();
                updateHighScore(countBeatTheClock);

                tvCount.setText(String.valueOf(countBeatTheClock));
            }
        });

        resetScene();

        setUpEnvironment();
        changeBackgroundColour();
    }

    @Override
    protected void onStop() {
        super.onStop();

        PlayGames.getLeaderboardsClient(this)
                .submitScore(getString(R.string.leaderboard_all_time_leaderboard), count_all_time);
        PlayGames.getLeaderboardsClient(this)
                .submitScore(getString(R.string.leaderboard_faster_than_time), countBeatTheClock);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflator = getMenuInflater();
        inflator.inflate(R.menu.menu_time_killer, menu);
        menuTimerKiller = menu;

        MenuItem menuItemClassic = menu.findItem(R.id.menu_modes_classic);
        MenuItem menuItemBeatTheClock = menu.findItem(R.id.menu_modes_beat_the_clock);

        if (menuItemBeatTheClock != null) {
            menuItemBeatTheClock.setVisible(false);
        }
        if (menuItemClassic != null) {
            menuItemClassic.setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_modes_classic) {
            Intent startIntent = new Intent(context, TimeKillerActivity.class);
            context.startActivity(startIntent);
            return true;
        } else if (id == R.id.menu_leaderboard_beat_the_clock) {
            PlayGames.getLeaderboardsClient(this)
                    .submitScore(getString(R.string.leaderboard_all_time_leaderboard), count_all_time);
            PlayGames.getLeaderboardsClient(this)
                    .submitScore(getString(R.string.leaderboard_faster_than_time), countBeatTheClock);
            PlayGames.getLeaderboardsClient(this)
                    .getLeaderboardIntent(getString(R.string.leaderboard_faster_than_time))
                    .addOnSuccessListener(intent -> startActivityForResult(intent, REQUEST_LEADERBOARD))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to launch beat the clock leaderboard", e));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void resetScene() {

        PlayGames.getLeaderboardsClient(this)
                .submitScore(getString(R.string.leaderboard_all_time_leaderboard), count_all_time);
        PlayGames.getLeaderboardsClient(this)
                .submitScore(getString(R.string.leaderboard_faster_than_time), countBeatTheClock);

        unlockCountAchievements();

        countBeatTheClock = 0;

        tvCount.setText(R.string.start);
        tvCount.setClickable(true);

        tvCount.animate()
                .translationY(tvCountY)
                .translationX(tvCountX)
                .setDuration(1000)
                .start();

        timerTimeLeft = new ExtendableCountDownTimer(START_TIME_LEFT, 239) {
            @Override
            public void onTimerTick(long l) {
                updateTimeLeftView(l);
                Log.d(TAG, ("Timer Left Ticked: " + l + "ms left"));
            }

            @Override
            public void onTimerFinish() {
                stopTimer();
            }
        };

        tvTimeLeft.setClickable(false);
        tvTimeLeft.setTextColor(ContextCompat.getColor(this, R.color.colorBlack));
        updateTimeLeftView(START_TIME_LEFT);
        if (animPulseTimeLeft != null) {
            animPulseTimeLeft.end();
            animPulseTimeLeft.setDuration(300);

            animPulseTimeLeft.setRepeatCount(1);
            animPulseTimeLeft.setRepeatMode(ObjectAnimator.REVERSE);
            animPulseTimeLeft.start();
        }

        Log.d(TAG, "Reset Scene");

    }

    private void stopTimer() {
        tvCount.setClickable(false);
        vibratePhone(1000);

        tvTimeLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetScene();
            }
        });
        tvTimeLeft.setTextColor(ContextCompat.getColor(this, R.color.colorRed));
        tvTimeLeft.setText(R.string.reset);

        animPulseTimeLeft = ObjectAnimator.ofPropertyValuesHolder(tvTimeLeft,
                PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                PropertyValuesHolder.ofFloat("scaleY", 1.2f));
        animPulseTimeLeft.setDuration(300);

        animPulseTimeLeft.setRepeatCount(ObjectAnimator.INFINITE);
        animPulseTimeLeft.setRepeatMode(ObjectAnimator.REVERSE);

        animPulseTimeLeft.start();
    }

    private void vibratePhone(long time) {
        Vibrator v = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(time);
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private void updateTimeLeftView(long millisLeft) {
        long millis;
        long secs;
        long mins;

        secs = millisLeft / 1000;
        millis = millisLeft % 1000;

        if (secs >= 60) {
            mins = secs / 60;
            secs = secs % 60;
            tvTimeLeft.setText(String.format("%02d:%02d.%03d secs", mins, secs, millis));
        } else {
            tvTimeLeft.setText(String.format("%02d.%03d secs", secs, millis));
        }
        Log.d(TAG, "Updated time");
    }

    private void updateHighScore(long newHighscore) {
        if (newHighscore >= highscoreBeatTheClock) {
            tvHighScore.setText(String.format(getString(R.string.highscore), newHighscore));

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putLong(HIGHSCORE_BEAT_THE_CLOCK, newHighscore).apply();

            highscoreBeatTheClock = newHighscore;
        }
    }

    private void addTime() {
        timerTimeLeft.addMillis(START_MILLIS_TO_ADD);

        AnimationSet as = new AnimationSet(true);
        as.setInterpolator(new AccelerateDecelerateInterpolator());

        ScaleAnimation scaleAnimation = new ScaleAnimation(1, 2, 1, 2);
        scaleAnimation.setDuration(START_MILLIS_TO_ADD / 2);
        as.addAnimation(scaleAnimation);

        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        alphaAnimation.setDuration(scaleAnimation.getDuration());
        as.addAnimation(alphaAnimation);

        tvAddedTime.startAnimation(as);
    }

    protected void countUp() {
        countBeatTheClock++;
        super.countUp();
    }

    protected void relocateView(View view) {
        int width = rlActivity.getWidth();
        int topOffset = tvTimeLeft.getBottom();
        int height = rlActivity.getHeight() - topOffset;

        if (width <= 0 || height <= 0) return;

        float x = (float) (Math.random() * (width - view.getWidth()));
        float y = (float) (Math.random() * (height - view.getHeight()) + topOffset);

        view.animate()
                .translationXBy(x - view.getX())
                .translationYBy(y - view.getY())
                .setDuration(200)
                .start();
    }

    protected void unlockCountAchievements() {
        super.unlockCountAchievements();

        if (countBeatTheClock == 100) {
            PlayGames.getAchievementsClient(this)
                    .unlock(getString(R.string.achievement_ftt_100_clicks));
        } else if (countBeatTheClock == 1000) {
            PlayGames.getAchievementsClient(this)
                    .unlock(getString(R.string.achievement_ftt_1000_clicks));
        } else if (countBeatTheClock == 10000) {
            PlayGames.getAchievementsClient(this)
                    .unlock(getString(R.string.achievement_ftt_10k_clicks));
        }
    }

    @Override
    protected void checkCountAchievements() {
        super.checkCountAchievements();

        if (countBeatTheClock >= 100) {
            PlayGames.getAchievementsClient(this)
                    .unlock(getString(R.string.achievement_ftt_100_clicks));
        }
        if (countBeatTheClock >= 1000) {
            PlayGames.getAchievementsClient(this)
                    .unlock(getString(R.string.achievement_ftt_1000_clicks));
        }
        if (countBeatTheClock >= 10000) {
            PlayGames.getAchievementsClient(this)
                    .unlock(getString(R.string.achievement_ftt_10k_clicks));
        }
    }
}
