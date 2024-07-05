package org.radarbase.connect.rest.fitbit.user

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.Scenario
import java.net.URL
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig

internal class ServiceUserRepositoryIntegrationTest {
    private val wireMockServer = WireMockServer(WireMockConfiguration.options().port(8089))
    private val serviceUserRepository = ServiceUserRepository()
    private val userId = "32"
    private val expectedToken = "placeholderAccessToken"
    private val scheduledResponse = WireMock.aResponse()
        .withHeader("Content-Length", "1000")
        .withHeader("WWW-Authenticate", "Bearer realm=\"RADAR-base\", error=\"invalid_token\", error_description=\"No registered validator in could authenticate this token: The Token has expired on 2024-07-04T01:20:31Z., The Token has expired on 2024-07-04T01:20:31Z.\"")
        .withBody(
            "{\n" +
                    "  \"refreshToken\": \"placeholderRefreshToken\",\n" +
                    "  \"accessToken\": \"${expectedToken}\",\n" +
                    "  \"expiresIn\": 3600\n" +
                    "}"
        )
        .withStatus(200)

    private val path = "/users/${userId}/token"

    private val mockUser: User = mock()
    private val mockConfig: FitbitRestSourceConnectorConfig = mock()


    @BeforeEach
    fun setup() {
        wireMockServer.start()
        WireMock.configureFor("localhost", 8089)

        whenever(mockConfig.getFitbitUserRepositoryUrl()).doReturn(wireMockServer.baseUrl().toHttpUrlOrNull())
        whenever(mockConfig.fitbitUserRepositoryClientId).doReturn("clientId")
        whenever(mockConfig.fitbitUserRepositoryClientSecret).doReturn("clientSecret")

        whenever(mockUser.isAuthorized).doReturn(true)
        whenever(mockUser.id).doReturn(userId)
    }

    @AfterEach
    fun teardown() {
        wireMockServer.stop()
    }

    @Test
    fun refreshAccessTokenTokenUrlHasAuthHeader() {
        whenever(mockConfig.getFitbitUserRepositoryTokenUrl()).doReturn(URL("http://localhost:8089/token"))
        val scheduledMPResponse = WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
                "{\n" +
                        "    \"access_token\": \"MyToken\",\n" +
                        "    \"token_type\": \"bearer\",\n" +
                        "    \"expires_in\": 899,\n" +
                        "    \"scope\": \"MEASUREMENT.CREATE SUBJECT.READ\",\n" +
                        "    \"iss\": \"ManagementPortal\",\n" +
                        "    \"grant_type\": \"client_credentials\",\n" +
                        "    \"iat\": 1720022075,\n" +
                        "    \"jti\": \"lyOAoyUeagcCD0kUVe8HmY8d2vU\"\n" +
                        "}"
            )
            .withStatus(200)

        serviceUserRepository.initialize(mockConfig)
        WireMock.stubFor(
            WireMock.get(WireMock.urlEqualTo(path))
                .inScenario("Temporary Response Scenario")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(scheduledResponse
                    .withStatus(401))
                .willSetStateTo("Old Response State")
        )
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/token"))
                .willReturn(scheduledMPResponse)
        )
        WireMock.stubFor(
            WireMock.get(WireMock.urlEqualTo(path))
                .inScenario("Temporary Response Scenario")
                .whenScenarioStateIs("Old Response State")
                .willReturn(scheduledResponse.withStatus(200))
        )

        // Act
        val refreshToken = serviceUserRepository.getAccessToken(mockUser)
        val events = wireMockServer.serveEvents.serveEvents

        // Assert
        assertEquals(expectedToken, refreshToken)
        assertEquals(events.size, 3)
        assert(events.first().request.headers.getHeader("Authorization").containsValue("Bearer MyToken"))
    }
}
