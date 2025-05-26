package com.mimeng.chess.api;

public class ApiResponse<T> {
    public int code;
    public String msg;
    public T data;
    public Object error;
}

