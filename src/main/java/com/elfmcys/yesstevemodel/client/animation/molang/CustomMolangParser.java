package com.elfmcys.yesstevemodel.client.animation.molang;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.MolangParser;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.MathBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.QueryBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.parser.ParseException;
import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

// Native Access
public class CustomMolangParser {
    private static final ConcurrentLinkedQueue<MolangParser> PARSER_POOL = new ConcurrentLinkedQueue<>();
    private static final Map<String, Object> EXTRA_BINDING = new HashMap<>();

    // Native Access
    public static MolangParser rentInstance() {
        MolangParser parser = PARSER_POOL.poll();
        if (parser == null) {
            return createMolangParser();
        } else {
            return parser;
        }
    }

    // Native Access
    public static void returnInstance(MolangParser parser) {
        parser.reset();
        PARSER_POOL.add(parser);
    }

    public static IValue parseSingleExpressionUnsafe(String expression) throws ParseException {
        MolangParser parser = rentInstance();
        try {
            return parser.parseExpressionUnsafe(expression, false);
        } finally {
            returnInstance(parser);
        }
    }

    private static MolangParser createMolangParser() {
        if (EXTRA_BINDING.isEmpty()) {
            EXTRA_BINDING.put("ysm", YSMBinding.INSTANCE);
            EXTRA_BINDING.put("ctrl", CtrlBinding.INSTANCE);
            EXTRA_BINDING.put("tlm", TLMBinding.INSTANCE);
            EXTRA_BINDING.put("args", UserFunctionArgument.INSTANCE);
        }
        var binding = new HashMap<>(EXTRA_BINDING);
        binding.put("fn", new UserFunctionBinding());     // Scoped 对象不能单例
        return new MolangParser(binding);
    }

    /**
     * 给客户端指令补全用的
     */
    public static Map<String, Object> getAllBinding() {
        Map<String, Object> output = Maps.newHashMap(EXTRA_BINDING);
        output.put("math", MathBinding.INSTANCE);
        output.put("q", QueryBinding.INSTANCE);
        return output;
    }
}
