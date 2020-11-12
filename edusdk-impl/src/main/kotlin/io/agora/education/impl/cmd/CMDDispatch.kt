package io.agora.education.impl.cmd

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.Constants.Companion.AgoraLog
import io.agora.education.api.manager.listener.EduManagerEventListener
import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.EduRoomChangeType
import io.agora.education.api.room.data.RoomType
import io.agora.education.api.user.data.EduChatState
import io.agora.education.api.user.data.EduUserEvent
import io.agora.education.impl.cmd.bean.*
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.util.Convert
import io.agora.rtc.Constants
import io.agora.rte.RteEngineImpl


internal class CMDDispatch(private val eduRoom: EduRoom) {
    private val TAG = CMDDispatch::class.java.simpleName

    private val cmdCallbackManager: CMDCallbackManager = CMDCallbackManager()

    fun dispatchMsg(cmdResponseBody: CMDResponseBody<Any>?) {
        val text = Gson().toJson(cmdResponseBody)
        cmdResponseBody?.let {
            dispatchChannelMsg(text)
        }
    }

    fun dispatchChannelMsg(text: String) {
        val cmdResponseBody = Gson().fromJson<CMDResponseBody<Any>>(text, object :
                TypeToken<CMDResponseBody<Any>>() {}.type)
        when (cmdResponseBody.cmd) {
            CMDId.RoomStateChange.value -> {
                /**课堂状态发生改变*/
                val rtmRoomState = Gson().fromJson<CMDResponseBody<CMDRoomState>>(text, object :
                        TypeToken<CMDResponseBody<CMDRoomState>>() {}.type).data
                val roomStatus = (eduRoom as EduRoomImpl).getCurRoomStatus()
                roomStatus.courseState = Convert.convertRoomState(rtmRoomState.state)
                AgoraLog.i("$TAG->Course state changed to :${roomStatus.courseState.value}")
                roomStatus.startTime = rtmRoomState.startTime
                val operator = Convert.convertUserInfo(rtmRoomState.operator, eduRoom.getCurRoomType())
                cmdCallbackManager.onRoomStatusChanged(EduRoomChangeType.CourseState, operator, eduRoom)
            }
            CMDId.RoomMuteStateChange.value -> {
                val rtmRoomMuteState = Gson().fromJson<CMDResponseBody<CMDRoomMuteState>>(text, object :
                        TypeToken<CMDResponseBody<CMDRoomMuteState>>() {}.type).data
                val roomStatus = (eduRoom as EduRoomImpl).getCurRoomStatus()
                when (eduRoom.getCurRoomType()) {
                    RoomType.ONE_ON_ONE, RoomType.SMALL_CLASS -> {
                        /**判断本次更改是否包含针对学生的全部禁聊;*/
                        val broadcasterMuteChat = rtmRoomMuteState.muteChat?.broadcaster
                        broadcasterMuteChat?.let {
                            roomStatus.isStudentChatAllowed =
                                    broadcasterMuteChat.toFloat().toInt() == EduChatState.Allow.value
                        }
                        /**
                         * roomStatus中仅定义了isStudentChatAllowed来标识是否全员禁聊；
                         * 没有属性来标识是否全员禁摄像头和麦克风；
                         * 需要确定
                         * */
                    }
                    RoomType.LARGE_CLASS -> {
                        /**判断本次更改是否包含针对学生的全部禁聊;*/
                        val audienceMuteChat = rtmRoomMuteState.muteChat?.audience
                        audienceMuteChat?.let {
                            roomStatus.isStudentChatAllowed =
                                    audienceMuteChat.toFloat().toInt() == EduChatState.Allow.value
                        }
                        val broadcasterMuteChat = rtmRoomMuteState.muteChat?.broadcaster
                        broadcasterMuteChat?.let {
                            roomStatus.isStudentChatAllowed =
                                    broadcasterMuteChat.toFloat().toInt() == EduChatState.Allow.value
                        }
                    }
                }
                val operator = Convert.convertUserInfo(rtmRoomMuteState.operator, eduRoom.getCurRoomType())
                cmdCallbackManager.onRoomStatusChanged(EduRoomChangeType.AllStudentsChat, operator, eduRoom)
            }
            CMDId.RoomPropertyChanged.value -> {
                AgoraLog.i("$TAG->Received RTM message for roomProperty change:${text}")
                val propertyChangeEvent = Gson().fromJson<CMDResponseBody<CMDRoomPropertyRes>>(
                        text, object : TypeToken<CMDResponseBody<CMDRoomPropertyRes>>() {}.type).data
                /**把变化(update or delete)的属性更新到本地*/
                CMDDataMergeProcessor.updateRoomProperties(eduRoom, propertyChangeEvent)
                /**通知用户房间属性发生改变*/
                AgoraLog.i("$TAG->Callback the received roomProperty to upper layer")
                cmdCallbackManager.onRoomPropertyChanged(eduRoom, propertyChangeEvent.cause)
            }
            CMDId.ChannelMsgReceived.value -> {
                /**频道内的聊天消息*/
                AgoraLog.i("$TAG->Receive channel chat message")
                val eduMsg = CMDUtil.buildEduMsg(text, eduRoom) as EduChatMsg
                AgoraLog.i("$TAG->Build eduChatMsg1:${Gson().toJson(eduMsg)}")
                if (eduMsg.fromUser == (eduRoom as EduRoomImpl).getCurLocalUserInfo()) {
                    AgoraLog.i("$TAG->In channel msg sent by localUser，auto shield1")
                } else {
                    AgoraLog.i("$TAG->In channel msg sent by remoteUser，callback to upper layer1")
                    cmdCallbackManager.onRoomChatMessageReceived(eduMsg, eduRoom)
                }
            }
            CMDId.ChannelCustomMsgReceived.value -> {
                /**频道内自定义消息(可以是用户的自定义的信令)*/
                AgoraLog.i("$TAG->Receive channel custom message")
                val eduMsg = CMDUtil.buildEduMsg(text, eduRoom)
                AgoraLog.i("$TAG->Build eduMsg2:${Gson().toJson(eduMsg)}")
                if (eduMsg.fromUser == (eduRoom as EduRoomImpl).getCurLocalUserInfo()) {
                    AgoraLog.i("$TAG->In channel msg sent by localUser，auto shield2")
                } else {
                    AgoraLog.i("$TAG->In channel msg sent by remoteUser，callback to upper layer2")
                    cmdCallbackManager.onRoomMessageReceived(eduMsg, eduRoom)
                }
            }
            CMDId.UserJoinOrLeave.value -> {
                val rtmInOutMsg = Gson().fromJson<CMDResponseBody<RtmUserInOutMsg>>(text, object :
                        TypeToken<CMDResponseBody<RtmUserInOutMsg>>() {}.type).data
                AgoraLog.i("$TAG->Receive RTM of user online or offline->" +
                        "${(eduRoom as EduRoomImpl).getCurRoomUuid()}:${text}")

                /**根据回调数据，维护本地存储的流列表，并返回有效数据(可能同时包含local和remote数据)*/
                val validOnlineUsers = CMDDataMergeProcessor.addUserWithOnline(rtmInOutMsg.onlineUsers,
                        eduRoom.getCurUserList(), eduRoom.getCurRoomType())
                val validOfflineUsers = CMDDataMergeProcessor.removeUserWithOffline(rtmInOutMsg.offlineUsers,
                        eduRoom.getCurUserList(), eduRoom.getCurRoomType())

                /**从online和offline数据中剥离出本地用户的数据*/
                val validOnlineLocalUser = CMDProcessor.filterLocalUserInfo(
                        eduRoom.getCurLocalUserInfo(), validOnlineUsers)
                val validOfflineLocalUser = CMDProcessor.filterLocalUserEvent(
                        eduRoom.getCurLocalUserInfo(), validOfflineUsers)

                /**提取出online和offline携带的流信息(可能同时包含local和remote数据)*/
                val validAddedStreams = CMDDataMergeProcessor.addStreamWithUserOnline(rtmInOutMsg.onlineUsers,
                        eduRoom.getCurStreamList(), eduRoom.getCurRoomType())
                val validRemovedStreams = CMDDataMergeProcessor.removeStreamWithUserOffline(rtmInOutMsg.offlineUsers,
                        eduRoom.getCurStreamList(), eduRoom.getCurRoomType())

                /**从有效的流数据中剥离出本地用户的流数据*/
                val validAddedLocalStream = CMDProcessor.filterLocalStreamInfo(
                        eduRoom.getCurLocalUserInfo(), validAddedStreams)
                val validRemovedLocalStream = CMDProcessor.filterLocalStreamInfo(
                        eduRoom.getCurLocalUserInfo(), validRemovedStreams)

                if (validOnlineUsers.size > 0) {
                    AgoraLog.i("$TAG->onRemoteUsersJoined:${Gson().toJson(validOnlineUsers)}")
                    cmdCallbackManager.onRemoteUsersJoined(validOnlineUsers, eduRoom)
                }
                if (validAddedStreams.size > 0) {
                    AgoraLog.i("$TAG->onRemoteStreamsAdded:${Gson().toJson(validAddedStreams)}")
                    cmdCallbackManager.onRemoteStreamsAdded(validAddedStreams, eduRoom)
                }
                validOnlineLocalUser?.let {
                    AgoraLog.i("$TAG->onLocalUserAdded:${Gson().toJson(it)}")
                    cmdCallbackManager.onLocalUserAdded(it, eduRoom.getCurLocalUser())
                }
                validOfflineLocalUser?.let {
                    AgoraLog.i("$TAG->onLocalUserRemoved:${Gson().toJson(it)}")
                    cmdCallbackManager.onLocalUserRemoved(it, eduRoom.getCurLocalUser())
                }
                validOfflineUsers?.let {
                    AgoraLog.i("$TAG->onRemoteUsersLeft:${Gson().toJson(it)}")
                    cmdCallbackManager.onRemoteUsersLeft(it, eduRoom)
                }
                if (validRemovedStreams.size > 0) {
                    AgoraLog.i("$TAG->onRemoteStreamsRemoved:${Gson().toJson(validRemovedStreams)}")
                    cmdCallbackManager.onRemoteStreamsRemoved(validRemovedStreams, eduRoom)
                }
                validAddedLocalStream?.let {
                    AgoraLog.i("$TAG->onLocalStreamAdded:${Gson().toJson(it)}")
                    cmdCallbackManager.onLocalStreamAdded(it, eduRoom.getCurLocalUser())
                }
                validRemovedLocalStream?.let {
                    AgoraLog.i("$TAG->onLocalStreamRemoved:${Gson().toJson(it)}")
                    cmdCallbackManager.onLocalStreamRemoved(it, eduRoom.getCurLocalUser())
                }
            }
            CMDId.UserStateChange.value -> {
                val cmdUserStateMsg = Gson().fromJson<CMDResponseBody<CMDUserStateMsg>>(text, object :
                        TypeToken<CMDResponseBody<CMDUserStateMsg>>() {}.type).data
                val changeEvents = CMDDataMergeProcessor.updateUserWithUserStateChange(cmdUserStateMsg,
                        (eduRoom as EduRoomImpl).getCurUserList(), eduRoom.getCurRoomType())

                /**判断有效的数据中是否有本地用户的数据,有则处理并回调*/
                val iterable = changeEvents.iterator()
                while (iterable.hasNext()) {
                    val element = iterable.next()
                    val event = element.event
                    if (event.modifiedUser.userUuid == eduRoom.getCurLocalUserInfo().userUuid) {
                        AgoraLog.e(TAG, "onLocalUserUpdated:${event.modifiedUser.userUuid}")
                        cmdCallbackManager.onLocalUserUpdated(EduUserEvent(event.modifiedUser,
                                event.operatorUser), element.type, eduRoom.getCurLocalUser())
                        iterable.remove()
                    }
                }
                /**把剩余的远端数据回调出去*/
                changeEvents?.forEach {
                    cmdCallbackManager.onRemoteUserUpdated(it.event, it.type, eduRoom)
                }
            }
            CMDId.UserPropertiedChanged.value -> {
                AgoraLog.e(TAG, "Receive RTM of userProperty change: ${text}")
                val cmdUserPropertyRes = Gson().fromJson<CMDResponseBody<CMDUserPropertyRes>>(text, object :
                        TypeToken<CMDResponseBody<CMDUserPropertyRes>>() {}.type).data
                val updatedUserInfo = CMDDataMergeProcessor.updateUserPropertyWithChange(cmdUserPropertyRes,
                        (eduRoom as EduRoomImpl).getCurUserList())
                updatedUserInfo?.let {
                    if (updatedUserInfo == eduRoom.getCurLocalUserInfo()) {
                        cmdCallbackManager.onLocalUserPropertyUpdated(it, cmdUserPropertyRes.cause,
                                eduRoom.getCurLocalUser())
                    } else {
                        /**远端用户property发生改变如何回调出去*/
                        cmdCallbackManager.onRemoteUserPropertiesUpdated(it, eduRoom,
                                cmdUserPropertyRes.cause)
                    }
                }
            }
            CMDId.StreamStateChange.value -> {
                val cmdStreamActionMsg = Gson().fromJson<CMDResponseBody<CMDStreamActionMsg>>(text, object :
                        TypeToken<CMDResponseBody<CMDStreamActionMsg>>() {}.type).data
                /**根据回调数据，维护本地存储的流列表*/
                when (cmdStreamActionMsg.action) {
                    CMDStreamAction.Add.value -> {
                        AgoraLog.e(TAG, "Receive RTM of newly added stream：${text}")
                        val validAddStreams = CMDDataMergeProcessor.addStreamWithAction(cmdStreamActionMsg,
                                (eduRoom as EduRoomImpl).getCurStreamList(), eduRoom.getCurRoomType())
                        AgoraLog.e(TAG, "Effective newly added stream size is: " + validAddStreams.size)
                        /**判断有效的数据中是否有本地流的数据,有则处理并回调*/
                        val iterable = validAddStreams.iterator()
                        while (iterable.hasNext()) {
                            val element = iterable.next()
                            val streamInfo = element.modifiedStream
                            if (streamInfo.publisher == eduRoom.getCurLocalUserInfo()) {
                                RteEngineImpl.updateLocalStream(streamInfo.hasAudio, streamInfo.hasVideo)
                                RteEngineImpl.setClientRole(eduRoom.getCurRoomUuid(),
                                        Constants.CLIENT_ROLE_BROADCASTER)
                                RteEngineImpl.publish(eduRoom.getCurRoomUuid())
                                AgoraLog.e(TAG, "Callback the newly added localStream to upper layer")
                                cmdCallbackManager.onLocalStreamAdded(element, eduRoom.getCurLocalUser())
                                iterable.remove()
                            }
                        }
                        if (validAddStreams.size > 0) {
                            AgoraLog.e(TAG, "Callback the newly added remoteStream to upper layer")
                            cmdCallbackManager.onRemoteStreamsAdded(validAddStreams, eduRoom)
                        }
                    }
                    CMDStreamAction.Modify.value -> {
                        AgoraLog.e(TAG, "Receive RTM of stream updated: ${text}")
                        val validModifyStreams = CMDDataMergeProcessor.updateStreamWithAction(cmdStreamActionMsg,
                                (eduRoom as EduRoomImpl).getCurStreamList(), eduRoom.getCurRoomType())
                        AgoraLog.e(TAG, "Effective updated stream size is: " + validModifyStreams.size)
                        /**判断有效的数据中是否有本地流的数据,有则处理并回调*/
                        val iterable = validModifyStreams.iterator()
                        while (iterable.hasNext()) {
                            val element = iterable.next()
                            val stream = element.event.modifiedStream
                            if (stream.publisher == eduRoom.getCurLocalUserInfo()) {
                                RteEngineImpl.updateLocalStream(stream.hasAudio, stream.hasVideo)
                                RteEngineImpl.setClientRole(eduRoom.getCurRoomUuid(),
                                        Constants.CLIENT_ROLE_BROADCASTER)
                                RteEngineImpl.publish(eduRoom.getCurRoomUuid())
                                AgoraLog.e(TAG, "Callback the updated localStream to upper layer")
                                cmdCallbackManager.onLocalStreamUpdated(element.event, element.type,
                                        eduRoom.getCurLocalUser())
                                iterable.remove()
                            }
                        }
                        if (validModifyStreams.size > 0) {
                            AgoraLog.e(TAG, "Callback the updated remoteStream to upper layer")
                            validModifyStreams?.forEach {
                                cmdCallbackManager.onRemoteStreamsUpdated(it.event, it.type, eduRoom)
                            }
                        }
                    }
                    CMDStreamAction.Remove.value -> {
                        AgoraLog.e(TAG, "Receive RTM of stream deleted：${text}")
                        val validRemoveStreams = CMDDataMergeProcessor.removeStreamWithAction(
                                cmdStreamActionMsg, (eduRoom as EduRoomImpl).getCurStreamList(),
                                eduRoom.getCurRoomType())

                        /**判断有效的数据中是否有本地流的数据,有则处理并回调*/
                        val iterable = validRemoveStreams.iterator()
                        while (iterable.hasNext()) {
                            val element = iterable.next()
                            if (element.modifiedStream.publisher == eduRoom.getCurLocalUserInfo()) {
                                RteEngineImpl.updateLocalStream(element.modifiedStream.hasAudio,
                                        element.modifiedStream.hasVideo)
                                RteEngineImpl.unpublish(eduRoom.getCurRoomUuid())
                                AgoraLog.e(TAG, "Callback the deleted localStream to upper layer")
                                cmdCallbackManager.onLocalStreamRemoved(element, eduRoom.getCurLocalUser())
                                iterable.remove()
                            }
                        }
                        if (validRemoveStreams.size > 0) {
                            AgoraLog.e(TAG, "Callback the deleted remoteStream to upper layer")
                            cmdCallbackManager.onRemoteStreamsRemoved(validRemoveStreams, eduRoom)
                        }
                    }
                }
            }
            CMDId.BoardRoomStateChange.value -> {
            }
            CMDId.BoardUserStateChange.value -> {
            }
        }
    }

