package com.example.quiz_json;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;



import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView questionText, timerText;
    private RadioGroup optionsGroup;
    private RadioButton option1, option2, option3, option4;
    private Button nextButton;
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private long timeLeftInMillis = 600000; // 10 minutes in milliseconds
    private CountDownTimer countDownTimer;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "quiz_prefs";
    private static final String KEY_INDEX = "current_index";
    private static final String KEY_TIME_LEFT = "time_left";
    private List<Integer> userAnswers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        questionText = findViewById(R.id.question_text);
        timerText = findViewById(R.id.timer);
        optionsGroup = findViewById(R.id.options_group);
        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4 = findViewById(R.id.option4);
        nextButton = findViewById(R.id.next_button);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentQuestionIndex = sharedPreferences.getInt(KEY_INDEX, 0);
        timeLeftInMillis = sharedPreferences.getLong(KEY_TIME_LEFT, 600000);

        loadQuestions();
        userAnswers = new ArrayList<>(questions.size());
        for (int i = 0; i < questions.size(); i++) {
            userAnswers.add(-1); // Initialize user answers with -1 (no answer selected)
        }
        displayQuestion();

        nextButton.setOnClickListener(v -> {
            saveAnswer();
            if (currentQuestionIndex < questions.size() - 1) {
                currentQuestionIndex++;
                displayQuestion();
            } else {
                finishQuiz();
            }
        });

        startTimer();
    }

    private void loadQuestions() {
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.questions);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();

            String json = new String(buffer, "UTF-8");
            Gson gson = new Gson();
            Type questionListType = new TypeToken<List<Question>>() {}.getType();
            questions = gson.fromJson(json, questionListType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayQuestion() {
        if (questions != null && currentQuestionIndex < questions.size()) {
            Question currentQuestion = questions.get(currentQuestionIndex);
            questionText.setText(currentQuestion.getQuestion());
            List<String> options = currentQuestion.getOptions();
            option1.setText(options.get(0));
            option2.setText(options.get(1));
            option3.setText(options.get(2));
            option4.setText(options.get(3));

            optionsGroup.clearCheck();
            int userAnswer = userAnswers.get(currentQuestionIndex);
            if (userAnswer != -1) {
                ((RadioButton) optionsGroup.getChildAt(userAnswer)).setChecked(true);
            }
        }
    }

    private void saveAnswer() {
        int selectedOption = optionsGroup.indexOfChild(findViewById(optionsGroup.getCheckedRadioButtonId()));
        userAnswers.set(currentQuestionIndex, selectedOption);
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                finishQuiz();
            }
        }.start();
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeFormatted = String.format("%02d:%02d", minutes, seconds);
        timerText.setText(timeFormatted);
    }

    private void finishQuiz() {
        // Calculate the score
        int score = 0;
        for (int i = 0; i < questions.size(); i++) {
            if (userAnswers.get(i) == questions.get(i).getAnswer()) {
                score++;
            }
        }

        // Display the score
        Toast.makeText(this, "Your score: " + score + "/" + questions.size(), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_INDEX, currentQuestionIndex);
        editor.putLong(KEY_TIME_LEFT, timeLeftInMillis);
        editor.apply();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startTimer();
    }
}