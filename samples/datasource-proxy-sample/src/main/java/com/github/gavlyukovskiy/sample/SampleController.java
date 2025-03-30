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

package com.github.gavlyukovskiy.sample;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class SampleController {

    private final DataSource dataSource;

    public SampleController(DataSource dataSource) {
        this.dataSource = dataSource;
        prepareFunctions(dataSource);
    }

    private static void prepareFunctions(DataSource dataSource) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                        CREATE ALIAS sleep AS '
                        void sleep(int millis) throws Exception {
                          Thread.sleep(millis);
                        }
                        ';
                        """)
        ) {
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @RequestMapping("/commit")
    public List<Map<String, String>> select() {
        List<Map<String, String>> results = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM INFORMATION_SCHEMA.COLUMNS")) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            while (resultSet.next()) {
                Map<String, String> result = new HashMap<>();
                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    String columnName = metaData.getColumnName(i + 1);
                    result.put(columnName, resultSet.getString(columnName));
                }
                results.add(result);
            }
            connection.commit();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return results;
    }

    @RequestMapping("/rollback")
    public List<Map<String, String>> rollback() {
        List<Map<String, String>> results = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM INFORMATION_SCHEMA.COLUMNS")) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            while (resultSet.next()) {
                Map<String, String> result = new HashMap<>();
                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    String columnName = metaData.getColumnName(i + 1);
                    result.put(columnName, resultSet.getString(columnName));
                }
                results.add(result);
            }
            connection.rollback();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return results;
    }

    @RequestMapping("/query-error")
    public void error() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT UNDEFINED()");
        }
        catch (Exception ignored) {
        }
    }

    @RequestMapping("/sleep")
    public void sleep(@RequestParam(defaultValue = "3000") int millis) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT SLEEP(?)")) {
            statement.setInt(1, millis);
            statement.execute();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
