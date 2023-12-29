package com.maltino.net.talkback;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class Speaker {
    private TextToSpeech textToSpeech;
    private Context context;
    private Bundle savedState;
    private String BERT_QA_MODEL = "mobilebert.tflite";
    public Speaker(Context c, Bundle savedInstanceState) {
        context = c;
        savedState = savedInstanceState;
        prepareSpeech();
    }
    private void prepareSpeech() {
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {

                // if No error is found then only it will run
                if(i!=TextToSpeech.ERROR){
                    // To Choose language of speech
                    textToSpeech.setLanguage(Locale.US);

                }
            }
        });
    }
    public void Say(String message) {
        textToSpeech.speak(message,TextToSpeech.QUEUE_FLUSH, savedState,null);
    }

    public void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }


}
