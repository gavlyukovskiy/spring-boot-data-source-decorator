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

import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.common.PreparedStatementInformation;
import com.p6spy.engine.common.ResultSetInformation;
import com.p6spy.engine.common.StatementInformation;
import com.p6spy.engine.event.SimpleJdbcEventListener;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.util.ExceptionUtils;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener to represent each connection and sql query as a span.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2
 */
public class TracingJdbcEventListener extends SimpleJdbcEventListener {

    private final Tracer tracer;
    private final P6SpySpanNameResolver p6SpySpanNameResolver;

    // using Tracer#currentSpan is not safe due to possibility to commit/rollback connection without closing Statement/ResultSet
    // in this case events and tags will be logged in wrong Span
    private final Map<ConnectionInformation, Span> connectionSpans = new ConcurrentHashMap<>();
    private final Map<StatementInformation, Span> statementSpans = new ConcurrentHashMap<>();

    TracingJdbcEventListener(Tracer tracer, P6SpySpanNameResolver p6SpySpanNameResolver) {
        this.tracer = tracer;
        this.p6SpySpanNameResolver = p6SpySpanNameResolver;
    }

    @Override
    public void onAfterGetConnection(ConnectionInformation connectionInformation, SQLException e) {
        Span connectionSpan = tracer.createSpan(p6SpySpanNameResolver.connectionSpanName(connectionInformation));
        //connectionSpan.logEvent(Span.CLIENT_SEND);
        connectionSpan.tag(Span.SPAN_LOCAL_COMPONENT_TAG_NAME, "database");
        if (e != null) {
            connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, ExceptionUtils.getExceptionMessage(e));
            tracer.close(connectionSpan);
        }
        else {
            connectionSpans.put(connectionInformation, connectionSpan);
        }
    }

    @Override
    public void onBeforeAnyExecute(StatementInformation statementInformation) {
        Span statementSpan = tracer.createSpan(p6SpySpanNameResolver.querySpanName(statementInformation));
        //statementSpan.logEvent(Span.CLIENT_SEND);
        statementSpan.tag(Span.SPAN_LOCAL_COMPONENT_TAG_NAME, "database");
        statementSpans.put(statementInformation, statementSpan);
    }

    @Override
    public void onAfterAnyExecute(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        Span statementSpan = statementSpans.remove(statementInformation);
        //statementSpan.logEvent(Span.CLIENT_RECV);
        statementSpan.tag(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, getSql(statementInformation));
        if (e != null) {
            statementSpan.tag(Span.SPAN_ERROR_TAG_NAME, ExceptionUtils.getExceptionMessage(e));
        }
        tracer.close(statementSpan);
    }

    @Override
    public void onAfterExecuteQuery(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        if (e != null) {
            super.onAfterExecuteQuery(statementInformation, timeElapsedNanos, e);
        }
        else {
            // if exception wasn't raised span will be closed after closing ResultSet
            onAfterExecuteQueryWithoutClosingSpan(statementInformation);
        }
    }

    @Override
    public void onAfterExecuteQuery(StatementInformation statementInformation, long timeElapsedNanos, String sql, SQLException e) {
        if (e != null) {
            super.onAfterExecuteQuery(statementInformation, timeElapsedNanos, sql, e);
        }
        else {
            // if exception wasn't raised span will be closed after closing ResultSet
            onAfterExecuteQueryWithoutClosingSpan(statementInformation);
        }
    }

    @Override
    public void onAfterExecuteUpdate(PreparedStatementInformation statementInformation, long timeElapsedNanos, int rowCount, SQLException e) {
        Span statementSpan = statementSpans.get(statementInformation);
        statementSpan.tag(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, String.valueOf(rowCount));
        super.onAfterExecuteUpdate(statementInformation, timeElapsedNanos, rowCount, e);
    }

    @Override
    public void onAfterExecuteUpdate(StatementInformation statementInformation, long timeElapsedNanos, String sql, int rowCount, SQLException e) {
        Span statementSpan = statementSpans.get(statementInformation);
        statementSpan.tag(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, String.valueOf(rowCount));
        super.onAfterExecuteUpdate(statementInformation, timeElapsedNanos, sql, rowCount, e);
    }

    private void onAfterExecuteQueryWithoutClosingSpan(StatementInformation statementInformation) {
        // close span after result set is closed to include fetch time
        Span statementSpan = statementSpans.get(statementInformation);
        statementSpan.logEvent("execute");
        statementSpan.tag(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, getSql(statementInformation));
    }

    private String getSql(StatementInformation statementInformation) {
        return StringUtils.hasText(statementInformation.getSqlWithValues())
                ? statementInformation.getSqlWithValues()
                : statementInformation.getSql();
    }

    @Override
    public void onAfterResultSetClose(ResultSetInformation resultSetInformation, SQLException e) {
        Span statementSpan = statementSpans.remove(resultSetInformation.getStatementInformation());
        //statementSpan.logEvent(Span.CLIENT_RECV);
        statementSpan.tag(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, String.valueOf(resultSetInformation.getCurrRow()));
        if (e != null) {
            statementSpan.tag(Span.SPAN_ERROR_TAG_NAME, ExceptionUtils.getExceptionMessage(e));
        }
        tracer.close(statementSpan);
    }

    @Override
    public void onAfterCommit(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
        Span connectionSpan = connectionSpans.get(connectionInformation);
        if (e != null) {
            connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, ExceptionUtils.getExceptionMessage(e));
        }
        connectionSpan.logEvent("commit");
    }

    @Override
    public void onAfterRollback(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
        Span connectionSpan = connectionSpans.get(connectionInformation);
        if (e != null) {
            connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, ExceptionUtils.getExceptionMessage(e));
        }
        else {
            connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, "Transaction rolled back");
        }
        connectionSpan.logEvent("rollback");
    }

    @Override
    public void onAfterConnectionClose(ConnectionInformation connectionInformation, SQLException e) {
        Span connectionSpan = connectionSpans.remove(connectionInformation);
        //connectionSpan.logEvent(Span.CLIENT_RECV);
        if (e != null) {
            connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, ExceptionUtils.getExceptionMessage(e));
        }
        tracer.close(connectionSpan);
    }
}
