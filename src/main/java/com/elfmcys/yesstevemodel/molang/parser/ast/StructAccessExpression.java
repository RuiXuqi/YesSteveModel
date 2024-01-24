package com.elfmcys.yesstevemodel.molang.parser.ast;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;

import javax.annotation.Nonnull;

public class StructAccessExpression implements Expression {
    private final Expression left;
    private final int path;

    public StructAccessExpression(Expression left, String path) {
        this.left = left;
        this.path = StringPool.computeIfAbsent(path);
    }

    @Override
    public <R> R visit(@Nonnull ExpressionVisitor<R> visitor) {
        return visitor.visitStruct(this);
    }

    public Expression left() {
        return left;
    }

    public int path() {
        return path;
    }
}
