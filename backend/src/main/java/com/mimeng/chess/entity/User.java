package com.mimeng.chess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("users")
public class User {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String email;
  private String password;
}

