package net.hoz.netapi.client.util;

import com.google.gson.Gson;
import com.google.inject.Inject;

public class GsonProvider {
    private static Gson gson;

    @Inject
    public GsonProvider(Gson gson) {
        GsonProvider.gson = gson;
    }

    public static Gson getGson() {
        return gson;
    }
}
