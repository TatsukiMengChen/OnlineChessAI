package com.mimeng.chess.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mimeng.chess.dto.ServiceResult;
import com.mimeng.chess.entity.Room;
import com.mimeng.chess.mapper.RoomMapper;
import com.mimeng.chess.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements RoomService {
  @Autowired
  private StringRedisTemplate redisTemplate;

  @Override
  public ServiceResult<Room> createRoom(Long userId, String name) {
    Room exist = lambdaQuery().eq(Room::getPlayer1Id, userId).eq(Room::getStatus, "waiting").one();
    if (exist != null) {
      return ServiceResult.error("您已创建了一个房间，不能重复创建");
    }
    Room room = new Room();
    if (name == null || name.trim().isEmpty()) {
      name = "房间-" + (int) (Math.random() * 9000 + 1000);
    }
    room.setName(name);
    room.setStatus("waiting");
    room.setPlayer1Id(userId);
    room.setUpdatedAt(java.time.LocalDateTime.now());
    save(room);
    // 存入redis，5分钟自动过期（用秒单位）
    redisTemplate.opsForValue().set("room:waiting:" + room.getId(), "1", 300, TimeUnit.SECONDS);
    return ServiceResult.success("房间创建成功", room);
  }

  @Override
  public ServiceResult<String> closeRoom(String roomId, Long userId) {
    Room room = getById(roomId);
    if (room == null) {
      return ServiceResult.error("房间不存在");
    }
    if (!userId.equals(room.getPlayer1Id())) {
      return ServiceResult.error("只有房主才能关闭房间");
    }
    removeById(roomId);
    redisTemplate.delete("room:waiting:" + roomId);
    return ServiceResult.success("房间已关闭并删除");
  }

  @Override
  public ServiceResult<Room> joinRoom(String roomId, Long userId) {
    Room room = getById(roomId);
    if (room == null) {
      return ServiceResult.error("房间不存在");
    }
    if (!"waiting".equals(room.getStatus())) {
      return ServiceResult.error("房间状态不是waiting，无法加入");
    }
    if (room.getPlayer1Id() != null && room.getPlayer1Id().equals(userId)) {
      return ServiceResult.error("您已是房主，无需加入");
    }
    if (room.getPlayer2Id() != null && room.getPlayer2Id() != 0) {
      return ServiceResult.error("房间已满");
    }
    room.setPlayer2Id(userId);
    room.setStatus("full"); // 只设置为满，不设置为playing
    room.setUpdatedAt(java.time.LocalDateTime.now());
    updateById(room);
    // 不再删除redis定时，等socket双方都准备好再删
    return ServiceResult.success("加入房间成功", room);
  }

  @Override
  public ServiceResult<String> quitRoom(String roomId, Long userId) {
    Room room = getById(roomId);
    if (room == null) {
      return ServiceResult.error("房间不存在");
    }
    // 只有房主或玩家2才能退出
    boolean isOwner = userId.equals(room.getPlayer1Id());
    boolean isPlayer2 = userId.equals(room.getPlayer2Id());
    if (!isOwner && !isPlayer2) {
      return ServiceResult.error("你不在该房间");
    }
    // 房主退出，房间直接关闭
    if (isOwner) {
      String redisKey = "room:waiting:" + roomId;
      room.setStatus("closed");
      room.setPlayer2Id(0L); // 退出时清空player2Id
      room.setUpdatedAt(java.time.LocalDateTime.now());
      updateById(room);
      removeById(roomId);
      redisTemplate.delete(redisKey);
      return ServiceResult.success("房主退出，房间已关闭并删除");
    }
    // 玩家2退出，房间回到waiting
    if (isPlayer2) {
      room.setPlayer2Id(0L); // 置为0而不是null
      room.setStatus("waiting");
      room.setUpdatedAt(java.time.LocalDateTime.now());
      updateById(room);
      // 重新设置redis定时，5分钟
      redisTemplate.opsForValue().set("room:waiting:" + roomId, "1", 300, java.util.concurrent.TimeUnit.SECONDS);
      return ServiceResult.success("已退出房间");
    }
    return ServiceResult.error("未知错误");
  }
}
