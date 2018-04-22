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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.DelayQueue
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import kotlin.concurrent.thread

data class Work(
  val job: JobRequest
) : Delayed {
  private val deliveryTime = Instant.now().toEpochMilli() - job.createdInstant().plusMillis(job.workTimeMs.toLong()).toEpochMilli()

  override fun getDelay(unit: TimeUnit): Long = unit.convert(deliveryTime, TimeUnit.MILLISECONDS)
  override fun compareTo(other: Delayed): Int = getDelay(TimeUnit.MILLISECONDS).compareTo(other.getDelay(TimeUnit.MILLISECONDS))
}

@Component
class WorkQueue(
  listenerProvider: CompletedJobListenerProvider
) {

  private val log = LoggerFactory.getLogger(javaClass)

  private val listener = listenerProvider.provide()
  private val queue = DelayQueue<Work>()

  @PostConstruct
  fun work() {
    thread(start = true, isDaemon = true) {
      while (true) {
        queue.poll()?.run {
          log.info("Completed $job")
          try {
            listener.invoke(job)
          } catch (e: Exception) {
            log.error("Could not invoke work listener", e)
          }
        }
      }
    }
  }

  fun addWork(job: JobRequest) {
    queue.add(Work(job))
    log.info("Added $job")
  }
}
