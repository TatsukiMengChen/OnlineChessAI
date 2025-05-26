package com.mimeng.chess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mimeng.chess.entity.User;

public interface UserService extends IService<User> {
  User findByEmail(String email);
}

