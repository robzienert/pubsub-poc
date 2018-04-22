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
package com.robzienert.pubsubpoc.producer

import com.robzienert.pubsubpoc.JobRequest
import com.robzienert.pubsubpoc.producer.strategy.JobProducer
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.*

@ConfigurationProperties
data class WorkGeneratorProperties(
  val minJobLengthMs: Int = Duration.ofSeconds(1).toMillis().toInt(),
  val maxJobLengthMs: Int = Duration.ofMinutes(1).toMillis().toInt()
)

@Component
class WorkGenerator(
  private val producer: JobProducer,
  private val recorder: Recorder,
  private val properties: WorkGeneratorProperties
) {

  private val log = LoggerFactory.getLogger(javaClass)

  private val r = Random()

  @Scheduled(fixedRateString = "\${work.rateMs:1000}")
  fun generateWork() {
    val job = JobRequest(
      createdAt = Instant.now().toEpochMilli(),
      workTimeMs = r.between(properties.minJobLengthMs, properties.maxJobLengthMs),
      notifiedAt = null
    )
    producer.send(job)
    recorder.addJob(job)
    log.info("Created $job")
  }

  private fun Random.between(from: Int, to: Int): Int =
    nextInt(to - from) + from
}
