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
package com.robzienert.pubsubpoc.worker.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.robzienert.pubsubpoc.JobRequest
import com.robzienert.pubsubpoc.RedisConfiguration.Companion.NOTIFY_SET_KEY
import com.robzienert.pubsubpoc.worker.CompletedJobListener
import org.springframework.stereotype.Component
import redis.clients.jedis.JedisPool
import redis.clients.jedis.params.sortedset.ZAddParams
import java.time.Instant

@Component
class RedisCompletedJobListener(
  private val jedisPool: JedisPool,
  private val mapper: ObjectMapper
) : CompletedJobListener {

  override fun invoke(p1: JobRequest) {
    p1.copy(notifiedAt = Instant.now().toEpochMilli()).let { request ->
      mapper.writeValueAsString(request).let { job ->
        jedisPool.resource.use { jedis ->
          jedis.zadd(
            NOTIFY_SET_KEY,
            mapOf(job to request.notifiedAt!!.toDouble()),
            ZAddParams.zAddParams().nx()
          )
        }
      }
    }
  }
}
