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

import com.blazebit.persistence.spi.FunctionRenderContext;

/**
 *
 * @author Moritz Becker
 * @since 1.2.0
 */
public class MySQLCountTupleFunction extends AbstractCountFunction {

    private static final String DISTINCT = "distinct ";

    @Override
    public void render(FunctionRenderContext context) {
        Count count = getCount(context);

        context.addChunk("count(");

        if (count.isDistinct()) {
            context.addChunk(DISTINCT);
        }

        int argumentStartIndex = count.getArgumentStartIndex();

        if (count.getCountArgumentSize() > 1) {
            if (count.isDistinct()) {
                context.addArgument(argumentStartIndex);
                for (int i = argumentStartIndex + 1; i < context.getArgumentsSize(); i++) {
                    context.addChunk(", ");
                    context.addArgument(i);
                }
            } else {
                context.addChunk("case when ");
                context.addArgument(argumentStartIndex);
                context.addChunk(" is null");
                for (int i = argumentStartIndex + 1; i < context.getArgumentsSize(); i++) {
                    context.addChunk(" or ");
                    context.addArgument(i);
                    context.addChunk(" is null");
                }
                context.addChunk(" then null else 1 end");
            }
        } else {
            context.addArgument(argumentStartIndex);
        }

        context.addChunk(")");
    }
}
