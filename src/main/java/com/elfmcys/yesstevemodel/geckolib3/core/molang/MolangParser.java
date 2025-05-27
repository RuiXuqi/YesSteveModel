package com.elfmcys.yesstevemodel.geckolib3.core.molang;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.PrimaryBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.DoubleValue;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.MolangValue;
import com.elfmcys.yesstevemodel.molang.MolangEngine;
import com.elfmcys.yesstevemodel.molang.parser.ParseException;

import java.util.Map;

public class MolangParser {
    private final MolangEngine engine;
    private final PrimaryBinding primaryBinding;

    public MolangParser(Map<String, Object> extraBindings) {
        primaryBinding = new PrimaryBinding(extraBindings);
        engine = MolangEngine.fromCustomBinding(primaryBinding);
    }

    // Native Access
    @SuppressWarnings("unused")
    public IValue parseExpression(String molangExpression, boolean allowComment) {
        try {
            return parseExpressionUnsafe(molangExpression, allowComment);
        } catch (Exception e) {
            YesSteveModel.LOGGER.debug("Failed to parse molang expression \"{}\": {}", molangExpression, e.getMessage());
            return DoubleValue.ZERO;
        }
    }

    public IValue parseExpressionUnsafe(String molangExpression, boolean allowComment) throws ParseException {
        MolangValue value = new MolangValue(engine.parse(allowComment ? filterComment(molangExpression) : molangExpression));
        primaryBinding.resetTransient();
        return value;
    }

    // C 风格注释
    private static String filterComment(String exp) {
        StringBuilder result = new StringBuilder(exp.length());
        boolean blockComment = false;
        boolean lineComment = false;
        boolean string = false;
        for (int i = 0; i < exp.length(); i++) {
            char c = exp.charAt(i);
            if (string) {
                if (c == '\'') {
                    string = false;
                }
                result.append(c);
                continue;
            }
            if (lineComment) {
                if (c == '\r' || c == '\n') {
                    lineComment = false;
                    result.append('\n');
                }
                continue;
            }
            if (blockComment) {
                if (c == '*' && i + 1 < exp.length()) {
                    char next = exp.charAt(i + 1);
                    if (next == '/') {
                        blockComment = false;
                        i++;
                    }
                }
                continue;
            }
            if (c == '\'') {
                string = true;
                result.append('\'');
                continue;
            }
            if (c == '/' && i + 1 < exp.length()) {
                char next = exp.charAt(i + 1);
                if (next == '/') {
                    lineComment = true;
                    i++;
                    continue;
                } else if (next == '*') {
                    blockComment = true;
                    i++;
                    continue;
                }
            }
            result.append(c);
        }

        return result.toString();
    }

    // Native Access
    @SuppressWarnings("unused")
    public IValue getConstant(double value) {
        return new DoubleValue(value);
    }

    public void reset() {
        primaryBinding.resetScoped();
    }
}
