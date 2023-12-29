package com.maltino.net.talkback;

import android.content.Context;

import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.text.qa.BertQuestionAnswerer;
import org.tensorflow.lite.task.text.qa.QaAnswer;
import java.io.IOException;
import java.util.List;

public class Intelligence {

    private Context context;
    private BertQuestionAnswerer bert;
    private String BERT_QA_MODEL = "mobilebert.tflite";
    public Intelligence(Context c) {
        context = c;
        prepareBert();
    }
    public String askBert(String contextOfTheQuestion, String questionToAsk) {
        List<QaAnswer> answers = bert.answer(contextOfTheQuestion, questionToAsk);
        if (answers.size() > 0)
            return answers.get(0).text;
        else
            return null;
    }
    private void prepareBert() {
        BertQuestionAnswerer.BertQuestionAnswererOptions options =
                BertQuestionAnswerer.BertQuestionAnswererOptions.builder()
                        .setBaseOptions(BaseOptions.builder().setNumThreads(4).build())
                        .build();

        try {
            bert = BertQuestionAnswerer.createFromFileAndOptions(context, BERT_QA_MODEL, options);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
