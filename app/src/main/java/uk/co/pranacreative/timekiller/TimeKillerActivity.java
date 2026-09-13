package uk.co.pranacreative.timekiller;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Main activity displaying the counter and game interface.
 */
public class TimeKillerActivity extends AppCompatActivity {

    // Games API constants
    protected static final int REQUEST_ACHIEVEMENTS = 123;
    protected static final int REQUEST_LEADERBOARD = 124;
    protected static final String COUNT_STR = "COUNT";

    // Background colours
    protected static final int[] MATERIAL_COLOURS_WHITE_TEXT = {0xFFF44336, 0xFFE91E63, 0xFF9C27B0,
            0xFF673AB7, 0xFF3F51B5, 0xFF009688, 0xFF795548, 0xFF795548, 0xFF607D8B};
    protected static final int[] MATERIAL_COLOURS_BLACK_TEXT = {0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4,
            0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722, 0xFF9E9E9E};
    protected static final int MATERIAL_COLOUR_WHITE = 0xFFFFFFFF;
    protected static final int MATERIAL_COLOUR_BLACK = 0xFF000000;

    protected static final String TAG = TimeKillerActivity.class.getSimpleName();

    // Logic Variables
    protected long count_all_time;
    protected TextView tvCount;
    protected Context context;
    protected RelativeLayout rlActivity;
    protected Menu menuTimerKiller;

    // Timer for checking how long to stay on a number
    protected Timer currentNumberTimer;
    protected Toast toastNoGoogleSignIn;
    protected Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_time_killer);

        context = this;
        activity = this;

        tvCount = findViewById(R.id.tv_count);
        rlActivity = findViewById(R.id.rl_activity);

        tvCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                countUp();
                relocateView(view);
                changeBackgroundColour();
                resetCurrentNumberTimer();

                tvCount.setText(String.valueOf(count_all_time));
            }
        });

        setUpEnvironment();
        changeBackgroundColour();

        if (count_all_time > 0) {
            tvCount.setText(String.valueOf(count_all_time));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetCurrentNumberTimer();
    }

    @Override
    protected void onPause() {
        if (currentNumberTimer != null) {
            currentNumberTimer.cancel();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        PlayGames.getLeaderboardsClient(this)
                .submitScore(getString(R.string.leaderboard_all_time_leaderboard), count_all_time);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflator = getMenuInflater();
        inflator.inflate(R.menu.menu_time_killer, menu);
        menuTimerKiller = menu;

        MenuItem menuItemClassic = menu.findItem(R.id.menu_modes_classic);
        MenuItem menuItemBeatTheClock = menu.findItem(R.id.menu_modes_beat_the_clock);

        if (menuItemBeatTheClock != null) {
            menuItemBeatTheClock.setVisible(true);
        }
        if (menuItemClassic != null) {
            menuItemClassic.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.menu_modes_beat_the_clock) {
            Intent startIntent = new Intent(context, BeatTheClockActivity.class);
            context.startActivity(startIntent);
            return true;
        } else if (id == R.id.menu_achievements) {
            PlayGames.getAchievementsClient(this)
                    .getAchievementsIntent()
                    .addOnSuccessListener(intent -> startActivityForResult(intent, REQUEST_ACHIEVEMENTS))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to launch achievements intent", e));
            return true;
        } else if (id == R.id.menu_leaderboard_all_time) {
            PlayGames.getLeaderboardsClient(this)
                    .submitScore(getString(R.string.leaderboard_all_time_leaderboard), count_all_time);
            PlayGames.getLeaderboardsClient(this)
                    .getLeaderboardIntent(getString(R.string.leaderboard_all_time_leaderboard))
                    .addOnSuccessListener(intent -> startActivityForResult(intent, REQUEST_LEADERBOARD))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to launch all time leaderboard intent", e));
            return true;
        } else if (id == R.id.menu_leaderboard_beat_the_clock) {
            PlayGames.getLeaderboardsClient(this)
                    .submitScore(getString(R.string.leaderboard_all_time_leaderboard), count_all_time);
            PlayGames.getLeaderboardsClient(this)
                    .getLeaderboardIntent(getString(R.string.leaderboard_faster_than_time))
                    .addOnSuccessListener(intent -> startActivityForResult(intent, REQUEST_LEADERBOARD))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to launch beat the clock leaderboard intent", e));
            return true;
        }
        return false;
    }

    protected void countUp() {
        count_all_time++;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putLong(COUNT_STR, count_all_time).apply();
        unlockCountAchievements();
    }

    protected void relocateView(View view) {
        int width = rlActivity.getWidth();
        int height = rlActivity.getHeight();

        if (width <= 0 || height <= 0) return;

        float x = (float) (Math.random() * (width - view.getWidth()));
        float y = (float) (Math.random() * (height - view.getHeight()));

        view.animate()
                .translationXBy(x - view.getX())
                .translationYBy(y - view.getY())
                .setDuration(200)
                .start();
    }

    protected void changeBackgroundColour() {
        // Black or white text
        int textColour;
        int[] backgroundColours;
        if (Math.random() >= 0.5) {
            // Black Text
            textColour = MATERIAL_COLOUR_BLACK;
            backgroundColours = MATERIAL_COLOURS_BLACK_TEXT;
        } else {
            // White Text
            textColour = MATERIAL_COLOUR_WHITE;
            backgroundColours = MATERIAL_COLOURS_WHITE_TEXT;
        }

        tvCount.setTextColor(textColour);
        int index = (int) Math.round(Math.random() * (backgroundColours.length - 1));
        int newColor = backgroundColours[index];
        rlActivity.setBackgroundColor(newColor);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(newColor));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(newColor);
        }
    }

    protected void resetCurrentNumberTimer() {
        if (currentNumberTimer != null) {
            currentNumberTimer.cancel();
        }

        currentNumberTimer = new Timer();

        currentNumberTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                unlockEnjoyAchievement();
            }
        }, 60 * 1000); // 1 minute delay
    }

    protected void notifyNoGoogleSignIn() {
        if (toastNoGoogleSignIn != null) {
            toastNoGoogleSignIn.cancel();
        }
        toastNoGoogleSignIn = Toast.makeText(this, R.string.note_no_google_sign_in, Toast.LENGTH_SHORT);
        toastNoGoogleSignIn.show();
    }

    protected void unlockEnjoyAchievement() {
        PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_enjoy_the_view));
    }

    protected void unlockCountAchievements() {
        if (count_all_time == 100) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_100_clicks));
        } else if (count_all_time == 1000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_1000_clicks));
        } else if (count_all_time == 10000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_10k_clicks));
        } else if (count_all_time == 100000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_100k_clicks));
        } else if (count_all_time == 1000000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_1_million_clicks));
        }
    }

    protected void checkCountAchievements() {
        if (count_all_time >= 100) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_100_clicks));
        }
        if (count_all_time >= 1000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_1000_clicks));
        }
        if (count_all_time >= 10000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_10k_clicks));
        }
        if (count_all_time >= 100000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_100k_clicks));
        }
        if (count_all_time >= 1000000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_1_million_clicks));
        }
    }

    protected void setUpEnvironment() {

        // Initialize Play Games Services SDK v2
        PlayGamesSdk.initialize(this);

        rlActivity.getLayoutTransition()
                .enableTransitionType(LayoutTransition.CHANGING);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            prefs.getLong(COUNT_STR, -1);
        } catch (ClassCastException e) {
            count_all_time = prefs.getInt(COUNT_STR, -1);
            prefs.edit().putLong(COUNT_STR, count_all_time).apply();
        }
        count_all_time = prefs.getLong(COUNT_STR, -1);
    }
}
