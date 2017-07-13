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
    private final DataSourceLookupUtil dataSourceLookupUtil;

    TracingQueryExecutionListener(Tracer tracer, DataSourceLookupUtil dataSourceLookupUtil) {
        this.tracer = tracer;
        this.dataSourceLookupUtil = dataSourceLookupUtil;
    }

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        String dataSourceName = dataSourceLookupUtil.dataSourceUrl(execInfo);
        Span span = tracer.createSpan(dataSourceName + "/query");
        span.tag("query", queryInfoList.stream().map(QueryInfo::getQuery).collect(Collectors.joining("\n")));
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        Span span = tracer.getCurrentSpan();
        if (execInfo.getThrowable() != null) {
            span.tag(Span.SPAN_ERROR_TAG_NAME, ExceptionUtils.getExceptionMessage(execInfo.getThrowable()));
        }
        tracer.close(span);
    }
}
