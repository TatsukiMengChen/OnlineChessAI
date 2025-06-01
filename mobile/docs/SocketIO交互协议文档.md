# 中国象棋对战平台 - Socket.IO 交互协议文档

## 概述

本文档定义了中国象棋对战平台前后端之间基于 Socket.IO 的实时通信协议。**该协议与现有 HTTP REST API 协调工作**：

- **HTTP API**负责房间的 CRUD 操作（创建、列表、加入、退出、关闭）
- **Socket.IO**负责实时游戏逻辑和状态同步

## 架构设计

### API 分工

| 功能模块 | HTTP REST API                   | Socket.IO               |
| -------- | ------------------------------- | ----------------------- |
| 房间管理 | ✅ 创建、列表、加入、退出、关闭 | ❌                      |
| 用户认证 | ✅ 登录、注册、JWT 验证         | ✅ Socket 连接认证      |
| 游戏逻辑 | ❌                              | ✅ 移动、选择、状态同步 |
| 实时通信 | ❌                              | ✅ 聊天、观战、事件广播 |

### 工作流程

1. **进入房间前**：用户通过 HTTP API 获取房间列表、创建房间、加入房间
2. **进入房间后**：用户通过 Socket.IO 连接到房间，进行游戏交互
3. **离开房间**：用户可通过 HTTP API 或 Socket.IO 断开离开

## 连接流程

### 1. 建立连接

**客户端连接 URL：**

```
ws://localhost:8080/socket.io/?roomId={roomId}
```

**参数说明：**

- `roomId`: 房间 ID（必须，通过 HTTP API 获取）

### 2. 认证流程

#### 服务端 → 客户端

```javascript
// 连接成功后，服务端要求认证
socket.emit("need_auth", "请发送token进行鉴权");
```

#### 客户端 → 服务端

```javascript
// 客户端发送认证信息
socket.emit("auth", {
  token: "jwt_token_here",
});
```

#### 服务端 → 客户端

```javascript
// 认证成功，返回房间信息
socket.emit("auth_success", {
  message: "鉴权成功",
  userInfo: {
    userId: "user123",
    username: "玩家名称",
    avatar: "avatar_url",
  },
  roomInfo: {
    id: "room_123",
    name: "我的象棋房间",
    status: "waiting", // waiting, full, playing, finished
    player1Id: "user123",
    player2Id: "user456",
    createdAt: "2025-06-01T10:00:00Z",
    // 扩展游戏信息
    gameState: null, // 如果游戏未开始
    spectators: ["user789"],
    settings: {
      timeLimit: 600,
      allowUndo: true,
    },
  },
});

// 认证失败
socket.emit("auth_fail", {
  message: "token无效",
  code: "INVALID_TOKEN",
});
```

## 房间状态管理

> **注意：房间的创建、加入、退出等操作通过 HTTP API 完成，Socket.IO 主要负责房间内的实时状态同步**

### 1. 进入房间（Socket 连接后）

当用户通过 Socket.IO 连接到已加入的房间后：

#### 服务端 → 房间内其他用户（不包括自己）

```javascript
// 广播玩家加入房间
socket.broadcast.to(roomId).emit("player_joined", {
  userId: "user456",
  userName: "User456",
  message: "User456 加入了房间",
});
```

#### 服务端 → 新加入的用户

```javascript
// 发送认证成功消息
socket.emit("auth_success", "鉴权成功");

// 发送当前游戏状态
socket.emit("game_state", {
  status: "WAITING",
  currentPlayer: "RED",
  redPlayer: {
    userId: "user123",
    name: "User123",
    ready: false,
    online: true,
    color: "RED"
  },
  blackPlayer: null,
  boardState: {
    pieces: [...] // 10x9 棋盘数组
  }
});

// 发送房间状态
socket.emit("room_status", {
  status: "waiting",
  message: "等待玩家准备",
  players: {
    red: {
      userId: "user123",
      name: "User123",
      ready: false,
      online: true,
      color: "RED"
    }
  },
  waitingForPlayers: true
});
```

