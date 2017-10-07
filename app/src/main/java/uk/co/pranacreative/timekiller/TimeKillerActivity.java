package uk.co.pranacreative.timekiller;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameUtils;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class TimeKillerActivity extends AppCompatActivity implements GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // Achievement IDs
    private static final int REQUEST_ACHIEVEMENTS = 123; // An arbitrary integer used as the request code
    // Leaderboard IDs
    private static final int REQUEST_LEADERBOARD = 124; // An arbitrary integer used as the request code
    private static final String COUNT_STR = "COUNT";
    private static final int[] MATERIAL_COLOURS_WHITE_TEXT = {0xFFF44336, 0xFFE91E63, 0xFF9C27B0,
            0xFF673AB7, 0xFF3F51B5, 0xFF009688, 0xFF795548, 0xFF795548, 0xFF607D8B};
    private static final int[] MATERIAL_COLOURS_BLACK_TEXT = {0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4,
            0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722, 0xFF9E9E9E};
    private static final int MATERIAL_COLOUR_WHITE = 0xFFFFFFFF;
    private static final int MATERIAL_COLOUR_BLACK = 0xFF000000;
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 1000;
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private static int RC_SIGN_IN = 9001;
    private final Handler mHideHandler = new Handler();
    private final Runnable mShowPart2Runnable = new Runnable() {
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
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingConnectionFailure = false;
    private boolean mAutoStartSignInFlow = true;
    private boolean mSignInClicked = false;
    // Logic Variables
    private GestureDetectorCompat mDetector;
    private int count;
    private Toast debugToast;
    private TextView tvCount;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            tvCount.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private RelativeLayout rlActivity;
    private Menu menuTimerKiller;
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_time_killer);

        mVisible = true;
        tvCount = (TextView) findViewById(R.id.tv_count);
        rlActivity = (RelativeLayout) findViewById(R.id.rl_activity);


        // Set up the user interaction to manually show or hide the system UI.
        tvCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hide();
                countUp();
                relocateView(view);
                changeBackgroundColour();

                tvCount.setText(String.valueOf(count));
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        count = prefs.getInt(COUNT_STR, -1);

        // Set value if a value greater than 0 exists
        if (count > 0) {
            tvCount.setText(String.valueOf(count));
        }


        mDetector = new GestureDetectorCompat(this, this);
        mDetector.setOnDoubleTapListener(this);

        // Create the Google Api Client with access to the Play Games services
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                // add other APIs and scopes here as needed
                .build();

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
    protected void onStart() {
        super.onStart();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Games.Leaderboards.submitScore(mGoogleApiClient, getString(R.string.leaderboard_all_time), count);
            mGoogleApiClient.disconnect();
        } else {
            notifyNoGoogleSignIn();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflator = getMenuInflater();
        inflator.inflate(R.menu.timer_killer, menu);
        menuTimerKiller = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_sign_in:
                signInClicked();
                return true;
            case R.id.menu_sign_out:
                signOutclicked();
                return true;
            case R.id.menu_achievements:
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    startActivityForResult(Games.Achievements.getAchievementsIntent(mGoogleApiClient),
                            REQUEST_ACHIEVEMENTS);
                } else {
                    notifyNoGoogleSignIn();
                }
                return true;
            case R.id.menu_leaderboard:

                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    // Submit scores before checking the leasderboard
                    Games.Leaderboards.submitScore(mGoogleApiClient, getString(R.string.leaderboard_all_time), count);
                    startActivityForResult(Games.Leaderboards.getLeaderboardIntent(mGoogleApiClient,
                            getString(R.string.leaderboard_all_time)), REQUEST_LEADERBOARD);
                } else {
                    notifyNoGoogleSignIn();
                }
                return true;
        }
        return false;
    }

    private void countUp() {
        count++;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putInt(COUNT_STR, count).commit();
        checkAchievements();
    }

    private void relocateView(View view) {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        float x = (float) (Math.random() * (width - view.getWidth()));
        float y = (float) (Math.random() * (height - view.getHeight()));

        view.setX(x);
        view.setY(y);
    }

    private void changeBackgroundColour() {
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

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void notifyNoGoogleSignIn() {
        Toast.makeText(this, R.string.note_no_google_sign_in, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("InlinedApi")
    private void show() {
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
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    // Call when the sign-in button is clicked
    private void signInClicked() {
        mSignInClicked = true;
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }

    }

    // Call when the sign-out button is clicked
    private void signOutclicked() {
        mSignInClicked = false;
        mAutoStartSignInFlow = false;

        Games.signOut(mGoogleApiClient);

        MenuItem signIn = menuTimerKiller.findItem(R.id.menu_sign_in);
        MenuItem signOut = menuTimerKiller.findItem(R.id.menu_sign_out);

        signIn.setVisible(true);
        signOut.setVisible(false);
    }

    private void checkAchievements() {
        // Achievements from clicking

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {

            switch (count) {
                case 100:
                    Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_100_clicks_id));
                    break;
                case 1000:
                    Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_1000_clicks_id));
                    break;
                case 10000:
                    Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_10k_clicks_id));
                    break;
                case 100000:
                    Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_100k_clicks_id));
                    break;
                case 1000000:
                    Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_1m_clicks_id));
                    break;
            }

        } else {
            notifyNoGoogleSignIn();
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // The player is signed in. Hide the sign-in button and allow the
        // player to proceed.
        MenuItem signIn = menuTimerKiller.findItem(R.id.menu_sign_in);
        MenuItem signOut = menuTimerKiller.findItem(R.id.menu_sign_out);

        mAutoStartSignInFlow = true;

        signIn.setVisible(false);
        signOut.setVisible(true);

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (mResolvingConnectionFailure) {
            // already resolving
            return;
        }

        // if the sign-in button was clicked or if auto sign-in is enabled,
        // launch the sign-in flow
        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = true;

            // Attempt to resolve the connection failure using BaseGameUtils.
            // The R.string.signin_other_error value should reference a generic
            // error string in your strings.xml file, such as "There was
            // an issue with sign-in, please try again later."
            if (!BaseGameUtils.resolveConnectionFailure(this,
                    mGoogleApiClient, connectionResult,
                    RC_SIGN_IN, R.string.sign_in_other_error)) {
                mResolvingConnectionFailure = false;
            }
        }

        // Put code here to display the sign-in button
        MenuItem signIn = menuTimerKiller.findItem(R.id.menu_sign_in);
        MenuItem signOut = menuTimerKiller.findItem(R.id.menu_sign_out);
        signIn.setVisible(true);
        signOut.setVisible(false);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Attempt to reconnect
        mGoogleApiClient.connect();
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (resultCode == RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                // Bring up an error dialog to alert the user that sign-in
                // failed. The R.string.signin_failure should reference an error
                // string in your strings.xml file that tells the user they
                // could not be signed in, such as "Unable to sign in."
                BaseGameUtils.showActivityResultError(this,
                        requestCode, resultCode, R.string.signin_failure);
            }
        }
    }

}


