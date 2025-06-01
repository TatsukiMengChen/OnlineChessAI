package com.mimeng.chess.api.room;

/**
 * 创建房间请求数据
 */
public class CreateRoomReq {
  public String name;

  public CreateRoomReq() {
  }

  public CreateRoomReq(String name) {
    this.name = name;
  }
}
