package com.github.gavlyukovskiy.cloud.sleuth;

import brave.Span;
import brave.Span.Kind;
import brave.Tracer;
import brave.Tracer.SpanInScope;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

class TracingListenerStrategy<CON, STMT, RS> {
    private final Map<CON, Collection<STMT>> nestedStatementsByConnection = new ConcurrentHashMap<>();
    private final Map<STMT, Collection<RS>> nestedResultSetsByStatement = new ConcurrentHashMap<>();
    
    private final Map<CON, Span> connectionSpans = new ConcurrentHashMap<>();
    private final Map<STMT, Map.Entry<Span, SpanInScope>> statementSpans = new ConcurrentHashMap<>();
    private final Map<RS, Map.Entry<Span, SpanInScope>> resultSetSpans = new ConcurrentHashMap<>();

    private final Tracer tracer;

    TracingListenerStrategy(Tracer tracer) {
        this.tracer = tracer;
    }

    void beforeGetConnection(CON connectionKey, String dataSourceName) {
        Span connectionSpan = tracer.nextSpan().name("jdbc:/" + dataSourceName + SleuthListenerAutoConfiguration.SPAN_CONNECTION_POSTFIX);
        connectionSpan.kind(Kind.CLIENT);
        connectionSpan.start();
        connectionSpans.put(connectionKey, connectionSpan);
    }

    void afterGetConnection(CON connectionKey, Throwable t) {
        Span connectionSpan = connectionSpans.get(connectionKey);
        if (t != null) {
            connectionSpans.remove(connectionKey);
            connectionSpan.kind(Kind.CLIENT);
            connectionSpan.finish();
        }
    }

    void beforeQuery(CON connectionKey, STMT statementKey, String dataSourceName) {
        Span statementSpan = tracer.nextSpan().name("jdbc:/" + dataSourceName + SleuthListenerAutoConfiguration.SPAN_QUERY_POSTFIX);
        statementSpan.kind(Kind.CLIENT);
        statementSpan.start();
        SpanInScope spanInScope = tracer.withSpanInScope(statementSpan);
        statementSpans.put(statementKey, new SimpleEntry<>(statementSpan, spanInScope));
        nestedStatementsByConnection.computeIfAbsent(connectionKey, key -> new HashSet<>()).add(statementKey);
    }

    void addQueryRowCount(STMT statementKey, int rowCount) {
        Map.Entry<Span, SpanInScope> statementEntry = statementSpans.get(statementKey);
        statementEntry.getKey().tag(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, String.valueOf(rowCount));
    }

    void afterQuery(STMT statementKey, String sql, Throwable t) {
        Map.Entry<Span, SpanInScope> statementEntry = statementSpans.remove(statementKey);
        Span statementSpan = statementEntry.getKey();
        statementSpan.tag(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, sql);
        if (t != null) {
            statementSpan.error(t);
        }
        statementEntry.getValue().close();
        statementSpan.finish();
    }

    void beforeResultSetNext(STMT statementKey, RS resultSetKey, String dataSourceName) {
        if (resultSetSpans.containsKey(resultSetKey)) {
            // ResultSet span is already created
            return;
        }
        Span resultSetSpan = tracer.nextSpan().name("jdbc:/" + dataSourceName + SleuthListenerAutoConfiguration.SPAN_FETCH_POSTFIX);
        resultSetSpan.kind(Kind.CLIENT);
        resultSetSpan.start();
        SpanInScope spanInScope = tracer.withSpanInScope(resultSetSpan);
        nestedResultSetsByStatement.computeIfAbsent(statementKey, key -> new HashSet<>()).add(resultSetKey);
        resultSetSpans.put(resultSetKey, new SimpleEntry<>(resultSetSpan, spanInScope));
    }

    void afterResultSetClose(RS resultSetKey, int rowCount, Throwable t) {
        Map.Entry<Span, SpanInScope>  resultSetEntry = resultSetSpans.remove(resultSetKey);
        // ResultSet span may be null if Connection or Statement were closed before ResultSet
        if (resultSetEntry == null) {
            return;
        }
        
        Span resultSetSpan = resultSetEntry.getKey();
        if (rowCount != -1) {
            resultSetSpan.tag(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, String.valueOf(rowCount));
        }
        if (t != null) {
            resultSetSpan.error(t);
        }
        resultSetEntry.getValue().close();
        resultSetSpan.finish();
    }

    void afterStatementClose(STMT statementKey) {
        Collection<RS> nestedResultSets = nestedResultSetsByStatement.remove(statementKey);
        if (nestedResultSets != null) {
            nestedResultSets.stream()
                    .map(resultSetSpans::remove)
                    .filter(Objects::nonNull)
                    .forEach(entry -> {
                        entry.getValue().close();
                        entry.getKey().finish();
                    });
        }
    }

    void afterCommit(CON connectionKey, Throwable t) {
        Span connectionSpan = connectionSpans.get(connectionKey);
        if (connectionSpan == null) {
            // Connection is already closed
            return;
        }
        if (t != null) {
            connectionSpan.error(t);
        }
        connectionSpan.annotate("commit");
    }

    void afterRollback(CON connectionKey, Throwable t) {
        Span connectionSpan = connectionSpans.get(connectionKey);
        if (connectionSpan == null) {
            // Connection is already closed
            return;
        }
        if (t != null) {
            connectionSpan.error(t);
        }
        else {
            connectionSpan.tag("error", "Transaction rolled back");
        }
        connectionSpan.annotate("rollback");
    }

    void afterConnectionClose(CON connectionKey, Throwable t) {
        Span connectionSpan = connectionSpans.remove(connectionKey);
        if (connectionSpan == null) {
            // connection is already closed
            return;
        }
        if (t != null) {
            connectionSpan.error(t);
        }
        Collection<STMT> nestedStatements = nestedStatementsByConnection.remove(connectionKey);
        if (nestedStatements != null) {
            nestedStatements.stream()
                    .map(nestedResultSetsByStatement::remove)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(resultSetSpans::remove)
                    .filter(Objects::nonNull)
                    .forEach(entry -> {
                        entry.getValue().close();
                        entry.getKey().finish();
                    });
        }
        connectionSpan.finish();
    }
}
