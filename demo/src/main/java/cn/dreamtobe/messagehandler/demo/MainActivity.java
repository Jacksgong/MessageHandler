/**
 * Copyright (c) 2016 Jacksgong(blog.dreamtobe.cn).
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.dreamtobe.messagehandler.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Random;

import cn.dreamtobe.messagehandler.MessageHandler;

/**
 * Created by Jacksgong on 1/14/16.
 * Calculagraph & calculator which strong reference Message flow
 */
public class MainActivity extends AppCompatActivity {


    private Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case WHAT_INCREASE_TENTH_SEC:
                    increase();
                    calculagraphTv.setText(generateCalculagraph());
                    totalTenthSecTv.setText(String.valueOf(totalTenthSec));
                    handler.sendEmptyMessageDelayed(WHAT_INCREASE_TENTH_SEC, 100);
                    break;
                case WHAT_RANDOM_1:
                    random1 = checkRandom(msg.what, random1, randomDivideExactly1Tv);
                    break;
                case WHAT_RANDOM_2:
                    random2 = checkRandom(msg.what, random2, randomDivideExactly2Tv);
                    break;
                case WHAT_RANDOM_3:
                    random3 = checkRandom(msg.what, random3, randomDivideExactly3Tv);
                    break;
                case WHAT_RANDOM_4:
                    random4 = checkRandom(msg.what, random4, randomDivideExactly4Tv);
                    break;
                case WHAT_RANDOM_5:
                    random5 = checkRandom(msg.what, random5, randomDivideExactly5Tv);
                    break;
                case WHAT_RANDOM_6:
                    random6 = checkRandom(msg.what, random6, randomDivideExactly6Tv);
                    break;
            }
            return false;
        }
    };


    private MessageHandler handler = new MessageHandler(callback);

    private final static int WHAT_INCREASE_TENTH_SEC = -1;
    private final static int WHAT_RANDOM_1 = 1;
    private final static int WHAT_RANDOM_2 = 2;
    private final static int WHAT_RANDOM_3 = 3;
    private final static int WHAT_RANDOM_4 = 4;
    private final static int WHAT_RANDOM_5 = 5;
    private final static int WHAT_RANDOM_6 = 6;

    private int checkRandom(final int what, int random, final TextView tv) {
        int nextWhat;
        int delay = 0;

        if (--random <= 0) {
            random = r.nextInt(50);
            nextWhat = what == WHAT_RANDOM_6 ? WHAT_RANDOM_1 : what + 1;
        } else {
            nextWhat = what;
            delay = r.nextInt(50) + 50;
        }

        tv.setText(String.valueOf(random));
        handler.sendEmptyMessageDelayed(nextWhat, delay);
        return random;

    }

    // 0.1sec
    private int tenthSec;
    // sec
    private int sec;
    // minute
    private int minute;
    // hour
    private int hour;
    // day
    private int day;
    // year
    private int year;

    private int totalTenthSec = 0;

    private CharSequence generateCalculagraph() {
        return String.format("%d, %d, %d:%d:%d.%d", year, day, hour, minute, sec, tenthSec);
    }

    private void increase() {
        totalTenthSec++;
        increaseTenthSec();
    }

    private void increaseTenthSec() {
        if (++tenthSec >= 10) {
            tenthSec = 0;
            increaseSec();
        }
    }

    private void increaseSec() {
        if (++sec >= 60) {
            sec = 0;
            increaseMinute();
        }
    }

    private void increaseMinute() {
        if (++minute >= 60) {
            minute = 0;
            increaseHour();
        }
    }

    private void increaseHour() {
        if (++hour >= 24) {
            hour = 0;
            increaseDay();
        }
    }

    private void increaseDay() {
        if (++day >= 365) {
            day = 0;
            increaseYear();
        }
    }

    private void increaseYear() {
        year++;
    }


    private int random1;
    private int random2;
    private int random3;
    private int random4;
    private int random5;
    private int random6;

    private final Random r = new Random();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        assignViews();

        // just for print log to debug
        MessageHandler.NEED_LOG = true;

        handleLifeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler.isDead()) {
                    onClickCreate(v);
                    handleLifeBtn.setText("kill");
                } else {
                    onClickKill(v);
                    handleLifeBtn.setText("new handler");
                }
            }
        });

    }

    private boolean needResumeMessageOnResume = false;

    @Override
    protected void onPause() {
        super.onPause();
        needResumeMessageOnResume = !handler.isPaused();
        handler.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (needResumeMessageOnResume) {
            handler.resume();
        }
    }

    public void onClickTriggerMessages(final View view) {
        handler.sendEmptyMessage(WHAT_INCREASE_TENTH_SEC);
        handler.sendEmptyMessage(WHAT_RANDOM_1);
        handler.sendEmptyMessage(WHAT_RANDOM_3);
    }

    public void onClickResume(final View view) {
        handler.resume();
    }

    public void onClickPause(final View view) {
        handler.pause();
    }

    public void onClickCancel(final View view) {
        handler.cancelAllMessage();
    }

    public void onClickKill(final View view) {
        handler.killSelf();
    }

    public void onClickCreate(final View view) {
        handler = new MessageHandler(callback);
    }

    private TextView randomDivideExactly1Tv;
    private TextView randomDivideExactly2Tv;
    private TextView randomDivideExactly3Tv;
    private TextView randomDivideExactly4Tv;
    private TextView randomDivideExactly5Tv;
    private TextView randomDivideExactly6Tv;
    private TextView totalTenthSecTv;
    private TextView calculagraphTv;
    private Button handleLifeBtn;

    private void assignViews() {
        randomDivideExactly1Tv = (TextView) findViewById(R.id.random_divide_exactly_1_tv);
        randomDivideExactly2Tv = (TextView) findViewById(R.id.random_divide_exactly_2_tv);
        randomDivideExactly3Tv = (TextView) findViewById(R.id.random_divide_exactly_3_tv);
        randomDivideExactly4Tv = (TextView) findViewById(R.id.random_divide_exactly_4_tv);
        randomDivideExactly5Tv = (TextView) findViewById(R.id.random_divide_exactly_5_tv);
        randomDivideExactly6Tv = (TextView) findViewById(R.id.random_divide_exactly_6_tv);
        totalTenthSecTv = (TextView) findViewById(R.id.total_tenth_sec_tv);
        calculagraphTv = (TextView) findViewById(R.id.calculagraph_tv);
        handleLifeBtn = (Button) findViewById(R.id.handle_life_btn);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_github:
                openGitHub();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openGitHub() {
        Uri uri = Uri.parse(getString(R.string.app_github_url));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
}
