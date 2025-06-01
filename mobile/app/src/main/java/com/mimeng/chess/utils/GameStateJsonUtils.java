package com.mimeng.chess.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mimeng.chess.entity.chess.*;
import com.mimeng.chess.entity.chess.pieces.*; // Import all piece classes

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏状态JSON工具类
 * 负责游戏状态与JSON之间的转换
 */
public class GameStateJsonUtils {

  private static final Gson gson = new Gson();

  /**
   * 从JSON解析游戏状态
   */
  public static ChessGameState parseGameState(JsonObject json, String roomId) {
    try {
      String gameId = json.has("gameId") ? json.get("gameId").getAsString() : "unknown";
      ChessGameState gameState = new ChessGameState(gameId, roomId);

      // 解析当前玩家
      if (json.has("currentPlayer")) {
        String currentPlayerStr = json.get("currentPlayer").getAsString();
        PlayerColor currentPlayer = "red".equalsIgnoreCase(currentPlayerStr) ? PlayerColor.RED : PlayerColor.BLACK;
        gameState.setCurrentPlayer(currentPlayer);
      }

      // 解析游戏状态
      if (json.has("status")) {
        String statusStr = json.get("status").getAsString();
        GameStatus status = parseGameStatus(statusStr);
        gameState.setStatus(status);
      }

      // 解析棋盘
      if (json.has("board")) {
        ChessBoard board = parseBoard(json.getAsJsonObject("board"));
        gameState.setBoard(board);
      }

      // 解析计时器信息
      if (json.has("redTimeLeft")) {
        gameState.setRedTimeLeft(json.get("redTimeLeft").getAsInt());
      }
      if (json.has("blackTimeLeft")) {
        gameState.setBlackTimeLeft(json.get("blackTimeLeft").getAsInt());
      }
      if (json.has("useTimer")) {
        gameState.setUseTimer(json.get("useTimer").getAsBoolean());
      }

      // 解析悔棋设置
      if (json.has("allowUndo")) {
        gameState.setAllowUndo(json.get("allowUndo").getAsBoolean());
      }

      return gameState;
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse game state", e);
    }
  }

  /**
   * 解析棋盘
   */
  private static ChessBoard parseBoard(JsonObject boardJson) {
    ChessBoard board = new ChessBoard();

    if (boardJson.has("pieces")) {
      JsonArray piecesArray = boardJson.getAsJsonArray("pieces");
      for (JsonElement element : piecesArray) {
        JsonObject pieceObj = element.getAsJsonObject();
        ChessPiece piece = parsePiece(pieceObj);
        if (piece != null) {
          int row = pieceObj.get("row").getAsInt();
          int col = pieceObj.get("col").getAsInt();
          board.setPieceAt(new Position(row, col), piece);
        }
      }
    }

    return board;
  }

