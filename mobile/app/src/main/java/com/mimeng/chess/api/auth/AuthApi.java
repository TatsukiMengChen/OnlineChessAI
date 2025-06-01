package com.mimeng.chess.api.auth;

import com.mimeng.chess.api.BaseApi;
import com.mimeng.chess.BuildConfig;
import okhttp3.Callback;

public class AuthApi extends BaseApi {
    // Auth模块自己的url集中管理
    public static class urls {
        public static final String BASE = BASE_URL + "/auth";
        public static final String LOGIN = BASE + "/login";
        public static final String REGISTER = BASE + "/register";
        public static final String SEND_CODE = BASE + "/sendCode";
        public static final String CHANGE_PASSWORD = BASE + "/changePassword";
    }

    public void sendCode(Object data, Callback callback) {
        post(urls.SEND_CODE, data, callback);
    }

    public void register(Object data, Callback callback) {
        post(urls.REGISTER, data, callback);
    }

    public void login(Object data, Callback callback) {
        post(urls.LOGIN, data, callback);
    }

    public void changePassword(Object data, Callback callback) {
        post(urls.CHANGE_PASSWORD, data, callback);
    }

    // 单例实例
    public static final AuthApi instance = new AuthApi();
}
