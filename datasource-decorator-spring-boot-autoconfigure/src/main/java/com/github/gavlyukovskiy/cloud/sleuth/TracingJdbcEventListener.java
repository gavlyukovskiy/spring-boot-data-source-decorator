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

import brave.Tracer;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceNameResolver;
import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.common.PreparedStatementInformation;
import com.p6spy.engine.common.ResultSetInformation;
import com.p6spy.engine.common.StatementInformation;
import com.p6spy.engine.event.SimpleJdbcEventListener;
import org.springframework.util.StringUtils;

import java.sql.SQLException;

/**
 * Listener to represent each connection and sql query as a span.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2
 */
public class TracingJdbcEventListener extends SimpleJdbcEventListener {

    private final DataSourceNameResolver dataSourceNameResolver;

    private final TracingListenerStrategy<ConnectionInformation, StatementInformation, ResultSetInformation> strategy;

    TracingJdbcEventListener(Tracer tracer, DataSourceNameResolver dataSourceNameResolver) {
        this.dataSourceNameResolver = dataSourceNameResolver;
        this.strategy = new TracingListenerStrategy<>(tracer);
    }

    @Override
    public void onBeforeGetConnection(ConnectionInformation connectionInformation) {
        String dataSourceName = dataSourceNameResolver.resolveDataSourceName(connectionInformation.getDataSource());
        strategy.beforeGetConnection(connectionInformation, dataSourceName);
    }

    @Override
    public void onAfterGetConnection(ConnectionInformation connectionInformation, SQLException e) {
        strategy.afterGetConnection(connectionInformation, e);
    }

    @Override
    public void onBeforeAnyExecute(StatementInformation statementInformation) {
        String dataSourceName = dataSourceNameResolver.resolveDataSourceName(statementInformation.getConnectionInformation().getDataSource());
        strategy.beforeQuery(statementInformation.getConnectionInformation(), statementInformation, dataSourceName);
    }

    @Override
    public void onAfterAnyExecute(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        strategy.afterQuery(getSql(statementInformation), e);
    }

    @Override
    public void onBeforeResultSetNext(ResultSetInformation resultSetInformation) {
        String dataSourceName = dataSourceNameResolver.resolveDataSourceName(resultSetInformation.getConnectionInformation().getDataSource());
        strategy.beforeResultSetNext(resultSetInformation.getStatementInformation(), resultSetInformation, dataSourceName);
    }

    @Override
    public void onAfterExecuteUpdate(PreparedStatementInformation statementInformation, long timeElapsedNanos, int rowCount, SQLException e) {
        strategy.addQueryRowCount(rowCount);
        super.onAfterExecuteUpdate(statementInformation, timeElapsedNanos, rowCount, e);
    }

    @Override
    public void onAfterExecuteUpdate(StatementInformation statementInformation, long timeElapsedNanos, String sql, int rowCount, SQLException e) {
        strategy.addQueryRowCount(rowCount);
        super.onAfterExecuteUpdate(statementInformation, timeElapsedNanos, sql, rowCount, e);
    }

    @Override
    public void onAfterStatementClose(StatementInformation statementInformation, SQLException e) {
        strategy.afterStatementClose(statementInformation);
    }

    @Override
    public void onAfterResultSetClose(ResultSetInformation resultSetInformation, SQLException e) {
        strategy.afterResultSetClose(resultSetInformation, resultSetInformation.getCurrRow() + 1, e);
    }

    @Override
    public void onAfterCommit(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
        strategy.afterCommit(connectionInformation, e);
    }

    @Override
    public void onAfterRollback(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
        strategy.afterRollback(connectionInformation, e);
    }

    @Override
    public void onAfterConnectionClose(ConnectionInformation connectionInformation, SQLException e) {
        strategy.afterConnectionClose(connectionInformation, e);
    }

    private String getSql(StatementInformation statementInformation) {
        return StringUtils.hasText(statementInformation.getSqlWithValues())
                ? statementInformation.getSqlWithValues()
                : statementInformation.getSql();
    }
}
