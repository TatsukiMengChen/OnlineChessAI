package com.mimeng.chess;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.mimeng.chess.mapper")
public class OnlineChessAiBeApplication {
  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    dotenv.entries().forEach(entry -> {
      if (System.getenv(entry.getKey()) == null) {
        System.setProperty(entry.getKey(), entry.getValue());
      }
    });
    SpringApplication.run(OnlineChessAiBeApplication.class, args);
  }
}
