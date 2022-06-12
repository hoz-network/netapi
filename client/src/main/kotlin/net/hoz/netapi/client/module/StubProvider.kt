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

import com.google.inject.Binder
import com.google.inject.Provider
import io.grpc.Channel
import io.grpc.stub.AbstractStub
import net.hoz.netapi.client.config.GrpcConfig
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

fun <T : AbstractStub<T>> Binder.bindStub(clazz: KClass<T>, grpcConfig: GrpcConfig, channel: Channel) {
    this.bind(clazz.java).toProvider(StubProvider(clazz.java, grpcConfig, channel))
}

class StubProvider<T : AbstractStub<T>>(
    private val clazz: Class<T>,
    private val grpcConfig: GrpcConfig,
    private val channel: Channel
) : Provider<T> {

    override fun get(): T {
        return try {
            val channelConstructor = clazz.getDeclaredConstructor(Channel::class.java)
            channelConstructor.isAccessible = true

            channelConstructor.newInstance(channel)
                .withDeadlineAfter(
                    grpcConfig.channelDeadlineMs,
                    TimeUnit.MILLISECONDS
                )
        } catch (e: InstantiationException) {
            throw RuntimeException(
                "Grpc stub class doesn't have a constructor which only takes  'Channel' as parameter",
                e
            )
        }
    }
}