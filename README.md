# 在线象棋 AI 对战平台

这是一个全栈的在线象棋对战平台，支持人机对战和在线对战，使用现代化的技术栈构建。

## 🚀 项目结构

```
OnlineChessAI/
├── backend/          # Java Spring Boot 后端服务
├── frontend/         # Web 前端应用
├── mobile/           # Android 移动应用
├── shared/           # 共享代码和类型定义
└── docs/             # 项目文档
```

## 🛠 技术栈

### 后端 (Backend)

- **Java 21** - 现代 Java 开发
- **Spring Boot 3** - 微服务框架
- **MyBatis-Plus** - 数据库 ORM
- **PostgreSQL** - 主数据库
- **Redis** - 缓存和会话存储
- **Socket.IO** - 实时通信
- **Docker** - 容器化部署

### 前端 (Frontend)

- **React/Vue.js** - 现代前端框架
- **TypeScript** - 类型安全的 JavaScript
- **Webpack/Vite** - 构建工具
- **Socket.IO Client** - 实时通信客户端

### 移动端 (Mobile)

- **Android Native** - 原生 Android 开发
- **Java/Kotlin** - 开发语言
- **Socket.IO Android** - 实时通信

### 共享 (Shared)

- **Chess Core Logic** - 象棋游戏核心逻辑
- **Protocol Buffers** - 数据序列化
- **API Types** - 接口类型定义

## 🚀 快速开始

### 使用 Docker 一键启动

```bash
# 克隆项目
git clone <your-repo-url>
cd OnlineChessAI

# 启动所有服务
docker-compose up -d

# 访问应用
# 前端: http://localhost:3000
# 后端API: http://localhost:8080
# 数据库: localhost:5432
```

### 开发环境启动

#### 后端开发

```bash
cd backend
./gradlew bootRun
```

#### 前端开发

```bash
cd frontend
npm install
npm run dev
```

#### 移动端开发

```bash
cd mobile
./gradlew assembleDebug
```

## 📁 目录详情

### Backend 后端

负责业务逻辑处理、数据存储、用户认证、游戏状态管理等核心功能。

### Frontend 前端

提供 Web 端用户界面，支持响应式设计，适配 PC 和移动端浏览器。

### Mobile 移动端

原生 Android 应用，提供更好的移动端用户体验。

### Shared 共享代码

- `chess-core/` - 象棋游戏核心逻辑，可被后端、前端、移动端共享
- `proto/` - Protocol Buffers 定义，用于客户端-服务器通信
- `types/` - 通用类型定义

## 🔧 开发指南

### 象棋核心逻辑

所有象棋相关的游戏逻辑都在 `shared/chess-core/` 中实现，包括：

- 棋子移动规则
- 胜负判断
- 游戏状态管理
- 玩家管理

### API 设计

- RESTful API 用于用户管理、房间管理等
- WebSocket 用于实时游戏通信
- 统一的错误处理和响应格式

### 数据库设计

- 用户表 (users)
- 房间表 (rooms)
- 游戏记录表 (game_records)
- 排行榜表 (leaderboards)

## 🔨 部署

### 生产环境部署

```bash
# 构建所有镜像
docker-compose -f docker-compose.prod.yml build

# 部署到生产环境
docker-compose -f docker-compose.prod.yml up -d
```

### CI/CD 流水线

- GitHub Actions 自动化构建
- 自动化测试
- 容器化部署

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

## 👥 团队

- **后端开发** - 负责 API 设计和业务逻辑
- **前端开发** - 负责用户界面和用户体验
- **移动端开发** - 负责 Android 应用开发
- **AI 算法** - 负责象棋 AI 算法实现

## 📞 联系我们

如有问题或建议，请通过以下方式联系：

- 提交 Issue
- 发送邮件到 [your-email@example.com]
- 加入我们的讨论群
