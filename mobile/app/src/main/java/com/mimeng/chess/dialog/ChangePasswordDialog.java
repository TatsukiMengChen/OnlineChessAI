package com.mimeng.chess.dialog;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;
import com.mimeng.chess.R;
import com.mimeng.chess.api.ApiResponse;
import com.mimeng.chess.api.auth.AuthApi;
import com.mimeng.chess.api.auth.ChangePasswordReq;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

/**
 * 修改密码对话框
 */
public class ChangePasswordDialog {

  public interface OnPasswordChangedListener {
    void onPasswordChanged();
  }

  private Context context;
  private OnPasswordChangedListener listener;
  private Dialog dialog;
  private EditText etOldPassword;
  private EditText etNewPassword;
  private EditText etConfirmPassword;
  private Button btnCancel;
  private Button btnConfirm;
  private TextView tvTitle;
  private boolean isLoading = false;
  private Gson gson;

  public ChangePasswordDialog(@NonNull Context context) {
    this.context = context;
    this.gson = new Gson();
    initDialog();
  }

  public void setOnPasswordChangedListener(OnPasswordChangedListener listener) {
    this.listener = listener;
  }

  private void initDialog() {
    // 创建自定义布局
    View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_change_password, null);

    // 初始化视图
    initViews(dialogView);

    // 设置监听器
    setupListeners();

    // 创建对话框
    dialog = new AlertDialog.Builder(context)
        .setView(dialogView)
        .setCancelable(true)
        .create();

    // 设置对话框样式
    if (dialog.getWindow() != null) {
      dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
    }
  }

  private void initViews(View dialogView) {
    tvTitle = dialogView.findViewById(R.id.tv_title);
    etOldPassword = dialogView.findViewById(R.id.et_old_password);
    etNewPassword = dialogView.findViewById(R.id.et_new_password);
    etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);
    btnCancel = dialogView.findViewById(R.id.btn_cancel);
    btnConfirm = dialogView.findViewById(R.id.btn_confirm);
  }

  private void setupListeners() {
    btnCancel.setOnClickListener(v -> dismiss());
    btnConfirm.setOnClickListener(v -> changePassword());
  }

  public void show() {
    if (dialog != null && !dialog.isShowing()) {
      // 清空输入框
      etOldPassword.setText("");
      etNewPassword.setText("");
      etConfirmPassword.setText("");
      dialog.show();
    }
  }

  public void dismiss() {
    if (dialog != null && dialog.isShowing()) {
      dialog.dismiss();
    }
  }

  private void changePassword() {
    String oldPassword = etOldPassword.getText().toString().trim();
    String newPassword = etNewPassword.getText().toString().trim();
    String confirmPassword = etConfirmPassword.getText().toString().trim();

    // 输入验证
    if (!validateInput(oldPassword, newPassword, confirmPassword)) {
      return;
    }

    // 设置加载状态
    setLoadingState(true);

    // 调用修改密码API
    ChangePasswordReq req = new ChangePasswordReq(oldPassword, newPassword);
    AuthApi.instance.changePassword(req, new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        // 切换到主线程更新UI
        if (context instanceof android.app.Activity) {
          ((android.app.Activity) context).runOnUiThread(() -> {
            setLoadingState(false);
            showMessage("网络错误，请稍后重试");
          });
        }
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        String body = response.body() != null ? response.body().string() : "";

        // 切换到主线程更新UI
        if (context instanceof android.app.Activity) {
          ((android.app.Activity) context).runOnUiThread(() -> {
            setLoadingState(false);
            handleResponse(body);
          });
        }
      }
    });
  }

  private boolean validateInput(String oldPassword, String newPassword, String confirmPassword) {
    if (TextUtils.isEmpty(oldPassword)) {
      showMessage("请输入当前密码");
      etOldPassword.requestFocus();
      return false;
    }

    if (TextUtils.isEmpty(newPassword)) {
      showMessage("请输入新密码");
      etNewPassword.requestFocus();
      return false;
    }

    if (newPassword.length() < 6) {
      showMessage("新密码长度不能少于6位");
      etNewPassword.requestFocus();
      return false;
    }

    if (TextUtils.isEmpty(confirmPassword)) {
      showMessage("请确认新密码");
      etConfirmPassword.requestFocus();
      return false;
    }

    if (!newPassword.equals(confirmPassword)) {
      showMessage("两次输入的新密码不一致");
      etConfirmPassword.requestFocus();
      return false;
    }

    if (oldPassword.equals(newPassword)) {
      showMessage("新密码不能与当前密码相同");
      etNewPassword.requestFocus();
      return false;
    }

    return true;
  }

  private void setLoadingState(boolean loading) {
    isLoading = loading;
    btnConfirm.setEnabled(!loading);
    btnConfirm.setText(loading ? "修改中..." : "确认修改");
    etOldPassword.setEnabled(!loading);
    etNewPassword.setEnabled(!loading);
    etConfirmPassword.setEnabled(!loading);
  }

  private void handleResponse(String body) {
    try {
      ApiResponse<String> response = gson.fromJson(body, ApiResponse.class);

      if (response.code == 200) {
        // 修改成功
        showMessage("密码修改成功");
        dismiss();
        if (listener != null) {
          listener.onPasswordChanged();
        }
      } else {
        // 修改失败
        String errorMsg = !TextUtils.isEmpty(response.msg) ? response.msg : "密码修改失败";
        showMessage(errorMsg);
      }
    } catch (Exception e) {
      showMessage("密码修改失败，请稍后重试");
    }
  }

  private void showMessage(String message) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
  }
}
