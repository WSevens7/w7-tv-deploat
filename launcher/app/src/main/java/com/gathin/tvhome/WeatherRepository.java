package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherRepository {
    private static final String TAG = "WeatherRepository";
    private static final String PREFS_NAME = "weather_cache";
    private static final String KEY_DATA = "cached_weather";
    private static final String KEY_TIMESTAMP = "cache_timestamp";
    private static final long CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(10);

    // Semarang coordinates
    private static final double DEFAULT_LAT = -6.9667;
    private static final double DEFAULT_LON = 110.4167;
    private static final String DEFAULT_LOCATION = "Semarang";

    private final Context context;
    private final OkHttpClient client;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final Handler mainHandler;

    public interface WeatherCallback {
        void onWeather(WeatherModel weather);
        void onError(String error);
    }

    public WeatherRepository(Context context) {
        this.context = context.getApplicationContext();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void fetchWeather(WeatherCallback callback) {
        // Try cache first
        long cachedTime = prefs.getLong(KEY_TIMESTAMP, 0);
        if (System.currentTimeMillis() - cachedTime < CACHE_DURATION_MS) {
            String cachedJson = prefs.getString(KEY_DATA, null);
            if (cachedJson != null) {
                try {
                    WeatherModel cached = gson.fromJson(cachedJson, WeatherModel.class);
                    if (cached != null) {
                        mainHandler.post(() -> callback.onWeather(cached));
                        return;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse cached weather", e);
                }
            }
        }

        // Fetch from network
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code&timezone=auto",
                DEFAULT_LAT, DEFAULT_LON);

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Weather fetch failed", e);
                // Try to serve stale cache
                String cachedJson = prefs.getString(KEY_DATA, null);
                if (cachedJson != null) {
                    try {
                        WeatherModel stale = gson.fromJson(cachedJson, WeatherModel.class);
                        if (stale != null) {
                            mainHandler.post(() -> callback.onWeather(stale));
                            return;
                        }
                    } catch (Exception ignored) {}
                }
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    onFailure(call, new IOException("HTTP " + response.code()));
                    return;
                }
                String body = response.body().string();
                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    JsonObject current = json.getAsJsonObject("current");

                    float temp = current.get("temperature_2m").getAsFloat();
                    int humidity = current.get("relative_humidity_2m").getAsInt();
                    float wind = current.get("wind_speed_10m").getAsFloat();
                    int code = current.get("weather_code").getAsInt();

                    WeatherModel weather = new WeatherModel(temp, humidity, wind, code, DEFAULT_LOCATION);

                    // Cache it
                    prefs.edit()
                            .putString(KEY_DATA, gson.toJson(weather))
                            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                            .apply();

                    mainHandler.post(() -> callback.onWeather(weather));

                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse weather JSON", e);
                    onFailure(call, new IOException("Parse error: " + e.getMessage()));
                }
            }
        });
    }

    // Allow setting custom location later
    public void fetchWeather(double lat, double lon, String locationName, WeatherCallback callback) {
        // For future multi-location support
        // Currently uses default
        fetchWeather(callback);
    }
}