### 2. 房间状态同步

#### 客户端请求房间完整状态

```javascript
socket.emit("get_room_state");
```

#### 服务端响应

```javascript
// 发送游戏状态
socket.emit("game_state", {
  status: "PLAYING",
  currentPlayer: "RED",
  redPlayer: {
    userId: "user123",
    name: "User123",
    ready: true,
    online: true,
    color: "RED"
  },
  blackPlayer: {
    userId: "user456",
    name: "User456",
    ready: true,
    online: true,
    color: "BLACK"
  },
  boardState: {
    pieces: [...] // 10x9 棋盘数组，格式：null 或 "red_king", "black_pawn" 等
  }
});

// 发送房间状态
socket.emit("room_status", {
  status: "playing",
  message: "游戏进行中，红方回合",
  players: {
    red: {
      userId: "user123",
      name: "User123",
      ready: true,
      online: true,
      color: "RED"
    },
    black: {
      userId: "user456",
      name: "User456",
      ready: true,
      online: true,
      color: "BLACK"
    }
  },
  waitingForPlayers: false
});
```

### 3. 离开房间

#### 客户端 → 服务端

```javascript
// 主动离开房间
socket.emit("leave_room");
// 或者直接断开连接
socket.disconnect();
```

#### 服务端 → 房间内其他用户

```javascript
// 广播玩家离开
socket.broadcast.to(roomId).emit("player_left", {
  userId: "user456",
  userName: "User456",
  message: "User456 离开了房间",
});
```

**注意事项：**

- 玩家离开房间后，服务端会自动更新游戏状态和房间状态
- 房间内其他玩家会收到更新的 `game_state` 和 `room_status` 事件
- 如果房间变为空房间，服务端会自动清理房间资源

## 游戏逻辑

### 1. 游戏准备

#### 客户端 → 服务端

```javascript
// 玩家准备
socket.emit("player_ready", {
  ready: true,
});
```

#### 服务端 → 房间内所有用户

```javascript
// 玩家准备状态更新广播
socket.emit("player_ready_changed", {
  userId: "user123",
  userName: "User123",
  ready: true,
  message: "User123 已准备",
});

// 游戏开始（当双方都准备好时）
socket.emit("game_started", {
  message: "游戏开始！",
});

// 同时发送更新的游戏状态
socket.emit("game_state", {
  status: "PLAYING",
  currentPlayer: "RED", // 红方先走
  redPlayer: {
    userId: "user123",
    name: "User123",
    ready: true,
    online: true,
    color: "RED",
  },
  blackPlayer: {
    userId: "user456",
    name: "User456",
    ready: true,
    online: true,
    color: "BLACK",
  },
  boardState: {
    pieces: [
      // 10x9 棋盘初始状态
      [
        "black_rook",
        "black_horse",
        "black_elephant",
        "black_advisor",
        "black_king",
        "black_advisor",
        "black_elephant",
        "black_horse",
        "black_rook",
      ],
      [null, null, null, null, null, null, null, null, null],
      [
        null,
        "black_cannon",
        null,
        null,
        null,
        null,
        null,
        "black_cannon",
        null,
      ],
      [
        "black_pawn",
        null,
        "black_pawn",
        null,
        "black_pawn",
        null,
        "black_pawn",
        null,
        "black_pawn",
      ],
      [null, null, null, null, null, null, null, null, null],
      [null, null, null, null, null, null, null, null, null],
      [
        "red_pawn",
        null,
        "red_pawn",
        null,
        "red_pawn",
        null,
        "red_pawn",
        null,
        "red_pawn",
      ],
      [null, "red_cannon", null, null, null, null, null, "red_cannon", null],
      [null, null, null, null, null, null, null, null, null],
      [
        "red_rook",
        "red_horse",
        "red_elephant",
        "red_advisor",
        "red_king",
        "red_advisor",
        "red_elephant",
        "red_horse",
        "red_rook",
      ],
    ],
  },
});

// 发送更新的房间状态
socket.emit("room_status", {
  status: "playing",
  message: "游戏进行中，红方回合",
  players: {
    red: {
      /* 红方玩家信息 */
    },
    black: {
      /* 黑方玩家信息 */
    },
  },
  waitingForPlayers: false,
});
```

