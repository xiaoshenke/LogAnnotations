package wuxian.me.logannotationsdemo.sub;

import android.util.Log;

import wuxian.me.logannotations.LOG;
import wuxian.me.logannotations.LogAll;
import wuxian.me.logannotations.NoLog;
import wuxian.me.logannotationsdemo.IWhatever;
import wuxian.me.logannotationsdemo.MainActivity;

/**
 * Created by wuxian on 23/11/2016.
 */

@NoLog
public class SubMainActivity extends MainActivity {

    @Override
    protected void testA() {

    }

    public void say() {

    }

    @LOG
    private void testC() {

        IWhatever whatever = new IWhatever() {
            @LOG
            @Override
            public void say() {

            }
        };
    }

    private class What implements IWhatever {
        @LOG
        @Override
        public void say() {

        }
    }

}
