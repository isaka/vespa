// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.config.application;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;
import java.io.IOException;

public class ValidationProcessor implements PreProcessor {

    @Override
    public Document process(Document input) throws IOException, TransformerException {
        NodeList includeItems = input.getElementsByTagNameNS("http://www.w3.org/2001/XInclude", "*");
        if (includeItems.getLength() > 0)
            throw new UnsupportedOperationException("XInclude not supported, use preprocess:include instead");
        return input;
    }

}
