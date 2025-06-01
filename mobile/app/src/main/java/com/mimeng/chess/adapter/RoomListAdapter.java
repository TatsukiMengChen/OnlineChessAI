package com.mimeng.chess.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mimeng.chess.R;
import com.mimeng.chess.api.room.Room;
import com.mimeng.chess.utils.AuthManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 房间列表适配器
 */
public class RoomListAdapter extends RecyclerView.Adapter<RoomListAdapter.RoomViewHolder> {
  private List<Room> roomList;
  private OnRoomActionListener listener;
  private int currentUserId;

  public interface OnRoomActionListener {
    void onJoinRoom(Room room);

    void onQuitRoom(Room room);

    void onCloseRoom(Room room);
  }

  public RoomListAdapter(AuthManager authManager) {
    this.roomList = new ArrayList<>();
    if (authManager.getUser() != null) {
      this.currentUserId = authManager.getUser().id;
    }
  }

  public void setOnRoomActionListener(OnRoomActionListener listener) {
    this.listener = listener;
  }

  public void updateRooms(List<Room> rooms) {
    this.roomList.clear();
    if (rooms != null) {
      this.roomList.addAll(rooms);
    }
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_room, parent, false);
    return new RoomViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
    Room room = roomList.get(position);
    holder.bind(room);
  }

  @Override
  public int getItemCount() {
    return roomList.size();
  }

  public class RoomViewHolder extends RecyclerView.ViewHolder {
    private TextView tvRoomName;
    private TextView tvRoomStatus;
    private TextView tvRoomId;
    private TextView tvPlayerInfo;
    private Button btnJoin;
    private Button btnQuit;
    private Button btnClose;

    public RoomViewHolder(@NonNull View itemView) {
      super(itemView);
      tvRoomName = itemView.findViewById(R.id.tv_room_name);
      tvRoomStatus = itemView.findViewById(R.id.tv_room_status);
      tvRoomId = itemView.findViewById(R.id.tv_room_id);
      tvPlayerInfo = itemView.findViewById(R.id.tv_player_info);
      btnJoin = itemView.findViewById(R.id.btn_join);
      btnQuit = itemView.findViewById(R.id.btn_quit);
      btnClose = itemView.findViewById(R.id.btn_close);
    }

    public void bind(Room room) {
      // 设置房间信息
      tvRoomName.setText(room.name != null ? room.name : "房间-" + room.id.substring(0, 8));
      tvRoomStatus.setText(room.getStatusText());
      tvRoomId.setText("ID: " + room.id.substring(0, 8) + "...");
      tvPlayerInfo.setText(room.getPlayerInfoText());

      // 设置状态背景颜色
      updateStatusBackground(room);

      // 设置按钮可见性和点击事件
      updateButtonVisibility(room);
      setupButtonListeners(room);
    }

    private void updateStatusBackground(Room room) {
      int backgroundColor;
      switch (room.status) {
        case Room.Status.WAITING:
          backgroundColor = 0xFF4CAF50; // 绿色
          break;
        case Room.Status.PLAYING:
          backgroundColor = 0xFF2196F3; // 蓝色
          break;
        case Room.Status.FINISHED:
          backgroundColor = 0xFF9E9E9E; // 灰色
          break;
        case Room.Status.FULL:
          backgroundColor = 0xFFFF9800; // 橙色
          break;
        case Room.Status.CLOSED:
          backgroundColor = 0xFFF44336; // 红色
          break;
        default:
          backgroundColor = 0xFFE0E0E0; // 默认灰色
          break;
      }
      tvRoomStatus.setBackgroundColor(backgroundColor);
    }

    private void updateButtonVisibility(Room room) {
      boolean isMember = room.isMember(currentUserId);
      boolean isOwner = room.isOwner(currentUserId);
      boolean canJoin = room.canJoin() && !isMember;

      // 默认隐藏所有按钮
      btnJoin.setVisibility(View.GONE);
      btnQuit.setVisibility(View.GONE);
      btnClose.setVisibility(View.GONE);

      if (canJoin) {
        // 可以加入房间
        btnJoin.setVisibility(View.VISIBLE);
      } else if (isMember) {
        if (isOwner) {
          // 房主显示关闭房间按钮
          btnClose.setVisibility(View.VISIBLE);
        } else {
          // 普通成员显示退出房间按钮
          btnQuit.setVisibility(View.VISIBLE);
        }
      }
    }

    private void setupButtonListeners(Room room) {
      btnJoin.setOnClickListener(v -> {
        if (listener != null) {
          listener.onJoinRoom(room);
        }
      });

      btnQuit.setOnClickListener(v -> {
        if (listener != null) {
          listener.onQuitRoom(room);
        }
      });

      btnClose.setOnClickListener(v -> {
        if (listener != null) {
          listener.onCloseRoom(room);
        }
      });
    }
  }
}
