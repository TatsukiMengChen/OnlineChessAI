package com.mimeng.chess.api;

import android.content.Context;
import android.text.TextUtils;

import com.mimeng.chess.utils.AuthManager;

import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiManager {
    private static OkHttpClient client;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static Context appContext;

    /**
     * 初始化ApiManager，设置应用上下文
     */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
        initClient();
    }

    /**
     * 初始化OkHttp客户端，添加认证拦截器
     */
    private static void initClient() {
        client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor())
                .build();
    }

    /**
     * 认证拦截器，自动添加Bearer token到请求头
     */
    private static class AuthInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();

            // 如果没有上下文，直接使用原始请求
            if (appContext == null) {
                return chain.proceed(originalRequest);
            }

            // 获取token
            AuthManager authManager = AuthManager.getInstance(appContext);
            String token = authManager.getBearerToken();

            // 如果没有token或者是登录/注册等认证接口，直接使用原始请求
            if (TextUtils.isEmpty(token) || isAuthEndpoint(originalRequest.url().toString())) {
                return chain.proceed(originalRequest);
            }

            // 添加Authorization头
            Request authenticatedRequest = originalRequest.newBuilder()
                    .header("Authorization", token)
                    .build();

            return chain.proceed(authenticatedRequest);
        }

        /**
         * 判断是否是认证相关的接口，这些接口不需要添加token
         */
        private boolean isAuthEndpoint(String url) {
            return url.contains("/auth/login") ||
                    url.contains("/auth/register") ||
                    url.contains("/auth/sendCode");
        }
    }

    /**
     * 获取客户端实例，如果未初始化则创建默认客户端
     */
    private static OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient();
        }
        return client;
    }

    public static void get(String url, Callback callback) {
        Request request = new Request.Builder().url(url).build();
        getClient().newCall(request).enqueue(callback);
    }

    public static void post(String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(url).post(body).build();
        getClient().newCall(request).enqueue(callback);
    }

    public static Response getSync(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        return getClient().newCall(request).execute();
    }

    public static Response postSync(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(url).post(body).build();
        return getClient().newCall(request).execute();
    }
}
