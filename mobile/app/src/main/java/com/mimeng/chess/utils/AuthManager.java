package com.mimeng.chess.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.mimeng.chess.api.auth.LoginRes;
import com.google.gson.Gson;

/**
 * 认证管理工具类
 * 用于管理用户登录状态、token和用户信息
 */
public class AuthManager {
  private static final String PREF_NAME = "chess_auth";
  private static final String KEY_TOKEN = "access_token";
  private static final String KEY_USER = "user_info";
  private static final String KEY_IS_LOGGED_IN = "is_logged_in";

  private static AuthManager instance;
  private SharedPreferences preferences;
  private Gson gson;

  private AuthManager(Context context) {
    preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    gson = new Gson();
  }

  /**
   * 获取单例实例
   */
  public static synchronized AuthManager getInstance(Context context) {
    if (instance == null) {
      instance = new AuthManager(context);
    }
    return instance;
  }

  /**
   * 保存登录信息
   */
  public void saveLoginInfo(String token, LoginRes.User user) {
    SharedPreferences.Editor editor = preferences.edit();
    editor.putString(KEY_TOKEN, token);
    editor.putString(KEY_USER, gson.toJson(user));
    editor.putBoolean(KEY_IS_LOGGED_IN, true);
    editor.apply();
  }

  /**
   * 保存登录响应信息
   */
  public void saveLoginResponse(LoginRes.Data data) {
    saveLoginInfo(data.accessToken, data.user);
  }

  /**
   * 获取访问令牌
   */
  public String getToken() {
    return preferences.getString(KEY_TOKEN, null);
  }

  /**
   * 获取用户信息
   */
  public LoginRes.User getUser() {
    String userJson = preferences.getString(KEY_USER, null);
    if (TextUtils.isEmpty(userJson)) {
      return null;
    }
    try {
      return gson.fromJson(userJson, LoginRes.User.class);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * 检查是否已登录
   */
  public boolean isLoggedIn() {
    boolean isLoggedIn = preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    String token = getToken();
    return isLoggedIn && !TextUtils.isEmpty(token);
  }

  /**
   * 清除登录信息（退出登录）
   */
  public void logout() {
    SharedPreferences.Editor editor = preferences.edit();
    editor.clear();
    editor.apply();
  }

  /**
   * 获取带Bearer前缀的Token
   */
  public String getBearerToken() {
    String token = getToken();
    if (TextUtils.isEmpty(token)) {
      return null;
    }
    return "Bearer " + token;
  }

  /**
   * 检查Token是否存在
   */
  public boolean hasValidToken() {
    return !TextUtils.isEmpty(getToken());
  }
}
