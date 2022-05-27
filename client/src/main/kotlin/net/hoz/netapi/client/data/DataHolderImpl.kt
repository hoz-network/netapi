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
import com.iamceph.resulter.kotlin.resultable
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.gson.GsonConfigurationLoader

internal data class DataHolderImpl(override var root: ConfigurationNode) : DataHolder {
    constructor(json: String) : this(GsonConfigurationLoader.builder().buildAndLoadString(json))

    override fun node(key: String): ConfigurationNode = root.node(key.split("\\."))

    override fun update(input: String): Resultable = resultable { root = GsonConfigurationLoader.builder().buildAndLoadString(input) }

    override fun json(): DataResultable<String> = dataResultable { GsonConfigurationLoader.builder().buildAndSaveString(root) }
}