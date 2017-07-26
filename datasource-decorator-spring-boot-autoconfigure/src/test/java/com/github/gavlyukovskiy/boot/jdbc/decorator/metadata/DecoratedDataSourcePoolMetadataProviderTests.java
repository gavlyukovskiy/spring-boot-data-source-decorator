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

package com.github.gavlyukovskiy.boot.jdbc.decorator.metadata;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorAutoConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DecoratedDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.metadata.CommonsDbcp2DataSourcePoolMetadata;
import org.springframework.boot.autoconfigure.jdbc.metadata.CommonsDbcpDataSourcePoolMetadata;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.autoconfigure.jdbc.metadata.HikariDataSourcePoolMetadata;
import org.springframework.boot.autoconfigure.jdbc.metadata.TomcatDataSourcePoolMetadata;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.sql.DataSource;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DecoratedDataSourcePoolMetadataProvider}.
 *
 * @author Arthur Gavlyukovskiy
 */
public class DecoratedDataSourcePoolMetadataProviderTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Before
	public void init() {
		EnvironmentTestUtils.addEnvironment(context,
			"datasource.initialize:false",
			"datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt());
	}

	@After
	public void after() {
        context.close();
	}

	@Test
	public void testReturnDataSourcePoolMetadataProviderForHikari() {
		EnvironmentTestUtils.addEnvironment(context,
			"spring.datasource.type:" + HikariDataSource.class.getName());
        context.register(DataSourceAutoConfiguration.class,
			DataSourceDecoratorAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

		DataSource dataSource = context.getBean(DataSource.class);
		assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
		DecoratedDataSourcePoolMetadataProvider poolMetadataProvider = context.getBean(DecoratedDataSourcePoolMetadataProvider.class);
		DataSourcePoolMetadata dataSourcePoolMetadata = poolMetadataProvider.getDataSourcePoolMetadata(dataSource);
		assertThat(dataSourcePoolMetadata).isInstanceOf(HikariDataSourcePoolMetadata.class);
	}

	@Test
	public void testReturnDataSourcePoolMetadataProviderForTomcat() {
        context.register(DataSourceAutoConfiguration.class,
			DataSourceDecoratorAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

		DataSource dataSource = context.getBean(DataSource.class);
		assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
		DecoratedDataSourcePoolMetadataProvider poolMetadataProvider = context.getBean(DecoratedDataSourcePoolMetadataProvider.class);
		DataSourcePoolMetadata dataSourcePoolMetadata = poolMetadataProvider.getDataSourcePoolMetadata(dataSource);
		assertThat(dataSourcePoolMetadata).isInstanceOf(TomcatDataSourcePoolMetadata.class);
	}

	@Test
	@Deprecated
	public void testReturnDataSourcePoolMetadataProviderForDbcp() {
		EnvironmentTestUtils.addEnvironment(context,
			"spring.datasource.type:" + org.apache.commons.dbcp.BasicDataSource.class.getName());
        context.register(DataSourceAutoConfiguration.class,
			DataSourceDecoratorAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

		DataSource dataSource = context.getBean(DataSource.class);
		assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
		DecoratedDataSourcePoolMetadataProvider poolMetadataProvider = context.getBean(DecoratedDataSourcePoolMetadataProvider.class);
		DataSourcePoolMetadata dataSourcePoolMetadata = poolMetadataProvider.getDataSourcePoolMetadata(dataSource);
		assertThat(dataSourcePoolMetadata).isInstanceOf(CommonsDbcpDataSourcePoolMetadata.class);
	}

	@Test
	public void testReturnDataSourcePoolMetadataProviderForDbcp2() {
		EnvironmentTestUtils.addEnvironment(context,
			"spring.datasource.type:" + org.apache.commons.dbcp2.BasicDataSource.class.getName());
        context.register(DataSourceAutoConfiguration.class,
			DataSourceDecoratorAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

		DataSource dataSource = context.getBean(DataSource.class);
		assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
		DecoratedDataSourcePoolMetadataProvider poolMetadataProvider = context.getBean(DecoratedDataSourcePoolMetadataProvider.class);
		DataSourcePoolMetadata dataSourcePoolMetadata = poolMetadataProvider.getDataSourcePoolMetadata(dataSource);
		assertThat(dataSourcePoolMetadata).isInstanceOf(CommonsDbcp2DataSourcePoolMetadata.class);
	}

	@Test
	public void testReturnNullForNonProxy() {
		EnvironmentTestUtils.addEnvironment(context,
			"decorator.datasource.exclude-beans:dataSource");
        context.register(DataSourceAutoConfiguration.class,
			DataSourceDecoratorAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

		DataSource dataSource = context.getBean(DataSource.class);
		DecoratedDataSourcePoolMetadataProvider poolMetadataProvider = context.getBean(DecoratedDataSourcePoolMetadataProvider.class);
		DataSourcePoolMetadata dataSourcePoolMetadata = poolMetadataProvider.getDataSourcePoolMetadata(dataSource);
		assertThat(dataSourcePoolMetadata).isNull();
	}
}
