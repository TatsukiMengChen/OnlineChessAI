# Socket.IO 中国象棋游戏协议 - 简化版

## 连接流程

### 1. 建立连接

```
ws://localhost:8080?id=房间ID
```

### 2. 认证

**服务器发送：** `need_auth` - "请发送 token 进行鉴权"

**客户端发送：**

```json
{
  "event": "auth",
  "data": "JWT_TOKEN_HERE"
}
```

**服务器响应：**

- 成功：`auth_success` - "鉴权成功"
- 失败：`auth_fail` - "token 无效"

**认证成功后的广播：**

```json
{
  "event": "player_joined",
  "data": {
    "userId": 123,
    "userName": "User123",
    "message": "User123 加入了房间"
  }
}
```

**注意：** 认证成功后，服务器会自动：

1. 向其他玩家广播新玩家加入信息
2. 发送当前游戏状态给新玩家
3. 发送房间状态给所有玩家

## 核心游戏事件

### 1. 玩家准备

**客户端 → 服务器：**

```json
{
  "event": "player_ready",
  "data": { "ready": true }
}
```

**服务器广播：**

```json
{
  "event": "player_ready_changed",
  "data": {
    "userId": 123,
    "userName": "User123",
    "ready": true,
    "message": "User123 已准备"
  }
}
```

**注意：** 玩家准备状态变更后，服务器会自动：

1. 广播准备状态变更给所有玩家
2. 更新游戏状态和房间状态
3. 检查是否可以开始游戏（双方都准备好时自动开始）

**游戏开始：**

```json
{
  "event": "game_started",
  "data": { "message": "游戏开始！" }
}
```

### 2. 选择棋子

**客户端 → 服务器：**

```json
{
  "event": "select_piece",
  "data": { "row": 0, "col": 4 }
}
```

**服务器广播：**

```json
{
  "event": "piece_selected",
  "data": { "userId": 123, "position": { "row": 0, "col": 4 } }
}
```

**可移动位置（发给选择者）：**

```json
{
  "event": "available_moves",
  "data": {
    "moves": [
      { "row": 1, "col": 4 },
      { "row": 2, "col": 4 }
    ]
  }
}
```

### 3. 移动棋子

**客户端 → 服务器：**

```json
{
  "event": "move_piece",
  "data": {
    "from": { "row": 0, "col": 4 },
    "to": { "row": 1, "col": 4 }
  }
}
```

**服务器广播（成功）：**

```json
{
  "event": "piece_moved",
  "data": {
    "userId": 123,
    "from": { "row": 0, "col": 4 },
    "to": { "row": 1, "col": 4 }
  }
}
```

**服务器响应（失败）：**

```json
{
  "event": "move_failed",
  "data": "移动失败"
}
```

### 4. 特殊操作

#### 投降

```json
// 客户端发送
{"event": "surrender", "data": {}}

// 服务器广播
{
  "event": "player_surrendered",
  "data": {"userId": 123, "userName": "玩家名"}
}
```

#### 悔棋

```json
// 客户端发送
{"event": "undo_move", "data": {}}

// 服务器广播（成功）
{"event": "move_undone", "data": {"userId": 123}}

// 服务器响应（失败）
{"event": "undo_failed", "data": "悔棋失败"}
```

## 状态同步

### 主动获取房间状态

**客户端 → 服务器：**

```json
{
  "event": "get_room_state",
  "data": {}
}
```

**说明：** 客户端可以主动请求获取当前房间状态，服务器会立即返回最新的游戏状态和房间状态。

### 游戏状态更新

```json
{
  "event": "game_state",
  "data": {
    "status": "PLAYING",
    "currentPlayer": "RED",
    "redPlayer": {
      "userId": 123,
      "name": "红方玩家",
      "ready": true
    },
    "blackPlayer": {
      "userId": 456,
      "name": "黑方玩家",
      "ready": true
    },
    "boardState": "updated"
  }
}
```

### 游戏结束

```json
{
  "event": "game_ended",
  "data": {
    "status": "RED_WIN",
    "message": "红方胜利"
  }
}
```

### 玩家进出

#### 玩家加入房间

**触发时机：** 玩家认证成功后自动广播

```json
{
  "event": "player_joined",
  "data": {
    "userId": 123,
    "userName": "User123",
    "message": "User123 加入了房间"
  }
}
```

#### 玩家离开房间

