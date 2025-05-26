package com.mimeng.chess.dto.auth;

import com.mimeng.chess.entity.User;

public class AuthRes {
  private String accessToken;
  private UserInfo user;

  public AuthRes() {
  }

  public AuthRes(String accessToken, UserInfo user) {
    this.accessToken = accessToken;
    this.user = user;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public UserInfo getUser() {
    return user;
  }

  public void setUser(UserInfo user) {
    this.user = user;
  }

  public static class UserInfo {
    private Long id;
    private String email;

    public UserInfo() {
    }

    public UserInfo(Long id, String email) {
      this.id = id;
      this.email = email;
    }

    public UserInfo(User user) {
      this.id = user.getId();
      this.email = user.getEmail();
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }
  }
}
