package com.mimeng.chess.activity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * 基础Activity - 处理全面屏适配和通用设置
 */
public abstract class BaseActivity extends AppCompatActivity {
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // 全面屏适配将在setContentView之后进行
  }

  /**
   * 初始化全面屏适配
   */
  private void initFullScreenAdapter() {
    // 确保Window对象已经初始化
    if (getWindow() == null) {
      return;
    }

    // 启用沉浸式状态栏
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

    // 设置状态栏和导航栏透明
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().setStatusBarColor(Color.TRANSPARENT);
      getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }

    // 设置状态栏图标颜色 - 默认为深色（适配浅色背景）
    setStatusBarIconsLight(false);

    // 设置导航栏图标颜色 - 默认为深色（适配浅色背景）
    setNavigationBarIconsLight(false);
  }

  /**
   * 设置状态栏图标颜色
   * 
   * @param isLight true为浅色图标，false为深色图标
   */
  protected void setStatusBarIconsLight(boolean isLight) {
    if (getWindow() == null) {
      return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // Android 11 及以上
      WindowInsetsController controller = getWindow().getInsetsController();
      if (controller != null) {
        if (isLight) {
          controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        } else {
          controller.setSystemBarsAppearance(
              WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
              WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      // Android 6.0 及以上
      View decorView = getWindow().getDecorView();
      int flags = decorView.getSystemUiVisibility();
      if (isLight) {
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      } else {
        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      }
      decorView.setSystemUiVisibility(flags);
    }
  }

  /**
   * 设置导航栏图标颜色
   * 
   * @param isLight true为浅色图标，false为深色图标
   */
  protected void setNavigationBarIconsLight(boolean isLight) {
    if (getWindow() == null) {
      return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // Android 11 及以上
      WindowInsetsController controller = getWindow().getInsetsController();
      if (controller != null) {
        if (isLight) {
          controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        } else {
          controller.setSystemBarsAppearance(
              WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
              WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      // Android 8.0 及以上
      View decorView = getWindow().getDecorView();
      int flags = decorView.getSystemUiVisibility();
      if (isLight) {
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
      } else {
        flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
      }
      decorView.setSystemUiVisibility(flags);
    }
  }

  /**
   * 设置内容视图并应用窗口内边距
   * 建议子类重写此方法来设置布局
   */
  @Override
  public void setContentView(int layoutResID) {
    super.setContentView(layoutResID);
    // 在setContentView之后进行全面屏适配
    initFullScreenAdapter();
    applyWindowInsets();
  }

  /**
   * 应用窗口内边距适配
   */
  protected void applyWindowInsets() {
    View rootView = findViewById(android.R.id.content);
    if (rootView != null) {
      ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
        Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

        // 只应用顶部状态栏内边距，底部导航栏不设置内边距
        // 让子类可以根据需要自定义处理
        v.setPadding(
            0, // 左边距设为0
            systemBars.top, // 保留状态栏内边距
            0, // 右边距设为0
            0); // 底部内边距设为0，让内容可以延伸到导航栏

        return insets;
      });
    }
  }

  /**
   * 为指定视图应用状态栏内边距
   * 
   * @param view 需要适配的视图
   */
  protected void applyStatusBarInsets(View view) {
    ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
      v.setPadding(
          v.getPaddingLeft(),
          systemBars.top,
          v.getPaddingRight(),
          v.getPaddingBottom());
      return insets;
    });
  }

  /**
   * 为指定视图应用导航栏内边距
   * 
   * @param view 需要适配的视图
   */
  protected void applyNavigationBarInsets(View view) {
    ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
      v.setPadding(
          v.getPaddingLeft(),
          v.getPaddingTop(),
          v.getPaddingRight(),
          systemBars.bottom);
      return insets;
    });
  }

  /**
   * 隐藏状态栏
   */
  protected void hideStatusBar() {
    if (getWindow() == null) {
      return; // Window未初始化，直接返回
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      WindowInsetsController controller = getWindow().getInsetsController();
      if (controller != null) {
        controller.hide(WindowInsets.Type.statusBars());
      }
    } else {
      getWindow().setFlags(
          WindowManager.LayoutParams.FLAG_FULLSCREEN,
          WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
  }

  /**
   * 显示状态栏
   */
  protected void showStatusBar() {
    if (getWindow() == null) {
      return; // Window未初始化，直接返回
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      WindowInsetsController controller = getWindow().getInsetsController();
      if (controller != null) {
        controller.show(WindowInsets.Type.statusBars());
      }
    } else {
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
  }

  /**
   * 隐藏导航栏
   */
  protected void hideNavigationBar() {
    if (getWindow() == null) {
      return; // Window未初始化，直接返回
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      WindowInsetsController controller = getWindow().getInsetsController();
      if (controller != null) {
        controller.hide(WindowInsets.Type.navigationBars());
      }
    } else {
      View decorView = getWindow().getDecorView();
      if (decorView != null) {
        int flags = decorView.getSystemUiVisibility();
        flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(flags);
      }
    }
  }

  /**
   * 显示导航栏
   */
  protected void showNavigationBar() {
    if (getWindow() == null) {
      return; // Window未初始化，直接返回
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      WindowInsetsController controller = getWindow().getInsetsController();
      if (controller != null) {
        controller.show(WindowInsets.Type.navigationBars());
      }
    } else {
      View decorView = getWindow().getDecorView();
      if (decorView != null) {
        int flags = decorView.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(flags);
      }
    }
  }

  /**
   * 设置沉浸式模式（隐藏状态栏和导航栏）
   */
  protected void setImmersiveMode() {
    if (getWindow() == null) {
      return; // Window未初始化，直接返回
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      WindowInsetsController controller = getWindow().getInsetsController();
      if (controller != null) {
        controller.hide(WindowInsets.Type.systemBars());
        controller.setSystemBarsBehavior(
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
      }
    } else {
      View decorView = getWindow().getDecorView();
      if (decorView != null) {
        int flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(flags);
      }
    }
  }

  /**
   * 退出沉浸式模式
   */
  protected void exitImmersiveMode() {
    if (getWindow() == null) {
      return; // Window未初始化，直接返回
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      WindowInsetsController controller = getWindow().getInsetsController();
      if (controller != null) {
        controller.show(WindowInsets.Type.systemBars());
      }
    } else {
      View decorView = getWindow().getDecorView();
      if (decorView != null) {
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
      }
    }
  }

  /**
   * 应用窗口内边距适配 - 完整版本
   * 子类可以调用此方法来应用完整的系统栏内边距
   */
  protected void applyFullWindowInsets() {
    View rootView = findViewById(android.R.id.content);
    if (rootView != null) {
      ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
        Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

        // 应用完整的系统栏内边距
        v.setPadding(
            systemBars.left,
            systemBars.top,
            systemBars.right,
            systemBars.bottom);

        return insets;
      });
    }
  }

  /**
   * 应用窗口内边距适配 - 仅状态栏版本
   * 子类可以调用此方法来只应用状态栏内边距
   */
  protected void applyStatusBarOnlyInsets() {
    View rootView = findViewById(android.R.id.content);
    if (rootView != null) {
      ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
        Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

        // 只应用状态栏内边距
        v.setPadding(
            0,
            systemBars.top,
            0,
            0);

        return insets;
      });
    }
  }
}
