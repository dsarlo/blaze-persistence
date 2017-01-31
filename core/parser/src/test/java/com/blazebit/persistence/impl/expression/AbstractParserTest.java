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

package com.blazebit.persistence.impl.expression;

import com.blazebit.persistence.impl.SimpleQueryGenerator;
import com.blazebit.persistence.impl.predicate.BooleanLiteral;
import com.blazebit.persistence.impl.predicate.CompoundPredicate;
import com.blazebit.persistence.impl.predicate.Predicate;
import com.blazebit.persistence.parser.JPQLSelectExpressionParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.*;
import java.util.logging.LogManager;

/**
 *
 * @author Moritz Becker
 */
public class AbstractParserTest {

    private final SetDelegate<String> setDelegate = new SetDelegate<String>() {

        @Override
        protected Set<String> getDelegate() {
            return AbstractParserTest.this.aggregateFunctions;
        }
        
    };
    protected ExpressionFactory ef = new AbstractTestExpressionFactory(setDelegate, false) {

        private final AbstractExpressionFactory.RuleInvoker simpleExpressionRuleInvoker = new AbstractExpressionFactory.RuleInvoker() {

            @Override
            public ParserRuleContext invokeRule(JPQLSelectExpressionParser parser) {
                return parser.parseSimpleExpression();
            }
        };

        @Override
        protected AbstractExpressionFactory.RuleInvoker getSimpleExpressionRuleInvoker() {
            return simpleExpressionRuleInvoker;
        }

    };
    protected ExpressionFactory optimizingEf = new AbstractTestExpressionFactory(setDelegate, true) {

        private final AbstractExpressionFactory.RuleInvoker simpleExpressionRuleInvoker = new AbstractExpressionFactory.RuleInvoker() {

            @Override
            public ParserRuleContext invokeRule(JPQLSelectExpressionParser parser) {
                return parser.parseSimpleExpression();
            }
        };

        @Override
        protected AbstractExpressionFactory.RuleInvoker getSimpleExpressionRuleInvoker() {
            return simpleExpressionRuleInvoker;
        }

    };
    protected ExpressionFactory subqueryEf = new AbstractTestExpressionFactory(setDelegate, false) {

        private final AbstractExpressionFactory.RuleInvoker simpleExpressionRuleInvoker = new AbstractExpressionFactory.RuleInvoker() {

            @Override
            public ParserRuleContext invokeRule(JPQLSelectExpressionParser parser) {
                return parser.parseSimpleSubqueryExpression();
            }
        };

        @Override
        protected AbstractExpressionFactory.RuleInvoker getSimpleExpressionRuleInvoker() {
            return simpleExpressionRuleInvoker;
        }

    };
    
    protected Set<String> aggregateFunctions;
    protected MacroConfiguration macroConfiguration;

