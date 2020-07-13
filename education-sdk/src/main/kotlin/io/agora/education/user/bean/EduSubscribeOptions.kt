package io.agora.education.user.bean

import io.agora.education.user.enums.EduVideoStreamType

class EduSubscribeOptions constructor() {
    var subscribeAudio: Boolean = false
    var subscribeVideo: Boolean = false
    lateinit var videoStreamType: EduVideoStreamType

    constructor(subscribeAudio: Boolean, subscribeVideo: Boolean, videoStreamType: EduVideoStreamType):
                this() {
        this.subscribeAudio = subscribeAudio
        this.subscribeVideo = subscribeVideo
        this.videoStreamType = videoStreamType
    }
}