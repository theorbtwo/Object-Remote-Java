package uk.me.desert_island.theorbtwo.bridge;

import android.app.Service;
import android.app.Activity;

// Misnamed somewhat...
public class AndroidServiceStash {
    private static Activity private_activity;

    public static Activity get_activity() {
        return private_activity;
    }

    public static void set_activity(Activity new_activity) {
        private_activity = new_activity;
    }
}