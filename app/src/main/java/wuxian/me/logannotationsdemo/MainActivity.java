package wuxian.me.logannotationsdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import wuxian.me.logannotations.Log;
import wuxian.me.logannotations.NoLog;

@NoLog
public class MainActivity extends AppCompatActivity {

    @Log
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    protected void testA() {
        ;
    }

    protected void testB() {
        ;
    }
}
