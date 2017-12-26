package uk.co.pranacreative.timekiller;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.Games;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class TimeKillerActivity extends AppCompatActivity implements GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        PurchasesUpdatedListener {

    // Games API constants
    // Achievement IDs
    protected static final int REQUEST_ACHIEVEMENTS = 123; // An arbitrary integer used as the request code
    // Leaderboard IDs
    protected static final int REQUEST_LEADERBOARD = 124; // An arbitrary integer used as the request code
    //Preferences
    protected static final String COUNT_STR = "COUNT";
    protected static final String DOUBLE_TAP_STR = "DOUBLE_TAP";
    // AdMobs constants
    protected static final String ADMOBS_APP_ID = "ca-app-pub-6355028338567451~1344025701";
    protected static final int RC_BILLING_REMOVE_ADS = 1001;
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
    protected BillingClient mBillingClient;
    protected boolean mIsServiceConnected;
    protected int mBillingClientResponseCode;
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
    // In-app Billing
    IInAppBillingService mService;
    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
            Log.d("TEST", "mService ready to go!");
            checkOwnedItems();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

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
        checkOwnedItems();
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

        mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (mGoogleSignInAccount != null) {
            Games.getLeaderboardsClient(this, mGoogleSignInAccount)
                    .submitScore(getString(R.string.leaderboard_all_time), count_all_time);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mServiceConn != null) {
            unbindService(mServiceConn);
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

        switch (item.getItemId()) {
            case R.id.menu_modes_beat_the_clock:
                Intent startIntent = new Intent(context, BeatTheClockActivity.class);
                context.startActivity(startIntent);
                return true;
            case R.id.menu_sign_in:
                signInClicked();
                return true;
            case R.id.menu_sign_out:
                signOutclicked();
                return true;
            case R.id.menu_achievements:
                mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
                if (mGoogleSignInAccount != null) {
                    Games.getAchievementsClient(this, mGoogleSignInAccount)
                            .getAchievementsIntent()
                            .addOnSuccessListener(new OnSuccessListener<Intent>() {
                                @Override
                                public void onSuccess(Intent intent) {
                                    startActivityForResult(intent, REQUEST_ACHIEVEMENTS);
                                }
                            });
                }
                return true;
            case R.id.menu_leaderboard_all_time:

                // Submit scores before checking the leasderboard

                mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
                if (mGoogleSignInAccount != null) {
                    Games.getLeaderboardsClient(this, mGoogleSignInAccount)
                            .submitScore(getString(R.string.leaderboard_all_time), count_all_time);
                    Games.getLeaderboardsClient(this, mGoogleSignInAccount)
                            .getLeaderboardIntent(getString(R.string.leaderboard_all_time))
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
            case R.id.menu_leaderboard_beat_the_clock:
                // Submit scores before checking the leaderboard

                mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
                if (mGoogleSignInAccount != null) {
                    Games.getLeaderboardsClient(this, mGoogleSignInAccount)
                            .submitScore(getString(R.string.leaderboard_all_time), count_all_time);
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
            case R.id.menu_remove_ads:
                removeAds();
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

    protected void removeAds() {

        executeServiceRequest(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                .setSku(getString(R.string.inapp_remove_ads_id))
                                .setType(BillingClient.SkuType.INAPP)
                                .build();
                        int responseCode = mBillingClient.launchBillingFlow(activity, flowParams);
                    }
                });
            }
        });
    }

    public void startServiceConnection(final Runnable executeOnSuccess) {
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                Log.d(TAG, "Setup finished. Response code: " + billingResponseCode);

                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    mIsServiceConnected = true;
                    if (executeOnSuccess != null) {
                        executeOnSuccess.run();
                    }
                }
                mBillingClientResponseCode = billingResponseCode;
            }

            @Override
            public void onBillingServiceDisconnected() {
                mIsServiceConnected = false;
            }
        });
    }

    protected void executeServiceRequest(Runnable runnable) {
        if (mIsServiceConnected) {
            runnable.run();
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
            startServiceConnection(runnable);
        }
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
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
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

        mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (mGoogleSignInAccount != null) {
            Games.getAchievementsClient(this, mGoogleSignInAccount).unlock(getString(R.string.achievement_enjoy_view_id));
        }
    }

    protected void unlockCountAchievements() {
        // Achievements from clicking

        mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (mGoogleSignInAccount != null) {
            if (count_all_time == 100) {
                Games.getAchievementsClient(this, mGoogleSignInAccount).unlock(getString(R.string.achievement_100_clicks_id));
            } else if (count_all_time == 1000) {
                Games.getAchievementsClient(this, mGoogleSignInAccount).unlock(getString(R.string.achievement_1000_clicks_id));
            } else if (count_all_time == 10000) {
                Games.getAchievementsClient(this, mGoogleSignInAccount).unlock(getString(R.string.achievement_10k_clicks_id));
            } else if (count_all_time == 100000) {
                Games.getAchievementsClient(this, mGoogleSignInAccount).unlock(getString(R.string.achievement_100k_clicks_id));
            } else if (count_all_time == 1000000) {
                Games.getAchievementsClient(this, mGoogleSignInAccount).unlock(getString(R.string.achievement_1m_clicks_id));
            }
        }
    }

    protected void checkCountAchievements() {
        // Achievements from clicking

        mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (mGoogleSignInAccount != null) {
            if (count_all_time >= 100) {
                Games.getAchievementsClient(this, mGoogleSignInAccount).unlock(getString(R.string.achievement_100_clicks_id));
            }
            if (count_all_time >= 1000) {
                Games.getAchievementsClient(this, mGoogleSignInAccount).unlock(getString(R.string.achievement_1000_clicks_id));
            }
            if (count_all_time >= 10000) {
                Games.getAchievementsClient(this, mGoogleSignInAccount).unlock(getString(R.string.achievement_10k_clicks_id));
            }
            if (count_all_time >= 100000) {
                Games.getAchievementsClient(this, mGoogleSignInAccount).unlock(getString(R.string.achievement_100k_clicks_id));
            }
            if (count_all_time >= 1000000) {
                Games.getAchievementsClient(this, mGoogleSignInAccount).unlock(getString(R.string.achievement_1m_clicks_id));
            }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            rlActivity.getLayoutTransition()
                    .enableTransitionType(LayoutTransition.CHANGING);
        }

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

        if (!prefs.getBoolean(getString(R.string.inapp_remove_ads_id), false)) {
            // Ads have not been removed
            // Initialise MobileAds for use
            MobileAds.initialize(this, ADMOBS_APP_ID);

            mAdView = findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }

        // Connect to Google Play Billing
        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
        mBillingClient = BillingClient.newBuilder(this).setListener(this).build();
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

    @Override
    public void onPurchasesUpdated(final int responseCode, @Nullable List<Purchase> purchases) {
        if ((responseCode == BillingClient.BillingResponse.OK ||
                responseCode == BillingClient.BillingResponse.ITEM_ALREADY_OWNED)
                && purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getSku().equals(getString(R.string.inapp_remove_ads_id))) {
                    if (mAdView != null) {
                        mAdView.setVisibility(View.INVISIBLE);
                    }
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    prefs.edit().putBoolean(getString(R.string.inapp_remove_ads_id), true).apply();
                    // Hide Ads
                    MenuItem removeAds = menuTimerKiller.findItem(R.id.menu_remove_ads);

                    if (removeAds != null) {
                        removeAds.setVisible(false);
                    }
                }
            }
        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else if (responseCode == BillingClient.BillingResponse.SERVICE_UNAVAILABLE) {
            // BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE - Network connection is down
            Toast.makeText(context, R.string.no_network, Toast.LENGTH_SHORT).show();
        } else {
            Snackbar.make(tvCount, R.string.note_sorry_bug, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.report_issue, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Send email to developer
                            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                                    "mailto", "meezpower@gmail.com", null));
                            intent.setType("text/plain");
                            intent.putExtra(Intent.EXTRA_EMAIL, "meezpower@egmail.com");
                            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.error_report_subject_billing));
                            intent.putExtra(Intent.EXTRA_TEXT,
                                    getString(R.string.error_report_body_billing) +
                                            responseCode + ".");
                            startActivity(Intent.createChooser(intent, "Send Email"));
                        }
                    }).show();
        }
    }

    protected void checkOwnedItems() {
        try {
            Bundle ownedItems = null;
            if (mService != null) {
                ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);

                if (ownedItems.getInt("RESPONSE_CODE") == 0) {
                    ArrayList<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");

                    if (ownedSkus != null && ownedSkus.size() > 0) {
                        for (String sku : ownedSkus) {
                            if (sku.equals(getString(R.string.inapp_remove_ads_id))) {
                                // Remove ads
                                if (mAdView != null) {
                                    mAdView.setVisibility(View.INVISIBLE);
                                }
                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                                prefs.edit().putBoolean(getString(R.string.inapp_remove_ads_id), true).apply();
                                // Hide Ads
                                MenuItem removeAds = null;
                                if (menuTimerKiller != null) {
                                    removeAds = menuTimerKiller.findItem(R.id.menu_remove_ads);
                                }

                                if (removeAds != null) {
                                    removeAds.setVisible(false);
                                }
                            }
                        }
                    }
                }
            } else {
                if (ownedItems != null) {
                    Log.e(TAG, "ERROR - checkOwnedItems: RESPONSE CODE = " + ownedItems.getInt("RESPONSE_CODE"));
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}