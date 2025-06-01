# 全面屏适配指南

## BaseActivity 使用说明

本项目提供了 `BaseActivity` 来处理全面屏适配问题，所有 Activity 都应该继承此类而不是直接继承 `AppCompatActivity`。

## 主要功能

### 1. 自动适配

- **状态栏透明**: 自动设置状态栏为透明
- **导航栏透明**: 自动设置导航栏为透明
- **图标颜色**: 默认设置为深色图标（适配浅色背景）
- **窗口内边距**: 自动处理系统 UI 的内边距

### 2. 布局要求

#### 根布局设置

在 Activity 的根布局中添加 `android:fitsSystemWindows="true"`：

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true"
    ...>
```

#### 特殊区域适配

对于需要特殊处理的 View（如顶部工具栏），使用 BaseActivity 提供的方法：

```java
// 在Activity中
private void setupWindowInsets() {
    View toolbar = findViewById(R.id.toolbar);
    if (toolbar != null) {
        applyStatusBarInsets(toolbar); // 仅应用状态栏内边距
    }

    View bottomView = findViewById(R.id.bottom_view);
    if (bottomView != null) {
        applyNavigationBarInsets(bottomView); // 仅应用导航栏内边距
    }
}
```

### 3. 可用方法

#### 状态栏控制

```java
setStatusBarIconsLight(false); // 深色图标（浅色背景）
setStatusBarIconsLight(true);  // 浅色图标（深色背景）
hideStatusBar();               // 隐藏状态栏
showStatusBar();               // 显示状态栏
```

#### 导航栏控制

```java
setNavigationBarIconsLight(false); // 深色图标（浅色背景）
setNavigationBarIconsLight(true);  // 浅色图标（深色背景）
hideNavigationBar();               // 隐藏导航栏
showNavigationBar();               // 显示导航栏
```

#### 沉浸式模式

```java
setImmersiveMode();  // 进入沉浸式模式（隐藏所有系统UI）
exitImmersiveMode(); // 退出沉浸式模式
```

#### 内边距适配

```java
applyStatusBarInsets(view);     // 仅为view应用状态栏内边距
applyNavigationBarInsets(view); // 仅为view应用导航栏内边距
applyWindowInsets();            // 为根视图应用全部系统UI内边距
```

## 实际使用示例

### 1. 普通 Activity

```java
public class MyActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        // BaseActivity已经自动处理了基本适配
        // 如果有特殊需求，可以调用相应方法
    }
}
```

### 2. 需要特殊工具栏适配的 Activity

```java
public class ToolbarActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbar);

        // 为工具栏应用状态栏内边距
        View toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            applyStatusBarInsets(toolbar);
        }
    }
}
```

### 3. 启动页（全屏）

```java
public class SplashActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 使用沉浸式模式
        setImmersiveMode();

        setContentView(R.layout.activity_splash);
    }
}
```

### 4. 深色主题 Activity

```java
public class DarkActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dark);

        // 设置为浅色图标（适配深色背景）
        setStatusBarIconsLight(true);
        setNavigationBarIconsLight(true);
    }
}
```

## 注意事项

1. **所有 Activity 都应继承 BaseActivity**，而不是 AppCompatActivity
2. **根布局必须添加** `android:fitsSystemWindows="true"`
3. **不要手动设置状态栏相关属性**，BaseActivity 会自动处理
4. **特殊区域（如工具栏）需要手动调用适配方法**
5. **对于不同主题，记得调整图标颜色**

## 兼容性

- **最低支持**: Android 5.0 (API 21)
- **完整功能**: Android 6.0+ (API 23+)
- **最新特性**: Android 11+ (API 30+)

低版本设备会自动降级到兼容的实现方式。
