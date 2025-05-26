package com.mimeng.chess.entity.chess;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * 象棋游戏状态类 - 核心游戏管理类
 * 支持前后端兼容，可用于Android客户端和后端服务器
 */
public class ChessGameState {

  // 基本游戏信息
  private String gameId; // 游戏ID
  private String roomId; // 房间ID
  private GameStatus status; // 游戏状态
  private PlayerColor currentPlayer; // 当前行棋方
  private LocalDateTime startTime; // 游戏开始时间
  private LocalDateTime lastMoveTime; // 最后一次移动时间

  // 玩家信息
  private Player redPlayer; // 红方玩家
  private Player blackPlayer; // 黑方玩家

  // 棋盘和移动历史
  private ChessBoard board; // 当前棋盘状态
  private Stack<Move> moveHistory; // 移动历史（用于悔棋）
  private List<String> gameRecord; // 游戏记录（棋谱）

  // 前端交互相关（主要用于Android客户端）
  private Position selectedPiece; // 当前选中的棋子位置
  private List<Position> availableMoves; // 选中棋子的可移动位置
  private boolean showHints; // 是否显示移动提示

  // 游戏计时
  private int redTimeLeft; // 红方剩余时间（秒）
  private int blackTimeLeft; // 黑方剩余时间（秒）
  private boolean useTimer; // 是否使用计时器

  // 游戏设置
  private boolean allowUndo; // 是否允许悔棋
  private int maxUndoCount; // 最大悔棋次数
  private int currentUndoCount; // 当前悔棋次数

  /**
   * 构造函数 - 创建新游戏
   */
  public ChessGameState(String gameId, String roomId) {
    this.gameId = gameId;
    this.roomId = roomId;
    this.status = GameStatus.WAITING;
    this.currentPlayer = PlayerColor.RED; // 红方先手
    this.board = new ChessBoard();
    this.moveHistory = new Stack<>();
    this.gameRecord = new ArrayList<>();
    this.availableMoves = new ArrayList<>();
    this.selectedPiece = null;
    this.showHints = true;
    this.useTimer = true;
    this.allowUndo = true;
    this.maxUndoCount = 3;
    this.currentUndoCount = 0;
    this.redTimeLeft = 1800; // 30分钟
    this.blackTimeLeft = 1800;
  }

  /**
   * 设置玩家
   */
  public void setRedPlayer(Player player) {
    this.redPlayer = player;
    if (player != null) {
      player.setColor(PlayerColor.RED);
    }
  }

  public void setBlackPlayer(Player player) {
    this.blackPlayer = player;
    if (player != null) {
      player.setColor(PlayerColor.BLACK);
    }
  }

  /**
   * 开始游戏
   */
  public boolean startGame() {
    if (redPlayer == null || blackPlayer == null) {
      return false;
    }

    if (!redPlayer.isReady() || !blackPlayer.isReady()) {
      return false;
    }

    this.status = GameStatus.PLAYING;
    this.startTime = LocalDateTime.now();
    this.lastMoveTime = LocalDateTime.now();
    return true;
  }

  /**
   * 选择棋子（主要用于前端交互）
   */
  public boolean selectPiece(Position position) {
    if (status != GameStatus.PLAYING) {
      return false;
    }

    ChessPiece piece = board.getPieceAt(position);

    // 如果点击的是空位或敌方棋子，尝试移动
    if (selectedPiece != null && (piece == null || piece.getColor() != currentPlayer)) {
      return tryMove(selectedPiece, position);
    }

    // 选择己方棋子
    if (piece != null && piece.getColor() == currentPlayer) {
      this.selectedPiece = position;
      this.availableMoves = getValidMovesForPiece(position);
      return true;
    }

    // 取消选择
    clearSelection();
    return false;
  }

  /**
   * 清除选择
   */
  public void clearSelection() {
    this.selectedPiece = null;
    this.availableMoves.clear();
  }

