package org.radarbase.dexcom.request

import okhttp3.Response
import org.radarbase.dexcom.converter.TopicData
import org.radarbase.dexcom.route.Route
import org.radarbase.dexcom.user.User


interface RequestGenerator {

    fun requests(user: User, max: Int): Sequence<RestRequest>

    fun requests(route: Route, user: User, max: Int): Sequence<RestRequest>

    fun requests(route: Route, max: Int): Sequence<RestRequest>

    fun requestSuccessful(request: RestRequest, response: Response): List<TopicData>

    fun requestFailed(request: RestRequest, response: Response): DexcomError
}



