package com.mimeng.chess.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordDTO {
  @NotBlank(message = "旧密码不能为空")
  @Size(min = 6, max = 20, message = "密码长度需为6~20位")
  private String oldPassword;

  @NotBlank(message = "新密码不能为空")
  @Size(min = 6, max = 20, message = "密码长度需为6~20位")
  private String newPassword;

  public String getOldPassword() {
    return oldPassword;
  }

  public void setOldPassword(String oldPassword) {
    this.oldPassword = oldPassword;
  }

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }
}

