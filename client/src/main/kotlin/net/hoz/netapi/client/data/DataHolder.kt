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

package net.hoz.netapi.client.data

import com.iamceph.resulter.core.DataResultable
import com.iamceph.resulter.core.Resultable
import com.iamceph.resulter.kotlin.dataResultable
import org.spongepowered.configurate.ConfigurationNode

sealed interface DataHolder {
    /**
     * Main configuration node.
     */
    var root: ConfigurationNode

    companion object {
        fun of(input: String): DataResultable<DataHolder> = dataResultable { DataHolderImpl(input) }
    }

    /**
     * Node with key
     * @param key key
     * @return node for the given key
     */
    fun node(key: String): ConfigurationNode

    /**
     * Updates the root node with given data
     * @param input json input
     */
    fun update(input: String): Resultable

    /**
     * Gets all the data to String
     * @return all data in json
     */
    fun json(): DataResultable<String>
}