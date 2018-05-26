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

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
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
            span.tag(Span.SPAN_ERROR_TAG_NAME, getExceptionMessage(execInfo.getThrowable()));
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
        Throwable e = executionContext.getThrown();
        if (target instanceof DataSource) {
            if (methodName.equals("getConnection")) {
                Span connectionSpan = connectionSpans.get(executionContext.getConnectionInfo());
                if (e != null) {
                    connectionSpans.remove(executionContext.getConnectionInfo());
                    connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, getExceptionMessage(e));
                    tracer.close(connectionSpan);
                }
            }
        }
        else if (target instanceof Connection) {
            Span connectionSpan = connectionSpans.get(executionContext.getConnectionInfo());
            if (connectionSpan == null) {
                // connection is already closed
                return;
            }
            if (methodName.equals("commit")) {
                if (e != null) {
                    connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, getExceptionMessage(e));
                }
                connectionSpan.logEvent("commit");
            }
            if (methodName.equals("rollback")) {
                if (e != null) {
                    connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, getExceptionMessage(e));
                }
                else {
                    connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, "Transaction rolled back");
                }
                connectionSpan.logEvent("rollback");
            }
            if (methodName.equals("close")) {
                connectionSpans.remove(executionContext.getConnectionInfo());
                if (e != null) {
                    connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, getExceptionMessage(e));
                }
                Span currentSpan = tracer.getCurrentSpan();
                // result set and statement were not closed but connection was, closing result set span as well
                if (currentSpan.getSpanId() == connectionSpan.getSpanId()) {
                    tracer.close(connectionSpan);
                }
                else {
                    if (currentSpan.getName().contains(SleuthListenerAutoConfiguration.SPAN_FETCH_POSTFIX)) {
                        tracer.close(currentSpan);
                        tracer.close(connectionSpan);
                    }
                    else {
                        tracer.continueSpan(connectionSpan);
                        tracer.close(connectionSpan);
                        tracer.continueSpan(currentSpan);
                    }
                }
            }
        }
        else if (target instanceof Statement) {
            if (methodName.equals("executeQuery") && e == null) {
                String dataSourceName = executionContext.getProxyConfig().getDataSourceName();
                Span resultSetSpan = tracer.createSpan("jdbc:/" + dataSourceName + SleuthListenerAutoConfiguration.SPAN_FETCH_POSTFIX);
                resultSetSpan.tag(Span.SPAN_LOCAL_COMPONENT_TAG_NAME, "database");
            }
            else if (methodName.equals("close")) {
                // if ResultSet is not closed statement span may be open at this moment, double checking it here
                Span currentSpan = tracer.getCurrentSpan();
                if (currentSpan.getName().contains(SleuthListenerAutoConfiguration.SPAN_FETCH_POSTFIX)) {
                    if (e != null) {
                        currentSpan.tag(Span.SPAN_ERROR_TAG_NAME, getExceptionMessage(e));
                    }
                    tracer.close(currentSpan);
                }
            }
        }
        else if (target instanceof ResultSet) {
            if (methodName.equals("close")) {
                Span currentSpan = tracer.getCurrentSpan();
                // current span may be not ResultSet span if Connection was closed before ResultSet
                if (currentSpan == null || !currentSpan.getName().contains(SleuthListenerAutoConfiguration.SPAN_FETCH_POSTFIX)) {
                    return;
                }
                if (e != null) {
                    currentSpan.tag(Span.SPAN_ERROR_TAG_NAME, getExceptionMessage(e));
                }
                tracer.close(currentSpan);
            }
        }
    }

    private String getExceptionMessage(Throwable e) {
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }
}
