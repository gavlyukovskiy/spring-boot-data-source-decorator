/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.gavlyukovskiy.cloud.sleuth;

import org.springframework.cloud.sleuth.Sampler;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanReporter;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
class SavingSpanReporterConfiguration {

    @Bean
    public CollectingSpanReporter spanReporter() {
        return new CollectingSpanReporter();
    }

    @Bean
    public Sampler sampler() {
        return new AlwaysSampler();
    }

    static class CollectingSpanReporter implements SpanReporter {
        private List<Span> spans = new ArrayList<>();
        @Override
        public void report(Span span) {
            spans.add(0, span);
        }

        List<Span> getSpans() {
            return spans;
        }
    }
}
