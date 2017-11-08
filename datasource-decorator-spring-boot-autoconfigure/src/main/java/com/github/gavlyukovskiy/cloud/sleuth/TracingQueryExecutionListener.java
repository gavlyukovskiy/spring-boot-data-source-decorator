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

import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.util.ExceptionUtils;

import javax.sql.DataSource;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Listener to represent each sql query as a span.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2
 */
public class TracingQueryExecutionListener implements QueryExecutionListener, MethodExecutionListener {

    private final Tracer tracer;
    // using Tracer#currentSpan is not safe due to possibility to commit/rollback connection without closing Statement/ResultSet
    // in this case events and tags will be logged in wrong Span
    private final Map<ConnectionInfo, Span> connectionSpans = new ConcurrentHashMap<>();

    TracingQueryExecutionListener(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        Span span = tracer.createSpan("jdbc:/" + execInfo.getDataSourceName() + SleuthListenerAutoConfiguration.SPAN_QUERY_POSTFIX);
        span.tag(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, queryInfoList.stream().map(QueryInfo::getQuery).collect(Collectors.joining("\n")));
        span.tag(Span.SPAN_LOCAL_COMPONENT_TAG_NAME, "database");
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        Span span = tracer.getCurrentSpan();
        if (execInfo.getThrowable() != null) {
            span.tag(Span.SPAN_ERROR_TAG_NAME, ExceptionUtils.getExceptionMessage(execInfo.getThrowable()));
        }
        if (execInfo.getMethod().getName().equals("executeUpdate")) {
            span.tag(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, String.valueOf(execInfo.getResult()));
        }
        tracer.close(span);
    }

    @Override
    public void beforeMethod(MethodExecutionContext executionContext) {
        Object target = executionContext.getTarget();
        String methodName = executionContext.getMethod().getName();
        if (target instanceof DataSource) {
            if (methodName.equals("getConnection")) {
                String dataSourceName = executionContext.getProxyConfig().getDataSourceName();
                Span connectionSpan = tracer.createSpan("jdbc:/" + dataSourceName + SleuthListenerAutoConfiguration.SPAN_CONNECTION_POSTFIX);
                connectionSpan.tag(Span.SPAN_LOCAL_COMPONENT_TAG_NAME, "database");
                connectionSpans.put(executionContext.getConnectionInfo(), connectionSpan);
            }
        }
    }

    @Override
    public void afterMethod(MethodExecutionContext executionContext) {
        Object target = executionContext.getTarget();
        String methodName = executionContext.getMethod().getName();
        if (target instanceof DataSource) {
            if (methodName.equals("getConnection")) {
                Span connectionSpan = connectionSpans.get(executionContext.getConnectionInfo());
                if (executionContext.getThrown() != null) {
                    connectionSpans.remove(executionContext.getConnectionInfo());
                    connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, ExceptionUtils.getExceptionMessage(executionContext.getThrown()));
                    tracer.close(connectionSpan);
                }
            }
        }
        else if (target instanceof Connection) {
            Span connectionSpan = connectionSpans.get(executionContext.getConnectionInfo());
            if (methodName.equals("commit")) {
                if (executionContext.getThrown() != null) {
                    connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, ExceptionUtils.getExceptionMessage(executionContext.getThrown()));
                }
                connectionSpan.logEvent("commit");
            }
            if (methodName.equals("rollback")) {
                if (executionContext.getThrown() != null) {
                    connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, ExceptionUtils.getExceptionMessage(executionContext.getThrown()));
                }
                else {
                    connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, "Transaction rolled back");
                }
                connectionSpan.logEvent("rollback");
            }
            if (methodName.equals("close")) {
                connectionSpans.remove(executionContext.getConnectionInfo());
                if (executionContext.getThrown() != null) {
                    connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, ExceptionUtils.getExceptionMessage(executionContext.getThrown()));
                }
                tracer.close(connectionSpan);
            }
        }
    }
}
