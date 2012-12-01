package uk.me.desert_island.theorbtwo.bridge;

import android.app.Service;

// Misnamed somewhat...
public class AndroidServiceStash {
    private static Service private_service;

    public static Service get_service() {
        return private_service;
    }

    public static void set_service(Service new_service) {
        private_service = new_service;
    }

}