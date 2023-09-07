package org.radarbase.oura.request

import okhttp3.Response
import org.radarbase.oura.converter.TopicData
import org.radarbase.oura.route.Route
import org.radarbase.oura.user.User

interface RequestGenerator {

    fun requests(user: User, max: Int): Sequence<RestRequest>

    fun requests(route: Route, user: User, max: Int): Sequence<RestRequest>

    fun requests(route: Route, max: Int): Sequence<RestRequest>

    fun requestSuccessful(request: RestRequest, response: Response): List<TopicData>

    fun requestFailed(request: RestRequest, response: Response)
}