#### 服务端 → 客户端（准备失败）

```javascript
// 准备状态设置失败
socket.emit("ready_failed", "设置准备状态失败");
```

### 2. 棋子移动

#### 客户端 → 服务端

```javascript
// 选择棋子
socket.emit("select_piece", {
  position: { row: 9, col: 4 },
});

// 移动棋子
socket.emit("move_piece", {
  from: { row: 9, col: 4 },
  to: { row: 8, col: 4 },
});
```

#### 服务端 → 客户端

```javascript
// 棋子选择成功响应
socket.emit("piece_selected", {
  userId: "user123",
  position: { row: 9, col: 4 },
});

// 发送可移动位置
socket.emit("available_moves", {
  moves: [
    { row: 8, col: 4 },
    { row: 9, col: 3 },
    { row: 9, col: 5 },
  ],
});

// 选择失败
socket.emit("select_failed", "选择棋子失败");
```

#### 服务端 → 房间内所有用户

```javascript
// 移动成功广播
socket.emit("piece_moved", {
  userId: "user123",
  from: { row: 9, col: 4 },
  to: { row: 8, col: 4 }
});

// 同时发送更新的游戏状态
socket.emit("game_state", {
  status: "PLAYING",
  currentPlayer: "BLACK", // 轮到下一个玩家
  redPlayer: { /* 玩家信息 */ },
  blackPlayer: { /* 玩家信息 */ },
  boardState: {
    pieces: [...] // 更新后的棋盘状态
  }
});

// 游戏结束（如果有胜负）
socket.emit("game_ended", {
  status: "RED_WIN", // RED_WIN, BLACK_WIN, DRAW
  message: "红方获胜！"
});
```

#### 服务端 → 客户端（移动失败）

```javascript
// 移动无效
socket.emit("move_failed", "移动失败");
```

    checkStatus: {
      inCheck: false,
      checkedKing: null,
    },

},
timeRemaining: {
red: 595,
black: 600,
},
});

````

### 3. 游戏结束

#### 服务端 → 房间内所有用户

```javascript
// 游戏结束
socket.emit("game_ended", {
  result: {
    winner: "RED", // RED, BLACK, DRAW
    reason: "CHECKMATE", // CHECKMATE, STALEMATE, TIMEOUT, RESIGN, DRAW_AGREEMENT
    finalPosition: {
      // 最终棋盘状态
    },
  },
  gameStats: {
    duration: 1800, // 游戏时长（秒）
    totalMoves: 45,
    capturedPieces: {
      red: ["PAWN", "HORSE"],
      black: ["CANNON", "GUARD"],
    },
  },
});
````

### 4. 特殊操作

#### 客户端 → 服务端

```javascript
// 请求和棋
socket.emit("request_draw");

// 认输
socket.emit("resign");

// 请求悔棋
socket.emit("request_undo");

// 响应和棋/悔棋请求
socket.emit("respond_request", {
  type: "DRAW_REQUEST", // DRAW_REQUEST, UNDO_REQUEST
  accept: true,
});
```

#### 服务端 → 房间内所有用户

```javascript
// 和棋请求
socket.emit("draw_requested", {
  fromPlayer: "user123",
  message: "玩家请求和棋",
});

// 悔棋请求
socket.emit("undo_requested", {
  fromPlayer: "user123",
  targetMove: {
    // 要撤销的移动
  },
});

// 请求被响应
socket.emit("request_responded", {
  type: "DRAW_REQUEST",
  accepted: true,
  respondedBy: "user456",
});
```

## AI 对战

### 1. AI 移动

#### 服务端 → 房间内所有用户

```javascript
// AI 思考中
socket.emit("ai_thinking", {
  aiColor: "BLACK",
  estimatedTime: 3000, // 预计思考时间（毫秒）
});

