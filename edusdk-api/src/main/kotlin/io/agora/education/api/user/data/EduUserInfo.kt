package io.agora.education.api.user.data

enum class EduUserRole(var value: Int) {
    TEACHER(1),
    STUDENT(2)
}

open class EduUserInfo(
        val userUuid: String,
        val userName: String,
        val role: EduUserRole,
        var isChatAllowed: Boolean?
): Cloneable {
    var userProperties: Map<String, String?>? = null

    override fun equals(other: Any?): Boolean {
        if(other == null || other !is EduUserInfo)
        {
            return false
        }
        return (other.userUuid == this.userUuid && other.userName == this.userName && other.role == this.role)
    }

    override fun hashCode(): Int {
        val var10000 = userUuid
        var var1 = (var10000?.hashCode() ?: 0) * 31
        val var10001 = userName
        var1 = (var1 + (var10001?.hashCode() ?: 0)) * 31
        val var2 = role
        var1 = (var1 + (var2?.hashCode() ?: 0)) * 31
        return var1
    }


}
enum class EduChatState(var value: Int) {
    NotAllow(1),
    Allow(0)
}