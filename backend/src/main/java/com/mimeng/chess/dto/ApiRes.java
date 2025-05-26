package com.mimeng.chess.dto;

public class ApiRes<T> {
  private int code;
  private String msg;
  private T data;
  private String error;

  public ApiRes() {
  }

  public ApiRes(int code, String msg, T data, String error) {
    this.code = code;
    this.msg = msg;
    this.data = data;
    this.error = error;
  }

  public static <T> ApiRes<T> success(T data) {
    return new ApiRes<>(200, "success", data, null);
  }

  public static <T> ApiRes<T> success(String msg, T data) {
    return new ApiRes<>(200, msg, data, null);
  }

  public static <T> ApiRes<T> error(int code, String msg, String error) {
    return new ApiRes<>(code, msg, null, error);
  }

  // getter/setter
  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }
}

