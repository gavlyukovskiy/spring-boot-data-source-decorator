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

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceNameResolver;
import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.common.PreparedStatementInformation;
import com.p6spy.engine.common.ResultSetInformation;
import com.p6spy.engine.common.StatementInformation;
import com.p6spy.engine.event.SimpleJdbcEventListener;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
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
    private final DataSourceNameResolver dataSourceNameResolver;

    // using Tracer#currentSpan is not safe due to possibility to commit/rollback connection without closing Statement/ResultSet
    // in this case events and tags will be logged in wrong Span
    private final Map<ConnectionInformation, Span> connectionSpans = new ConcurrentHashMap<>();

    TracingJdbcEventListener(Tracer tracer, DataSourceNameResolver dataSourceNameResolver) {
        this.tracer = tracer;
        this.dataSourceNameResolver = dataSourceNameResolver;
    }

    @Override
    public void onBeforeGetConnection(ConnectionInformation connectionInformation) {
        String dataSourceName = dataSourceNameResolver.resolveDataSourceName(connectionInformation.getDataSource());
        Span connectionSpan = tracer.createSpan("jdbc:/" + dataSourceName + SleuthListenerAutoConfiguration.SPAN_CONNECTION_POSTFIX);
        connectionSpan.tag(Span.SPAN_LOCAL_COMPONENT_TAG_NAME, "database");
        connectionSpans.put(connectionInformation, connectionSpan);
    }

    @Override
    public void onAfterGetConnection(ConnectionInformation connectionInformation, SQLException e) {
        Span connectionSpan = connectionSpans.get(connectionInformation);
        if (e != null) {
            connectionSpans.remove(connectionInformation);
            connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, getExceptionMessage(e));
            tracer.close(connectionSpan);
        }
    }

    @Override
    public void onBeforeAnyExecute(StatementInformation statementInformation) {
        String dataSourceName = dataSourceNameResolver.resolveDataSourceName(statementInformation.getConnectionInformation().getDataSource());
        Span statementSpan = tracer.createSpan("jdbc:/" + dataSourceName + SleuthListenerAutoConfiguration.SPAN_QUERY_POSTFIX);
        statementSpan.tag(Span.SPAN_LOCAL_COMPONENT_TAG_NAME, "database");
    }

    @Override
    public void onAfterAnyExecute(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        Span statementSpan = tracer.getCurrentSpan();
        statementSpan.tag(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, getSql(statementInformation));
        if (e != null) {
            statementSpan.tag(Span.SPAN_ERROR_TAG_NAME, getExceptionMessage(e));
        }
        tracer.close(statementSpan);
    }

    @Override
    public void onAfterExecuteQuery(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        super.onAfterExecuteQuery(statementInformation, timeElapsedNanos, e);
        if (e == null) {
            createResultSetSpan(statementInformation);
        }
    }

    @Override
    public void onAfterExecuteQuery(StatementInformation statementInformation, long timeElapsedNanos, String sql, SQLException e) {
        super.onAfterExecuteQuery(statementInformation, timeElapsedNanos, sql, e);
        if (e == null) {
            createResultSetSpan(statementInformation);
        }
    }

    private void createResultSetSpan(StatementInformation statementInformation) {
        String dataSourceName = dataSourceNameResolver.resolveDataSourceName(statementInformation.getConnectionInformation().getDataSource());
        Span resultSetSpan = tracer.createSpan("jdbc:/" + dataSourceName + SleuthListenerAutoConfiguration.SPAN_FETCH_POSTFIX);
        resultSetSpan.tag(Span.SPAN_LOCAL_COMPONENT_TAG_NAME, "database");
    }

    @Override
    public void onAfterExecuteUpdate(PreparedStatementInformation statementInformation, long timeElapsedNanos, int rowCount, SQLException e) {
        tracer.addTag(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, String.valueOf(rowCount));
        super.onAfterExecuteUpdate(statementInformation, timeElapsedNanos, rowCount, e);
    }

    @Override
    public void onAfterExecuteUpdate(StatementInformation statementInformation, long timeElapsedNanos, String sql, int rowCount, SQLException e) {
        tracer.addTag(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, String.valueOf(rowCount));
        super.onAfterExecuteUpdate(statementInformation, timeElapsedNanos, sql, rowCount, e);
    }

    @Override
    public void onAfterResultSetClose(ResultSetInformation resultSetInformation, SQLException e) {
        Span currentSpan = tracer.getCurrentSpan();
        // current span may be not ResultSet span if Connection was closed before ResultSet
        if (currentSpan == null || !currentSpan.getName().contains(SleuthListenerAutoConfiguration.SPAN_FETCH_POSTFIX)) {
            return;
        }
        int rowCount = resultSetInformation.getCurrRow() + 1;
        currentSpan.tag(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, String.valueOf(rowCount));
        if (e != null) {
            currentSpan.tag(Span.SPAN_ERROR_TAG_NAME, getExceptionMessage(e));
        }
        tracer.close(currentSpan);
    }

    @Override
    public void onAfterStatementClose(StatementInformation statementInformation, SQLException e) {
        // if Connection was closed before Statement current span may be null (see #18)
        // if ResultSet is not closed Statement span may be open at this moment, double checking it here
        Span currentSpan = tracer.getCurrentSpan();
        if (currentSpan == null || !currentSpan.getName().contains(SleuthListenerAutoConfiguration.SPAN_FETCH_POSTFIX)) {
            return;
        }
        if (e != null) {
            currentSpan.tag(Span.SPAN_ERROR_TAG_NAME, getExceptionMessage(e));
        }
        tracer.close(currentSpan);
    }

    @Override
    public void onAfterCommit(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
        Span connectionSpan = connectionSpans.get(connectionInformation);
        if (connectionSpan == null) {
            // connection is already closed
            return;
        }
        if (e != null) {
            connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, getExceptionMessage(e));
        }
        connectionSpan.logEvent("commit");
    }

    @Override
    public void onAfterRollback(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
        Span connectionSpan = connectionSpans.get(connectionInformation);
        if (connectionSpan == null) {
            // connection is already closed
            return;
        }
        if (e != null) {
            connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, getExceptionMessage(e));
        }
        else {
            connectionSpan.tag(Span.SPAN_ERROR_TAG_NAME, "Transaction rolled back");
        }
        connectionSpan.logEvent("rollback");
    }

    @Override
    public void onAfterConnectionClose(ConnectionInformation connectionInformation, SQLException e) {
        Span connectionSpan = connectionSpans.remove(connectionInformation);
        if (connectionSpan == null) {
            // connection is already closed
            return;
        }
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

    private String getSql(StatementInformation statementInformation) {
        return StringUtils.hasText(statementInformation.getSqlWithValues())
                ? statementInformation.getSqlWithValues()
                : statementInformation.getSql();
    }

    private String getExceptionMessage(Throwable e) {
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }
}
