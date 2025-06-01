package com.mimeng.chess;

import android.app.Application;
import com.mimeng.chess.api.ApiManager;

/**
 * 国际象棋应用程序类
 * 用于应用程序初始化
 */
public class ChessApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    // 初始化API管理器
    ApiManager.init(this);
  }
}