    fun dispatchPeerMsg(text: String, listener: EduManagerEventListener?) {
        val cmdResponseBody = Gson().fromJson<CMDResponseBody<Any>>(text, object :
                TypeToken<CMDResponseBody<Any>>() {}.type)
        when (cmdResponseBody.cmd) {
            CMDId.PeerMsgReceived.value -> {
                /**点对点的聊天消息*/
                val eduMsg = CMDUtil.buildEduMsg(text, eduRoom) as EduChatMsg
                cmdCallbackManager.onUserChatMessageReceived(eduMsg, listener)
            }
            CMDId.ActionMsgReceived.value -> {
                /**邀请申请动作消息*/
                val actionMsg = Convert.convertEduActionMsg(text)
                cmdCallbackManager.onUserActionMessageReceived(actionMsg, listener)
            }
            CMDId.PeerCustomMsgReceived.value -> {
                /**点对点的自定义消息(可以是用户自定义的信令)*/
                val eduMsg = CMDUtil.buildEduMsg(text, eduRoom)
                cmdCallbackManager.onUserMessageReceived(eduMsg, listener)
            }
//            /**只要发起数据同步请求就会受到此消息*/
//            CMDId.SyncRoomInfo.value -> {
//                Log.e(TAG, "收到同步房间信息的消息:" + text)
//                /**接收到需要同步的房间信息*/
//                val cmdSyncRoomInfoRes = Gson().fromJson<CMDResponseBody<CMDSyncRoomInfoRes>>(text,
//                        object : TypeToken<CMDResponseBody<CMDSyncRoomInfoRes>>() {}.type)
//                /**数据同步流程中需要根据requestId判断，此次接收到的数据是否对应于当前请求*/
//                if (cmdResponseBody.requestId == (eduRoom as EduRoomImpl).roomSyncHelper.getCurRequestId()) {
//                    val event = CMDDataMergeProcessor.syncRoomInfoToEduRoom(cmdSyncRoomInfoRes.data, eduRoom)
//                    synchronized(eduRoom.joinSuccess) {
//                        /**在join成功之后同步数据过程中，如果教室数据发生改变就回调出去*/
//                        if (eduRoom.joinSuccess && event != null) {
//                            cmdCallbackManager.onRoomStatusChanged(event, null, eduRoom)
//                        }
//                    }
//                    /**roomInfo同步完成，打开开关*/
//                    roomStateChangeEnable = true
//                    /**roomInfo同步成功*/
//                    Log.e(TAG, "房间信息同步完成")
//                    eduRoom.syncRoomOrAllUserStreamSuccess(
//                            true, null, null)
//                }
//            }
//            /**同步人流数据的消息*/
//            CMDId.SyncUsrStreamList.value -> {
//                /**接收到需要同步的人流信息*/
//                val cmdSyncUserStreamRes = Gson().fromJson<CMDResponseBody<CMDSyncUserStreamRes>>(text,
//                        object : TypeToken<CMDResponseBody<CMDSyncUserStreamRes>>() {}.type)
//                /**数据同步流程中需要根据requestId判断，此次接收到的数据是否对应于当前请求*/
//                if (cmdResponseBody.requestId == (eduRoom as EduRoomImpl).roomSyncHelper.getCurRequestId()) {
//                    val syncUserStreamData: CMDSyncUserStreamRes = cmdSyncUserStreamRes.data
//                    /**第一阶段（属于join流程）（根据nextId全量），如果中间断连，可根据nextId续传;
//                     * 第二阶段（不属于join流程）（根据ts增量），如果中间断连，可根据ts续传*/
//                    when (syncUserStreamData.step) {
//                        EduSyncStep.FIRST.value -> {
//                            Log.e(TAG, "收到同步人流的消息-第一阶段:" + text)
//                            /**把此部分的全量人流数据同步到本地缓存中*/
//                            CMDDataMergeProcessor.syncUserStreamListToEduRoomWithFirst(syncUserStreamData, eduRoom)
//                            val firstFinished = syncUserStreamData.isFinished == EduSyncFinished.YES.value
//                            /**接收到一部分全量数据，就调用一次，目的是为了刷新rtm超时任务*/
//                            eduRoom.syncRoomOrAllUserStreamSuccess(null,
//                                    firstFinished, null)
//                            /**更新全局的nextId,方便在后续出现异常的时候可以以当前节点为起始步骤继续同步*/
//                            eduRoom.roomSyncHelper.updateNextId(syncUserStreamData.nextId)
//                            /**如果步骤一同步完成，则说明join流程中的同步全量人流数据阶段完成，同时还需要把全局的step改为2，
//                             * 防止在步骤二(join流程中的同步增量人流数据阶段)过程出现异常后，再次发起的同步请求中step还是1*/
//                            if (firstFinished) {
//                                Log.e(TAG, "收到同步人流的消息-第一阶段完成")
//                                eduRoom.roomSyncHelper.updateStep(EduSyncStep.SECOND.value)
//                            }
//                        }
//                        EduSyncStep.SECOND.value -> {
//                            Log.e(TAG, "收到同步人流的消息-第二阶段:" + text)
//                            /**增量数据合并到本地缓存中去*/
//                            val validDatas = CMDDataMergeProcessor.syncUserStreamListToEduRoomWithSecond(
//                                    syncUserStreamData, eduRoom)
//                            val incrementFinished = syncUserStreamData.isFinished == EduSyncFinished.YES.value
//                            synchronized(eduRoom.joinSuccess) {
//                                /**接收到一部分增量数据，就调用一次，目的是为了刷新rtm超时任务*/
//                                if (eduRoom.joinSuccess) {
//                                    Log.e(TAG, "收到同步人流的消息-join成功后的增量")
//                                    eduRoom.roomSyncHelper.interruptRtmTimeout(!incrementFinished)
//                                } else {
//                                    if (incrementFinished) {
//                                        Log.e(TAG, "收到同步人流的消息-第二阶段完成")
//                                        (eduRoom as EduRoomImpl).syncRoomOrAllUserStreamSuccess(
//                                                null, null, incrementFinished)
//                                    }
//                                }
//                                /**更新全局的nextTs,方便在后续出现异常的时候可以以当前节点为起始步骤继续同步*/
//                                eduRoom.roomSyncHelper.updateNextTs(syncUserStreamData.nextTs)
//                                /**获取有效的增量数据*/
//                                if (eduRoom.joinSuccess) {
//                                    eduRoom.dataCache.addValidDataBySyncing(validDatas)
//                                }
//                                if (incrementFinished) {
//                                    /**成功加入房间后的全部增量数据需要回调出去*/
//                                    if (eduRoom.joinSuccess) {
//                                        Log.e(TAG, "收到同步人流的消息-join成功后的增量-完成")
//                                        cmdCallbackManager.callbackValidData(eduRoom)
//                                    }
//                                    /**userStream同步完成，打开开关*/
//                                    userStreamChangeEnable = true
//                                }
//                            }
//                        }
//                    }
//                }
//            }
        }
    }

}