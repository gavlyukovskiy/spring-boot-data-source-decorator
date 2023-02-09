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

import com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy.DataSourceProxyProperties;
import com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool.FlexyPoolProperties;
import com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy.P6SpyProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Collection;
import java.util.Collections;

/**
 * Properties for configuring proxy providers.
 *
 * @author Arthur Gavlyukovskiy
 */
@ConfigurationProperties(prefix = "decorator.datasource")
public class DataSourceDecoratorProperties {

    /**
     * Enables data source decorating.
     */
    private boolean enabled = true;
    /**
     * Beans that won't be decorated.
     */
    private Collection<String> excludeBeans = Collections.emptyList();

    /**
     * If AbstractRoutingDataSource will be decorated or not
     */
    private boolean ignoreRoutingDataSources = false;

    @NestedConfigurationProperty
    private DataSourceProxyProperties datasourceProxy = new DataSourceProxyProperties();

    @NestedConfigurationProperty
    private P6SpyProperties p6spy = new P6SpyProperties();

    @NestedConfigurationProperty
    private FlexyPoolProperties flexyPool = new FlexyPoolProperties();

    public boolean isEnabled() {
        return this.enabled;
    }

    public Collection<String> getExcludeBeans() {
        return this.excludeBeans;
    }

    public boolean isIgnoreRoutingDataSources() {
        return ignoreRoutingDataSources;
    }

    public DataSourceProxyProperties getDatasourceProxy() {
        return this.datasourceProxy;
    }

    public P6SpyProperties getP6spy() {
        return this.p6spy;
    }

    public FlexyPoolProperties getFlexyPool() {
        return this.flexyPool;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setExcludeBeans(Collection<String> excludeBeans) {
        this.excludeBeans = excludeBeans;
    }

    public void setIgnoreRoutingDataSources(boolean ignoreRoutingDataSources) {
        this.ignoreRoutingDataSources = ignoreRoutingDataSources;
    }


    public void setDatasourceProxy(DataSourceProxyProperties datasourceProxy) {
        this.datasourceProxy = datasourceProxy;
    }

    public void setP6spy(P6SpyProperties p6spy) {
        this.p6spy = p6spy;
    }

    public void setFlexyPool(FlexyPoolProperties flexyPool) {
        this.flexyPool = flexyPool;
    }
}
