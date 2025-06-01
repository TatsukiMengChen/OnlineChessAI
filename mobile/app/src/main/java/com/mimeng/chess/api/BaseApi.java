package com.mimeng.chess.api;

import com.google.gson.Gson;
import okhttp3.Callback;
import com.mimeng.chess.BuildConfig;

public abstract class BaseApi {
    protected static final String BASE_URL = BuildConfig.BASE_URL;
    protected static final Gson gson = new Gson();

    protected void post(String url, Object body, Callback callback) {
        String json = body != null ? gson.toJson(body) : "{}";
        ApiManager.post(url, json, callback);
    }

    protected void get(String url, Callback callback) {
        ApiManager.get(url, callback);
    }
}
