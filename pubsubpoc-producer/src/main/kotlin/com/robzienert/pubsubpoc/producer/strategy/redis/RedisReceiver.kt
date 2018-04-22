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
package com.robzienert.pubsubpoc.producer.strategy.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.robzienert.pubsubpoc.JobRequest
import com.robzienert.pubsubpoc.RedisConfiguration.Companion.NOTIFY_SET_KEY
import com.robzienert.pubsubpoc.producer.Recorder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import redis.clients.jedis.JedisPool
import javax.annotation.PostConstruct

@Component
class RedisReceiver(
  private val jedisPool: JedisPool,
  private val mapper: ObjectMapper,
  private val recorder: Recorder
) {

  @PostConstruct
  fun cleanup() {
    jedisPool.resource.use { jedis ->
      jedis.del(NOTIFY_SET_KEY)
    }
  }

  @Scheduled(fixedRate = 10)
  fun receive() {
    jedisPool.resource.use { jedis ->
      jedis.zrangeByScore(NOTIFY_SET_KEY, "-inf", "+inf", 0, 1)
        .firstOrNull()
        ?.also { jedis.zrem(NOTIFY_SET_KEY, it) }
        ?.let { mapper.readValue<JobRequest>(it) }
        ?.let { recorder.completeJob(it, false) }
    }
  }
}