**触发时机：** 玩家断开连接时自动广播

```json
{
  "event": "player_left",
  "data": {
    "userId": 123,
    "userName": "User123",
    "message": "User123 离开了房间"
  }
}
```

### 房间状态更新

**自动发送时机：**

- 玩家加入/离开房间时
- 玩家准备状态变更时
- 游戏开始/结束时

```json
{
  "event": "room_status",
  "data": {
    "status": "WAITING",
    "message": "等待玩家准备",
    "players": {
      "red": {
        "userId": 123,
        "name": "User123",
        "ready": true,
        "online": true,
        "color": "RED"
      },
      "black": {
        "userId": 456,
        "name": "User456",
        "ready": false,
        "online": true,
        "color": "BLACK"
      }
    },
    "waitingForPlayers": false
  }
}
```

## 枚举值

### GameStatus

- `WAITING` - 等待玩家
- `PLAYING` - 游戏中
- `RED_WIN` - 红方胜利
- `BLACK_WIN` - 黑方胜利
- `DRAW` - 平局
- `ABANDONED` - 游戏终止

### PlayerColor

- `RED` - 红方（先手）
- `BLACK` - 黑方（后手）

## 棋盘坐标

- 行（row）：0-9，红方在下方（7-9），黑方在上方（0-2）
- 列（col）：0-8，从左到右

## 客户端示例

### JavaScript

```javascript
const socket = io("ws://localhost:8080?id=房间ID");

// 认证
socket.on("need_auth", () => {
  socket.emit("auth", localStorage.getItem("jwt_token"));
});

// 准备游戏
socket.on("auth_success", () => {
  socket.emit("player_ready", { ready: true });
});

// 移动棋子
function movePiece(fromRow, fromCol, toRow, toCol) {
  socket.emit("move_piece", {
    from: { row: fromRow, col: fromCol },
    to: { row: toRow, col: toCol },
  });
}

// 监听游戏事件
socket.on("piece_moved", (data) => {
  updateBoard(data.from, data.to);
});

socket.on("game_ended", (data) => {
  showGameResult(data.status, data.message);
});
```

## 实现最小化功能

要实现基本可玩的象棋游戏，需要以下核心功能：

1. **认证和房间加入** ✅
2. **玩家准备和游戏开始** ✅
3. **棋子选择和移动** ✅
4. **基本移动验证** ✅
5. **游戏状态同步** ✅
6. **简单的游戏结束检测** ✅

这套协议提供了最基本但完整的象棋游戏体验。

## 广播机制详解

### 自动广播事件

服务器会在以下情况自动向房间内所有客户端广播消息：

#### 1. 玩家生命周期事件

- **玩家加入房间**：认证成功后广播 `player_joined`
- **玩家离开房间**：断开连接时广播 `player_left`
- **玩家准备状态变更**：准备/取消准备时广播 `player_ready_changed`

#### 2. 游戏进程事件

- **游戏开始**：双方都准备好时广播 `game_started`
- **棋子选择**：选择棋子时广播 `piece_selected`
- **棋子移动**：移动棋子时广播 `piece_moved`
- **游戏结束**：将军、投降、超时时广播 `game_ended`

#### 3. 状态同步事件

- **游戏状态更新**：每次游戏状态变更后发送 `game_state`
- **房间状态更新**：玩家进出、准备状态变更后发送 `room_status`

### 广播范围

- **房间内广播**：消息发送给房间内所有在线玩家
- **排除发送者**：某些事件（如玩家加入）不会发送给触发事件的玩家本人
- **单独发送**：可移动位置等信息只发送给相关玩家

### 客户端处理建议

```javascript
// 监听所有广播事件
socket.on("player_joined", (data) => {
  console.log(`${data.userName} 加入了房间`);
  updatePlayerList();
});

socket.on("player_left", (data) => {
  console.log(`${data.userName} 离开了房间`);
  updatePlayerList();
});

socket.on("player_ready_changed", (data) => {
  console.log(`${data.userName} ${data.ready ? "已准备" : "取消准备"}`);
  updateReadyStatus(data.userId, data.ready);
});

socket.on("game_started", (data) => {
  console.log("游戏开始！");
  showGameBoard();
});

socket.on("room_status", (data) => {
  updateRoomStatus(data);
});

socket.on("game_state", (data) => {
  updateGameState(data);
});
```
