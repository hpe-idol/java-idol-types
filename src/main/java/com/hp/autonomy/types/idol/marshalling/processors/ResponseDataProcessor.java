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

package com.hp.autonomy.types.idol.marshalling.processors;

import com.autonomy.aci.client.services.Processor;
import com.autonomy.aci.client.transport.AciResponseInputStream;
import com.hp.autonomy.types.idol.marshalling.marshallers.ResponseDataParser;

/**
 * Generic processor for handling Idol responses with known content type.
 * Note that this uses DOM processing behind the scenes so should not be used for very large responses.
 */
@SuppressWarnings({"WeakerAccess", "NonSerializableFieldInSerializableClass"})
public class ResponseDataProcessor<T> implements Processor<T> {
    private static final long serialVersionUID = -1983490659468698548L;

    private final ResponseDataParser<T> responseDataMarshaller;

    public ResponseDataProcessor(final ResponseDataParser<T> responseDataMarshaller) {
        this.responseDataMarshaller = responseDataMarshaller;
    }

    @Override
    public T process(final AciResponseInputStream aciResponse) {
        return responseDataMarshaller.parseResponseData(aciResponse);
    }
}
