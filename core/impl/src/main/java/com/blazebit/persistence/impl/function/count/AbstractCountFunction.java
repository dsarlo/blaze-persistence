/*
 * Copyright 2014 - 2017 Blazebit.
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

package com.blazebit.persistence.impl.function.count;

import com.blazebit.persistence.impl.util.SqlUtils;
import com.blazebit.persistence.spi.FunctionRenderContext;
import com.blazebit.persistence.spi.JpqlFunction;

/**
 *
 * @author Moritz Becker
 * @since 1.2.0
 */
public abstract class AbstractCountFunction implements JpqlFunction {

    public static final String FUNCTION_NAME = "count_tuple";
    public static final String DISTINCT_QUALIFIER = "DISTINCT";

    @Override
    public boolean hasArguments() {
        return true;
    }

    @Override
    public boolean hasParenthesesIfNoArguments() {
        return true;
    }

    @Override
    public Class<?> getReturnType(Class<?> firstArgumentType) {
        return Long.class;
    }

    protected Count getCount(FunctionRenderContext context) {
        if (context.getArgumentsSize() == 0) {
            throw new RuntimeException("The " + FUNCTION_NAME + " function needs at least one argument!");
        }

        boolean distinct = false;
        int startIndex = 0;
        String maybeDistinct = context.getArgument(0);

        if (("'" + DISTINCT_QUALIFIER + "'").equalsIgnoreCase(maybeDistinct)) {
            distinct = true;
            startIndex++;
        }

        // Hibernate puts the columns for embeddables into a single string, so we have to count items
        int argsSize = 0;
        for (int i = startIndex; i < context.getArgumentsSize(); i++) {
            argsSize += SqlUtils.countSelectItems(context.getArgument(i));
        }

        if (startIndex > argsSize) {
            throw new RuntimeException("The " + AbstractCountFunction.FUNCTION_NAME + " function needs at least one expression to count! args=" + context);
        }

        return new Count(distinct, startIndex, argsSize);
    }

    protected static final class Count {

        private final boolean distinct;
        private final int argumentStartIndex;
        private final int countArgumentSize;

        public Count(boolean distinct, int argumentStartIndex, int countArgumentSize) {
            this.distinct = distinct;
            this.argumentStartIndex = argumentStartIndex;
            this.countArgumentSize = countArgumentSize;
        }

        public boolean isDistinct() {
            return distinct;
        }

        public int getArgumentStartIndex() {
            return argumentStartIndex;
        }

        public int getCountArgumentSize() {
            return countArgumentSize;
        }
    }
}
