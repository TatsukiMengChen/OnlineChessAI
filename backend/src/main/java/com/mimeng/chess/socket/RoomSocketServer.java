package com.mimeng.chess.socket;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.SocketIOClient;
import io.jsonwebtoken.Claims;
import com.mimeng.chess.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.InitializingBean;

@Component
public class RoomSocketServer implements InitializingBean {
  @Autowired
  private SocketIOServer socketIOServer;

  @Override
  public void afterPropertiesSet() {
    socketIOServer.addConnectListener(new ConnectListener() {
      @Override
      public void onConnect(SocketIOClient client) {
        String roomId = client.getHandshakeData().getSingleUrlParam("id");
        if (roomId == null) {
          client.disconnect();
          return;
        }
        client.sendEvent("need_auth", "请发送token进行鉴权");
      }
    });
    socketIOServer.addEventListener("auth", Object.class, (client, data, ackSender) -> {
      String token = null;
      if (data instanceof String) {
        token = (String) data;
      } else if (data instanceof java.util.Map map && map.get("token") instanceof String) {
        token = (String) map.get("token");
      }
      if (token == null) {
        client.sendEvent("auth_fail", "token缺失");
        client.disconnect();
        return;
      }
      try {
        Claims claims = JwtUtil.parseToken(token);
        // 可根据 claims 获取用户信息
        // client.joinRoom(roomId); // 可选：加入房间
        client.sendEvent("auth_success", "鉴权成功");
      } catch (Exception e) {
        client.sendEvent("auth_fail", "token无效");
        client.disconnect();
      }
    });
    socketIOServer.addDisconnectListener(new DisconnectListener() {
      @Override
      public void onDisconnect(SocketIOClient client) {
        // 断开连接处理
      }
    });
    socketIOServer.start();
  }
}
