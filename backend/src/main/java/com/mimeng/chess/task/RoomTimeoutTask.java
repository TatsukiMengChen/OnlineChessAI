package com.mimeng.chess.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mimeng.chess.entity.Room;
import com.mimeng.chess.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RoomTimeoutTask {
  @Autowired
  private StringRedisTemplate redisTemplate;
  @Autowired
  private RoomService roomService;

  // 每分钟检查一次超时房间
  @Scheduled(cron = "0 * * * * ?")
  public void cleanTimeoutRooms() {
    Set<String> keys = redisTemplate.keys("room:waiting:*");
    if (keys != null) {
      for (String key : keys) {
        // 如果key不存在，说明已过期自动清理，无需处理
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key)))
          continue;
        String roomId = key.substring("room:waiting:".length());
        Room room = roomService.getById(roomId);
        if (room != null && "waiting".equals(room.getStatus())) {
          roomService.removeById(roomId);
        }
        redisTemplate.delete(key);
      }
    }
  }
}
