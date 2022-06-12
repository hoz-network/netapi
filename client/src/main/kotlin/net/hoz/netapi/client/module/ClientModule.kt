/*
 * Copyright 2022 hoz-network.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.hoz.netapi.client.module

import com.google.inject.*
import io.grpc.Channel
import io.grpc.ManagedChannelBuilder
import net.hoz.api.data.DataOperation.OriginSource
import net.hoz.api.data.GameType
import net.hoz.api.service.NetGameServiceGrpcKt
import net.hoz.api.service.NetLangServiceGrpcKt
import net.hoz.api.service.NetPlayerServiceGrpcKt
import net.hoz.netapi.api.Controlled
import net.hoz.netapi.api.ControlledService
import net.hoz.netapi.client.config.DataConfig
import net.hoz.netapi.client.config.GrpcConfig
import net.hoz.netapi.client.provider.NetGameProvider
import net.hoz.netapi.client.provider.NetLangProvider
import net.hoz.netapi.client.provider.NetPlayerProvider

class ClientModule(
    private val config: DataConfig,
) : AbstractModule() {
    private lateinit var channel: Channel

    override fun configure() {
        val grpcConfig = getGrpcConfig()

        channel = ManagedChannelBuilder.forAddress(grpcConfig.address, grpcConfig.port).build()
        binder().requestInjection(channel)

        binder().bindStub(NetPlayerServiceGrpcKt.NetPlayerServiceCoroutineStub::class, grpcConfig, channel)
        binder().bindStub(NetLangServiceGrpcKt.NetLangServiceCoroutineStub::class, grpcConfig, channel)

        bind(GameType::class.java).toInstance(config.gameType)
        bind(OriginSource::class.java).toInstance(config.origin)

        bind(NetPlayerProvider::class.java).asEagerSingleton()
        bind(NetLangProvider::class.java).asEagerSingleton()

        if (config.origin == OriginSource.GAME_SERVER) {
            binder().bindStub(NetGameServiceGrpcKt.NetGameServiceCoroutineStub::class, grpcConfig, channel)
            bind(NetGameProvider::class.java).asEagerSingleton()
        }
    }

    @Provides
    @Singleton
    fun buildControlledService(injector: Injector): ControlledService = ControlledService(
        injector.allBindings.keys
            .filter { Controlled::class.java.isAssignableFrom(it.typeLiteral.rawType) }
            .map { injector.getInstance(it) as Controlled }
            .toList()
    )

    fun getGrpcConfig(): GrpcConfig {
        val address = System.getenv()["GRPC_ADDRESS"]
        val port = System.getenv()["GRPC_PORT"]

        if (address == null || port == null) {
            return GrpcConfig()
        }

        return GrpcConfig(address, port.toInt())
    }
}
