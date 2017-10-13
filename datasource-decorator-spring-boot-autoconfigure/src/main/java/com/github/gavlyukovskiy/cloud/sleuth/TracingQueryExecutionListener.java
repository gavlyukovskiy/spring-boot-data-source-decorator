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

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.util.ExceptionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Listener to represent each sql query as a span.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2
 */
public class TracingQueryExecutionListener implements QueryExecutionListener {

    private final Tracer tracer;
    private final DataSourceProxySpanNameResolver dataSourceProxySpanNameResolver;

    TracingQueryExecutionListener(Tracer tracer, DataSourceProxySpanNameResolver dataSourceProxySpanNameResolver) {
        this.tracer = tracer;
        this.dataSourceProxySpanNameResolver = dataSourceProxySpanNameResolver;
    }

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        Span span = tracer.createSpan(dataSourceProxySpanNameResolver.querySpanName(execInfo));
        //span.logEvent(Span.CLIENT_SEND);
        span.tag(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, queryInfoList.stream().map(QueryInfo::getQuery).collect(Collectors.joining("\n")));
        span.tag(Span.SPAN_LOCAL_COMPONENT_TAG_NAME, "database");
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        Span span = tracer.getCurrentSpan();
        //span.logEvent(Span.CLIENT_RECV);
        if (execInfo.getThrowable() != null) {
            span.tag(Span.SPAN_ERROR_TAG_NAME, ExceptionUtils.getExceptionMessage(execInfo.getThrowable()));
        }
        if (execInfo.getMethod().getName().equals("executeUpdate")) {
            span.tag(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, String.valueOf(execInfo.getResult()));
        }
        tracer.close(span);
    }
}