// AI 移动
socket.emit("ai_move_made", {
  move: {
    from: { row: 2, col: 1 },
    to: { row: 4, col: 2 },
    piece: { type: "HORSE", color: "BLACK" },
    capturedPiece: null,
    notation: "马二进三",
  },
  aiAnalysis: {
    evaluationScore: -0.3,
    searchDepth: 6,
    nodesSearched: 15000,
    thinkingTime: 2500,
  },
  gameState: {
    // 更新后的游戏状态
  },
});
```

## 观战功能

### 1. 观战者管理

#### 服务端 → 房间内所有用户

```javascript
// 观战者加入
socket.emit("spectator_joined", {
  spectator: {
    userId: "user789",
    username: "观战者1",
    avatar: "avatar_url",
  },
  spectatorCount: 5,
});

// 观战者离开
socket.emit("spectator_left", {
  userId: "user789",
  spectatorCount: 4,
});
```

### 2. 观战者消息

#### 客户端 → 服务端

```javascript
// 观战者发送聊天消息
socket.emit("spectator_chat", {
  message: "精彩的对局！",
});
```

#### 服务端 → 房间内所有用户

```javascript
// 观战者聊天
socket.emit("chat_message", {
  from: {
    userId: "user789",
    username: "观战者1",
    role: "SPECTATOR",
  },
  message: "精彩的对局！",
  timestamp: "2025-06-01T10:15:30Z",
});
```

## 聊天系统

### 1. 发送消息

#### 客户端 → 服务端

```javascript
socket.emit("send_message", {
  message: "下得不错！",
  type: "TEXT", // TEXT, EMOJI, QUICK_MESSAGE
  recipient: null, // null表示房间内所有人，或指定用户ID
});
```

#### 服务端 → 房间内所有用户（或指定用户）

```javascript
socket.emit("chat_message", {
  from: {
    userId: "user123",
    username: "玩家1",
    role: "PLAYER",
  },
  message: "下得不错！",
  type: "TEXT",
  isPrivate: false,
  timestamp: "2025-06-01T10:15:30Z",
});
```

### 2. 快捷消息

#### 客户端 → 服务端

```javascript
socket.emit("quick_message", {
  messageId: "GOOD_MOVE", // GOOD_MOVE, THANKS, GG, THINKING
});
```

## 错误处理

### 通用错误格式

```javascript
socket.emit("error", {
  code: "ERROR_CODE",
  message: "错误描述",
  details: {
    // 额外错误信息
  },
  timestamp: "2025-06-01T10:15:30Z",
});
```

### 常见错误代码

| 错误代码            | 描述         |
| ------------------- | ------------ |
| `AUTH_REQUIRED`     | 需要认证     |
| `INVALID_TOKEN`     | 无效令牌     |
| `ROOM_NOT_FOUND`    | 房间不存在   |
| `ROOM_FULL`         | 房间已满     |
| `PERMISSION_DENIED` | 权限不足     |
| `INVALID_MOVE`      | 无效移动     |
| `GAME_NOT_STARTED`  | 游戏未开始   |
| `NOT_YOUR_TURN`     | 不是你的回合 |
| `CONNECTION_ERROR`  | 连接错误     |

## 心跳检测

### 客户端 → 服务端

```javascript
// 每30秒发送心跳
socket.emit("ping", {
  timestamp: Date.now(),
});
```

### 服务端 → 客户端

```javascript
// 心跳响应
socket.emit("pong", {
  timestamp: Date.now(),
  serverTime: "2025-06-01T10:15:30Z",
});
```

## 状态同步

### 客户端请求完整状态

#### 客户端 → 服务端

```javascript
socket.emit("request_full_state");
```

#### 服务端 → 客户端

```javascript
socket.emit("full_state", {
  roomInfo: {
    // 完整房间信息
  },
  gameState: {
    // 完整游戏状态
  },
  userList: [
    // 房间内所有用户
  ],
});
```

## 断线重连

### 重连后状态恢复

#### 客户端重连后

```javascript
socket.emit("reconnect_restore", {
  lastKnownState: {
    roomId: "room_123",
    lastMoveId: "move_45",
  },
});
```

#### 服务端响应

```javascript
socket.emit("state_restored", {
  missedEvents: [
    // 断线期间错过的事件
  ],
  currentState: {
    // 当前完整状态
  },
});
```

## 使用示例

### 完整交互流程

#### 1. 前端加入房间流程

```javascript
// 第一步：通过HTTP API加入房间
fetch("/room/join?roomId=room_123", {
  method: "POST",
  headers: {
    Authorization: "Bearer " + jwt_token,
    "Content-Type": "application/json",
  },
})
  .then((response) => response.json())
  .then((data) => {
    if (data.code === 200) {
      // 第二步：加入成功后，建立Socket连接
      connectToRoom(data.data.id);
    }
  });

