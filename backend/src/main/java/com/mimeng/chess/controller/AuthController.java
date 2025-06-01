package com.mimeng.chess.controller;

import com.mimeng.chess.dto.auth.LoginDTO;
import com.mimeng.chess.dto.auth.RegisterDTO;
import com.mimeng.chess.dto.auth.SendCodeDTO;
import com.mimeng.chess.dto.auth.ChangePasswordDTO;
import com.mimeng.chess.dto.auth.AuthRes;
import com.mimeng.chess.dto.ApiRes;
import com.mimeng.chess.dto.ServiceResult;
import com.mimeng.chess.service.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.mimeng.chess.dto.auth.ResetPasswordDTO;

@RestController
@RequestMapping("/auth")
public class AuthController {
  @Autowired
  private AuthService authService;

  @PostMapping("/sendCode")
  public ApiRes<String> sendCode(@Valid @RequestBody SendCodeDTO sendCodeDTO) {
    ServiceResult<String> result = authService.sendCode(sendCodeDTO.getEmail());
    if (result.isSuccess()) {
      return ApiRes.success(result.getMessage(), result.getData());
    } else {
      return ApiRes.error(400, "发送失败", result.getMessage());
    }
  }

  @PostMapping("/register")
  public ApiRes<AuthRes> register(@Valid @RequestBody RegisterDTO registerDTO) {
    ServiceResult<AuthRes> result = authService.register(registerDTO.getEmail(), registerDTO.getPassword(),
        registerDTO.getCode());
    if (result.isSuccess()) {
      return ApiRes.success(result.getMessage(), result.getData());
    } else {
      return ApiRes.error(400, "注册失败", result.getMessage());
    }
  }

  @PostMapping("/login")
  public ApiRes<AuthRes> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletResponse response) {
    ServiceResult<AuthRes> result = authService.login(loginDTO.getEmail(), loginDTO.getPassword());
    if (result.isSuccess()) {
      response.setHeader("Authorization", "Bearer " + result.getData().getAccessToken());
      return ApiRes.success(result.getMessage(), result.getData());
    } else {
      return ApiRes.error(401, "登录失败", result.getMessage());
    }
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping("/changePassword")
  public ApiRes<String> changePassword(@Valid @RequestBody ChangePasswordDTO dto, Authentication authentication) {
    String email = authentication.getName();
    ServiceResult<String> result = authService.changePassword(email, dto.getOldPassword(), dto.getNewPassword());
    if (result.isSuccess()) {
      return ApiRes.success(result.getMessage(), result.getData());
    } else {
      return ApiRes.error(400, "修改失败", result.getMessage());
    }
  }

  @PostMapping("/resetPassword")
  public ApiRes<String> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
    ServiceResult<String> result = authService.resetPassword(dto.getEmail(), dto.getCode(), dto.getNewPassword());
    if (result.isSuccess()) {
      return ApiRes.success(result.getMessage(), result.getData());
    } else {
      return ApiRes.error(400, "重置失败", result.getMessage());
    }
  }
}
