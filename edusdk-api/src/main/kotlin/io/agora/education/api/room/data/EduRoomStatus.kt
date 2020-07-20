package io.agora.education.api.room.data

// TODO NEED CONFIRM
enum class EduRoomState(var value: Int) {
    /**未开始*/
    INIT(0),
    /**开始*/
    START(1),
    /**结束*/
    END(2)
}

data class EduRoomStatus(
        val courseState: EduRoomState,
        val startTime: Long,
        var isStudentChatAllowed: Boolean,
        var onlineUsersCount: Int
)

enum class RoomStatusEvent {
    STUDENT_CHAT,
    COURSE_STATE,
}
