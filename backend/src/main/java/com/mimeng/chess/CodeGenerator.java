package com.mimeng.chess;

import java.nio.file.Paths;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import io.github.cdimascio.dotenv.Dotenv;

public class CodeGenerator {
  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    String dbUrl = dotenv.get("SPRING_DATASOURCE_URL");
    String dbUser = dotenv.get("SPRING_DATASOURCE_USERNAME");
    String dbPassword = dotenv.get("SPRING_DATASOURCE_PASSWORD");
    FastAutoGenerator.create(
        dbUrl, // 数据库地址
        dbUser, // 数据库用户名
        dbPassword // 数据库密码
    )
        .globalConfig(builder -> builder
            .author("your_name")
            .outputDir(Paths.get(System.getProperty("user.dir"), "src/main/java").toString())
            .commentDate("yyyy-MM-dd"))
        .packageConfig(builder -> builder
            .parent("com.mimeng.chess")
            .entity("entity")
            .mapper("mapper")
            .service("service")
            .serviceImpl("service.impl")
            .controller("controller")
            .xml("mapper.xml"))
        .strategyConfig(builder -> builder
            .addInclude("users") // 需要生成的表名
            .entityBuilder().enableLombok())
        .templateEngine(new FreemarkerTemplateEngine()) // 使用 Freemarker 模板引擎
        .execute();
  }
}
