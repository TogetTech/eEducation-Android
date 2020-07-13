package io.agora.education.room.bean

import io.agora.education.room.enums.EduCourseState

class EduClassroomStatus constructor() {
    lateinit var courseState: EduCourseState
    var startTime: Long = System.currentTimeMillis()
    var isStudentChatAllowed: Boolean = false
    var onlineUsersCount: Int = 0

    constructor(courseState: EduCourseState, startTime: Long, isStudentChatAllowed: Boolean,
                onlineUsersCount: Int): this() {
        this.courseState = courseState
        this.startTime = startTime
        this.isStudentChatAllowed = isStudentChatAllowed
        this.onlineUsersCount = onlineUsersCount
    }
}