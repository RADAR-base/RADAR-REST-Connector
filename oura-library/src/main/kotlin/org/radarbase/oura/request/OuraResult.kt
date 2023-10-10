package org.radarbase.oura.request

import okhttp3.Response
import org.radarbase.oura.converter.TopicData
import org.radarbase.oura.route.Route
import org.radarbase.oura.user.User

sealed class OuraResult<out T : Any> { 
    data class Success<out T : Any>(val value: T) : OuraResult<T>() 
    data class Error(val error: OuraError) : OuraResult<Nothing>()  
}

sealed interface OuraError 

sealed class OuraErrorBase(val message: String, val cause: Exception? = null): OuraError { } 

class OuraRateLimitError: OuraErrorBase("Rate limit reached..", TooManyRequestsException()) { }