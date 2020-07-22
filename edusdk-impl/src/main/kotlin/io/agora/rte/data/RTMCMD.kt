package io.agora.rte.data

enum class RTMCMD(var value: Int) {
    /**频道-房间-开始/结束*/
    RoomStateChange(1),
    /**频道-房间-禁用状态*/
    RoomMuteStateChange(2),
    /**频道-房间-即时聊天--文本消息*/
    ChannelMsgReceived(3),
    /**频道--自定义消息*/
    ChannelCustomMsgReceived(99),
    /**频道-用户-进出
     * 有人员进出时会触发此消息(包括进入，离开，踢出)*/
    UserJoinOrLeave(20),
    /**频道-流-新增/更新/删除*/
    StreamStateChange(40),
    /**频道-白板房间状态*/
    BoardRoomStateChange(60),
    /**频道-白板用户状态*/
    BoardUserStateChange(61),



    /**点对点-用户-私聊--文本消息*/
    PeerMsgReceived(1),
    /**点对点--自定义消息*/
    PeerCustomMsgReceived(99)
}