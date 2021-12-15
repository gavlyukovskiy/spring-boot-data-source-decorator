package com.github.gavlyukovskiy.boot.jdbc.decorator.testapplication;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DecoratedDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
public class DatabaseTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldAutoConfigureDataSourceDecorationByDefault() {
        assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
    }
}
