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
import com.robzienert.pubsubpoc.producer.writer.Writer
import org.HdrHistogram.Histogram
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock



data class Summary(
  val total: Int,
  val pending: Int,
  val completed: Int,
  val lost: Int,
  val duplicates: Int,
  val histogram: Histogram
)

@Component
class Recorder(
  private val writers: List<Writer>
) {

  private val log = LoggerFactory.getLogger(javaClass)

  private val lock = ReentrantReadWriteLock()
  private val write = lock.writeLock()
  private val read = lock.readLock()

  private val pendingJobs: MutableList<JobRequest> = mutableListOf()
  private val completedJobs: MutableList<JobRequest> = mutableListOf()
  private val recordings: MutableSet<JobRecording> = mutableSetOf()

  private val counter = AtomicInteger()

  private val timeout = Duration.ofMinutes(2)

  @Scheduled(fixedDelayString = "\${record.lostRateMs:5000}")
  fun recordLost() {
    log.info("Searching for lost records")
    val lost = write.withLock {
      val jobs = pendingJobs.filter { Instant.now().isAfter(it.createdInstant().plus(timeout)) }
      if (jobs.isNotEmpty()) {
        pendingJobs.removeAll(jobs)
        completedJobs.addAll(jobs)
      }
      jobs.size
    }
    if (lost != 0) {
      log.warn("Found $lost lost jobs (timed out after $timeout")
    }
  }

  @Scheduled(fixedDelayString = "\${report.rateMs:5000}")
  fun report() {
    val summary: Summary = read.withLock {
      val histogram = Histogram(3600000000000L, 0)
      recordings
        .filter { it.receiveTime != null && it.job.notifiedAt != null }
        .map { it.receiveTime!!.toEpochMilli() - it.job.notifiedInstant()!!.toEpochMilli() }
        .forEach { histogram.recordValue(it) }

      val pending = pendingJobs.size
      val completed = completedJobs.size
      Summary(
        total = pending + completed,
        pending = pending,
        completed = completed,
        lost = recordings.filter { it.receiveTime == null }.size,
        duplicates = recordings.filter { it.duplicate }.size,
        histogram = histogram
      )
    }

    val sequence = counter.incrementAndGet().toString().padStart(4, '0')
    writers.forEach {
      it.write("=SQ $sequence ".padEnd(80, '='))
      it.write("Total      ${summary.total}")
      it.write("Pending    ${summary.pending}")
      it.write("Complete   ${summary.completed}")
      it.write("Lost       ${summary.lost}")
      it.write("Duplicates ${summary.duplicates}")
      it.write("-- Notify Lag (ms) ".padEnd(80, '-'))
      summary.histogram.run {
        val percentile = { percentile: Double ->
          val value = getValueAtPercentile(percentile)
          it.write("$percentile%".padEnd(15) + "$value".padEnd(10) + "${getCountAtValue(value)}")
        }

        it.write("Percentile".padEnd(15) + "Value".padEnd(10) + "Count")
        percentile(50.0)
        percentile(75.0)
        percentile(90.0)
        percentile(95.0)
        percentile(99.0)
        it.write("Mean = %.2f".format(mean).padEnd(20)     + "StdDev      = %.2f".format(stdDeviation))
        it.write("Max  = $maxValue".padEnd(20) + "Total count = $totalCount")
      }
      it.write("=SQ $sequence ".padEnd(80, '='))
      it.write("")
    }
  }

  fun addJob(job: JobRequest) {
    write.withLock {
      pendingJobs.add(job)
    }
  }

  fun completeJob(job: JobRequest, lost: Boolean) {
    write.withLock {
      recordings.add(JobRecording(
        job = job,
        duplicate = completedJobs.contains(job),
        receiveTime = if (lost) null else Instant.now()
      ))
      pendingJobs.add(job)
      completedJobs.add(job)
    }
  }
}
