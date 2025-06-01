package com.mimeng.chess.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.mimeng.chess.entity.chess.ChessBoard;
import com.mimeng.chess.entity.chess.ChessGameState;
import com.mimeng.chess.entity.chess.ChessPiece;
import com.mimeng.chess.entity.chess.PlayerColor;
import com.mimeng.chess.entity.chess.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义棋盘视图
 * 负责绘制10x9的中国象棋棋盘、棋子，处理触摸事件
 */
public class ChessBoardView extends View {

  private static final String TAG = "ChessBoardView";

  // 棋盘尺寸常量
  private static final int ROWS = 10; // 10行
  private static final int COLS = 9; // 9列

  // 绘制相关
  private Paint linePaint; // 线条画笔
  private Paint textPaint; // 文字画笔
  private Paint piecePaint; // 棋子画笔
  private Paint selectedPaint; // 选中状态画笔
  private Paint availableMovePaint; // 可移动位置画笔
  private Paint backgroundPaint; // 背景画笔

  // 布局相关
  private float cellWidth; // 格子宽度
  private float cellHeight; // 格子高度
  private float boardLeft; // 棋盘左边距
  private float boardTop; // 棋盘上边距
  private float pieceRadius; // 棋子半径

  // 游戏状态
  private ChessGameState gameState;
  private PlayerColor playerColor = PlayerColor.RED; // 当前玩家颜色
  private Position selectedPosition; // 选中的位置
  private List<Position> availableMoves = new ArrayList<>(); // 可移动位置

  // 事件监听器
  private OnPieceClickListener onPieceClickListener;

  public interface OnPieceClickListener {
    void onPieceClick(Position position);

    void onMoveComplete(Position from, Position to);
  }

  public ChessBoardView(Context context) {
    super(context);
    init();
  }

  public ChessBoardView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public ChessBoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  /**
   * 初始化画笔和样式
   */
  private void init() {
    // 线条画笔
    linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    linePaint.setColor(Color.BLACK);
    linePaint.setStrokeWidth(2f);
    linePaint.setStyle(Paint.Style.STROKE);

    // 文字画笔
    textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    textPaint.setColor(Color.BLACK);
    textPaint.setTextAlign(Paint.Align.CENTER);

    // 棋子画笔
    piecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    piecePaint.setStyle(Paint.Style.FILL);

    // 选中状态画笔
    selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    selectedPaint.setColor(Color.parseColor("#4CAF50"));
    selectedPaint.setStyle(Paint.Style.STROKE);
    selectedPaint.setStrokeWidth(4f); // 可移动位置画笔
    availableMovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    availableMovePaint.setColor(Color.parseColor("#4CAF50")); // 绿色
    availableMovePaint.setStyle(Paint.Style.FILL);

    // 背景画笔
    backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    backgroundPaint.setColor(Color.parseColor("#FFF8DC")); // 米色背景
    backgroundPaint.setStyle(Paint.Style.FILL);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    calculateLayout(w, h);
  }

  /**
   * 计算布局尺寸
   */
  private void calculateLayout(int width, int height) {
    // 保持棋盘比例，预留边距
    int padding = 20;
    int availableWidth = width - 2 * padding;
    int availableHeight = height - 2 * padding;

    // 计算格子大小，保持正方形
    cellWidth = (float) availableWidth / (COLS - 1);
    cellHeight = (float) availableHeight / (ROWS - 1);

    // 使用较小的值保持正方形格子
    float cellSize = Math.min(cellWidth, cellHeight);
    cellWidth = cellHeight = cellSize;

    // 计算棋盘偏移量以居中显示
    boardLeft = (width - (COLS - 1) * cellWidth) / 2f;
    boardTop = (height - (ROWS - 1) * cellHeight) / 2f;

    // 棋子半径为格子大小的35%
    pieceRadius = cellSize * 0.35f;

    // 更新文字大小
    textPaint.setTextSize(pieceRadius * 0.8f);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    Log.d(TAG, "onDraw called - cellWidth: " + cellWidth + ", cellHeight: " + cellHeight);

    if (cellWidth <= 0 || cellHeight <= 0) {
      Log.d(TAG, "onDraw skipped due to invalid cell dimensions");
      return;
    }

    // 绘制背景
    drawBackground(canvas);

    // 绘制棋盘线条
    drawBoardLines(canvas);

    // 绘制河界文字
    drawRiverText(canvas);

    // 绘制可移动位置
    drawAvailableMoves(canvas);

    // 绘制棋子
    drawPieces(canvas);

    // 绘制选中状态
    drawSelectedPosition(canvas);

    Log.d(TAG, "onDraw completed");
  }

