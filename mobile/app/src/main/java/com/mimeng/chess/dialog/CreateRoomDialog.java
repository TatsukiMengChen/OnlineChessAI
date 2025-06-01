package com.mimeng.chess.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import android.widget.Button;
import com.google.gson.Gson;
import com.mimeng.chess.R;
import com.mimeng.chess.api.room.CreateRoomReq;
import com.mimeng.chess.api.room.Room;
import com.mimeng.chess.api.room.RoomApi;
import com.mimeng.chess.api.ApiResponse;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

/**
 * 创建房间对话框
 */
public class CreateRoomDialog extends Dialog {
  private EditText etRoomName;
  private Button btnCancel;
  private Button btnConfirm;
  private OnRoomCreatedListener listener;
  private RoomApi roomApi;
  private Gson gson;
  private Handler mainHandler;

  public interface OnRoomCreatedListener {
    void onRoomCreated(Room room);
  }

  public CreateRoomDialog(@NonNull Context context) {
    super(context);
    roomApi = new RoomApi();
    gson = new Gson();
    mainHandler = new Handler(Looper.getMainLooper());
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.dialog_create_room);

    // 设置对话框背景为圆角
    if (getWindow() != null) {
      getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);

      // 设置对话框宽度
      android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
      params.width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.85);
      getWindow().setAttributes(params);
    }

    initViews();
    setupListeners();
  }

  private void initViews() {
    etRoomName = findViewById(R.id.et_room_name);
    btnCancel = findViewById(R.id.btn_cancel);
    btnConfirm = findViewById(R.id.btn_confirm);
  }

  private void setupListeners() {
    btnCancel.setOnClickListener(v -> dismiss());
    btnConfirm.setOnClickListener(v -> createRoom());
  }

  private void createRoom() {
    String roomName = etRoomName.getText().toString().trim();

    // 禁用按钮防止重复点击
    btnConfirm.setEnabled(false);
    btnConfirm.setText("创建中...");

    CreateRoomReq request = new CreateRoomReq();
    if (!TextUtils.isEmpty(roomName)) {
      request.name = roomName;
    }

    roomApi.createRoom(request, new Callback() {
      @Override
      public void onFailure(@NonNull Call call, @NonNull IOException e) {
        // 切换到主线程更新UI
        mainHandler.post(() -> {
          showMessage("网络错误，请检查网络连接");
          resetButton();
        });
      }

      @Override
      public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
        String responseBody = response.body().string();

        mainHandler.post(() -> {
          if (response.isSuccessful()) {
            try {
              ApiResponse<Room> apiResponse = gson.fromJson(responseBody,
                  new com.google.gson.reflect.TypeToken<ApiResponse<Room>>() {
                  }.getType());

              if (apiResponse.code == 200 && apiResponse.data != null) {
                showMessage("房间创建成功");
                if (listener != null) {
                  listener.onRoomCreated(apiResponse.data);
                }
                dismiss();
              } else {
                showMessage(apiResponse.msg != null ? apiResponse.msg : "创建房间失败");
                resetButton();
              }
            } catch (Exception e) {
              e.printStackTrace();
              showMessage("响应解析失败");
              resetButton();
            }
          } else {
            try {
              ApiResponse<?> errorResponse = gson.fromJson(responseBody, ApiResponse.class);
              showMessage(errorResponse.msg != null ? errorResponse.msg : "创建房间失败");
            } catch (Exception e) {
              showMessage("创建房间失败");
            }
            resetButton();
          }
        });
      }
    });
  }

  private void resetButton() {
    btnConfirm.setEnabled(true);
    btnConfirm.setText("创建");
  }

  private void showMessage(String message) {
    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
  }

  public void setOnRoomCreatedListener(OnRoomCreatedListener listener) {
    this.listener = listener;
  }
}
