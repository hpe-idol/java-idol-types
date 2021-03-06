/*
 * (c) Copyright 2016 Micro Focus or one of its affiliates.
 *
 * Licensed under the MIT License (the "License"); you may not use this file
 * except in compliance with the License.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are as may be set forth in the express warranty
 * statements accompanying such products and services. Nothing herein should be
 * construed as constituting an additional warranty. Micro Focus shall not be
 * liable for technical or editorial errors or omissions contained herein. The
 * information contained herein is subject to change without notice.
 */

package com.hp.autonomy.types.idol.marshalling;

import com.autonomy.aci.client.services.Processor;
import com.hp.autonomy.types.idol.responses.QueryResponse;
import com.hp.autonomy.types.idol.marshalling.marshallers.DocumentGenerator;
import com.hp.autonomy.types.idol.marshalling.marshallers.MarshallerFactory;
import com.hp.autonomy.types.idol.marshalling.marshallers.ResponseDataParser;
import com.hp.autonomy.types.idol.marshalling.marshallers.ResponseParser;
import com.hp.autonomy.types.idol.marshalling.processors.ResponseDataProcessor;
import com.hp.autonomy.types.idol.marshalling.processors.VoidProcessor;
import com.hp.autonomy.types.idol.marshalling.processors.WrapperProcessor;
import com.hp.autonomy.types.idol.responses.QueueInfoGetStatusResponseData;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProcessorFactoryImpl implements ProcessorFactory {
    private final MarshallerFactory marshallerFactory;
    private final ResponseParser responseParser;

    private final Map<Class<?>, ResponseDataParser<?>> responseDataMarshallerMap = new ConcurrentHashMap<>();
    private final Map<QueryResponseCacheKey<?, ?>, ResponseDataParser<?>> queryResponseDataMarshallerMap = new ConcurrentHashMap<>();
    private final Map<Map<String, Class<?>>, ResponseDataParser<?>> queueInfoGetStatusMarshallerMap = new ConcurrentHashMap<>();
    private final Map<Class<?>, DocumentGenerator<?>> documentGeneratorMap = new ConcurrentHashMap<>();

    @SuppressWarnings("WeakerAccess")
    public ProcessorFactoryImpl(final MarshallerFactory marshallerFactory) {
        this.marshallerFactory = marshallerFactory;
        responseParser = marshallerFactory.getResponseParser();
    }

    @Override
    public Processor<Void> getVoidProcessor() {
        return new VoidProcessor(responseParser);
    }

    @Override
    public <T> Processor<T> getResponseDataProcessor(final Class<T> type) {
        final ResponseDataParser<T> responseDataMarshaller = getMarshaller(responseDataMarshallerMap, type, () -> marshallerFactory.getResponseDataParser(type));
        return new ResponseDataProcessor<>(responseDataMarshaller);
    }

    @Override
    public <T, U> Processor<U> getResponseDataWrapperProcessor(final Class<T> responseDataType, final Function<T, U> function) {
        final Processor<T> innerProcessor = getResponseDataProcessor(responseDataType);
        return new WrapperProcessor<>(innerProcessor, function);
    }

    @Override
    public <R extends QueryResponse, C> Processor<R> getQueryResponseDataProcessor(final Class<R> responseDataType, final Class<C> contentType) {
        final ResponseDataParser<R> responseDataMarshaller = getMarshaller(responseDataMarshallerMap, responseDataType, () -> marshallerFactory.getResponseDataParser(responseDataType));
        final QueryResponseCacheKey<R, C> cacheKey = new QueryResponseCacheKey<>(responseDataType, contentType);
        final ResponseDataParser<R> queryResponseDataMarshaller = getMarshaller(queryResponseDataMarshallerMap, cacheKey, () -> marshallerFactory.getQueryResponseDataParser(responseDataMarshaller, responseDataType, contentType));
        return new ResponseDataProcessor<>(queryResponseDataMarshaller);
    }

    @Override
    public Processor<QueueInfoGetStatusResponseData> getQueueInfoGetStatusResponseDataProcessor(final Map<String, Class<?>> resultTypes) {
        final ResponseDataParser<QueueInfoGetStatusResponseData> responseDataMarshaller = getMarshaller(responseDataMarshallerMap, QueueInfoGetStatusResponseData.class,
                () -> marshallerFactory.getResponseDataParser(QueueInfoGetStatusResponseData.class));

        final ResponseDataParser<QueueInfoGetStatusResponseData> queueInfoGetStatusResponseDataResponseDataParser = getMarshaller(queueInfoGetStatusMarshallerMap, resultTypes,
                () -> marshallerFactory.getQueueInfoGetStatusResponseDataParser(responseDataMarshaller, resultTypes));

        return new ResponseDataProcessor<>(queueInfoGetStatusResponseDataResponseDataParser);
    }

    @Override
    public <T> DocumentGenerator<T> getDocumentGenerator(final Class<T> type) {
        return getDocumentGenerator(type, () -> marshallerFactory.getDocumentGenerator(type));
    }

    private <K, T> ResponseDataParser<T> getMarshaller(final Map<K, ResponseDataParser<?>> map, final K type, final Supplier<ResponseDataParser<T>> supplier) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        final ResponseDataParser<T> marshaller = (ResponseDataParser<T>) getFromCache((Map) map, type, (Class) ResponseDataParser.class, (Supplier<?>) supplier);
        return marshaller;
    }

    private <T> DocumentGenerator<T> getDocumentGenerator(final Class<T> type, final Supplier<DocumentGenerator<T>> supplier) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        final DocumentGenerator<T> documentGenerator = (DocumentGenerator<T>) getFromCache((Map) documentGeneratorMap, type, (Class) DocumentGenerator.class, (Supplier<?>) supplier);
        return documentGenerator;
    }

    private <K, V> V getFromCache(final Map<K, V> cache, final K key, final Class<V> cacheValueType, final Supplier<? extends V> supplier) {
        return Optional.ofNullable(cache.get(key))
                .map(cacheValueType::cast)
                .orElseGet(() -> {
                    final V newObject = supplier.get();
                    cache.put(key, newObject);
                    return newObject;
                });
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    private static class QueryResponseCacheKey<R extends QueryResponse, C> {
        private final Class<R> responseDataType;
        private final Class<C> contentType;
    }
}
