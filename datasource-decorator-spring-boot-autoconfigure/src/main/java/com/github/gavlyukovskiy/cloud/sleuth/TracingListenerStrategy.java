package com.github.gavlyukovskiy.cloud.sleuth;

import brave.Span;
import brave.Span.Kind;
import brave.Tracer;
import brave.Tracer.SpanInScope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class TracingListenerStrategy<CON, STMT, RS> {
    private final Map<CON, ConnectionInfo> openConnections = new ConcurrentHashMap<>();

    private final Tracer tracer;

    TracingListenerStrategy(Tracer tracer) {
        this.tracer = tracer;
    }

    void beforeGetConnection(CON connectionKey, String dataSourceName) {
        Span connectionSpan = tracer.nextSpan().name("jdbc:/" + dataSourceName + SleuthListenerAutoConfiguration.SPAN_CONNECTION_POSTFIX);
        connectionSpan.kind(Kind.CLIENT);
        connectionSpan.start();
        ConnectionInfo connectionInfo = new ConnectionInfo(new SpanWithScope(connectionSpan, tracer.withSpanInScope(connectionSpan)));
        openConnections.put(connectionKey, connectionInfo);
    }

    void afterGetConnection(CON connectionKey, Throwable t) {
        if (t != null) {
            ConnectionInfo connectionInfo = openConnections.remove(connectionKey);
            connectionInfo.getSpan().getSpan().error(t);
            connectionInfo.getSpan().finish();
        }
    }

    void beforeQuery(CON connectionKey, STMT statementKey, String dataSourceName) {
        Span statementSpan = tracer.nextSpan().name("jdbc:/" + dataSourceName + SleuthListenerAutoConfiguration.SPAN_QUERY_POSTFIX);
        statementSpan.kind(Kind.CLIENT);
        statementSpan.start();
        StatementInfo statementInfo = new StatementInfo(new SpanWithScope(statementSpan, tracer.withSpanInScope(statementSpan)));
        openConnections.get(connectionKey).getNestedStatements().put(statementKey, statementInfo);
    }

    void addQueryRowCount(CON connectionKey, STMT statementKey, int rowCount) {
        ConnectionInfo connectionInfo = openConnections.get(connectionKey);
        StatementInfo statementInfo = connectionInfo.getNestedStatements().get(statementKey);
        statementInfo.getSpan().getSpan().tag(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, String.valueOf(rowCount));
    }

    void afterQuery(CON connectionKey, STMT statementKey, String sql, Throwable t) {
        ConnectionInfo connectionInfo = openConnections.get(connectionKey);
        StatementInfo statementInfo = connectionInfo.getNestedStatements().get(statementKey);
        SpanWithScope statementSpan = statementInfo.getSpan();
        statementSpan.getSpan().tag(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, sql);
        if (t != null) {
            statementSpan.getSpan().error(t);
        }
        statementSpan.finish();
    }

    void beforeResultSetNext(CON connectionKey, STMT statementKey, RS resultSetKey, String dataSourceName) {
        ConnectionInfo connectionInfo = openConnections.get(connectionKey);
        // ConnectionInfo may be null if Connection was closed before ResultSet
        if (connectionInfo == null) {
            return;
        }
        if (connectionInfo.getNestedResultSetSpans().containsKey(resultSetKey)) {
            // ResultSet span is already created
            return;
        }
        Span resultSetSpan = tracer.nextSpan().name("jdbc:/" + dataSourceName + SleuthListenerAutoConfiguration.SPAN_FETCH_POSTFIX);
        resultSetSpan.kind(Kind.CLIENT);
        resultSetSpan.start();
        SpanWithScope spanWithScope = new SpanWithScope(resultSetSpan, tracer.withSpanInScope(resultSetSpan));
        connectionInfo.getNestedResultSetSpans().put(resultSetKey, spanWithScope);

        StatementInfo statementInfo = connectionInfo.getNestedStatements().get(statementKey);
        // StatementInfo may be null when Statement is proxied and instance returned from ResultSet is different from instance returned in query method
        // in this case if Statement is closed before ResultSet span won't be finished immediately, but when Connection is closed
        if (statementInfo != null) {
            statementInfo.getNestedResultSetSpans().put(resultSetKey, spanWithScope);
        }
    }

    void afterResultSetClose(CON connectionKey, RS resultSetKey, int rowCount, Throwable t) {
        ConnectionInfo connectionInfo = openConnections.get(connectionKey);
        // ConnectionInfo may be null if Connection was closed before ResultSet
        if (connectionInfo == null) {
            return;
        }
        SpanWithScope resultSetSpan = connectionInfo.getNestedResultSetSpans().remove(resultSetKey);
        // ResultSet span may be null if Statement or ResultSet were already closed
        if (resultSetSpan == null) {
            return;
        }

        if (rowCount != -1) {
            resultSetSpan.getSpan().tag(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, String.valueOf(rowCount));
        }
        if (t != null) {
            resultSetSpan.getSpan().error(t);
        }
        resultSetSpan.finish();
    }

    void afterStatementClose(CON connectionKey, STMT statementKey) {
        ConnectionInfo connectionInfo = openConnections.get(connectionKey);
        // ConnectionInfo may be null if Connection was closed before Statement
        if (connectionInfo == null) {
            return;
        }
        StatementInfo statementInfo = connectionInfo.getNestedStatements().remove(statementKey);
        if (statementInfo != null) {
            statementInfo.getNestedResultSetSpans()
                    .forEach((resultSetKey, span) -> {
                        connectionInfo.getNestedResultSetSpans().remove(resultSetKey);
                        span.finish();
                    });
            statementInfo.getNestedResultSetSpans().clear();
        }
    }

    void afterCommit(CON connectionKey, Throwable t) {
        ConnectionInfo connectionInfo = openConnections.get(connectionKey);
        if (connectionInfo == null) {
            // Connection is already closed
            return;
        }
        SpanWithScope connectionSpan = connectionInfo.getSpan();
        if (t != null) {
            connectionSpan.getSpan().error(t);
        }
        connectionSpan.getSpan().annotate("commit");
    }

    void afterRollback(CON connectionKey, Throwable t) {
        ConnectionInfo connectionInfo = openConnections.get(connectionKey);
        if (connectionInfo == null) {
            // Connection is already closed
            return;
        }
        SpanWithScope connectionSpan = connectionInfo.getSpan();
        if (t != null) {
            connectionSpan.getSpan().error(t);
        }
        else {
            connectionSpan.getSpan().tag("error", "Transaction rolled back");
        }
        connectionSpan.getSpan().annotate("rollback");
    }

    void afterConnectionClose(CON connectionKey, Throwable t) {
        ConnectionInfo connectionInfo = openConnections.remove(connectionKey);
        if (connectionInfo == null) {
            // connection is already closed
            return;
        }
        SpanWithScope connectionSpan = connectionInfo.getSpan();
        if (t != null) {
            connectionSpan.getSpan().error(t);
        }
        connectionInfo.getNestedResultSetSpans().values().forEach(SpanWithScope::finish);
        connectionInfo.getNestedStatements().values().forEach(statementInfo -> statementInfo.getSpan().finish());
        connectionSpan.finish();
    }

    private class ConnectionInfo {
        private final SpanWithScope span;
        private final Map<STMT, StatementInfo> nestedStatements = new ConcurrentHashMap<>();
        private final Map<RS, SpanWithScope> nestedResultSetSpans = new ConcurrentHashMap<>();

        private ConnectionInfo(SpanWithScope span) {
            this.span = span;
        }

        public SpanWithScope getSpan() {
            return span;
        }

        public Map<STMT, StatementInfo> getNestedStatements() {
            return nestedStatements;
        }

        public Map<RS, SpanWithScope> getNestedResultSetSpans() {
            return nestedResultSetSpans;
        }
    }

    private class StatementInfo {
        private final SpanWithScope span;
        private final Map<RS, SpanWithScope> nestedResultSetSpans = new ConcurrentHashMap<>();

        private StatementInfo(SpanWithScope span) {
            this.span = span;
        }

        public SpanWithScope getSpan() {
            return span;
        }

        public Map<RS, SpanWithScope> getNestedResultSetSpans() {
            return nestedResultSetSpans;
        }
    }

    private static class SpanWithScope {
        private final Span span;
        private final SpanInScope spanInScope;

        private SpanWithScope(Span span, SpanInScope spanInScope) {
            this.span = span;
            this.spanInScope = spanInScope;
        }

        public Span getSpan() {
            return span;
        }

        void finish() {
            spanInScope.close();
            span.finish();
        }
    }
}
