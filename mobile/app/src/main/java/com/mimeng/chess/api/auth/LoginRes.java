package com.mimeng.chess.api.auth;

import com.mimeng.chess.api.ApiResponse;

public class LoginRes extends ApiResponse<LoginRes.Data> {
    public static class Data {
        public String accessToken;
        public User user;
    }

    public static class User {
        public int id;
        public String email;
    }
}

