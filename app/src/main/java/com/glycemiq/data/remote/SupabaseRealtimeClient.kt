package com.glycemiq.data.remote

import com.glycemiq.BuildConfig
import com.glycemiq.util.DeviceIdProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseRealtimeClient @Inject constructor(
    private val httpClient: HttpClient,
    private val deviceIdProvider: DeviceIdProvider
) {
    private val refCounter = AtomicInteger(1)
    private val apiKey = BuildConfig.SUPABASE_KEY
    private val wsBaseUrl = BuildConfig.SUPABASE_URL
        .replace("https://", "wss://")
        .replace("http://", "ws://") + "/realtime/v1/websocket"

    suspend fun listen(scope: CoroutineScope, onDatabaseChange: suspend () -> Unit) {
        val deviceId = deviceIdProvider.getDeviceId()

        while (scope.isActive) {
            try {
                httpClient.webSocket(
                    request = {
                        url("$wsBaseUrl?apikey=$apiKey&vsn=1.0.0")
                        header("apikey", apiKey)
                    }
                ) {
                    send(Frame.Text(joinMessage("glucose_records", deviceId, nextRef())))
                    send(Frame.Text(joinMessage("medications", deviceId, nextRef())))

                    val heartbeatJob: Job = scope.launch {
                        while (isActive) {
                            delay(HEARTBEAT_MS)
                            send(
                                Frame.Text(
                                    """{"topic":"phoenix","event":"heartbeat","payload":{},"ref":"${nextRef()}"}"""
                                )
                            )
                        }
                    }

                    try {
                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            val payload = frame.readText()
                            if (payload.contains("postgres_changes") ||
                                payload.contains("\"type\":\"INSERT\"") ||
                                payload.contains("\"type\":\"UPDATE\"") ||
                                payload.contains("\"type\":\"DELETE\"")
                            ) {
                                onDatabaseChange()
                            }
                        }
                    } finally {
                        heartbeatJob.cancel()
                    }
                }
            } catch (_: Exception) {
                delay(RECONNECT_MS)
            }
        }
    }

    private fun joinMessage(table: String, deviceId: String, ref: String): String =
        """{"topic":"realtime:public:$table","event":"phx_join","payload":{"config":{"broadcast":{"self":false},"presence":{"key":""},"postgres_changes":[{"event":"*","schema":"public","table":"$table","filter":"device_id=eq.$deviceId"}]},"access_token":"$apiKey"},"ref":"$ref"}"""

    private fun nextRef(): String = refCounter.getAndIncrement().toString()

    companion object {
        private const val HEARTBEAT_MS = 25_000L
        private const val RECONNECT_MS = 4_000L
    }
}
