package com.mimeng.chess.service;

import com.mimeng.chess.dto.ServiceResult;
import com.mimeng.chess.dto.auth.AuthRes;

public interface AuthService {
  ServiceResult<String> sendCode(String email);

  ServiceResult<AuthRes> register(String email, String password, String code);

  ServiceResult<AuthRes> login(String email, String password);

  ServiceResult<String> changePassword(String email, String oldPassword, String newPassword);
}