    @BeforeClass
    public static void initLogging() {
        try {
            LogManager.getLogManager().readConfiguration(GeneralParserTest.class.getResourceAsStream(
                    "/logging.properties"));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    @Before
    public void initTest() {
        aggregateFunctions = new HashSet<String>();
        macroConfiguration = null;
    }

    protected Predicate not(Predicate p) {
        p.negate();
        return p;
    }

    protected Predicate wrapNot(Predicate p) {
        CompoundPredicate wrapper = new CompoundPredicate(CompoundPredicate.BooleanOperator.AND, p);
        wrapper.negate();
        return wrapper;
    }

    protected Expression parseOrderBy(String expr) {
        return ef.createOrderByExpression(expr, macroConfiguration);
    }
    
    protected Expression parseArithmeticExpr(String expr) {
        return ef.createArithmeticExpression(expr, macroConfiguration);
    }
    
    protected Expression parse(String expr) {
        return ef.createSimpleExpression(expr, false, macroConfiguration);
    }

    protected Expression parseOptimized(String expr) {
        return optimizingEf.createSimpleExpression(expr, false, macroConfiguration);
    }

    protected Expression parseJoin(String expr) {
        return ef.createJoinPathExpression(expr, macroConfiguration);
    }

    protected Predicate parsePredicate(String expr, boolean allowQuantifiedPredicates) {
        return ef.createBooleanExpression(expr, allowQuantifiedPredicates, macroConfiguration);
    }

    protected Predicate parsePredicateOptimized(String expr, boolean allowQuantifiedPredicates) {
        return optimizingEf.createBooleanExpression(expr, allowQuantifiedPredicates, macroConfiguration);
    }

    protected Expression parseSubqueryExpression(String expr) {
        return subqueryEf.createSimpleExpression(expr, false, macroConfiguration);
    }
    
    protected PathExpression parsePath(String expr){
        return ef.createPathExpression(expr, macroConfiguration);
    }

    protected MapKeyExpression keyExpression(String expression) {
        return keyExpression(parsePath(expression));
    }

    protected MapKeyExpression keyExpression(PathExpression expression) {
        return new MapKeyExpression(expression);
    }

    protected MapValueExpression valueExpression(PathExpression expression) {
        return new MapValueExpression(expression);
    }

    protected MapEntryExpression entryExpression(PathExpression expression) {
        return new MapEntryExpression(expression);
    }

    protected FunctionExpression function(String name, Expression... args) {
        if (aggregateFunctions.contains(name)) {
            return new AggregateExpression(false, name, Arrays.asList(args));
        } else {
            return new FunctionExpression(name, Arrays.asList(args));
        }
    }

    protected TypeFunctionExpression typeFunction(Expression arg) {
        return new TypeFunctionExpression(arg);
    }

    protected AggregateExpression aggregate(String name, PathExpression arg, boolean distinct) {
        return new AggregateExpression(distinct, name, Arrays.asList((Expression) arg));
    }

    protected AggregateExpression aggregate(String name, PathExpression arg) {
        return new AggregateExpression(false, name, Arrays.asList((Expression) arg));
    }

    protected PathExpression path(String... properties) {
        PathExpression p = new PathExpression(new ArrayList<PathElementExpression>());
        for (String pathElem : properties) {
            if (pathElem.contains("[")) {
                p.getExpressions().add(array(pathElem));
            } else {
                p.getExpressions().add(new PropertyExpression(pathElem));
            }
        }
        return p;
    }

    protected PathExpression path(Object... pathElements) {
        PathExpression p = new PathExpression(new ArrayList<PathElementExpression>());
        for (Object pathElem : pathElements) {
            if (pathElem instanceof String) {
                String property = (String) pathElem;
                if (property.contains("[")) {
                    p.getExpressions().add(array(property));
                } else {
                    p.getExpressions().add(new PropertyExpression(property));
                }
            } else {
                p.getExpressions().add((PathElementExpression) pathElem);
            }
        }
        return p;
    }

    protected ArrayExpression array(String expr) {
        int firstIndex = expr.indexOf('[');
        int lastIndex = expr.indexOf(']');
        String base = expr.substring(0, firstIndex);
        String index = expr.substring(firstIndex + 1, lastIndex);
        Expression indexExpr;

        /**
         * TODO: change this to not use parse here - we actually do not want to rely on parsing for constructing the
         * comparison expressions
         */

        indexExpr = parse(index);
        return new ArrayExpression(new PropertyExpression(base), indexExpr);
    }

    protected TreatExpression treat(PathExpression path, String type) {
        return new TreatExpression(path, type);
    }

    protected ParameterExpression parameter(String name) {
        return new ParameterExpression(name);
    }

    protected NumericLiteral _bigint(String value) {
        return new NumericLiteral(value, NumericType.BIG_INTEGER);
    }

    protected NumericLiteral _int(String value) {
        return new NumericLiteral(value, NumericType.INTEGER);
    }

    protected NumericLiteral _long(String value) {
        return new NumericLiteral(value, NumericType.LONG);
    }

    protected NumericLiteral _float(String value) {
        return new NumericLiteral(value, NumericType.FLOAT);
    }

    protected NumericLiteral _double(String value) {
        return new NumericLiteral(value, NumericType.DOUBLE);
    }

    protected NumericLiteral _bigdec(String value) {
        return new NumericLiteral(value, NumericType.BIG_DECIMAL);
    }

    protected BooleanLiteral _boolean(boolean value) {
        return new BooleanLiteral(value);
    }

    protected StringLiteral _string(String value) {
        return new StringLiteral(value);
    }

    protected DateLiteral _date(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month - 1, day);
        return new DateLiteral(cal.getTime());
    }

    protected TimeLiteral _time(int hour, int minute, int second, int millisecond) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.HOUR, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, millisecond);
        return new TimeLiteral(cal.getTime());
    }

    protected TimestampLiteral _timestamp(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month - 1, day);
        cal.set(Calendar.HOUR, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, millisecond);
        return new TimestampLiteral(cal.getTime());
    }

    protected EnumLiteral _enum(String priginalExpression) {
        return new EnumLiteral(null, priginalExpression);
    }

    protected EntityLiteral _entity(String priginalExpression) {
        return new EntityLiteral(null, priginalExpression);
    }

    protected ArithmeticExpression add(Expression left, Expression right) {
        return new ArithmeticExpression(left, right, ArithmeticOperator.ADDITION);
    }

    protected ArithmeticExpression subtract(Expression left, Expression right) {
        return new ArithmeticExpression(left, right, ArithmeticOperator.SUBTRACTION);
    }

    protected ArithmeticExpression multiply(Expression left, Expression right) {
        return new ArithmeticExpression(left, right, ArithmeticOperator.MULTIPLICATION);
    }

    protected ArithmeticExpression divide(Expression left, Expression right) {
        return new ArithmeticExpression(left, right, ArithmeticOperator.DIVISION);
    }

    protected String render(Expression expression) {
        SimpleQueryGenerator queryGen = new SimpleQueryGenerator();
        StringBuilder sb = new StringBuilder();
        queryGen.setQueryBuffer(sb);
        expression.accept(queryGen);
        return sb.toString();
    }
}