// 第二步：建立Socket连接
function connectToRoom(roomId) {
  const socket = io("ws://localhost:8080", {
    query: {
      roomId: roomId,
      token: localStorage.getItem("jwt_token"),
    },
  });

  // 认证处理
  socket.on("need_auth", () => {
    socket.emit("auth", {
      token: localStorage.getItem("jwt_token"),
    });
  });

  socket.on("auth_success", (data) => {
    console.log("进入房间成功:", data.roomInfo);
    // 初始化游戏界面
    initChessBoard(data.roomInfo);
  });

  // 游戏事件监听
  socket.on("player_ready_changed", (data) => {
    updatePlayerReadyStatus(data);
  });

  socket.on("game_started", (data) => {
    startGame(data.gameState);
  });

  socket.on("move_made", (data) => {
    updateChessBoard(data.gameState);
    addMoveToHistory(data.move);
  });
}
```

#### 2. 后端集成示例

```java
// 扩展现有RoomSocketServer
@Component
public class RoomSocketServer implements InitializingBean {
    @Autowired
    private SocketIOServer socketIOServer;

    @Autowired
    private RoomService roomService; // 复用现有HTTP服务

    @Override
    public void afterPropertiesSet() {
        // 连接处理
        socketIOServer.addConnectListener(client -> {
            String roomId = client.getHandshakeData().getSingleUrlParam("roomId");
            String token = client.getHandshakeData().getSingleUrlParam("token");

            if (roomId == null || token == null) {
                client.disconnect();
                return;
            }

            // 验证用户是否在该房间中
            try {
                Claims claims = JwtUtil.parseToken(token);
                Long userId = Long.valueOf(claims.getSubject());

                // 检查用户是否在房间中
                Room room = roomService.getById(roomId);
                if (room == null || !isUserInRoom(room, userId)) {
                    client.sendEvent("auth_fail", "用户不在该房间中");
                    client.disconnect();
                    return;
                }

                // 认证成功，加入房间
                client.joinRoom(roomId);
                client.set("userId", userId);
                client.set("roomId", roomId);

                client.sendEvent("auth_success", buildAuthSuccessData(room, userId));

                // 广播用户进入
                client.getNamespace().getRoomOperations(roomId)
                    .sendEvent("user_entered_room", buildUserEnteredData(userId));

            } catch (Exception e) {
                client.sendEvent("auth_fail", "token无效");
                client.disconnect();
            }
        });

        // 游戏准备
        socketIOServer.addEventListener("player_ready", Boolean.class,
            (client, ready, ackSender) -> {
                String roomId = client.get("roomId");
                Long userId = client.get("userId");

                // 更新准备状态
                updatePlayerReadyStatus(roomId, userId, ready);

                // 广播准备状态变化
                client.getNamespace().getRoomOperations(roomId)
                    .sendEvent("player_ready_changed",
                        Map.of("userId", userId, "ready", ready));

                // 检查是否可以开始游戏
                if (canStartGame(roomId)) {
                    startGame(roomId);
                }
            });

        // 棋子移动
        socketIOServer.addEventListener("move_piece", Map.class,
            (client, moveData, ackSender) -> {
                String roomId = client.get("roomId");
                Long userId = client.get("userId");

                // 验证移动
                if (validateMove(roomId, userId, moveData)) {
                    // 执行移动
                    GameState newState = executeMove(roomId, moveData);

                    // 广播移动
                    client.getNamespace().getRoomOperations(roomId)
                        .sendEvent("move_made", buildMoveData(moveData, newState));
                } else {
                    client.sendEvent("invalid_move",
                        Map.of("message", "无效移动", "move", moveData));
                }
            });

        socketIOServer.start();
    }

