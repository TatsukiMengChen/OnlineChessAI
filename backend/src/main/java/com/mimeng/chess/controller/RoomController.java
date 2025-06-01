package com.mimeng.chess.controller;

import com.mimeng.chess.dto.ApiRes;
import com.mimeng.chess.dto.ServiceResult;
import com.mimeng.chess.dto.room.CreateRoomDTO;
import com.mimeng.chess.entity.Room;
import com.mimeng.chess.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/room")
public class RoomController {
  @Autowired
  private RoomService roomService;

  @PreAuthorize("isAuthenticated()")
  @PostMapping("/create")
  public ApiRes<Room> createRoom(@Valid @RequestBody CreateRoomDTO dto, Authentication authentication) {
    Long userId = Long.valueOf(authentication.getDetails().toString());
    ServiceResult<Room> result = roomService.createRoom(userId, dto.getName());
    if (result.isSuccess()) {
      return ApiRes.success(result.getMessage(), result.getData());
    } else {
      return ApiRes.error(400, result.getMessage(), null);
    }
  }

  @GetMapping("/list")
  public ApiRes<List<Room>> listRooms() {
    List<Room> rooms = roomService.list();
    return ApiRes.success("房间列表获取成功", rooms);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping("/close")
  public ApiRes<String> closeRoom(@RequestParam("roomId") String roomId, Authentication authentication) {
    Long userId = Long.valueOf(authentication.getDetails().toString());
    ServiceResult<String> result = roomService.closeRoom(roomId, userId);
    if (result.isSuccess()) {
      return ApiRes.success(result.getMessage(), null);
    } else {
      return ApiRes.error(400, result.getMessage(), null);
    }
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping("/join")
  public ApiRes<Room> joinRoom(@RequestParam("roomId") String roomId, Authentication authentication) {
    Long userId = Long.valueOf(authentication.getDetails().toString());
    ServiceResult<Room> result = roomService.joinRoom(roomId, userId);
    if (result.isSuccess()) {
      return ApiRes.success(result.getMessage(), result.getData());
    } else {
      return ApiRes.error(400, result.getMessage(), null);
    }
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping("/quit")
  public ApiRes<String> quitRoom(@RequestParam("roomId") String roomId, Authentication authentication) {
    Long userId = Long.valueOf(authentication.getDetails().toString());
    ServiceResult<String> result = roomService.quitRoom(roomId, userId);
    if (result.isSuccess()) {
      return ApiRes.success(result.getMessage(), null);
    } else {
      return ApiRes.error(400, result.getMessage(), null);
    }
  }
}
