package io.agora.education.impl.cmd

import io.agora.education.impl.room.data.request.RoleConfig
import io.agora.education.impl.room.data.response.EduUserRes
import io.agora.education.impl.user.data.request.RoleMuteConfig

class CMDRoomMuteState(val muteChat: RoleMuteConfig?, val muteVideo: RoleMuteConfig?,
                       val muteAudio: RoleMuteConfig?, val operator: EduUserRes) {
}