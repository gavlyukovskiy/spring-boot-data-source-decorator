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
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.springframework.core.Ordered;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Listener to represent each connection and sql query as a span.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2
 */
public class TracingQueryExecutionListener implements QueryExecutionListener, MethodExecutionListener, Ordered {

    private final TracingListenerStrategy<String, Statement, ResultSet> strategy;

    TracingQueryExecutionListener(Tracer tracer) {
        this.strategy = new TracingListenerStrategy<>(tracer);
    }

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        strategy.beforeQuery(execInfo.getConnectionId(), execInfo.getStatement(), execInfo.getDataSourceName());
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        if (execInfo.getMethod().getName().equals("executeUpdate") && execInfo.getThrowable() == null) {
            strategy.addQueryRowCount(execInfo.getStatement(), (int) execInfo.getResult());
        }
        String sql = queryInfoList.stream().map(QueryInfo::getQuery).collect(Collectors.joining("\n"));
        strategy.afterQuery(execInfo.getStatement(), sql, execInfo.getThrowable());
    }

    @Override
    public void beforeMethod(MethodExecutionContext executionContext) {
        Object target = executionContext.getTarget();
        String methodName = executionContext.getMethod().getName();
        String dataSourceName = executionContext.getProxyConfig().getDataSourceName();
        if (target instanceof DataSource) {
            if (methodName.equals("getConnection")) {
                strategy.beforeGetConnection(executionContext.getConnectionInfo().getConnectionId(), dataSourceName);
            }
        }
        if (target instanceof ResultSet) {
            ResultSet resultSet = (ResultSet) target;
            if (methodName.equals("next")) {
                try {
                    strategy.beforeResultSetNext(resultSet.getStatement(), resultSet, dataSourceName);
                }
                catch (SQLException e) {
                    // ignore
                }
            }
        }
    }

    @Override
    public void afterMethod(MethodExecutionContext executionContext) {
        Object target = executionContext.getTarget();
        String methodName = executionContext.getMethod().getName();
        String connectionId = executionContext.getConnectionInfo().getConnectionId();
        Throwable t = executionContext.getThrown();
        if (target instanceof DataSource) {
            if (methodName.equals("getConnection")) {
                strategy.afterGetConnection(connectionId, t);
            }
        }
        else if (target instanceof Connection) {
            if (methodName.equals("commit")) {
                strategy.afterCommit(connectionId, t);
            }
            if (methodName.equals("rollback")) {
                strategy.afterRollback(connectionId, t);
            }
            if (methodName.equals("close")) {
                strategy.afterConnectionClose(connectionId, t);
            }
        }
        else if (target instanceof Statement) {
            if (methodName.equals("close")) {
                strategy.afterStatementClose((Statement) target);
            }
        }
        else if (target instanceof ResultSet) {
            if (methodName.equals("close")) {
                ResultSet resultSet = (ResultSet) target;
                strategy.afterResultSetClose(resultSet, -1, t);
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
