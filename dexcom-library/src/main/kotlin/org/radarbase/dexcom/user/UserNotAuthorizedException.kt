package org.radarbase.dexcom.user

class UserNotAuthorizedException(message: String) : Exception(message) {
    constructor(user: User) : this("User ${user.id} is not authorized")
}
