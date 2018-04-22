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

import com.robzienert.pubsubpoc.JobRequest
import com.robzienert.pubsubpoc.worker.CompletedJobListener
import org.springframework.stereotype.Component
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import java.time.Instant

interface ProducerService {
  @POST("/notify")
  fun notify(@Body req: JobRequest): Call<Void>
}

@Component
class HttpCompletedJobListener(
  private val producerService: ProducerService
) : CompletedJobListener {

  override fun invoke(p1: JobRequest) {
    producerService.notify(p1.copy(notifiedAt = Instant.now().toEpochMilli())).execute()
  }
}
