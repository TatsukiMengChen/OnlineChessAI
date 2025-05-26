package com.mimeng.chess.dto;

public class ServiceResult<T> {
  private boolean success;
  private String message;
  private T data;

  public ServiceResult() {
  }

  public ServiceResult(boolean success, String message, T data) {
    this.success = success;
    this.message = message;
    this.data = data;
  }

  public static <T> ServiceResult<T> success(T data) {
    return new ServiceResult<>(true, "操作成功", data);
  }

  public static <T> ServiceResult<T> success(String message, T data) {
    return new ServiceResult<>(true, message, data);
  }

  public static <T> ServiceResult<T> error(String message) {
    return new ServiceResult<>(false, message, null);
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }
}
