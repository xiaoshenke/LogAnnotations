package wuxian.me.logannotationsdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import wuxian.me.logannotations.LOG;
import wuxian.me.logannotations.NoLog;

@NoLog
public class MainActivity extends AppCompatActivity {

    @LOG
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    protected void testA() {
        Log.e("main", "testA");
    }

    protected void testB() {
        ;
    }
}
