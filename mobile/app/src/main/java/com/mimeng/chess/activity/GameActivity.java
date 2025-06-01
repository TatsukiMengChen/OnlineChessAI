package com.mimeng.chess.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.gson.JsonObject;
import com.mimeng.chess.R;
import com.mimeng.chess.entity.chess.ChessGameState;
import com.mimeng.chess.entity.chess.ChessPiece;
import com.mimeng.chess.entity.chess.Position;
import com.mimeng.chess.entity.chess.Move;
import com.mimeng.chess.entity.chess.PlayerColor;
import com.mimeng.chess.socket.SocketEventListener;
import com.mimeng.chess.socket.SocketManager;
import com.mimeng.chess.utils.AuthManager; // 添加 AuthManager 导入
import com.mimeng.chess.utils.GameStateJsonUtils;
import com.mimeng.chess.view.ChessBoardView;

import java.util.List;
import java.util.ArrayList; // 添加 ArrayList 导入

/**
 * 象棋游戏主界面
 * 负责游戏进行时的UI交互、棋盘显示、Socket.IO通信等
 */
public class GameActivity extends BaseActivity implements SocketEventListener {

  private static final String TAG = "GameActivity";
  private static final String EXTRA_ROOM_ID = "room_id";
  private static final String EXTRA_GAME_STATE = "game_state";

  // 添加结果码常量
  public static final int RESULT_GAME_ENDED = 100;

  // UI组件
  private ChessBoardView chessBoardView;
  private TextView tvCurrentPlayer;
  private TextView tvRedTimer;
  private TextView tvBlackTimer;
  private TextView tvGameStatus;
  private Button btnSurrender;
  private Button btnUndo;

  // 游戏状态
  private String roomId;
  private ChessGameState gameState;
  private PlayerColor myColor; // 当前玩家的颜色
  private SocketManager socketManager;

  /**
   * 启动GameActivity的静态方法
   *
   * @param context       上下文
   * @param roomId        房间ID
   * @param gameStateJson 游戏状态JSON
   */
  public static void start(Context context, String roomId, JsonObject gameStateJson) {
    Intent intent = new Intent(context, GameActivity.class);
    intent.putExtra(EXTRA_ROOM_ID, roomId);
    intent.putExtra(EXTRA_GAME_STATE, gameStateJson.toString());
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_game);

    // 获取传入参数
    roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
    String gameStateJson = getIntent().getStringExtra(EXTRA_GAME_STATE);