  /**
   * 解析棋子
   */
  private static ChessPiece parsePiece(JsonObject pieceJson) {
    try {
      String type = pieceJson.get("type").getAsString();
      String colorStr = pieceJson.get("color").getAsString();
      PlayerColor color = "red".equalsIgnoreCase(colorStr) ? PlayerColor.RED : PlayerColor.BLACK;
      int row = pieceJson.get("row").getAsInt();
      int col = pieceJson.get("col").getAsInt();
      Position position = new Position(row, col);

      switch (type.toLowerCase()) {
        case "king":
          return new King(color, position);
        case "guard":
          return new Guard(color, position);
        case "elephant":
          return new Elephant(color, position);
        case "horse":
          return new Horse(color, position);
        case "rook":
          return new Rook(color, position);
        case "cannon":
          return new Cannon(color, position);
        case "pawn":
          return new Pawn(color, position);
        default:
          return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 解析游戏状态枚举
   */
  private static GameStatus parseGameStatus(String statusStr) {
    switch (statusStr.toLowerCase()) {
      case "waiting":
        return GameStatus.WAITING;
      case "playing": // Changed from in_progress to playing
      case "in_progress": // Keep in_progress for compatibility if backend might send it
        return GameStatus.PLAYING; // Map both to PLAYING
      case "red_win":
        return GameStatus.RED_WIN;
      case "black_win":
        return GameStatus.BLACK_WIN;
      case "draw":
        return GameStatus.DRAW;
      default:
        return GameStatus.WAITING;
    }
  }

  /**
   * 解析可移动位置列表
   */
  public static List<Position> parseAvailableMoves(JsonObject json) {
    List<Position> moves = new ArrayList<>();

    if (json.has("availableMoves")) {
      JsonArray movesArray = json.getAsJsonArray("availableMoves");
      for (JsonElement element : movesArray) {
        JsonObject moveObj = element.getAsJsonObject();
        int row = moveObj.get("row").getAsInt();
        int col = moveObj.get("col").getAsInt();
        moves.add(new Position(row, col));
      }
    }

    return moves;
  }

  /**
   * 解析移动
   */
  public static Move parseMove(JsonObject json) {
    try {
      JsonObject fromObj = json.getAsJsonObject("from");
      JsonObject toObj = json.getAsJsonObject("to");

      Position from = new Position(fromObj.get("row").getAsInt(), fromObj.get("col").getAsInt());
      Position to = new Position(toObj.get("row").getAsInt(), toObj.get("col").getAsInt());

      // 现在 Move 类有一个接受 from 和 to 的构造函数
      return new Move(from, to);
    } catch (Exception e) {
      System.err.println("GameStateJsonUtils.parseMove: Error parsing move JSON: " + e.getMessage());
      return null;
    }
  }

  /**
   * 确定当前玩家的颜色
   */
  public static PlayerColor determineMyColor(JsonObject gameStateJson, String myUserId) {
    try {
      // 从玩家信息中确定颜色
      if (gameStateJson.has("redPlayer")) {
        JsonObject redPlayer = gameStateJson.getAsJsonObject("redPlayer");
        if (redPlayer.has("userId") && myUserId.equals(redPlayer.get("userId").getAsString())) {
          return PlayerColor.RED;
        }
      }

      if (gameStateJson.has("blackPlayer")) {
        JsonObject blackPlayer = gameStateJson.getAsJsonObject("blackPlayer");
        if (blackPlayer.has("userId") && myUserId.equals(blackPlayer.get("userId").getAsString())) {
          return PlayerColor.BLACK;
        }
      }

      // 默认返回红方
      return PlayerColor.RED;
    } catch (Exception e) {
      return PlayerColor.RED;
    }
  }

  /**
   * 将游戏状态转换为JSON
   */
  public static JsonObject gameStateToJson(ChessGameState gameState) {
    JsonObject json = new JsonObject();

    json.addProperty("gameId", gameState.getGameId());
    json.addProperty("roomId", gameState.getRoomId());
    json.addProperty("currentPlayer", gameState.getCurrentPlayer().toString().toLowerCase());
    json.addProperty("status", gameState.getStatus().toString().toLowerCase());
    json.addProperty("useTimer", gameState.isUseTimer());
    json.addProperty("redTimeLeft", gameState.getRedTimeLeft());
    json.addProperty("blackTimeLeft", gameState.getBlackTimeLeft());
    json.addProperty("allowUndo", gameState.isAllowUndo());

    // 添加棋盘信息
    if (gameState.getBoard() != null) {
      json.add("board", boardToJson(gameState.getBoard()));
    }

    return json;
  }

  /**
   * 将棋盘转换为JSON
   */
  private static JsonObject boardToJson(ChessBoard board) {
    JsonObject boardJson = new JsonObject();
    JsonArray piecesArray = new JsonArray();

    for (int row = 0; row < 10; row++) {
      for (int col = 0; col < 9; col++) {
        Position pos = new Position(row, col);
        ChessPiece piece = board.getPieceAt(pos);
        if (piece != null) {
          JsonObject pieceObj = pieceToJson(piece);
          piecesArray.add(pieceObj);
        }
      }
    }

    boardJson.add("pieces", piecesArray);
    return boardJson;
  }

  /**
   * 将棋子转换为JSON
   */
  private static JsonObject pieceToJson(ChessPiece piece) {
    JsonObject pieceObj = new JsonObject();
    pieceObj.addProperty("type", piece.getClass().getSimpleName().toLowerCase());
    pieceObj.addProperty("color", piece.getColor().toString().toLowerCase());
    pieceObj.addProperty("row", piece.getPosition().getRow());
    pieceObj.addProperty("col", piece.getPosition().getCol());
    return pieceObj;
  }
}
