package uk.co.pranacreative.timekiller;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.games.Games;
import com.google.android.gms.tasks.OnSuccessListener;

import uk.co.pranacreative.timekiller.utils.ExtendableCountDownTimer;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class BeatTheClockActivity extends TimeKillerActivity {

    private static final String TAG = BeatTheClockActivity.class.getSimpleName();

    // Preferences
    private static final String HIGHSCORE_BEAT_THE_CLOCK = "HS_BTC";

    private long countBeatTheClock;
    private long highscoreBeatTheClock;
    private TextView tvTimeLeft;
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

        mVisible = true;
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

        // Set up the user interaction to manually show or hide the system UI.
        tvCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start timer if not running
                if (countBeatTheClock == 0) {
                    timerTimeLeft.start();
                } else {
                    addTime();
                }
                hide();
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
    }

    @Override
    protected void onStop() {
        super.onStop();

        mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (mGoogleSignInAccount != null) {
            Games.getLeaderboardsClient(this, mGoogleSignInAccount)
                    .submitScore(getString(R.string.leaderboard_all_time), count_all_time);
            Games.getLeaderboardsClient(this, mGoogleSignInAccount)
                    .submitScore(getString(R.string.leaderboard_beat_the_clock), countBeatTheClock);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflator = getMenuInflater();
        inflator.inflate(R.menu.menu_time_killer, menu);
        menuTimerKiller = menu;

        // Check if Ads have been removed
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(getString(R.string.inapp_remove_ads_id), false)) {
            // Ads have been removed
            MenuItem removeAds = menu.findItem(R.id.menu_remove_ads);
            if (removeAds != null) {
                removeAds.setVisible(false);
            }
        }

        MenuItem menuItemClassic = findViewById(R.id.menu_modes_classic);
        MenuItem menuItemBeatTheClock = findViewById(R.id.menu_modes_beat_the_clock);

        menuItemBeatTheClock.setVisible(false);
        menuItemClassic.setVisible(true);

        mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        updateSignInOutUI(mGoogleSignInAccount);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_modes_classic:
                Intent startIntent = new Intent(context, TimeKillerActivity.class);
                context.startActivity(startIntent);
                return true;
            case R.id.menu_leaderboard_beat_the_clock:
                // Submit scores before checking the leaderboard
                mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
                if (mGoogleSignInAccount != null) {
                    Games.getLeaderboardsClient(this, mGoogleSignInAccount)
                            .submitScore(getString(R.string.leaderboard_all_time), count_all_time);
                    Games.getLeaderboardsClient(this, mGoogleSignInAccount)
                            .submitScore(getString(R.string.leaderboard_beat_the_clock), countBeatTheClock);
                    Games.getLeaderboardsClient(this, mGoogleSignInAccount)
                            .getLeaderboardIntent(getString(R.string.leaderboard_beat_the_clock))
                            .addOnSuccessListener(new OnSuccessListener<Intent>() {
                                @Override
                                public void onSuccess(Intent intent) {
                                    startActivityForResult(intent, REQUEST_LEADERBOARD);
                                }
                            });
                } else {
                    notifyNoGoogleSignIn();
                }
                return true;
        }
        return false;
    }

    private void resetScene() {

        // Submit scores before resetting
        mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (mGoogleSignInAccount != null) {
            Games.getLeaderboardsClient(this, mGoogleSignInAccount)
                    .submitScore(getString(R.string.leaderboard_all_time), count_all_time);
            Games.getLeaderboardsClient(this, mGoogleSignInAccount)
                    .submitScore(getString(R.string.leaderboard_beat_the_clock), countBeatTheClock);
        }
        // Unlock achievements before resetting
        unlockCountAchievements();

        // Reset Beat the Clock count_all_time to 0
        countBeatTheClock = 0;

        // Reset Text
        tvCount.setText(R.string.start);
        updateTimeLeftView(START_TIME_LEFT);

        tvCount.animate()
                .translationY(tvCountY)
                .translationX(tvCountX)
                .setDuration(1000)
                .start();

        /*  Set up timer to run for default time and update every 239ms
            239 ms will make sure the millis second units change every time, making it look like
            it is updating every millisecond.*/
        timerTimeLeft = new ExtendableCountDownTimer(START_TIME_LEFT, 239) {
            @Override
            public void onTimerTick(long l) {
                updateTimeLeftView(l);
                Log.d(TAG, ("Timer Left Ticked: " + l + "ms left"));
            }

            @Override
            public void onTimerFinish() {
                resetScene();
            }
        };
        Log.d(TAG, "Reset Scene");

    }

    @SuppressLint("DefaultLocale")
    private void updateTimeLeftView(long millisLeft) {
        long millis;
        long secs;
        long mins;

        secs = millisLeft / 1000;
        millis = millisLeft % 1000;

        if (secs >= 60) {
            // At least 1 minute left
            mins = secs / 60;
            secs = secs % 60;
            tvTimeLeft.setText(String.format("%02d:%02d.%03d secs", mins, secs, millis));
        } else {
            // Less than 1 minutes left
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
        // Always change same amount of time
        timerTimeLeft.addMillis(START_MILLIS_TO_ADD);

        AnimationSet as = new AnimationSet(true);
        as.setInterpolator(new AccelerateDecelerateInterpolator());
        // Show
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
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y - tvTimeLeft.getHeight();

        // Make sure number doesn't land under an ad
        if (mAdView != null) {
            height -= mAdView.getHeight();
        }

        float x = (float) (Math.random() * (width - view.getWidth()));
        float y = (float) (Math.random() * (height - view.getHeight()) + tvTimeLeft.getHeight());

        view.animate()
                .translationXBy(x - view.getX())
                .translationYBy(y - view.getY())
                .setDuration(200)
                .start();
    }

    protected void unlockCountAchievements() {
        // Achievements from clicking
        super.unlockCountAchievements();

        // Beat the clock
        mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (mGoogleSignInAccount != null) {
            if (countBeatTheClock == 100) {
                Games.getAchievementsClient(this, mGoogleSignInAccount)
                        .unlock(getString(R.string.achievement_ftt_100_clicks_id));
            } else if (countBeatTheClock == 1000) {
                Games.getAchievementsClient(this, mGoogleSignInAccount)
                        .unlock(getString(R.string.achievement_ftt_1000_clicks_id));
            } else if (countBeatTheClock == 10000) {
                Games.getAchievementsClient(this, mGoogleSignInAccount)
                        .unlock(getString(R.string.achievement_ftt_10k_clicks_id));
            }
        }
    }

    @Override
    protected void checkCountAchievements() {
        // Achievements from clicking

        super.checkCountAchievements();

        // Beat the clock

        mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (mGoogleSignInAccount != null) {
            if (countBeatTheClock >= 100) {
                Games.getAchievementsClient(this, mGoogleSignInAccount)
                        .unlock(getString(R.string.achievement_ftt_100_clicks_id));
            }
            if (countBeatTheClock >= 1000) {
                Games.getAchievementsClient(this, mGoogleSignInAccount)
                        .unlock(getString(R.string.achievement_ftt_1000_clicks_id));
            }
            if (countBeatTheClock >= 10000) {
                Games.getAchievementsClient(this, mGoogleSignInAccount)
                        .unlock(getString(R.string.achievement_ftt_10k_clicks_id));
            }
        }
    }
}