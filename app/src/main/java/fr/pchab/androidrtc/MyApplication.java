package fr.pchab.androidrtc;

import android.app.Application;

/**
 * Created by duongnx on 3/14/2017.
 */

public class MyApplication extends Application {
    static MyApplication instance;
    private String loginUser;

    public static MyApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }


    public String getLoginUser() {
        return loginUser;
    }

    public void setLoginUser(String loginUser) {
        this.loginUser = loginUser;
    }
}
