package com.mimeng.chess.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mimeng.chess.entity.User;
import com.mimeng.chess.mapper.UserMapper;
import com.mimeng.chess.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
  @Autowired
  private UserMapper userMapper;

  @Override
  public User findByEmail(String email) {
    return lambdaQuery().eq(User::getEmail, email).one();
  }
}

