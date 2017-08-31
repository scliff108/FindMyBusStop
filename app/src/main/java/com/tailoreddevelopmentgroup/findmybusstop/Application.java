package com.tailoreddevelopmentgroup.findmybusstop;

import com.parse.Parse;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(new Parse.Configuration.Builder(getApplicationContext())
                .applicationId("[app_id]")
                .clientKey(null)
                .server("[server_url]")
                .build());
    }
}
