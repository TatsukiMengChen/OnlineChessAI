package com.mimeng.chess;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mimeng.chess.api.auth.AuthApi;
import com.mimeng.chess.api.auth.LoginReq;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 删除标题栏
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 设置状态栏图标为深色，适配白色背景
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        // 打印 BASE_URL
        Log.d("BASE_URL", BuildConfig.BASE_URL);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        Button btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(v -> {
            LoginReq req = new LoginReq("email@example.com", "123456");
            AuthApi.instance.login(req, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("LOGIN_RES", "请求失败: " + e.getMessage(), e);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    Log.d("LOGIN_RES", body);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, body, Toast.LENGTH_LONG).show());
                }
            });
        });
    }
}
