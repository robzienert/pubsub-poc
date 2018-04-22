/*
 * Copyright 2018 Netflix, Inc.
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
package com.robzienert.pubsubpoc.worker

import com.robzienert.pubsubpoc.JobRequest
import com.robzienert.pubsubpoc.worker.ListenerStrategy.HTTP
import com.robzienert.pubsubpoc.worker.ListenerStrategy.REDIS
import com.robzienert.pubsubpoc.worker.listener.HttpCompletedJobListener
import com.robzienert.pubsubpoc.worker.listener.RedisCompletedJobListener
import org.springframework.stereotype.Component

typealias CompletedJobListener = (JobRequest) -> Unit

@Component
class CompletedJobListenerProvider(
  private val listenerProperties: ListenerProperties,
  private val listeners: List<CompletedJobListener>
) {

  fun provide(): CompletedJobListener =
    when (listenerProperties.strategy) {
      HTTP -> listeners.filterIsInstance<HttpCompletedJobListener>()
      REDIS -> listeners.filterIsInstance<RedisCompletedJobListener>()
    }.first()
}
