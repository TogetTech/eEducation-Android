package io.agora.education.impl

import io.agora.education.api.room.data.RoomType
import io.agora.education.api.stream.data.EduAudioState
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.EduVideoState
import io.agora.education.api.stream.data.VideoSourceType
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.data.EduUserRole
import io.agora.education.impl.role.data.EduUserRoleStr
import io.agora.education.impl.room.data.response.*
import io.agora.education.impl.stream.EduStreamInfoImpl
import io.agora.education.impl.user.data.EduUserInfoImpl

class Convert {
    companion object {

        /**根据EduUserRole枚举返回角色字符串*/
        fun convertUserRole(role: EduUserRole, roomType: RoomType): String {
            return if (role == EduUserRole.TEACHER) {
                EduUserRoleStr.host.name
            } else {
                when (roomType) {
                    RoomType.ONE_ON_ONE -> {
                        EduUserRoleStr.broadcaster.name
                    }
                    RoomType.SMALL_CLASS -> {
                        EduUserRoleStr.broadcaster.name
                    }
                    RoomType.LARGE_CLASS -> {
                        EduUserRoleStr.audience.name
                    }
                }
            }
        }

        /**根据角色字符串返回EduUserRole枚举值*/
        fun convertUserRole(role: String, roomType: RoomType): EduUserRole {
            when (role) {
                EduUserRoleStr.host.name -> {
                    return EduUserRole.TEACHER
                }
                EduUserRoleStr.broadcaster.name -> {
                    if(roomType == RoomType.ONE_ON_ONE || roomType == RoomType.SMALL_CLASS)
                    {
                        return EduUserRole.STUDENT
                    }
                }
                EduUserRoleStr.audience.name -> {
                    if(roomType == RoomType.LARGE_CLASS)
                    {
                        return EduUserRole.STUDENT
                    }
                }
            }
            return EduUserRole.STUDENT
        }

        /**根据返回的用户和stream列表提取出用户列表*/
        fun getUserInfoList(eduUserListRes: EduUserListRes?, roomType: RoomType): MutableList<EduUserInfo> {
            var list = eduUserListRes?.list
            if (list?.size == 0) {
                return mutableListOf()
            }
            var userInfoList: MutableList<EduUserInfo> = mutableListOf()
            for ((index, element) in list?.withIndex()!!) {
                var eduUser = convertUserInfo(element, roomType)
                userInfoList.add(index, eduUser)
            }
            return userInfoList
        }

        private fun convertUserInfo(eduUserRes: EduUserRes, roomType: RoomType): EduUserInfo {
            var role = convertUserRole(eduUserRes.role, roomType)
            return EduUserInfoImpl(null, eduUserRes.userUuid,
                    eduUserRes.userName, role)
        }

        private fun convertUserInfo(eduUserRes: EduFromUserRes, roomType: RoomType): EduUserInfo {
            var role = convertUserRole(eduUserRes.role, roomType)
            return EduUserInfoImpl(null, eduUserRes.userUuid,
                    eduUserRes.userName, role)
        }

        /**根据返回的用户和stream列表提取出stream列表*/
        fun getStreamInfoList(eduStreamListRes: EduStreamListRes?, roomType: RoomType): MutableList<EduStreamInfo> {
            var userResList = eduStreamListRes?.list
            if (userResList?.size == 0) {
                return mutableListOf()
            }
            var streamInfoList: MutableList<EduStreamInfo> = mutableListOf()
            for ((index, element) in userResList?.withIndex()!!) {
                var eduUserInfo = convertUserInfo(element.fromUser, roomType)
                var videoSourceType = if (element.videoSourceType == 1) VideoSourceType.CAMERA else VideoSourceType.SCREEN
                var hasVideo = element.videoState == EduVideoState.Open.value
                var hasAudio = element.audioState == EduAudioState.Open.value
                var eduStreamInfo = EduStreamInfoImpl(null, element.streamUuid, element.streamName,
                        videoSourceType, hasVideo, hasAudio, eduUserInfo)
                streamInfoList.add(index, eduStreamInfo)
            }
            return streamInfoList
        }
    }
}
