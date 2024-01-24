/*
 * This file is part of molang, licensed under the MIT license
 *
 * Copyright (c) 2021-2023 Unnamed Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.elfmcys.yesstevemodel.molang.parser.ast;

import javax.annotation.Nonnull;

/**
 * An {@link Expression} visitor. Provides a way to add
 * functionalities to the expression interface and all
 * of its implementations.
 *
 * <p>See the following example on visiting an expression:</p>
 * <pre>{@code
 *      Expression expr = ...;
 *      String str = expr.visit(new ToStringVisitor());
 * }</pre>
 *
 * <p>Please note that users MUST use {@link Expression#visit(ExpressionVisitor)}
 * and NOT ExpressionVisitor's {@link ExpressionVisitor#visit(Expression)}, because
 * it will not work as intended.</p>
 *
 * @param <R> The visit result type
 * @since 3.0.0
 */
public interface ExpressionVisitor<R> {

    /**
     * Evaluate for the given unknown expression.
     *
     * @param expression The expression.
     * @return The result.
     * @since 3.0.0
     */
    R visit(final @Nonnull Expression expression);

    /**
     * Evaluate for double expression.
     *
     * @param expression The expression.
     * @return The result.
     * @since 3.0.0
     */
    default R visitDouble(final @Nonnull DoubleExpression expression) {
        return visit(expression);
    }

    /**
     * Evaluate for string expression.
     *
     * @param expression The expression.
     * @return The result.
     * @since 3.0.0
     */
    default R visitString(final @Nonnull StringExpression expression) {
        return visit(expression);
    }

    /**
     * Evaluate for identifier expression.
     *
     * @param expression The expression.
     * @return The result.
     * @since 3.0.0
     */
    default R visitIdentifier(final @Nonnull IdentifierExpression expression) {
        return visit(expression);
    }

    default R visitVariable(final @Nonnull VariableExpression expression) {
        return visit(expression);
    }

    default R visitAssignableVariable(final @Nonnull AssignableVariableExpression expression) {
        return visit(expression);
    }

    default R visitStruct(final @Nonnull StructAccessExpression expression) {
        return visit(expression);
    }

    /**
     * Evaluate for ternary conditional expression.
     *
     * @param expression The expression.
     * @return The result.
     * @since 3.0.0
     */
    default R visitTernaryConditional(final @Nonnull TernaryConditionalExpression expression) {
        return visit(expression);
    }

    /**
     * Evaluate for unary expression.
     *
     * @param expression The expression.
     * @return The result.
     * @since 3.0.0
     */
    default R visitUnary(final @Nonnull UnaryExpression expression) {
        return visit(expression);
    }

    /**
     * Evaluate for execution scope expression.
     *
     * @param expression The expression.
     * @return The result.
     * @since 3.0.0
     */
    default R visitExecutionScope(final @Nonnull ExecutionScopeExpression expression) {
        return visit(expression);
    }

    /**
     * Evaluate for binary expression.
     *
     * @param expression The expression.
     * @return The result.
     * @since 3.0.0
     */
    default R visitBinary(final @Nonnull BinaryExpression expression) {
        return visit(expression);
    }

    /**
     * Evaluate for call expression.
     *
     * @param expression The expression.
     * @return The result.
     * @since 3.0.0
     */
    default R visitCall(final @Nonnull CallExpression expression) {
        return visit(expression);
    }

    /**
     * Evaluate for statement expression.
     *
     * @param expression The expression.
     * @return The result.
     * @since 3.0.0
     */
    default R visitStatement(final @Nonnull StatementExpression expression) {
        return visit(expression);
    }

}