  /**
   * 获取指定棋子的有效移动位置
   */
  public List<Position> getValidMovesForPiece(Position position) {
    ChessPiece piece = board.getPieceAt(position);
    if (piece == null || piece.getColor() != currentPlayer) {
      return new ArrayList<>();
    }

    List<Move> legalMoves = GameLogic.getLegalMoves(board, currentPlayer);
    List<Position> validPositions = new ArrayList<>();

    for (Move move : legalMoves) {
      if (move.getFrom().equals(position)) {
        validPositions.add(move.getTo());
      }
    }

    return validPositions;
  }

  /**
   * 尝试移动棋子
   */
  public boolean tryMove(Position from, Position to) {
    if (status != GameStatus.PLAYING) {
      return false;
    }

    ChessPiece piece = board.getPieceAt(from);
    if (piece == null || piece.getColor() != currentPlayer) {
      return false;
    }

    // 创建移动对象
    ChessPiece capturedPiece = board.getPieceAt(to);
    Move move = new Move(from, to, piece, capturedPiece);

    // 检查移动是否合法
    if (!GameLogic.isLegalMove(board, move)) {
      return false;
    }

    // 执行移动
    return executeMove(move);
  }

  /**
   * 执行移动
   */
  private boolean executeMove(Move move) {
    // 执行移动
    Move executedMove = board.makeMove(move.getFrom(), move.getTo());
    if (executedMove == null) {
      return false;
    }

    // 记录移动
    moveHistory.push(executedMove);
    gameRecord.add(moveToString(executedMove));

    // 清除选择
    clearSelection();

    // 更新游戏状态
    lastMoveTime = LocalDateTime.now();

    // 切换玩家
    switchPlayer();

    // 检查游戏是否结束
    GameStatus newStatus = GameLogic.checkGameStatus(board, currentPlayer);
    if (newStatus != GameStatus.PLAYING) {
      this.status = newStatus;
    }

    return true;
  }

  /**
   * 切换当前玩家
   */
  private void switchPlayer() {
    currentPlayer = (currentPlayer == PlayerColor.RED) ? PlayerColor.BLACK : PlayerColor.RED;
  }

  /**
   * 悔棋
   */
  public boolean undoMove() {
    if (!allowUndo || moveHistory.isEmpty() || currentUndoCount >= maxUndoCount) {
      return false;
    }

    if (status != GameStatus.PLAYING) {
      return false;
    }

    Move lastMove = moveHistory.pop();
    board.undoMove(lastMove);
    gameRecord.add("悔棋");

    currentUndoCount++;
    switchPlayer();
    clearSelection();

    return true;
  }

  /**
   * 投降
   */
  public void surrender() {
    if (status == GameStatus.PLAYING) {
      status = (currentPlayer == PlayerColor.RED) ? GameStatus.BLACK_WIN : GameStatus.RED_WIN;
      gameRecord.add(getCurrentPlayerName() + "投降");
    }
  }

  /**
   * 求和
   */
  public void proposeDraw() {
    // 这里可以实现求和逻辑，需要对方同意
    gameRecord.add(getCurrentPlayerName() + "求和");
  }

  /**
   * 同意和棋
   */
  public void acceptDraw() {
    if (status == GameStatus.PLAYING) {
      status = GameStatus.DRAW;
      gameRecord.add("双方同意和棋");
    }
  }

  /**
   * 获取当前玩家
   */
  public Player getCurrentPlayerInfo() {
    return (currentPlayer == PlayerColor.RED) ? redPlayer : blackPlayer;
  }

  /**
   * 获取当前玩家名称
   */
  public String getCurrentPlayerName() {
    Player player = getCurrentPlayerInfo();
    return player != null ? player.getName() : "未知玩家";
  }

  /**
   * 检查是否轮到指定玩家
   */
  public boolean isPlayerTurn(Long userId) {
    Player currentPlayerInfo = getCurrentPlayerInfo();
    return currentPlayerInfo != null &&
        currentPlayerInfo.getUserId() != null &&
        currentPlayerInfo.getUserId().equals(userId);
  }

