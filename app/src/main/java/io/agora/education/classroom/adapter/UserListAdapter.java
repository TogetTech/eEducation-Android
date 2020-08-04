package io.agora.education.classroom.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.agora.education.R;
import io.agora.education.api.stream.data.EduStreamInfo;
import io.agora.education.api.user.EduUser;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.classroom.bean.channel.User;

public class UserListAdapter extends BaseQuickAdapter<EduUserInfo, UserListAdapter.ViewHolder> {

    private String localUserUuid;
    private EduStreamInfo eduStreamInfo;
    private int localUserIndex;

    public void setEduStreamInfo(@NonNull EduStreamInfo eduStreamInfo) {
        this.eduStreamInfo = eduStreamInfo;
        notifyItemChanged(localUserIndex);
    }

    public UserListAdapter(@NonNull String localUserUuid, @NonNull EduStreamInfo eduStreamInfo) {
        super(R.layout.item_user_list);
        this.localUserUuid = localUserUuid;
        this.eduStreamInfo = eduStreamInfo;
        addChildClickViewIds(R.id.iv_btn_mute_audio, R.id.iv_btn_mute_video);
    }

    @Override
    protected void convert(@NonNull ViewHolder viewHolder, EduUserInfo eduUserInfo) {
        viewHolder.tv_name.setText(eduUserInfo.getUserName());
        if (eduUserInfo.getUserUuid() == localUserUuid) {
            localUserIndex = getItemPosition(eduUserInfo);
            viewHolder.iv_btn_grant_board.setVisibility(View.VISIBLE);
            viewHolder.iv_btn_mute_audio.setVisibility(View.VISIBLE);
            viewHolder.iv_btn_mute_video.setVisibility(View.VISIBLE);
//            viewHolder.iv_btn_grant_board.setSelected(eduUser.isBoardGrant());
            viewHolder.iv_btn_grant_board.setSelected(false);
            viewHolder.iv_btn_mute_audio.setSelected(eduStreamInfo.getHasAudio());
            viewHolder.iv_btn_mute_video.setSelected(eduStreamInfo.getHasVideo());
        } else {
            viewHolder.iv_btn_grant_board.setVisibility(View.GONE);
            viewHolder.iv_btn_mute_video.setVisibility(View.GONE);
            viewHolder.iv_btn_mute_audio.setVisibility(View.GONE);
        }
    }



    static class ViewHolder extends BaseViewHolder {
        @BindView(R.id.tv_name)
        TextView tv_name;
        @BindView(R.id.iv_btn_grant_board)
        ImageView iv_btn_grant_board;
        @BindView(R.id.iv_btn_mute_audio)
        ImageView iv_btn_mute_audio;
        @BindView(R.id.iv_btn_mute_video)
        ImageView iv_btn_mute_video;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