    private boolean isUserInRoom(Room room, Long userId) {
        return userId.equals(room.getPlayer1Id()) ||
               userId.equals(room.getPlayer2Id());
    }
}
```

#### 3. Android 客户端示例

````java
// 房间加入后连接Socket
public class ChessGameActivity extends AppCompatActivity {
    private Socket socket;
    private String roomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取从房间列表传来的roomId
        roomId = getIntent().getStringExtra("roomId");

        connectToGameRoom();
    }

    private void connectToGameRoom() {
        try {
            IO.Options options = new IO.Options();
            options.query = "roomId=" + roomId + "&token=" + getJwtToken();

            socket = IO.socket("ws://localhost:8080", options);

            socket.on("auth_success", args -> {
                JSONObject data = (JSONObject) args[0];
                runOnUiThread(() -> {
                    // 初始化游戏界面
                    initGameUI(data);
                });
            });

            socket.on("game_started", args -> {
                JSONObject gameState = (JSONObject) args[0];
                runOnUiThread(() -> {
                    startChessGame(gameState);
                });
            });

            socket.on("move_made", args -> {
                JSONObject moveData = (JSONObject) args[0];
                runOnUiThread(() -> {
                    updateChessBoard(moveData);
                });
            });

            socket.connect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 玩家准备
    public void onPlayerReady() {
        if (socket != null) {
            socket.emit("player_ready", true);
        }
    }

    // 移动棋子
    public void makeMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (socket != null) {
            JSONObject moveData = new JSONObject();
            try {
                JSONObject from = new JSONObject();
                from.put("row", fromRow);
                from.put("col", fromCol);

                JSONObject to = new JSONObject();
                to.put("row", toRow);
                to.put("col", toCol);

                moveData.put("from", from);
                moveData.put("to", to);

                socket.emit("move_piece", moveData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

## 性能考虑

1. **消息频率限制**：限制客户端发送消息的频率，防止恶意刷屏
2. **状态压缩**：对大型状态对象进行压缩传输
3. **增量更新**：只传输变化的部分，而不是完整状态
4. **连接池管理**：合理管理Socket连接，及时清理无效连接
5. **内存管理**：游戏状态仅在内存中维护，定期清理非活跃房间

## 安全考虑

1. **JWT验证**：所有Socket操作都需要有效的JWT令牌
2. **房间权限**：确保用户只能连接到已加入的房间
3. **输入验证**：验证所有游戏操作的合法性
4. **频率限制**：限制客户端操作频率，防止DDoS攻击
5. **状态验证**：服务端验证每个游戏状态变化的合法性

## 部署建议

### 开发环境
```javascript
// 前端开发配置
const SOCKET_URL = 'ws://localhost:8080';
const API_BASE_URL = 'http://localhost:8080/api';
````

### 生产环境

```javascript
// 前端生产配置
const SOCKET_URL = "wss://your-domain.com";
const API_BASE_URL = "https://your-domain.com/api";
```

### 后端配置

```yaml
# application.yml
server:
  port: 8080

socketio:
  host: 0.0.0.0
  port: 8080
  bossCount: 1
  workCount: 100
  allowCustomRequests: true
  upgradeTimeout: 1000000
  pingTimeout: 6000000
  pingInterval: 25000000
```

---

_本文档版本：v1.0_  
_最后更新：2025 年 6 月 1 日_
