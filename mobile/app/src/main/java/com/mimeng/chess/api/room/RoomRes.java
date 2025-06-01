package com.mimeng.chess.api.room;

import com.mimeng.chess.api.ApiResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 房间API响应数据
 */
public class RoomRes extends ApiResponse<RoomRes.Data> {

  public static class Data {
    public Room room;
    public List<Room> rooms;
  }

  public static class Room {
    public String id;
    public String name;
    public Long player1Id;
    public Long player2Id;
    public String status;
    public String createdAt;
    public String updatedAt;
  }
}
