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
  "data": { "userId": 123, "ready": true }
}
```

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

```json
// 玩家加入
{
  "event": "player_joined",
  "data": {"userId": 123, "userName": "新玩家"}
}

// 玩家离开
{
  "event": "player_left",
  "data": {"userId": 123, "userName": "离开的玩家"}
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
