package com.maltino.net.talkback;

import android.os.Bundle;
import android.app.Activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.speech.RecognizerIntent;


import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.maltino.net.talkback.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private TextView localAreaInformation;
    private TextView userSpeechTranscription;
    private ImageView microphonePushToTalk;
    private LocalAreaDescriptions locationInfo;
    private Context context;
    private Locator gps;
    private VoiceListener ears;
    private Intelligence brain;
    private Speaker mouth;
    private Bundle memory;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1;
    private static final int REQUEST_GPS_LOCATION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        memory = savedInstanceState;
        prepareUserInterface();
        prepareLocalContextData();
        prepareLocation(localAreaInformation);
        prepareEars(userSpeechTranscription);
        prepareMouth();
        prepareBrain();
    }

    private void prepareUserInterface() {

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
    }

    private void prepareLocation(TextView userDisplay) {
        gps = new Locator(context,MainActivity.this);
        gps.Start();
        gps.AddressText = userDisplay;
        gps.LocationInfo = locationInfo;
    }

    private void prepareEars(TextView userDisplay) {
        ears = new VoiceListener(MainActivity.this);
        ears.tv_Speech_to_text = userDisplay;
        ears.setPushToTalkButton(microphonePushToTalk);
    }

    private void prepareMouth() {
        mouth = new Speaker(context, memory);
    }

    private void prepareLocalContextData() {

        try {
            locationInfo = ObjectSerialization.getInstance().DeserializeFromFile(LocalAreaDescriptions.StorageFileName, LocalAreaDescriptions.class);
        } catch (Exception x) {
            x.printStackTrace();
        }
        if (locationInfo == null || locationInfo.IsEmpty()) {
            useTestLocalContextData();
        }
    }

    private void useTestLocalContextData() {
        locationInfo = LocalAreaDescriptions.getTestData();
        ObjectSerialization.getInstance().SerializeToFile(locationInfo, LocalAreaDescriptions.StorageFileName);
    }

    private void prepareBrain() {
        brain = new Intelligence(context);
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
        switch (requestCode) {
            case REQUEST_CODE_SPEECH_INPUT:
                if (resultCode == Activity.RESULT_OK) {
                    String speech = getSpeechRecognitionResult(data);
                    userSpeechTranscription.setText(speech);
                    askBert(locationInfo.currentContext, speech);
                }
                break;
            case REQUEST_GPS_LOCATION:
                if (resultCode == Activity.RESULT_OK) {
                    gps.getCurrentLocation();
                }
                break;
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

    @Override
    protected void onDestroy() {
        if (mouth != null) {
            mouth.onDestroy();
        }
        super.onDestroy();
    }

}