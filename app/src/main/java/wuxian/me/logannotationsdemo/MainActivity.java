package wuxian.me.logannotationsdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import wuxian.me.logannotations.LOG;
import wuxian.me.logannotations.LogAll;
import wuxian.me.logannotations.NoLog;

@LogAll
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("MainActivity", "in func onCreate ,powered by LOG Annotation");
        setContentView(R.layout.activity_main);
    }


    protected void testA() {
        Log.i("MainActivity", "in func testA ,powered by LOG Annotation");
        ;
    }

    protected void testB() {
        Log.i("MainActivity", "in func testB ,powered by LOG Annotation");
        ;
    }
}
