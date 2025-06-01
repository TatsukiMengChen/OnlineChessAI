package com.mimeng.chess.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.mimeng.chess.R;
import com.mimeng.chess.api.auth.AuthApi;
import com.mimeng.chess.api.auth.SendCodeReq;
import com.mimeng.chess.api.auth.SendCodeRes;
import com.mimeng.chess.api.auth.RegisterReq;
import com.mimeng.chess.api.auth.LoginRes;
import com.mimeng.chess.utils.AuthManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

/**
 * 注册页面
 */
public class RegisterActivity extends AppCompatActivity {
  private EditText etEmail;
  private EditText etPassword;
  private EditText etCode;
  private Button btnSendCode;
  private Button btnRegister;
  private TextView tvLogin;
  private ScrollView scrollView;

  private AuthManager authManager;
  private Gson gson;
  private CountDownTimer countDownTimer;
  private boolean isCodeSent = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // 删除标题栏
    if (getSupportActionBar() != null) {
      getSupportActionBar().hide();
    }

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_register);

    // 设置状态栏图标为深色，适配白色背景
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

    initViews();
    initData();
    setupListeners();
  }

  /**
   * 初始化视图
   */
  private void initViews() {
    etEmail = findViewById(R.id.et_email);
    etPassword = findViewById(R.id.et_password);
    etCode = findViewById(R.id.et_code);
    btnSendCode = findViewById(R.id.btn_send_code);
    btnRegister = findViewById(R.id.btn_register);
    tvLogin = findViewById(R.id.tv_login);
    scrollView = findViewById(R.id.scroll_view);
  }

  /**
   * 初始化数据
   */
  private void initData() {
    authManager = AuthManager.getInstance(this);
    gson = new Gson();
  }

  /**
   * 设置监听器
   */
  private void setupListeners() {
    btnSendCode.setOnClickListener(v -> sendVerificationCode());
    btnRegister.setOnClickListener(v -> performRegister());
    tvLogin.setOnClickListener(v -> navigateToLogin());

    // 设置输入框的IME监听器
    setupInputListeners();
  }

  /**
   * 设置输入框的软键盘监听器
   */
  private void setupInputListeners() {
    // 邮箱输入框按下"下一步"时焦点移到密码输入框
    etEmail.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_NEXT) {
        etPassword.requestFocus();
        return true;
      }
      return false;
    });

    // 密码输入框按下"下一步"时焦点移到验证码输入框
    etPassword.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_NEXT) {
        if (isCodeSent) {
          etCode.requestFocus();
        } else {
          sendVerificationCode();
        }
        return true;
      }
      return false;
    });

    // 验证码输入框按下"完成"时执行注册
    etCode.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        hideKeyboard();
        performRegister();
        return true;
      }
      return false;
    });

    // 输入框获得焦点时滚动到可见位置
    etEmail.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        v.postDelayed(() -> scrollToView(v), 300);
      }
    });

    etPassword.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        v.postDelayed(() -> scrollToView(v), 300);
      }
    });

    etCode.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        v.postDelayed(() -> scrollToView(v), 300);
      }
    });
  }

  /**
   * 滚动到指定视图位置
   */
  private void scrollToView(View view) {
    scrollView.post(() -> {
      int[] location = new int[2];
      view.getLocationInWindow(location);
      int viewTop = location[1];

      int screenHeight = getResources().getDisplayMetrics().heightPixels;
      int keyboardHeight = screenHeight / 3;
      int visibleHeight = screenHeight - keyboardHeight;

      if (viewTop > visibleHeight - view.getHeight() - 100) {
        int scrollY = viewTop - visibleHeight + view.getHeight() + 100;
        scrollView.smoothScrollTo(0, Math.max(0, scrollY));
      }
    });
  }

  /**
   * 隐藏软键盘
   */
  private void hideKeyboard() {
    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    View currentFocus = getCurrentFocus();
    if (currentFocus != null && imm != null) {
      imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
    }
  }

  /**
   * 发送验证码
   */
  private void sendVerificationCode() {
    String email = etEmail.getText().toString().trim();

    // 验证邮箱
    if (!validateEmail(email)) {
      return;
    }

    // 设置发送验证码的加载状态
    setSendCodeLoadingState(true);

    // 调用发送验证码API
    SendCodeReq req = new SendCodeReq(email);
    AuthApi.instance.sendCode(req, new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        Log.e("SEND_CODE", "发送验证码请求失败: " + e.getMessage(), e);
        runOnUiThread(() -> {
          setSendCodeLoadingState(false);
          showMessage("网络错误，请稍后重试");
        });
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        String body = response.body() != null ? response.body().string() : "";
        Log.d("SEND_CODE", "发送验证码响应: " + body);

        runOnUiThread(() -> {
          setSendCodeLoadingState(false);
          handleSendCodeResponse(body);
        });
      }
    });
  }

  /**
   * 执行注册
   */
  private void performRegister() {
    hideKeyboard();

    String email = etEmail.getText().toString().trim();
    String password = etPassword.getText().toString().trim();
    String code = etCode.getText().toString().trim();

    // 输入验证
    if (!validateRegisterInput(email, password, code)) {
      return;
    }

    // 设置加载状态
    setRegisterLoadingState(true);

    // 调用注册API
    RegisterReq req = new RegisterReq(email, password, code);
    AuthApi.instance.register(req, new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        Log.e("REGISTER", "注册请求失败: " + e.getMessage(), e);
        runOnUiThread(() -> {
          setRegisterLoadingState(false);
          showMessage("网络错误，请稍后重试");
        });
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        String body = response.body() != null ? response.body().string() : "";
        Log.d("REGISTER", "注册响应: " + body);

        runOnUiThread(() -> {
          setRegisterLoadingState(false);
          handleRegisterResponse(body);
        });
      }
    });
  }

  /**
   * 验证邮箱
   */
  private boolean validateEmail(String email) {
    if (TextUtils.isEmpty(email)) {
      showMessage("请输入邮箱");
      etEmail.requestFocus();
      return false;
    }

    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      showMessage("请输入有效的邮箱地址");
      etEmail.requestFocus();
      return false;
    }

    return true;
  }

  /**
   * 验证注册输入
   */
  private boolean validateRegisterInput(String email, String password, String code) {
    if (!validateEmail(email)) {
      return false;
    }

    if (TextUtils.isEmpty(password)) {
      showMessage("请输入密码");
      etPassword.requestFocus();
      return false;
    }

    if (password.length() < 6) {
      showMessage("密码长度不能少于6位");
      etPassword.requestFocus();
      return false;
    }

    if (TextUtils.isEmpty(code)) {
      showMessage("请输入验证码");
      etCode.requestFocus();
      return false;
    }

    if (!isCodeSent) {
      showMessage("请先发送验证码");
      return false;
    }

    return true;
  }

  /**
   * 设置发送验证码加载状态
   */
  private void setSendCodeLoadingState(boolean isLoading) {
    btnSendCode.setEnabled(!isLoading);
    btnSendCode.setText(isLoading ? "发送中..." : "发送验证码");
    etEmail.setEnabled(!isLoading);
  }

  /**
   * 设置注册加载状态
   */
  private void setRegisterLoadingState(boolean isLoading) {
    btnRegister.setEnabled(!isLoading && isCodeSent);
    btnRegister.setText(isLoading ? "注册中..." : "注册");
    etPassword.setEnabled(!isLoading);
    etCode.setEnabled(!isLoading && isCodeSent);
  }

  /**
   * 处理发送验证码响应
   */
  private void handleSendCodeResponse(String body) {
    try {
      SendCodeRes sendCodeRes = gson.fromJson(body, SendCodeRes.class);

      if (sendCodeRes.code == 200) {
        // 发送成功
        isCodeSent = true;
        etCode.setEnabled(true);
        btnRegister.setEnabled(true);
        showMessage("验证码已发送，请查收邮箱");
        startCountDown();
      } else {
        // 发送失败
        String errorMsg = !TextUtils.isEmpty(sendCodeRes.msg) ? sendCodeRes.msg : "发送验证码失败";
        showMessage(errorMsg);
      }
    } catch (Exception e) {
      Log.e("SEND_CODE", "解析发送验证码响应失败", e);
      showMessage("发送验证码失败，请稍后重试");
    }
  }

  /**
   * 处理注册响应
   */
  private void handleRegisterResponse(String body) {
    try {
      LoginRes registerRes = gson.fromJson(body, LoginRes.class);

      if (registerRes.code == 200 && registerRes.data != null) {
        // 注册成功，直接保存登录信息并跳转到主页面
        authManager.saveLoginResponse(registerRes.data);
        showMessage("注册成功");
        navigateToMain();
      } else {
        // 注册失败
        String errorMsg = !TextUtils.isEmpty(registerRes.msg) ? registerRes.msg : "注册失败";
        showMessage(errorMsg);
      }
    } catch (Exception e) {
      Log.e("REGISTER", "解析注册响应失败", e);
      showMessage("注册失败，请稍后重试");
    }
  }

  /**
   * 开始倒计时
   */
  private void startCountDown() {
    countDownTimer = new CountDownTimer(60000, 1000) {
      @Override
      public void onTick(long millisUntilFinished) {
        btnSendCode.setEnabled(false);
        btnSendCode.setText("重新发送(" + millisUntilFinished / 1000 + "s)");
      }

      @Override
      public void onFinish() {
        btnSendCode.setEnabled(true);
        btnSendCode.setText("重新发送");
      }
    }.start();
  }

  /**
   * 跳转到主页面
   */
  private void navigateToMain() {
    Intent intent = new Intent(this, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
  }

  /**
   * 跳转到登录页面
   */
  private void navigateToLogin() {
    finish(); // 关闭注册页面，返回登录页面
  }

  /**
   * 显示消息
   */
  private void showMessage(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (countDownTimer != null) {
      countDownTimer.cancel();
    }
  }
}
