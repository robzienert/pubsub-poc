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

import com.robzienert.pubsubpoc.HttpConfiguration
import com.robzienert.pubsubpoc.producer.strategy.http.WorkerService
import okhttp3.OkHttpClient
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.support.SpringBootServletInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

object MainDefaults {
  val PROPS = mapOf(
    "netflix.environment" to "test",
    "netflix.account" to "\${netflix.environment}",
    "netflix.stack" to "test",
    "spring.config.location" to "\${user.home}/.spinnaker/",
    "spring.application.name" to "producer",
    "spring.config.name" to "spinnaker,\${spring.application.name}",
    "spring.profiles.active" to "\${netflix.environment},local",
    "server.port" to 8080
  )
}

@Configuration
@EnableScheduling
@EnableAsync
@EnableAutoConfiguration
@ComponentScan(basePackages = ["com.robzienert.pubsubpoc.producer"])
@Import(HttpConfiguration::class)
@EnableConfigurationProperties(WorkGeneratorProperties::class)
open class ProducerMain : SpringBootServletInitializer() {

  override fun configure(builder: SpringApplicationBuilder): SpringApplicationBuilder
    = builder.properties(MainDefaults.PROPS).sources(ProducerMain::class.java)

  @Bean open fun producerService(okHttpClient: OkHttpClient) =
    Retrofit.Builder()
      .baseUrl("http://localhost:8081")
      .client(okHttpClient)
      .addConverterFactory(JacksonConverterFactory.create())
      .build()
      .create(WorkerService::class.java)
}

fun main(args: Array<String>) {
  SpringApplicationBuilder().properties(MainDefaults.PROPS).sources(ProducerMain::class.java).run(*args)
}
