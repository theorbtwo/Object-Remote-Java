package uk.me.desert_island.theorbtwo.bridge;

import android.util.Log;
//import .PrintyThing;

public class PrintyThingAndroid extends PrintyThing {
    private String LOGTAG = "JavaBridgeService::PrintyThing";
    private Log logger;

    public PrintyThingAndroid(String my_tag) {
        LOGTAG = my_tag;
    }

    void print(String str) {
        Log.e(LOGTAG, str);
    }
}

