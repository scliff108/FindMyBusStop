package com.tailoreddevelopmentgroup.findmybusstop;

import com.parse.Parse;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(new Parse.Configuration.Builder(getApplicationContext())
                .applicationId("69a0d581afcd5eedffe508968c5e11ade47dd4a0")
                .clientKey(null)
                .server("http://ec2-35-163-254-224.us-west-2.compute.amazonaws.com:80/parse")
                .build());
    }
}
