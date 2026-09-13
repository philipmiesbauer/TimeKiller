package uk.co.pranacreative.timekiller;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

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

    protected static final int RC_SIGN_IN = 9001;
    private static final String TAG = TimeKillerActivity.class.getSimpleName();

    // Google API
    protected GoogleSignInClient mGoogleSignInClient;
    protected GoogleSignInAccount mGoogleSignInAccount;

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
                .submitScore(getString(R.string.leaderboard_all_time), count_all_time);
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

        mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        updateSignInOutUI(mGoogleSignInAccount);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.menu_modes_beat_the_clock) {
            Intent startIntent = new Intent(context, BeatTheClockActivity.class);
            context.startActivity(startIntent);
            return true;
        } else if (id == R.id.menu_sign_in) {
            signInClicked();
            return true;
        } else if (id == R.id.menu_sign_out) {
            signOutclicked();
            return true;
        } else if (id == R.id.menu_achievements) {
            PlayGames.getAchievementsClient(this)
                    .getAchievementsIntent()
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            startActivityForResult(intent, REQUEST_ACHIEVEMENTS);
                        }
                    });
            return true;
        } else if (id == R.id.menu_leaderboard_all_time) {
            PlayGames.getLeaderboardsClient(this)
                    .submitScore(getString(R.string.leaderboard_all_time), count_all_time);
            PlayGames.getLeaderboardsClient(this)
                    .getLeaderboardIntent(getString(R.string.leaderboard_all_time))
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            startActivityForResult(intent, REQUEST_LEADERBOARD);
                        }
                    });
            return true;
        } else if (id == R.id.menu_leaderboard_beat_the_clock) {
            PlayGames.getLeaderboardsClient(this)
                    .submitScore(getString(R.string.leaderboard_all_time), count_all_time);
            PlayGames.getLeaderboardsClient(this)
                    .getLeaderboardIntent(getString(R.string.leaderboard_beat_the_clock))
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            startActivityForResult(intent, REQUEST_LEADERBOARD);
                        }
                    });
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
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

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
        rlActivity.setBackgroundColor(backgroundColours[index]);
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

    // Call when the sign-in button is clicked
    protected void signInClicked() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // Call when the sign-out button is clicked
    protected void signOutclicked() {

        GoogleSignInClient signInClient = GoogleSignIn.getClient(this,
                GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);
        signInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateSignInOutUI(null);
                    }
                });
    }

    protected void unlockEnjoyAchievement() {
        PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_enjoy_view_id));
    }

    protected void unlockCountAchievements() {
        if (count_all_time == 100) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_100_clicks_id));
        } else if (count_all_time == 1000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_1000_clicks_id));
        } else if (count_all_time == 10000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_10k_clicks_id));
        } else if (count_all_time == 100000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_100k_clicks_id));
        } else if (count_all_time == 1000000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_1m_clicks_id));
        }
    }

    protected void checkCountAchievements() {
        if (count_all_time >= 100) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_100_clicks_id));
        }
        if (count_all_time >= 1000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_1000_clicks_id));
        }
        if (count_all_time >= 10000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_10k_clicks_id));
        }
        if (count_all_time >= 100000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_100k_clicks_id));
        }
        if (count_all_time >= 1000000) {
            PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_1m_clicks_id));
        }
    }

    public void updateSignInOutUI(GoogleSignInAccount account) {
        if (menuTimerKiller != null) {
            MenuItem signIn = menuTimerKiller.findItem(R.id.menu_sign_in);
            MenuItem signOut = menuTimerKiller.findItem(R.id.menu_sign_out);
            if (signIn != null && signOut != null) {
                if (account != null) {
                    signIn.setVisible(false);
                    signOut.setVisible(true);
                } else {
                    signIn.setVisible(true);
                    signOut.setVisible(false);
                }
            }
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

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_games_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mGoogleSignInClient.silentSignIn()
                .addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        handleSignInResult(task);
                    }
                });

        mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        updateSignInOutUI(mGoogleSignInAccount);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
        try {
            mGoogleSignInAccount = completedTask.getResult(ApiException.class);
            updateSignInOutUI(mGoogleSignInAccount);
        } catch (ApiException e) {
            Log.w(TAG, "handleSignInResult:error", e);
            updateSignInOutUI(null);
        }
    }
}