    if (roomId == null || gameStateJson == null) {
      Log.e(TAG, "Missing required parameters");
      finish();
      return;
    }
    initViews();
    // 使用单例模式的 SocketManager
    socketManager = SocketManager.getInstance(this);
    initSocketConnection();
    parseGameState(gameStateJson);
    setupEventListeners();
  }

  /**
   * 初始化UI组件
   */
  private void initViews() {
    chessBoardView = findViewById(R.id.chess_board_view);
    tvCurrentPlayer = findViewById(R.id.tv_current_player);
    tvRedTimer = findViewById(R.id.tv_red_timer);
    tvBlackTimer = findViewById(R.id.tv_black_timer);
    tvGameStatus = findViewById(R.id.tv_game_status);
    btnSurrender = findViewById(R.id.btn_surrender);
    btnUndo = findViewById(R.id.btn_undo);
  }

  /**
   * 初始化Socket连接
   */
  private void initSocketConnection() {
    // 使用单例模式注册监听器
    if (socketManager != null) {
      socketManager.registerListener(this, roomId);
    }
  }

  /**
   * 获取当前用户ID
   */
  private String getUserId() {
    try {
      AuthManager authManager = AuthManager.getInstance(this);
      com.mimeng.chess.api.auth.LoginRes.User user = authManager.getUser();
      if (user != null) {
        // 强制返回字符串类型
        String userId = String.valueOf(user.id);
        Log.d(TAG, "getUserId: " + userId);
        return userId;
      }
    } catch (Exception e) {
      Log.e(TAG, "Failed to get user ID", e);
    }
    Log.w(TAG, "getUserId: null");
    return null;
  }

  /**
   * 解析游戏状态JSON
   */
  private void parseGameState(String gameStateJson) {
    try {
      Log.d(TAG, "Game state JSON: " + gameStateJson);

      // 使用工具类解析JSON
      JsonObject jsonObject = com.google.gson.JsonParser.parseString(gameStateJson).getAsJsonObject();
      gameState = GameStateJsonUtils.parseGameState(jsonObject, roomId);

      // 获取当前用户ID，确定玩家颜色
      String myUserId = getUserId();
      Log.d(TAG, "parseGameState: myUserId=" + myUserId);

      if (myUserId != null) {
        myColor = GameStateJsonUtils.determineMyColor(jsonObject, myUserId);
        Log.d(TAG, "parseGameState: determined myColor=" + myColor);
      } else {
        Log.w(TAG, "Unable to get user ID, defaulting to RED");
        myColor = PlayerColor.RED; // 默认红方
      }

      updateUI();
    } catch (Exception e) {
      Log.e(TAG, "Failed to parse game state", e);
      Toast.makeText(this, "游戏状态解析失败", Toast.LENGTH_SHORT).show();
      finish();
    }
  }

  /**
   * 设置事件监听器
   */
  private void setupEventListeners() {
    // 棋盘点击事件
    chessBoardView.setOnPieceClickListener(new ChessBoardView.OnPieceClickListener() {
      @Override
      public void onPieceClick(Position position) {
        handlePieceClick(position);
      }

      @Override
      public void onMoveComplete(Position from, Position to) {
        handleMoveComplete(from, to);
      }
    });

    // 投降按钮 - 添加确认对话框
    btnSurrender.setOnClickListener(v -> onSurrenderClicked());

    // 悔棋按钮
    btnUndo.setOnClickListener(v -> {
      if (gameState != null && gameState.isAllowUndo()) {
        if (socketManager != null) {
          // 向服务器发送悔棋请求事件
          socketManager.emit("undo_move_request", new JsonObject()); // 假设事件名为 "undo_move_request"
          Log.d(TAG, "Sent undo_move_request event");
          Toast.makeText(this, "已发送悔棋请求", Toast.LENGTH_SHORT).show();
        } else {
          Log.w(TAG, "SocketManager is null, cannot send undo_move_request event.");
          Toast.makeText(this, "无法连接到服务器以悔棋", Toast.LENGTH_SHORT).show();
        }
      } else {
        Toast.makeText(this, "当前不能悔棋", Toast.LENGTH_SHORT).show();
      }
    });
  }

  /**
   * 投降操作 - 从RoomDetailActivity移动过来
   */
  private void onSurrenderClicked() {
    if (socketManager == null || !socketManager.isConnected()) {
      Toast.makeText(this, "连接断开，无法投降", Toast.LENGTH_SHORT).show();
      return;
    }

    new AlertDialog.Builder(this)
        .setTitle("投降确认")
        .setMessage("确定要投降吗？投降后将结束游戏并返回房间。")
        .setPositiveButton("确定", (dialog, which) -> {
          socketManager.sendSurrender();
          Toast.makeText(this, "已发送投降请求", Toast.LENGTH_SHORT).show();

          // 禁用投降按钮，防止重复点击
          btnSurrender.setEnabled(false);

          // 设置超时退出，防止服务器响应丢失的情况
          btnSurrender.postDelayed(() -> {
            if (!isFinishing()) {
              Toast.makeText(this, "投降成功，退出游戏", Toast.LENGTH_SHORT).show();
              finish();
            }
          }, 3000); // 3秒后强制退出
        })
        .setNegativeButton("取消", null)
        .show();
  }

  /**
   * 处理棋子点击事件
   */
  private void handlePieceClick(Position position) {
    if (gameState == null)
      return;

    // 检查是否轮到当前玩家
    Log.d(TAG, "handlePieceClick: myColor=" + myColor + ", currentPlayer=" + gameState.getCurrentPlayer()
        + ", position=" + position);
    if (gameState.getCurrentPlayer() != myColor) {
      String message = "还没轮到您下棋 (当前: " + gameState.getCurrentPlayer() + ", 您是: " + myColor + ")";
      Log.d(TAG, message);
      Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
      return;
    }

    // 获取点击位置的棋子
    ChessPiece piece = gameState.getBoard().getPieceAt(position);

    // 如果点击的是己方棋子，选中并显示可移动位置
    if (piece != null && piece.getColor() == myColor) {
      // 选中棋子
      chessBoardView.setSelectedPosition(position);

      // 获取并显示可移动位置
      List<Position> validMoves = gameState.getValidMovesForPiece(position);
      chessBoardView.setAvailableMoves(validMoves);

      Log.d(TAG, "Selected piece at " + position + ", found " + validMoves.size() + " valid moves");
    } else if (piece == null || piece.getColor() != myColor) {
      // 点击空位或敌方棋子，清除选择
      chessBoardView.setSelectedPosition(null);
      chessBoardView.setAvailableMoves(new ArrayList<>());
    }

    // 向服务端发送选择棋子事件
    if (socketManager != null) {
      JsonObject data = new JsonObject();
      data.addProperty("row", position.getRow());
      data.addProperty("col", position.getCol());
      socketManager.emit("select_piece", data); // 假设事件名为 "select_piece"
      Log.d(TAG, "Sent select_piece event: " + data.toString());
    } else {
      Log.w(TAG, "SocketManager is null, cannot send select_piece event.");
    }
  }

  /**
   * 处理移动完成事件
   */
  private void handleMoveComplete(Position from, Position to) {
    if (gameState == null)
      return;

    // 检查是否轮到当前玩家
    Log.d(TAG, "handleMoveComplete: myColor=" + myColor + ", currentPlayer=" + gameState.getCurrentPlayer() + ", from="
        + from + ", to=" + to);
    if (gameState.getCurrentPlayer() != myColor) {
      String message = "还没轮到您下棋 (当前: " + gameState.getCurrentPlayer() + ", 您是: " + myColor + ")";
      Log.d(TAG, message);
      return;
    }

    // 清除选择状态和可移动位置显示
    chessBoardView.setSelectedPosition(null);
    chessBoardView.setAvailableMoves(new ArrayList<>()); // 向服务端发送移动事件
    if (socketManager != null) {
      JsonObject moveData = new JsonObject();

      // 创建 from 对象
      JsonObject fromObj = new JsonObject();
      fromObj.addProperty("row", from.getRow());
      fromObj.addProperty("col", from.getCol());

      // 创建 to 对象
      JsonObject toObj = new JsonObject();
      toObj.addProperty("row", to.getRow());
      toObj.addProperty("col", to.getCol());

      // 添加到主对象
      moveData.add("from", fromObj);
      moveData.add("to", toObj);

      socketManager.emit("move_piece", moveData);
      Log.d(TAG, "Sent move_piece event: " + moveData.toString());
    }
  }

  /**
   * 更新UI显示
   */
  private void updateUI() {
    if (gameState == null)
      return;

    runOnUiThread(() -> {
      // 更新当前玩家显示
      String currentPlayerText = gameState.getCurrentPlayer() == PlayerColor.RED ? "红方" : "黑方";
      tvCurrentPlayer.setText("当前回合: " + currentPlayerText);

      // 更新计时器
      if (gameState.isUseTimer()) {
        updateTimerDisplay();
      } else {
        tvRedTimer.setVisibility(View.GONE);
        tvBlackTimer.setVisibility(View.GONE);
      }

      // 更新游戏状态
      updateGameStatusDisplay();

      // 更新棋盘
      chessBoardView.setGameState(gameState);
      chessBoardView.setPlayerColor(myColor);
    });
  }

  /**
   * 更新计时器显示
   */
  private void updateTimerDisplay() {
    int redTime = gameState.getRedTimeLeft();
    int blackTime = gameState.getBlackTimeLeft();

    tvRedTimer.setText(String.format("红方: %02d:%02d", redTime / 60, redTime % 60));
    tvBlackTimer.setText(String.format("黑方: %02d:%02d", blackTime / 60, blackTime % 60));

    tvRedTimer.setVisibility(View.VISIBLE);
    tvBlackTimer.setVisibility(View.VISIBLE);
  }

  /**
   * 更新游戏状态显示
   */
  private void updateGameStatusDisplay() {
    switch (gameState.getStatus()) {
      // 使用非限定名称
      case WAITING: // 假设 GameStatus 有 WAITING 状态
        tvGameStatus.setText("等待开始");
        break;
      case PLAYING: // 假设 GameStatus 有 PLAYING 状态 (原 IN_PROGRESS)
        tvGameStatus.setText("游戏进行中");
        break;
      case RED_WIN:
        tvGameStatus.setText("红方获胜");
        // 游戏结束，设置结果码并关闭Activity
        setResult(RESULT_GAME_ENDED);
        finish();
        break;
      case BLACK_WIN:
        tvGameStatus.setText("黑方获胜");
        // 游戏结束，设置结果码并关闭Activity
        setResult(RESULT_GAME_ENDED);
        finish();
        break;
      case DRAW:
        tvGameStatus.setText("平局");
        // 游戏结束，设置结果码并关闭Activity
        setResult(RESULT_GAME_ENDED);
        finish();
        break;
      default:
        tvGameStatus.setText("等待开始");
        break;
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    // 设置结果码，通知调用者游戏已结束，需要刷新列表
    setResult(RESULT_GAME_ENDED);

    // 清理Socket监听器
    if (socketManager != null) {
      socketManager.unregisterListener(this);
    }
  }

  // ===== Socket事件处理 =====

  @Override
  public void onConnectionChanged(boolean connected) {
    Log.d(TAG, "Socket connection changed: " + connected);
    if (connected) {
      Log.d(TAG, "Socket connected");
    } else {
      Log.d(TAG, "Socket disconnected");
      runOnUiThread(() -> {
        Toast.makeText(this, "网络连接断开", Toast.LENGTH_LONG).show();
      });
    }
  }

  @Override
  public void onAuthSuccess() {
    Log.d(TAG, "Auth success");
  }

  @Override
  public void onAuthFail(String message) {
    Log.e(TAG, "Auth failed: " + message);
    runOnUiThread(() -> {
      Toast.makeText(this, "认证失败: " + message, Toast.LENGTH_LONG).show();
      finish();
    });
  }

  @Override
  public void onRoomNotFound(String message) {
    runOnUiThread(() -> {
      Toast.makeText(this, "房间不存在", Toast.LENGTH_LONG).show();
      finish();
    });
  }

  @Override
  public void onPlayerJoined(JsonObject data) {
    // 游戏中不需要处理玩家加入
    Log.d(TAG, "Player joined: " + data.toString());
  }

  @Override
  public void onPlayerLeft(JsonObject data) {
    Log.d(TAG, "Player left: " + data.toString());
    runOnUiThread(() -> {
      Toast.makeText(this, "对手离开了游戏，您获得胜利！", Toast.LENGTH_LONG).show();
      // 延迟2秒后退出活动，让用户看到消息
      chessBoardView.postDelayed(() -> {
        setResult(RESULT_GAME_ENDED);
        finish();
      }, 2000);
    });
  }

  @Override
  public void onPlayerReadyChanged(JsonObject data) {
    // 游戏中不需要处理玩家准备
    Log.d(TAG, "Player ready changed: " + data.toString());
  }

  @Override
  public void onRoomStatus(JsonObject data) {
    Log.d(TAG, "Room status updated: " + data.toString());
    // 可能需要根据房间状态更新UI，例如等待对手，或游戏已开始等
    // 具体的解析逻辑取决于 data 的内容
    // 示例：
    // if (data.has("status") &&
    // "waiting_for_opponent".equals(data.get("status").getAsString())) {
    // // 更新UI显示等待对手
    // }
  }

  @Override
  public void onGameState(JsonObject data) {
    Log.d(TAG, "Game state received: " + data.toString());
    runOnUiThread(() -> {
      try {
        // 重新确定玩家颜色，以防之前判断错误
        String myUserId = getUserId();
        if (myUserId != null) {
          PlayerColor newMyColor = GameStateJsonUtils.determineMyColor(data, myUserId);
          if (newMyColor != myColor) {
            Log.d(TAG, "Player color changed from " + myColor + " to " + newMyColor);
            myColor = newMyColor;
          }
        }

        // 解析新的游戏状态
        ChessGameState newGameState = GameStateJsonUtils.parseGameState(data, roomId);
        Log.d(TAG, "Parsed game state - Current player: " + newGameState.getCurrentPlayer() +
            ", Status: " + newGameState.getStatus() + ", My color: " + myColor);

        // 检查棋盘是否有变化
        if (this.gameState != null && newGameState.getBoard() != null) {
          Log.d(TAG, "Updating game state - board pieces count may have changed");
        }

        // 验证棋盘解析是否成功
        if (newGameState.getBoard() != null) {
          int pieceCount = 0;
          StringBuilder boardDebug = new StringBuilder();
          for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 9; col++) {
              ChessPiece p = newGameState.getBoard().getPieceAt(new Position(row, col));
              if (p != null) {
                pieceCount++;
                boardDebug
                    .append("[" + row + "," + col + "] " + p.getClass().getSimpleName() + " " + p.getColor() + "; ");
              }
            }
          }
          Log.d(TAG, "Parsed board contains " + pieceCount + " pieces");
          Log.d(TAG, "Board detail: " + boardDebug.toString());
        } else {
          Log.w(TAG, "Failed to parse board from game state");
        }

        this.gameState = newGameState;

        // 清除选中状态和可移动位置
        if (chessBoardView != null) {
          chessBoardView.setSelectedPosition(null);
          chessBoardView.setAvailableMoves(new ArrayList<>());

          // 强制刷新棋盘 - 多重保险
          Log.d(TAG, "Updating chess board with new game state");
          chessBoardView.setGameState(gameState);

          // 添加延迟刷新确保绘制完成
          chessBoardView.post(new Runnable() {
            @Override
            public void run() {
              chessBoardView.invalidate();
              Log.d(TAG, "Delayed invalidation completed");
            }
          });
        }

        updateUI();
        Log.d(TAG, "Game state update completed");
      } catch (Exception e) {
        Log.e(TAG, "Failed to handle game state update", e);
      }
    });
  }

  @Override
  public void onGameStarted(JsonObject gameStateJson) {
    Log.d(TAG, "Game started: " + gameStateJson.toString());
    // 游戏已经开始，通常在这里会收到完整的初始游戏状态
    parseGameState(gameStateJson.toString());
  }

  // onPieceMoved 已被 onGameState 或类似的通用状态更新事件取代，注释掉
  // @Override
  // public void onPieceMoved(JsonObject data) {
  // Log.d(TAG, \"Piece moved: \" + data.toString());
  // runOnUiThread(() -> {
  // try {
  // // 解析移动数据并更新棋盘
  // // 这部分逻辑可能需要根据实际的 data 结构来调整
  // // 例如，如果 data 直接是新的 gameState:
  // // this.gameState = GameStateJsonUtils.parseGameState(data, roomId);

  // // 如果 data 只包含移动信息，需要更新本地的 gameState
  // // Move move = GameStateJsonUtils.parseMove(data); // 假设有这样的解析方法
  // // if (this.gameState != null && move != null) {
  // // this.gameState.applyMove(move); // 假设 gameState 有 applyMove 方法
  // // }

  // // 清除选中状态和可移动位置
  // chessBoardView.setSelectedPosition(null);
  // chessBoardView.setAvailableMoves(new ArrayList<>());

  // updateUI();
  // } catch (Exception e) {
  // Log.e(TAG, \"Failed to handle piece moved\", e);
  // }
  // });
  // }

  // Ensure this method correctly overrides the one in SocketEventListener
  public void onGameEnded(JsonObject data) {
    runOnUiThread(() -> {
      try {
        String result = data.has("result") ? data.get("result").getAsString() : "Unknown";
        String reason = data.has("reason") ? data.get("reason").getAsString() : "";

        String message = "游戏结束: " + result;
        if (!reason.isEmpty()) {
          message += " (" + reason + ")";
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        // 游戏结束后设置结果码并关闭Activity
        setResult(RESULT_GAME_ENDED);
        finish();
      } catch (Exception e) {
        Log.e(TAG, "Failed to handle game ended", e);
        Toast.makeText(this, "游戏结束", Toast.LENGTH_LONG).show();
        // 即使解析失败也要设置结果码并关闭Activity
        setResult(RESULT_GAME_ENDED);
        finish();
      }
    });
  }

  @Override
  public void onPlayerSurrendered(JsonObject data) {
    runOnUiThread(() -> {
      try {
        String playerName = data.has("userName") ? data.get("userName").getAsString() : "对手";
        Toast.makeText(this, playerName + " 投降了，游戏结束", Toast.LENGTH_LONG).show();
      } catch (Exception e) {
        Toast.makeText(this, "有玩家投降，游戏结束", Toast.LENGTH_LONG).show();
      }

      // 投降后设置结果码并关闭Activity，返回房间或上一个界面
      setResult(RESULT_GAME_ENDED);
      finish();
    });
  }

  @Override
  public void onRoomClosing(JsonObject data) {
    Log.d(TAG, "Room closing: " + data.toString());
    runOnUiThread(() -> {
      Toast.makeText(this, "房间即将关闭", Toast.LENGTH_LONG).show();
      setResult(RESULT_GAME_ENDED);
      finish(); // 直接退出Activity
    });
  }

  @Override
  public void onRoomClosed(String message) {
    Log.d(TAG, "Room closed: " + message);
    runOnUiThread(() -> {
      Toast.makeText(this, "房间已关闭: " + message, Toast.LENGTH_LONG).show();
      setResult(RESULT_GAME_ENDED);
      finish(); // 关闭 GameActivity
    });
  }

  // Ensure this method correctly overrides the one in SocketEventListener
  @Override
  public void onError(String error) { // Signature matches SocketEventListener
    Log.e(TAG, "Socket error: " + error);
    runOnUiThread(() -> {
      Toast.makeText(this, "错误: " + error, Toast.LENGTH_LONG).show();
    });
  }

  @Override
  public void onPlayerColorAssigned(JsonObject data) {
    Log.d(TAG, "Player color assigned: " + data.toString());
    runOnUiThread(() -> {
      try {
        if (data.has("color")) {
          String colorStr = data.get("color").getAsString();
          PlayerColor newColor = "red".equalsIgnoreCase(colorStr) ? PlayerColor.RED : PlayerColor.BLACK;
          Log.d(TAG, "Color assigned: " + newColor);

          if (newColor != myColor) {
            Log.d(TAG, "Updating player color from " + myColor + " to " + newColor);
            myColor = newColor;
            updateUI();

            if (chessBoardView != null) {
              chessBoardView.setPlayerColor(myColor);
            }
          }
        }
      } catch (Exception e) {
        Log.e(TAG, "Failed to handle player color assignment", e);
      }
    });
  }

  @Override
  public void onBackPressed() {
    // 游戏进行中禁止直接返回，需要先投降
    new AlertDialog.Builder(this)
        .setTitle("退出游戏")
        .setMessage("游戏进行中，确定要投降并退出吗？")
        .setPositiveButton("投降退出", (dialog, which) -> {
          if (socketManager != null && socketManager.isConnected()) {
            socketManager.sendSurrender();
          }
          setResult(RESULT_GAME_ENDED);
          finish();
        })
        .setNegativeButton("继续游戏", null)
        .show();
  }
}
