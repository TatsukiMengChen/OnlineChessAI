package com.mimeng.chess.entity.chess;

import com.mimeng.chess.entity.chess.pieces.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 象棋棋盘类
 */
public class ChessBoard {
  private static final int ROWS = 10;
  private static final int COLS = 9;

  private ChessPiece[][] board;

  public ChessBoard() {
    this.board = new ChessPiece[ROWS][COLS];
    initializeBoard();
  }

  /**
   * 初始化棋盘（标准开局）
   */
  private void initializeBoard() {
    // 清空棋盘
    for (int i = 0; i < ROWS; i++) {
      for (int j = 0; j < COLS; j++) {
        board[i][j] = null;
      }
    }

    // 黑方棋子（上方）
    board[0][0] = new Rook(PlayerColor.BLACK, new Position(0, 0));
    board[0][1] = new Horse(PlayerColor.BLACK, new Position(0, 1));
    board[0][2] = new Elephant(PlayerColor.BLACK, new Position(0, 2));
    board[0][3] = new Guard(PlayerColor.BLACK, new Position(0, 3));
    board[0][4] = new King(PlayerColor.BLACK, new Position(0, 4));
    board[0][5] = new Guard(PlayerColor.BLACK, new Position(0, 5));
    board[0][6] = new Elephant(PlayerColor.BLACK, new Position(0, 6));
    board[0][7] = new Horse(PlayerColor.BLACK, new Position(0, 7));
    board[0][8] = new Rook(PlayerColor.BLACK, new Position(0, 8));

    board[2][1] = new Cannon(PlayerColor.BLACK, new Position(2, 1));
    board[2][7] = new Cannon(PlayerColor.BLACK, new Position(2, 7));

    for (int col = 0; col < COLS; col += 2) {
      board[3][col] = new Pawn(PlayerColor.BLACK, new Position(3, col));
    }

    // 红方棋子（下方）
    board[9][0] = new Rook(PlayerColor.RED, new Position(9, 0));
    board[9][1] = new Horse(PlayerColor.RED, new Position(9, 1));
    board[9][2] = new Elephant(PlayerColor.RED, new Position(9, 2));
    board[9][3] = new Guard(PlayerColor.RED, new Position(9, 3));
    board[9][4] = new King(PlayerColor.RED, new Position(9, 4));
    board[9][5] = new Guard(PlayerColor.RED, new Position(9, 5));
    board[9][6] = new Elephant(PlayerColor.RED, new Position(9, 6));
    board[9][7] = new Horse(PlayerColor.RED, new Position(9, 7));
    board[9][8] = new Rook(PlayerColor.RED, new Position(9, 8));

    board[7][1] = new Cannon(PlayerColor.RED, new Position(7, 1));
    board[7][7] = new Cannon(PlayerColor.RED, new Position(7, 7));

    for (int col = 0; col < COLS; col += 2) {
      board[6][col] = new Pawn(PlayerColor.RED, new Position(6, col));
    }
  }

  /**
   * 获取指定位置的棋子
   */
  public ChessPiece getPieceAt(Position position) {
    if (!position.isValid()) {
      return null;
    }
    return board[position.getRow()][position.getCol()];
  }

  /**
   * 设置指定位置的棋子
   */
  public void setPieceAt(Position position, ChessPiece piece) {
    if (position.isValid()) {
      board[position.getRow()][position.getCol()] = piece;
      if (piece != null) {
        piece.setPosition(position);
      }
    }
  }

  /**
   * 移除指定位置的棋子
   */
  public ChessPiece removePieceAt(Position position) {
    ChessPiece piece = getPieceAt(position);
    setPieceAt(position, null);
    return piece;
  }

  /**
   * 执行移动
   */
  public Move makeMove(Position from, Position to) {
    ChessPiece piece = getPieceAt(from);
    ChessPiece capturedPiece = getPieceAt(to);

    if (piece != null && piece.canMoveTo(to, this)) {
      removePieceAt(from);
      setPieceAt(to, piece);
      return new Move(from, to, piece, capturedPiece);
    }

    return null;
  }

  /**
   * 撤销移动
   */
  public void undoMove(Move move) {
    ChessPiece piece = getPieceAt(move.getTo());
    setPieceAt(move.getFrom(), piece);
    setPieceAt(move.getTo(), move.getCapturedPiece());
  }

  /**
   * 获取所有指定颜色的棋子
   */
  public List<ChessPiece> getPieces(PlayerColor color) {
    List<ChessPiece> pieces = new ArrayList<>();
    for (int i = 0; i < ROWS; i++) {
      for (int j = 0; j < COLS; j++) {
        ChessPiece piece = board[i][j];
        if (piece != null && piece.getColor() == color) {
          pieces.add(piece);
        }
      }
    }
    return pieces;
  }

  /**
   * 获取指定颜色的将/帅
   */
  public ChessPiece getKing(PlayerColor color) {
    for (ChessPiece piece : getPieces(color)) {
      if (piece.getType() == PieceType.KING) {
        return piece;
      }
    }
    return null;
  }

  /**
   * 获取所有可能的移动
   */
  public List<Move> getAllPossibleMoves(PlayerColor color) {
    List<Move> moves = new ArrayList<>();
    for (ChessPiece piece : getPieces(color)) {
      for (Position target : piece.getPossibleMoves(this)) {
        ChessPiece capturedPiece = getPieceAt(target);
        moves.add(new Move(piece.getPosition(), target, piece, capturedPiece));
      }
    }
    return moves;
  }

  /**
   * 复制棋盘状态
   */
  public ChessBoard copy() {
    ChessBoard newBoard = new ChessBoard();
    for (int i = 0; i < ROWS; i++) {
      for (int j = 0; j < COLS; j++) {
        ChessPiece piece = board[i][j];
        if (piece != null) {
          ChessPiece newPiece = createPieceCopy(piece);
          newBoard.board[i][j] = newPiece;
        }
      }
    }
    return newBoard;
  }

  /**
   * 创建棋子副本
   */
  private ChessPiece createPieceCopy(ChessPiece piece) {
    Position pos = piece.getPosition();
    PlayerColor color = piece.getColor();

    switch (piece.getType()) {
      case KING:
        return new King(color, pos);
      case GUARD:
        return new Guard(color, pos);
      case ELEPHANT:
        return new Elephant(color, pos);
      case HORSE:
        return new Horse(color, pos);
      case ROOK:
        return new Rook(color, pos);
      case CANNON:
        return new Cannon(color, pos);
      case PAWN:
        return new Pawn(color, pos);
      default:
        return null;
    }
  }

  /**
   * 转换为字符串表示（用于调试）
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("  0 1 2 3 4 5 6 7 8\n");
    for (int i = 0; i < ROWS; i++) {
      sb.append(i).append(" ");
      for (int j = 0; j < COLS; j++) {
        ChessPiece piece = board[i][j];
        if (piece == null) {
          sb.append("· ");
        } else {
          sb.append(piece.getName()).append(" ");
        }
      }
      sb.append("\n");
    }
    return sb.toString();
  }
}
