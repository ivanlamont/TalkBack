package com.maltino.net.talkback;

import static android.app.ProgressDialog.show;

import android.location.Location;
import android.os.Bundle;
import android.app.Activity;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.PersistableBundle;
import android.speech.RecognizerIntent;


import androidx.media3.common.BuildConfig;
import androidx.media3.common.util.Log;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.maltino.net.talkback.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private AppUpdateManager appUpdateManager;
    private ActivityMainBinding binding;
    private TextView localAreaInformation;
    private TextView userSpeechTranscription;
    private TextView contextFileName;
    private ImageView microphonePushToTalk;
    private FloatingActionButton refreshButton;
    private Context context;
    private Locator gps;
    private VoiceListener ears;
    private Intelligence brain;
    private Speaker mouth;
    private Bundle memory;
    private CloudContextStore cloud;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1;
    private static final int REQUEST_GPS_LOCATION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {

            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable e) {
                    // handle the exception
                    e.printStackTrace();
                }
            });

            memory = savedInstanceState;
            prepareUserInterface();
            prepareCloud();
            prepareLocation(localAreaInformation);
            prepareEars(userSpeechTranscription);
            prepareMouth();
            prepareBrain();
            checkForUpdate();
        } catch (Exception e) {
            reportException("loading", e);
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        startLocation();
    }
    @Override
    public void onStop() {
        super.onStop();
        stopLocation();
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        checkForUpdate();
    }

    private void prepareCloud() {
        try {
            cloud = new CloudContextStore(this, contextFileName);
            cloud.setRefreshButton(refreshButton);
        } catch (Exception e) {
            reportException("preparing the source data", e);
        }
    }

    private void prepareUserInterface() {
        try {
            context = getApplicationContext();
            binding = ActivityMainBinding.inflate(getLayoutInflater());

            setContentView(binding.getRoot());
            setSupportActionBar(binding.toolbar);

            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

            //binding.fab.setOnClickListener(new locationRequestor());

            microphonePushToTalk = findViewById(R.id.id_mic);
            userSpeechTranscription = findViewById(R.id.textview_Question);
            localAreaInformation = findViewById(R.id.textview_Location);
            contextFileName = findViewById(R.id.textview_ContextFileName);
            refreshButton = findViewById(R.id.fab);
            Button testButton = findViewById(R.id.button_test);

            if (debugMode()) {
                testButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        android.location.Location testLocation = new Location("TEST");
                        testLocation.setLatitude(37.803);
                        testLocation.setLongitude(-122.45);
                        gps.onLocationChanged(testLocation);
                    }
                });
            } else {
                testButton.setVisibility(View.INVISIBLE);
            }

        } catch (Exception e) {
            reportException("preparing the user interface", e);
        }

    }

    private boolean debugMode() {
        return BuildConfig.BUILD_TYPE.equals("debug");
    }

    private void prepareLocation(TextView userDisplay) {
        try {
            gps = new Locator(context,MainActivity.this);
            gps.Start();
            gps.AddressText = userDisplay;
            gps.LocationContextManager = cloud;
        } catch (Exception e) {
            reportException("preparing the GPS", e);
        }
    }

    private void startLocation() {
        try {
            gps.Start();
        } catch (Exception e) {
            reportException("starting the GPS", e);
        }
    }

    private void stopLocation() {
        try {
            gps.Stop();
        } catch (Exception e) {
            reportException("stopping the GPS", e);
        }
    }

    private void prepareEars(TextView userDisplay) {
        try {
            ears = new VoiceListener(MainActivity.this);
            ears.tv_Speech_to_text = userDisplay;
            ears.setPushToTalkButton(microphonePushToTalk);
        } catch (Exception e) {
            reportException("preparing speech to text", e);
        }
    }

    private void prepareMouth() {
        try {
            mouth = new Speaker(context, memory);
        } catch (Exception e) {
            reportException("preparing the text to speech", e);
        }
    }

    private void prepareBrain() {
        try {
            brain = new Intelligence(context);
        } catch (Exception e) {
            reportException("preparing BERT", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
//            NavHostFragment navHostFragment =
//                    (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
//            NavController navController = navHostFragment.getNavController();
            return true;
        }
        if (id == R.id.action_show_about) {
            Intent mIntent = new Intent(this, AboutActivity.class);
            startActivity(mIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String descriptionOfActivity = "handing activity";
        try {
            switch (requestCode) {
                case REQUEST_CODE_SPEECH_INPUT:
                    descriptionOfActivity = "processing speech";
                    if (resultCode == Activity.RESULT_OK) {
                        String speech = getSpeechRecognitionResult(data);
                        userSpeechTranscription.setText(speech);
                        askBert(cloud.CurrentContext(), speech);
                    }
                    break;
                case REQUEST_GPS_LOCATION:
                    descriptionOfActivity = "updating location";
                    if (resultCode == Activity.RESULT_OK) {
                        gps.getCurrentLocation();
                    }
                    break;
            }
        } catch (Exception e) {
            reportException(descriptionOfActivity, e);
        }
    }

    private void askBert(String contextOfTheQuestion, String questionToAsk) {
        String answer = brain.askBert(contextOfTheQuestion, questionToAsk);
        if (answer != null)
            mouth.Say(answer);
    }

    private String getSpeechRecognitionResult(Intent data) {
        ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        return Objects.requireNonNull(result).get(0);
    }

    private void checkForUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(context);

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    // This example applies an immediate update. To apply a flexible update
                    // instead, pass in AppUpdateType.FLEXIBLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                       // Request the update.
                startApplicationUpdate(appUpdateInfo);
            }
        });
    }

    private ActivityResultLauncher getResultCallback() {
        ActivityResultLauncher x = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        // handle callback
                        if (result.getResultCode() != RESULT_OK) {
                            reportError("Update flow failed! Result code: " + result.getResultCode());
                            // If the update is canceled or fails,
                            // you can request to start the update again.
                        }
                    }
                });
        return x;
    }

    private void startApplicationUpdate(AppUpdateInfo appUpdateInfo) {
        ActivityResultLauncher activityResultLauncher = getResultCallback();
        appUpdateManager.startUpdateFlowForResult(
                // Pass the intent that is returned by 'getAppUpdateInfo()'.
                appUpdateInfo,
                // an activity result launcher registered via registerForActivityResult
                activityResultLauncher,
                // Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
                // flexible updates.
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build());
    }

    private void reportException(String actionDescription, Exception ex) {
        reportError("Error while" + actionDescription + ": " + ex.getMessage());
    }
    private void reportError(String completeErrorMessage) {
        Toast.makeText(this.context, completeErrorMessage, Toast.LENGTH_LONG);
    }


    @Override
    protected void onDestroy() {
        if (mouth != null) {
            mouth.onDestroy();
        }
        super.onDestroy();
    }

}