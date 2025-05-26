package com.mimeng.chess.config;

import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocketIOConfig {
  @Bean
  public SocketIOServer socketIOServer() {
    SocketConfig config = new SocketConfig();
    config.setTcpNoDelay(true);
    config.setSoLinger(0);
    com.corundumstudio.socketio.Configuration serverConfig = new com.corundumstudio.socketio.Configuration();
    serverConfig.setPort(9092); // 可根据需要修改端口
    serverConfig.setSocketConfig(config);
    serverConfig.setOrigin(null);
    return new SocketIOServer(serverConfig);
  }
}
