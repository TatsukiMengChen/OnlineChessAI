package com.mimeng.chess.api.auth;

public class ResetPasswordReq {
  public String email;
  public String code;
  public String newPassword;

  public ResetPasswordReq(String email, String code, String newPassword) {
    this.email = email;
    this.code = code;
    this.newPassword = newPassword;
  }
}