  /**
   * 绘制背景
   */
  private void drawBackground(Canvas canvas) {
    canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
  }

  /**
   * 绘制棋盘线条
   */
  private void drawBoardLines(Canvas canvas) {
    // 绘制横线
    for (int row = 0; row < ROWS; row++) {
      float y = boardTop + row * cellHeight;
      canvas.drawLine(boardLeft, y, boardLeft + (COLS - 1) * cellWidth, y, linePaint);
    }

    // 绘制竖线
    for (int col = 0; col < COLS; col++) {
      float x = boardLeft + col * cellWidth;
      // 上半部分（0-4行）
      canvas.drawLine(x, boardTop, x, boardTop + 4 * cellHeight, linePaint);
      // 下半部分（5-9行）
      canvas.drawLine(x, boardTop + 5 * cellHeight, x, boardTop + 9 * cellHeight, linePaint);
    }

    // 绘制九宫格对角线
    drawPalaceDiagonals(canvas);
  }

  /**
   * 绘制九宫格对角线
   */
  private void drawPalaceDiagonals(Canvas canvas) {
    // 上方九宫格 (3,0) 到 (5,2)
    float x1 = boardLeft + 3 * cellWidth;
    float y1 = boardTop;
    float x2 = boardLeft + 5 * cellWidth;
    float y2 = boardTop + 2 * cellHeight;
    canvas.drawLine(x1, y1, x2, y2, linePaint);
    canvas.drawLine(x2, y1, x1, y2, linePaint);

    // 下方九宫格 (3,7) 到 (5,9)
    x1 = boardLeft + 3 * cellWidth;
    y1 = boardTop + 7 * cellHeight;
    x2 = boardLeft + 5 * cellWidth;
    y2 = boardTop + 9 * cellHeight;
    canvas.drawLine(x1, y1, x2, y2, linePaint);
    canvas.drawLine(x2, y1, x1, y2, linePaint);
  }

  /**
   * 绘制河界文字
   */
  private void drawRiverText(Canvas canvas) {
    float riverY = boardTop + 4.5f * cellHeight;

    // 楚河
    String chuHe = "楚河";
    canvas.drawText(chuHe, boardLeft + 2 * cellWidth, riverY + textPaint.getTextSize() / 3, textPaint);

    // 汉界
    String hanJie = "汉界";
    canvas.drawText(hanJie, boardLeft + 6 * cellWidth, riverY + textPaint.getTextSize() / 3, textPaint);
  }

  /**
   * 绘制可移动位置
   */
  private void drawAvailableMoves(Canvas canvas) {
    for (Position position : availableMoves) {
      float x = boardLeft + position.getCol() * cellWidth;
      float y = boardTop + position.getRow() * cellHeight;

      // 绘制绿色圆点表示可移动位置
      float dotRadius = pieceRadius * 0.4f;
      canvas.drawCircle(x, y, dotRadius, availableMovePaint);

      // 添加白色边框使绿点更明显
      Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      borderPaint.setColor(Color.WHITE);
      borderPaint.setStyle(Paint.Style.STROKE);
      borderPaint.setStrokeWidth(2f);
      canvas.drawCircle(x, y, dotRadius, borderPaint);
    }
  }

