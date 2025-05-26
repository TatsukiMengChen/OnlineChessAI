package com.mimeng.chess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rooms")
public class Room {
  @TableId(type = IdType.ASSIGN_UUID)
  private String id;
  private String name; // 房间名
  private Long player1Id; // 玩家1用户ID
  private Long player2Id; // 玩家2用户ID
  private String status; // 房间状态（如：waiting, playing, finished）
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
