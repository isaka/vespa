// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.indexinglanguage.expressions;

import com.yahoo.document.DataType;
import com.yahoo.document.datatypes.LongFieldValue;

/**
 * @author Simon Thoresen Hult
 */
public final class ToLongExpression extends Expression {

    @Override
    public DataType setInputType(DataType input, TypeContext context) {
        super.setInputType(input, context);
        return DataType.LONG;
    }

    @Override
    public DataType setOutputType(DataType output, TypeContext context) {
        super.setOutputType(DataType.LONG, output, null, context);
        return getInputType(context);
    }

    @Override
    protected void doExecute(ExecutionContext context) {
        context.setCurrentValue(new LongFieldValue(Long.valueOf(String.valueOf(context.getCurrentValue()))));
    }

    @Override
    public String toString() { return "to_long"; }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ToLongExpression;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
