package com.mimeng.chess.api.room;

import com.mimeng.chess.api.BaseApi;
import okhttp3.Callback;

/**
 * 房间相关API
 */
public class RoomApi extends BaseApi {

  // 房间模块URL集中管理
  public static class urls {
    public static final String BASE = BASE_URL + "/room";
    public static final String LIST = BASE + "/list";
    public static final String CREATE = BASE + "/create";
    public static final String JOIN = BASE + "/join";
    public static final String CLOSE = BASE + "/close";
    public static final String QUIT = BASE + "/quit";
  }

  /**
   * 获取房间列表
   */
  public void listRooms(Callback callback) {
    get(urls.LIST, callback);
  }

  /**
   * 创建房间
   */
  public void createRoom(Object data, Callback callback) {
    post(urls.CREATE, data, callback);
  }

  /**
   * 加入房间
   */
  public void joinRoom(String roomId, Callback callback) {
    post(urls.JOIN + "?roomId=" + roomId, "{}", callback);
  }

  /**
   * 关闭房间
   */
  public void closeRoom(String roomId, Callback callback) {
    post(urls.CLOSE + "?roomId=" + roomId, "{}", callback);
  }

  /**
   * 退出房间
   */
  public void quitRoom(String roomId, Callback callback) {
    post(urls.QUIT + "?roomId=" + roomId, "{}", callback);
  }

  // 单例实例
  public static final RoomApi instance = new RoomApi();
}
