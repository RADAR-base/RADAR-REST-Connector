package org.radarbase.oura.request

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

import okhttp3.Response
import org.radarbase.oura.user.User

interface RequestGenerator {

    fun requests(user: User, max: Int): Sequence<RestRequest>

    fun requestSuccessful(request: RestRequest, response: Response)

    fun requestFailed(request: RestRequest, response: Response)
}