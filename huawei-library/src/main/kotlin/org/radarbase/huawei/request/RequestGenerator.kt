package org.radarbase.huawei.request

import okhttp3.Response
import org.radarbase.huawei.converter.TopicData
import org.radarbase.huawei.route.Route
import org.radarbase.huawei.user.User

interface RequestGenerator {

    fun requests(user: User, max: Int): Sequence<RestRequest>

    fun requests(route: Route, user: User, max: Int): Sequence<RestRequest>

    fun requests(route: Route, max: Int): Sequence<RestRequest>

    fun requestSuccessful(request: RestRequest, response: Response): List<TopicData>

    fun requestFailed(request: RestRequest, response: Response): HuaweiError
}
