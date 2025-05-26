# OnlineChessAI-BE

## 简介
本项目为基于 Spring Boot + MyBatis-Plus + PostgreSQL + Redis 的象棋 AI 对战平台

## 技术栈
- Java 21
- Spring Boot 3
- MyBatis-Plus
- PostgreSQL
- Redis
- Docker

## 快速开始

### 1. 克隆项目
```bash
git clone <your-repo-url>
cd OnlineChessAI-BE
```

### 2. 配置环境变量
复制 `.env` 文件并根据实际情况填写数据库、Redis、邮箱等信息：
```bash
cp .env.example .env
# 编辑 .env 文件，填写你的配置信息
```

### 3. 启动服务（推荐 Docker Compose）
确保已安装 Docker 和 Docker Compose。
```bash
docker-compose up --build
```

- 服务启动后，后端接口默认监听 8080 端口。
- PostgreSQL 默认监听 5432，Redis 默认监听 6379。

### 4. 数据库初始化
首次启动会自动创建数据库。请手动建表：
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);
```

### 5. 常用接口
- `POST /auth/sendCode` 发送邮箱验证码
- `POST /auth/register` 用户注册
- `POST /auth/login` 用户登录

参数均为 `email`、`password`、`code`（验证码）等。

## 本地开发
如需本地开发，可直接运行：
```bash
./gradlew bootRun
```
或用 IDEA 等工具直接运行主类。

## 其他说明
- 敏感信息全部通过 `.env` 文件配置，`application.properties` 仅作变量占位。
- 邮箱服务需使用 QQ 邮箱授权码。
- 详细配置请参考 `application-example.properties` 和 `.env` 文件。

---

如有问题欢迎提 issue。

---

## 使用 Prisma 自动生成 SQL 脚本（推荐自动建表方案）

1. 安装 Node.js 和 Prisma CLI：

```bash
npm install -g prisma
```

2. 初始化 Prisma 项目：

```bash
npx prisma init
```

3. 编辑 `prisma/schema.prisma`，例如：

```prisma
model User {
  id       Int    @id @default(autoincrement())
  email    String @unique
  password String
}
```

4. 配置 `.env` 文件（Prisma 连接字符串格式）：

```
DB_URL=postgresql://chessuser:chesspass@localhost:5432/onlinechess
```

5. 生成 SQL 脚本（不会直接执行到数据库）：

```bash
npx prisma migrate diff --from-empty --to-schema-datamodel=./prisma/schema.prisma --script > ./prisma/schema.sql
```

6. 将生成的 `prisma_schema.sql` 拷贝到 Java 项目中执行，或用数据库工具导入。

---

如需自动同步数据库结构，推荐用 Prisma 维护表结构，结合 Java 项目开发。
