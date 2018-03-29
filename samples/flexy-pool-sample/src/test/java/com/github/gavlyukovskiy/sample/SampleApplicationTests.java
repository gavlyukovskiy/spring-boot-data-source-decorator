package com.github.gavlyukovskiy.sample;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DecoratedDataSource;
import com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool.FlexyPoolDataSourceDecorator;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SampleApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    public void contextLoads() {
        assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);

        DecoratedDataSource decoratedDataSource = (DecoratedDataSource) dataSource;

        assertThat(decoratedDataSource.getDecoratingChain().get(0).getDataSourceDecorator()).isInstanceOf(FlexyPoolDataSourceDecorator.class);
    }
}