  /**
   * 绘制棋子
   */
  private void drawPieces(Canvas canvas) {
    if (gameState == null || gameState.getBoard() == null) {
      Log.d(TAG, "Cannot draw pieces: gameState or board is null");
      return;
    }

    ChessBoard board = gameState.getBoard();
    int piecesDrawn = 0;

    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLS; col++) {
        ChessPiece piece = board.getPieceAt(new Position(row, col));
        if (piece != null) {
          drawPiece(canvas, piece, row, col);
          piecesDrawn++;
        }
      }
    }

    Log.d(TAG, "Drew " + piecesDrawn + " pieces on the board");
  }

  /**
   * 绘制单个棋子
   */
  private void drawPiece(Canvas canvas, ChessPiece piece, int row, int col) {
    float x = boardLeft + col * cellWidth;
    float y = boardTop + row * cellHeight;

    // 设置棋子颜色
    if (piece.getColor() == PlayerColor.RED) {
      piecePaint.setColor(Color.parseColor("#F44336")); // 红色
    } else {
      piecePaint.setColor(Color.parseColor("#424242")); // 深灰色
    }

    // 绘制棋子圆形背景
    canvas.drawCircle(x, y, pieceRadius, piecePaint);

    // 绘制棋子边框
    Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    borderPaint.setColor(Color.BLACK);
    borderPaint.setStyle(Paint.Style.STROKE);
    borderPaint.setStrokeWidth(2f);
    canvas.drawCircle(x, y, pieceRadius, borderPaint);

    // 绘制棋子文字
    Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    textPaint.setColor(Color.WHITE);
    textPaint.setTextSize(pieceRadius * 1.2f);
    textPaint.setTextAlign(Paint.Align.CENTER);
    textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

    String text = getPieceText(piece);

    // 计算文字垂直居中位置
    Rect bounds = new Rect();
    textPaint.getTextBounds(text, 0, text.length(), bounds);
    float textY = y + bounds.height() / 2f;

    canvas.drawText(text, x, textY, textPaint);
  }

  /**
   * 获取棋子显示文字
   */
  private String getPieceText(ChessPiece piece) {
    String pieceClass = piece.getClass().getSimpleName();
    boolean isRed = piece.getColor() == PlayerColor.RED;

    switch (pieceClass) {
      case "King":
        return isRed ? "帅" : "将";
      case "Guard":
        return isRed ? "仕" : "士";
      case "Elephant":
        return isRed ? "相" : "象";
      case "Horse":
        return "马";
      case "Rook":
        return "车";
      case "Cannon":
        return "炮";
      case "Pawn":
        return isRed ? "兵" : "卒";
      default:
        return "?";
    }
  }

  /**
   * 绘制选中位置
   */
  private void drawSelectedPosition(Canvas canvas) {
    if (selectedPosition != null) {
      float x = boardLeft + selectedPosition.getCol() * cellWidth;
      float y = boardTop + selectedPosition.getRow() * cellHeight;

      // 绘制选中框
      canvas.drawCircle(x, y, pieceRadius + 5, selectedPaint);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      return handleTouch(event.getX(), event.getY());
    }
    return super.onTouchEvent(event);
  }

  /**
   * 处理触摸事件
   */
  private boolean handleTouch(float touchX, float touchY) {
    // 转换为棋盘坐标
    Position position = screenToBoard(touchX, touchY);

    if (position == null) {
      return false;
    }

    Log.d(TAG, "Touch at position: " + position.getRow() + ", " + position.getCol());

    if (onPieceClickListener != null) {
      // 如果当前有选中的棋子，且点击的是可移动位置，则执行移动
      if (selectedPosition != null && availableMoves.contains(position)) {
        onPieceClickListener.onMoveComplete(selectedPosition, position);
        // 清除选中状态
        setSelectedPosition(null);
        setAvailableMoves(new ArrayList<>());
      } else {
        // 否则触发棋子点击事件
        onPieceClickListener.onPieceClick(position);
      }
    }

    return true;
  }

  /**
   * 屏幕坐标转换为棋盘坐标
   */
  private Position screenToBoard(float screenX, float screenY) {
    if (cellWidth <= 0 || cellHeight <= 0) {
      return null;
    }

    // 计算相对于棋盘的坐标
    float relativeX = screenX - boardLeft;
    float relativeY = screenY - boardTop;

    // 转换为行列坐标
    int col = Math.round(relativeX / cellWidth);
    int row = Math.round(relativeY / cellHeight);

    // 检查边界
    if (row >= 0 && row < ROWS && col >= 0 && col < COLS) {
      return new Position(row, col);
    }

    return null;
  }

  /**
   * 棋盘坐标转换为屏幕坐标
   */
  private float[] boardToScreen(Position position) {
    float x = boardLeft + position.getCol() * cellWidth;
    float y = boardTop + position.getRow() * cellHeight;
    return new float[] { x, y };
  }

  // ===== Getters and Setters =====
  public void setGameState(ChessGameState gameState) {
    Log.d(TAG, "Setting new game state, forcing redraw");
    this.gameState = gameState;

    // 强制立即重绘
    invalidate();

    // 如果在主线程中，立即请求重绘
    if (Looper.getMainLooper() == Looper.myLooper()) {
      post(new Runnable() {
        @Override
        public void run() {
          invalidate();
        }
      });
    }
  }

  public void setPlayerColor(PlayerColor playerColor) {
    this.playerColor = playerColor;
    invalidate();
  }

  public void setSelectedPosition(Position position) {
    this.selectedPosition = position;
    invalidate();
  }

  public void setAvailableMoves(List<Position> availableMoves) {
    this.availableMoves = availableMoves != null ? availableMoves : new ArrayList<>();
    invalidate();
  }

  public void setOnPieceClickListener(OnPieceClickListener listener) {
    this.onPieceClickListener = listener;
  }

  public Position getSelectedPosition() {
    return selectedPosition;
  }

  public List<Position> getAvailableMoves() {
    return new ArrayList<>(availableMoves);
  }
}
