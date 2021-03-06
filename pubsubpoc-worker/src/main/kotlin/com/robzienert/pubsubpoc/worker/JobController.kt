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
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
class ReceivingController(
  private val workQueue: WorkQueue
) {

  private val log = LoggerFactory.getLogger(javaClass)

  @RequestMapping(value = ["/work"], method = [(RequestMethod.POST)])
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun receive(@RequestBody req: JobRequest) {
    workQueue.addWork(req)
  }
}