  /**
   * 获取游戏状态摘要（用于前端显示）
   */
  public String getGameStatusSummary() {
    switch (status) {
      case WAITING:
        return "等待玩家加入";
      case PLAYING:
        if (GameLogic.isInCheck(board, currentPlayer)) {
          return getCurrentPlayerName() + "被将军";
        }
        return getCurrentPlayerName() + "思考中";
      case RED_WIN:
        return "红方胜利";
      case BLACK_WIN:
        return "黑方胜利";
      case DRAW:
        return "平局";
      case PAUSED:
        return "游戏暂停";
      case ABANDONED:
        return "游戏终止";
      default:
        return "未知状态";
    }
  }

  /**
   * 将移动转换为字符串记录
   */
  private String moveToString(Move move) {
    ChessPiece piece = move.getPiece();
    String pieceName = piece.getName();
    String from = positionToString(move.getFrom());
    String to = positionToString(move.getTo());

    if (move.isCapture()) {
      return pieceName + from + "吃" + to;
    } else {
      return pieceName + from + "到" + to;
    }
  }

  /**
   * 将位置转换为中国象棋记录格式
   */
  private String positionToString(Position pos) {
    // 简化版本，实际中国象棋记谱更复杂
    return "(" + pos.getRow() + "," + pos.getCol() + ")";
  }

  /**
   * 序列化为JSON字符串（用于网络传输）
   */
  public String toJson() {
    // 这里可以使用JSON库进行序列化
    // 为了保持原生Java，这里只是一个示例结构
    StringBuilder json = new StringBuilder();
    json.append("{");
    json.append("\"gameId\":\"").append(gameId).append("\",");
    json.append("\"roomId\":\"").append(roomId).append("\",");
    json.append("\"status\":\"").append(status).append("\",");
    json.append("\"currentPlayer\":\"").append(currentPlayer).append("\",");
    json.append("\"redTimeLeft\":").append(redTimeLeft).append(",");
    json.append("\"blackTimeLeft\":").append(blackTimeLeft);
    json.append("}");
    return json.toString();
  }

  // Getters and Setters
  public String getGameId() {
    return gameId;
  }

  public void setGameId(String gameId) {
    this.gameId = gameId;
  }

  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
  }

  public GameStatus getStatus() {
    return status;
  }

  public void setStatus(GameStatus status) {
    this.status = status;
  }

  public PlayerColor getCurrentPlayer() {
    return currentPlayer;
  }

  public void setCurrentPlayer(PlayerColor currentPlayer) {
    this.currentPlayer = currentPlayer;
  }

  public Player getRedPlayer() {
    return redPlayer;
  }

  public Player getBlackPlayer() {
    return blackPlayer;
  }

  public ChessBoard getBoard() {
    return board;
  }

  public void setBoard(ChessBoard board) {
    this.board = board;
  }

  public Position getSelectedPiece() {
    return selectedPiece;
  }

  public List<Position> getAvailableMoves() {
    return availableMoves;
  }

  public boolean isShowHints() {
    return showHints;
  }

  public void setShowHints(boolean showHints) {
    this.showHints = showHints;
  }

  public List<Move> getMoveHistory() {
    return new ArrayList<>(moveHistory);
  }

  public List<String> getGameRecord() {
    return new ArrayList<>(gameRecord);
  }

  public int getRedTimeLeft() {
    return redTimeLeft;
  }

  public void setRedTimeLeft(int redTimeLeft) {
    this.redTimeLeft = redTimeLeft;
  }

  public int getBlackTimeLeft() {
    return blackTimeLeft;
  }

  public void setBlackTimeLeft(int blackTimeLeft) {
    this.blackTimeLeft = blackTimeLeft;
  }

  public boolean isUseTimer() {
    return useTimer;
  }

  public void setUseTimer(boolean useTimer) {
    this.useTimer = useTimer;
  }

  public boolean isAllowUndo() {
    return allowUndo;
  }

  public void setAllowUndo(boolean allowUndo) {
    this.allowUndo = allowUndo;
  }

  public int getMaxUndoCount() {
    return maxUndoCount;
  }

  public void setMaxUndoCount(int maxUndoCount) {
    this.maxUndoCount = maxUndoCount;
  }

  public int getCurrentUndoCount() {
    return currentUndoCount;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public LocalDateTime getLastMoveTime() {
    return lastMoveTime;
  }
}
