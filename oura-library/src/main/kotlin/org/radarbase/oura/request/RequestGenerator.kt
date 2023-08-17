package org.radarbase.oura.request

import okhttp3.Response
import org.radarbase.oura.route.Route
import org.radarbase.oura.user.User
import org.apache.avro.specific.SpecificRecord

interface RequestGenerator {

    fun requests(user: User, max: Int): Sequence<RestRequest>

    fun requests(route: Route, user: User, max: Int): Sequence<RestRequest>

    fun requests(route: Route, max: Int): Sequence<RestRequest>

    fun requestSuccessful(request: RestRequest, response: Response): List<Pair<SpecificRecord, SpecificRecord>>

    fun requestFailed(request: RestRequest, response: Response)
}
