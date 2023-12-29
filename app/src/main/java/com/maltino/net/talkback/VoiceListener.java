package com.maltino.net.talkback;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class VoiceListener {

    private Activity appActivity;
    public TextView tv_Speech_to_text;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1;
    public VoiceListener(MainActivity parent) {
        appActivity = parent;
    }

    public void Listen() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What's your question?");
            appActivity.startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);

        }
        catch (Exception e) {
            Toast
                    .makeText(appActivity, " " + e.getMessage(), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private String getSpeechResult(Intent data) {
        ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        return Objects.requireNonNull(result).get(0);
    }

    public void setPushToTalkButton(ImageView microphonePushToTalk) {
        microphonePushToTalk.setOnClickListener(new audioRequestor());
    }

    class audioRequestor implements View.OnClickListener {
        @Override
        public void onClick(View v)
        {
            Listen();
        }
    }


}
