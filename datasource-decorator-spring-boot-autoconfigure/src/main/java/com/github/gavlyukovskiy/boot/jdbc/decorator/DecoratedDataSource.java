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

package com.github.gavlyukovskiy.boot.jdbc.decorator;

import org.springframework.lang.UsesJava7;

import javax.sql.DataSource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * {@link DataSource} that keeps link on both real {@link DataSource} and
 * decorated {@link DataSource} and delegates all calls to the latter.
 *
 * @author Arthur Gavlyukovskiy
 */
public class DecoratedDataSource implements DataSource {

    /**
     * Initially wrapped {@link DataSource}, used in places where proxy
     * {@link DataSource} can not be used.
     */
    private final DataSource realDataSource;
    /**
     * {@link DataSource} with all decorators set, used to delegate all calls.
     */
    private final DataSource decoratedDataSource;
    /**
     * Holds chain of decorators bean names
     */
    private final String decoratingChain;

    DecoratedDataSource(DataSource realDataSource, DataSource decoratedDataSource, String decoratingChain) {
        this.realDataSource = realDataSource;
        this.decoratedDataSource = decoratedDataSource;
        this.decoratingChain = decoratingChain;
    }

    public DataSource getRealDataSource() {
        return realDataSource;
    }

    public DataSource getDecoratedDataSource() {
        return decoratedDataSource;
    }

    public String getDecoratingChain() {
        return decoratingChain;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return decoratedDataSource.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return decoratedDataSource.getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return decoratedDataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        decoratedDataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        decoratedDataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return decoratedDataSource.getLoginTimeout();
    }

    @Override
    @UsesJava7
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return decoratedDataSource.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return decoratedDataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return decoratedDataSource.isWrapperFor(iface);
    }
}
