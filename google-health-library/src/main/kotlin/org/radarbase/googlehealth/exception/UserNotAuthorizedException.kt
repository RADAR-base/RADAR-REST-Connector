package org.radarbase.googlehealth.exception

import org.radarbase.googlehealth.user.User

class UserNotAuthorizedException(message: String) : Exception(message) {
    constructor(user: User) : this("User ${user.id} is not authorized")
}
