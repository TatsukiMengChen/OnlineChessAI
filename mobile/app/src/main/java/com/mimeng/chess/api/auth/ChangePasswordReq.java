package com.mimeng.chess.api.auth;

public class ChangePasswordReq {
  public String oldPassword;
  public String newPassword;

  public ChangePasswordReq(String oldPassword, String newPassword) {
    this.oldPassword = oldPassword;
    this.newPassword = newPassword;
  }
}
