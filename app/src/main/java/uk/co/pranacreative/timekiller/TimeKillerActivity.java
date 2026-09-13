package uk.co.pranacreative.timekiller;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
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
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class TimeKillerActivity extends AppCompatActivity implements GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

    // Games API constants
    // Achievement IDs
    protected static final int REQUEST_ACHIEVEMENTS = 123; // An arbitrary integer used as the request code
    // Leaderboard IDs
    protected static final int REQUEST_LEADERBOARD = 124; // An arbitrary integer used as the request code
    //Preferences
    protected static final String COUNT_STR = "COUNT";
    protected static final String DOUBLE_TAP_STR = "DOUBLE_TAP";
    // Background colours
    protected static final int[] MATERIAL_COLOURS_WHITE_TEXT = {0xFFF44336, 0xFFE91E63, 0xFF9C27B0,
            0xFF673AB7, 0xFF3F51B5, 0xFF009688, 0xFF795548, 0xFF795548, 0xFF607D8B};
    protected static final int[] MATERIAL_COLOURS_BLACK_TEXT = {0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4,
            0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722, 0xFF9E9E9E};
    protected static final int MATERIAL_COLOUR_WHITE = 0xFFFFFFFF;
    protected static final int MATERIAL_COLOUR_BLACK = 0xFF000000;
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    protected static final int UI_ANIMATION_DELAY = 300;
    protected static final int RC_SIGN_IN = 9001;
    private static final String TAG = TimeKillerActivity.class.getSimpleName();
    protected final Handler mHideHandler = new Handler();
    protected final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };
    // Google API
    protected GoogleSignInClient mGoogleSignInClient;
    protected GoogleSignInAccount mGoogleSignInAccount;
    // Logic Variables
    protected GestureDetectorCompat mDetector;
    protected long count_all_time;
    protected Toast debugToast;
    protected TextView tvCount;
    protected final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar
            tvCount.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    protected Context context;
    protected RelativeLayout rlActivity;
    protected Menu menuTimerKiller;
    protected boolean mVisible;
    protected final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    // Timer for checking how long to stay on the a number
    protected Timer currentNumberTimer;
    protected Toast toastNoGoogleSignIn;
    protected Activity activity;
    protected AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_time_killer);

        context = this;
        activity = this;

        mVisible = true;
        tvCount = findViewById(R.id.tv_count);
        rlActivity = findViewById(R.id.rl_activity);

        // Set up the user interaction to manually show or hide the system UI.
        tvCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hide();
                countUp();
                relocateView(view);
                changeBackgroundColour();
                resetCurrentNumberTimer();

                tvCount.setText(String.valueOf(count_all_time));
            }
        });

        setUpEnvironment();

        // Set value if a value greater than 0 exists
        if (count_all_time > 0) {
            tvCount.setText(String.valueOf(count_all_time));
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
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

        // Make sure number doesn't land under an ad
        if (mAdView != null) {
            height -= mAdView.getHeight();
        }

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
        }, 60 * 1000); // 1 minutes delay
    }

    protected void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    protected void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    protected void show() {
        // Show the system bar
        tvCount.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    protected void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
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
                        // at this point, the user is signed out.
                        updateSignInOutUI(null);
                    }
                });
    }

    protected void unlockEnjoyAchievement() {
        PlayGames.getAchievementsClient(this).unlock(getString(R.string.achievement_enjoy_view_id));
    }

    protected void unlockCountAchievements() {
        // Achievements from clicking
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
        // Achievements from clicking
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mDetector != null) return this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        toggle();
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    public void updateSignInOutUI(GoogleSignInAccount account) {
        if (menuTimerKiller != null) {
            MenuItem signIn = menuTimerKiller.findItem(R.id.menu_sign_in);
            MenuItem signOut = menuTimerKiller.findItem(R.id.menu_sign_out);
            if (signIn != null && signOut != null) {
                if (account != null) {
                    // The player is signed in. Hide the sign-in button and allow the
                    // player to proceed.
                    signIn.setVisible(false);
                    signOut.setVisible(true);
                } else {
                    // The player is NOT signed in. Hide the sign-in button and allow the
                    // player to proceed.
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

        // Manage older versions where it used to be a int value instead of a long
        try {
            prefs.getLong(COUNT_STR, -1);
        } catch (ClassCastException e) {
            count_all_time = prefs.getInt(COUNT_STR, -1);
            prefs.edit().putLong(COUNT_STR, count_all_time).apply();
        }
        count_all_time = prefs.getLong(COUNT_STR, -1);

        mDetector = new GestureDetectorCompat(this, this);
        mDetector.setOnDoubleTapListener(this);

        // DoubleTap instructions
        if (!prefs.getBoolean(DOUBLE_TAP_STR, false)) {
            Snackbar.make(tvCount, R.string.note_double_tap, Snackbar.LENGTH_LONG)
                    .setAction(R.string.note_got_it, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                            prefs.edit().putBoolean(DOUBLE_TAP_STR, true).apply();
                        }
                    }).show();
        }

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_games_web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mGoogleSignInClient.silentSignIn()
                .addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        handleSignInResult(task);
                    }
                });

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        updateSignInOutUI(mGoogleSignInAccount);

        // Initialise MobileAds for use
        MobileAds.initialize(this);

        mAdView = findViewById(R.id.adView);
        if (mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
        try {
            mGoogleSignInAccount = completedTask.getResult(ApiException.class);
            String idToken = mGoogleSignInAccount.getIdToken();

            // TODO(developer): send ID Token to server and validate

            updateSignInOutUI(mGoogleSignInAccount);
        } catch (ApiException e) {
            Log.w(TAG, "handleSignInResult:error", e);
            updateSignInOutUI(null);
        }
    }
}
