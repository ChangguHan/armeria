/*
 * Copyright 2024 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.armeria.server.thrift;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.Executors;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;

/**
 * A fluent builder to build an instance of {@link ThriftCallService}.
 *
 * <h2>Example</h2>
 *  * <pre>{@code
 *  * ThriftCallService service = ThriftCallService
 *                 .builder()
 *                 .addService(defaultServiceImpl) // Adds an service
 *                 .addService("foo", fooServiceImpl) // Adds an service with a key
 *                 .addService("foobar", fooServiceImpl)  // Adds multiple services to the same key
 *                 .addService("foobar", barServiceImpl)
 *                 .addService("foobarOnce", fooServiceImpl, barServiceImpl) // Adds multiple services at once
 *                  // Adds multiple services by list
 *                 .addService("foobarList", ImmutableList.of(fooServiceImpl, barServiceImpl))
 *                  // Adds multiple services by map
 *                 .addServices(ImmutableMap.of("fooMap", fooServiceImpl, "barMap", barServiceImpl))
 *                  // Adds multiple services by multimap
 *                 .addServices(ImmutableMultimap.of("foobarMultiMap", fooServiceImpl,
 *                                                   "foobarMultiMap", barServiceImpl))
 *                 .build();
 *  * }</pre>
 *
 * @see ThriftCallService
 */
public final class ThriftCallServiceBuilder {
    private final ImmutableListMultimap.Builder<String, Object> servicesBuilder =
            ImmutableListMultimap.builder();

    private boolean useBlockingTaskExecutor;

    ThriftCallServiceBuilder() {}

    /**
     * Adds multiple services by map for {@link ThriftServiceEntry}.
     */
    public ThriftCallServiceBuilder addServices(Map<String, ?> services) {
        requireNonNull(services, "services");
        if (services.isEmpty()) {
            throw new IllegalArgumentException("empty services");
        }
        services.forEach((k, v) -> {
            if (v instanceof Iterable<?>) {
                servicesBuilder.putAll(k, (Iterable<?>) v);
            } else {
                servicesBuilder.put(k, v);
            }
        });
        return this;
    }

    /**
     * Adds an service for {@link ThriftServiceEntry}.
     */
    public ThriftCallServiceBuilder addService(Object... service) {
        requireNonNull(service, "service");
        return addService("", service);
    }

    /**
     * Adds an service with a key for {@link ThriftServiceEntry}.
     */
    public ThriftCallServiceBuilder addService(String key, Object... service) {
        requireNonNull(service, "service");
        servicesBuilder.putAll(key, service);
        return this;
    }

    /**
     * Adds services with key by iterable for {@link ThriftServiceEntry}.
     */
    public ThriftCallServiceBuilder addService(String key, Iterable<?> service) {
        if (!service.iterator().hasNext()) {
            throw new IllegalArgumentException("service should not be empty");
        }
        servicesBuilder.putAll(key, service);
        return this;
    }

    /**
     * Sets whether the service executes service methods using the blocking executor. By default, service
     * methods are executed directly on the event loop for implementing fully asynchronous services. If your
     * service uses blocking logic, you should either execute such logic in a separate thread using something
     * like {@link Executors#newCachedThreadPool()} or enable this setting.
     */
    public ThriftCallServiceBuilder useBlockingTaskExecutor(boolean useBlockingTaskExecutor) {
        this.useBlockingTaskExecutor = useBlockingTaskExecutor;
        return this;
    }

    /**
     * Builds a new instance of {@link ThriftCallService}.
     */
    public ThriftCallService build() {
        return new ThriftCallService(
                Multimaps.asMap(servicesBuilder.build()).entrySet().stream().collect(
                        toImmutableMap(Map.Entry::getKey, ThriftServiceEntry::new)),
                useBlockingTaskExecutor
        );
    }
}
