# 中国象棋对战平台 - 设计文档

## 概述

这是一个完整的中国象棋对战平台的核心类设计，支持前后端兼容，适用于 Java 后端服务器和 Android 客户端开发。

## 主要特性

✅ **棋局状态管理** - 完整的游戏状态存储和管理  
✅ **玩家信息管理** - 支持人类玩家和 AI 玩家  
✅ **房间信息管理** - 房间创建、加入、观战等功能  
✅ **棋子移动逻辑** - 每个棋子都有完整的移动规则实现  
✅ **实时胜负判断** - 将军、将死、困毙等情况判断  
✅ **前后端兼容** - 原生 Java 设计，无外部依赖  
✅ **可扩展设计** - 工厂模式，便于扩展新功能

## 核心类结构

### 1. 基础枚举类

- `PlayerColor` - 玩家颜色（红/黑）
- `PieceType` - 棋子类型（将、仕、象、马、车、炮、兵）
- `PlayerType` - 玩家类型（人类、简单 AI、中等 AI、困难 AI）
- `GameStatus` - 游戏状态（等待、进行中、胜负、平局等）

### 2. 基础数据类

- `Position` - 棋盘坐标类，包含位置验证和计算方法
- `Move` - 移动操作类，记录移动信息
- `Player` - 玩家信息类，支持人类和 AI 玩家

### 3. 棋子类体系

- `ChessPiece` - 棋子抽象基类
- `pieces/` 包下的具体棋子实现：
  - `King` - 将/帅
  - `Guard` - 仕/士
  - `Elephant` - 象/相
  - `Horse` - 马
  - `Rook` - 车
  - `Cannon` - 炮
  - `Pawn` - 兵/卒

### 4. 游戏逻辑类

- `ChessBoard` - 棋盘类，管理棋子布局和移动
- `GameLogic` - 游戏逻辑类，处理将军、将死等规则判断
- `ChessGameState` - 核心游戏状态类，管理完整游戏流程

### 5. 房间管理类

- `ChessRoom` - 象棋房间类，扩展基础 Room，添加游戏功能
- `ChessGameFactory` - 游戏工厂类，创建不同类型的游戏

## 使用场景

### 后端 Socket 消息处理

```java
// 创建房间
ChessRoom room = ChessGameFactory.createHumanVsHumanGame("在线对战");

// 处理玩家加入
room.joinPlayer(userId, playerName, PlayerType.HUMAN);

// 处理移动消息
if (room.playerMove(userId, from, to)) {
    // 广播移动给所有客户端
    broadcastMove(room, from, to);
}
```

### Android 前端渲染

```java
ChessGameState game = room.getGameState();

// 获取棋盘状态用于渲染
ChessBoard board = game.getBoard();
for (int row = 0; row < 10; row++) {
    for (int col = 0; col < 9; col++) {
        ChessPiece piece = board.getPieceAt(new Position(row, col));
        // 渲染棋子到UI
    }
}

// 处理用户点击
game.selectPiece(clickedPosition);
List<Position> availableMoves = game.getAvailableMoves();
// 高亮显示可移动位置
```

### 人机对战

```java
// 创建人机对战
ChessRoom room = ChessGameFactory.createHumanVsAIGame(
    "人机对战", userId, playerName, PlayerType.AI_MEDIUM
);

// AI自动移动（需要实现AI算法）
if (game.getCurrentPlayerInfo().isAI()) {
    Position aiMove = calculateAIMove(game);
    game.tryMove(aiMove.from, aiMove.to);
}
```

## 核心特性详解

### 1. 完整的中国象棋规则

- **棋子移动规则**：每个棋子都有完整的移动规则实现
- **特殊限制**：九宫格限制、过河限制、马腿卡子、象眼卡子等
- **将军检查**：实时检查将军状态
- **胜负判断**：将死、困毙、将帅照面等

### 2. 前端交互支持

- **棋子选择**：`selectedPiece` 记录当前选中的棋子
- **移动提示**：`availableMoves` 提供可移动位置列表
- **视觉提示**：`showHints` 控制是否显示移动提示

### 3. 扩展性设计

- **工厂模式**：轻松创建不同类型的游戏
- **策略模式**：AI 难度可以通过 PlayerType 扩展
- **观察者模式**：游戏状态变化可以触发事件

### 4. 网络兼容

- **状态序列化**：支持 JSON 序列化用于网络传输
- **增量更新**：只传输变化的部分，减少网络开销
- **状态同步**：确保前后端状态一致

## 扩展建议

### 1. AI 算法集成

可以在 Player 类的基础上实现不同难度的 AI：

- 简单 AI：随机移动
- 中等 AI：基础评估函数
- 困难 AI：Alpha-Beta 剪枝

### 2. 数据持久化

可以添加数据库持久化：

- 游戏记录保存
- 玩家统计
- 排行榜系统

### 3. 实时通信

集成 WebSocket 或 Socket.IO：

- 实时移动同步
- 聊天功能
- 观战功能

### 4. 移动端优化

针对 Android 开发：

- 触摸手势处理
- 动画效果
- 离线模式

## 文件清单

```
chess/
├── PlayerColor.java          # 玩家颜色枚举
├── PieceType.java           # 棋子类型枚举
├── PlayerType.java          # 玩家类型枚举
├── GameStatus.java          # 游戏状态枚举
├── Position.java            # 棋盘坐标类
├── Move.java               # 移动操作类
├── Player.java             # 玩家信息类
├── ChessPiece.java         # 棋子抽象基类
├── ChessBoard.java         # 棋盘管理类
├── GameLogic.java          # 游戏逻辑类
├── ChessGameState.java     # 核心游戏状态类
├── ChessRoom.java          # 象棋房间类
├── ChessGameFactory.java   # 游戏工厂类
├── ChessGameExample.java   # 使用示例
└── pieces/                 # 具体棋子实现
    ├── King.java           # 将/帅
    ├── Guard.java          # 仕/士
    ├── Elephant.java       # 象/相
    ├── Horse.java          # 马
    ├── Rook.java           # 车
    ├── Cannon.java         # 炮
    └── Pawn.java           # 兵/卒
```
