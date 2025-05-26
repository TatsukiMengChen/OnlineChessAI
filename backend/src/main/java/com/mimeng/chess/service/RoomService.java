package com.mimeng.chess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mimeng.chess.dto.ServiceResult;
import com.mimeng.chess.entity.Room;

public interface RoomService extends IService<Room> {
  ServiceResult<Room> createRoom(Long userId, String name);

  ServiceResult<String> closeRoom(String roomId, Long userId);

  ServiceResult<Room> joinRoom(String roomId, Long userId);

  ServiceResult<String> quitRoom(String roomId, Long userId);
}
