package com.mimeng.chess.service.impl;

import com.mimeng.chess.dto.ServiceResult;
import com.mimeng.chess.dto.auth.AuthRes;
import com.mimeng.chess.entity.User;
import com.mimeng.chess.service.AuthService;
import com.mimeng.chess.service.UserService;
import com.mimeng.chess.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {
  @Autowired
  private UserService userService;
  @Autowired
  private StringRedisTemplate redisTemplate;
  @Autowired
  private JavaMailSender mailSender;
  @Autowired
  private PasswordEncoder passwordEncoder;

  @Value("${spring.mail.username}")
  private String mailUsername;

  @Override
  public ServiceResult<String> sendCode(String email) {
    try {
      String code = String.valueOf(new Random().nextInt(899999) + 100000);
      redisTemplate.opsForValue().set("email:code:" + email, code, 5, TimeUnit.MINUTES);
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(mailUsername); // 设置发件人，必须与认证用户一致
      message.setTo(email);
      message.setSubject("【OnlineChessAI】验证码");
      message.setText("您的验证码是：" + code + "，有效期5分钟。");
      mailSender.send(message);
      return ServiceResult.success("验证码已发送");
    } catch (Exception e) {
      return ServiceResult.error("验证码发送失败");
    }
  }

  @Override
  public ServiceResult<AuthRes> register(String email, String password, String code) {
    String redisCode = redisTemplate.opsForValue().get("email:code:" + email);
    if (!StringUtils.hasText(redisCode) || !redisCode.equals(code)) {
      return ServiceResult.error("验证码错误或已过期");
    }
    if (userService.findByEmail(email) != null) {
      return ServiceResult.error("邮箱已注册");
    }

    String hash = passwordEncoder.encode(password);
    User user = new User();
    user.setEmail(email);
    user.setPassword(hash);
    userService.save(user);
    redisTemplate.delete("email:code:" + email);

    // 生成JWT token
    String token = JwtUtil.generateToken(String.valueOf(user.getId()), user.getEmail());
    AuthRes authRes = new AuthRes(token, new AuthRes.UserInfo(user));

    return ServiceResult.success("注册成功", authRes);
  }

  @Override
  public ServiceResult<AuthRes> login(String email, String password) {
    User user = userService.findByEmail(email);
    if (user == null) {
      return ServiceResult.error("邮箱或密码错误");
    }
    if (!passwordEncoder.matches(password, user.getPassword())) {
      return ServiceResult.error("邮箱或密码错误");
    }

    // 登录成功，生成JWT
    String token = JwtUtil.generateToken(String.valueOf(user.getId()), user.getEmail());
    AuthRes authRes = new AuthRes(token, new AuthRes.UserInfo(user));

    return ServiceResult.success("登录成功", authRes);
  }

  @Override
  public ServiceResult<String> changePassword(String email, String oldPassword, String newPassword) {
    User user = userService.findByEmail(email);
    if (user == null) {
      return ServiceResult.error("用户不存在");
    }
    if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
      return ServiceResult.error("旧密码错误");
    }
    user.setPassword(passwordEncoder.encode(newPassword));
    userService.updateById(user); // 修改为 updateById，避免主键冲突
    return ServiceResult.success("密码修改成功");
  }
}
