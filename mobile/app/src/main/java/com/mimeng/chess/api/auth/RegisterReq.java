package com.mimeng.chess.api.auth;

public class RegisterReq {
  public String email;
  public String password;
  public String code;

  public RegisterReq(String email, String password, String code) {
    this.email = email;
    this.password = password;
    this.code = code;
  }
}
