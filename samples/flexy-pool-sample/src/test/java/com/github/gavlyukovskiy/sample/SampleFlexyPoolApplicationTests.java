package com.github.gavlyukovskiy.sample;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DecoratedDataSource;
import com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool.FlexyPoolDataSourceDecorator;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class SampleFlexyPoolApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
        assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);

        DecoratedDataSource decoratedDataSource = (DecoratedDataSource) dataSource;

        assertThat(decoratedDataSource.getDecoratingChain().get(0).getDataSourceDecorator()).isInstanceOf(FlexyPoolDataSourceDecorator.class);
    }
}