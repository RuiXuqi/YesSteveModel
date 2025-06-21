package com.elfmcys.yesstevemodel.client.animation.molang;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.MolangParser;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.MathBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.QueryBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.parser.ParseException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

// Native Access
public class CustomMolangParser {
    private static final ConcurrentLinkedQueue<MolangParser> PARSER_POOL = new ConcurrentLinkedQueue<>();
    private static final Map<String, Object> EXTRA_BINDING = new HashMap<>();
    private static final Map<String, Object> COMMAND_HINT = new HashMap<>();
    private static final Pattern ROAMING_ASSIGNMENT_PATTERN = Pattern.compile("^([;\\s]*(v|variable)\\.roaming\\.[A-Za-z0-9_]+\\s*=[^;]+[;\\s]*)+$", Pattern.CASE_INSENSITIVE);

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
        if (COMMAND_HINT.isEmpty()) {
            COMMAND_HINT.putAll(EXTRA_BINDING);
            COMMAND_HINT.put("math", MathBinding.INSTANCE);
            COMMAND_HINT.put("q", QueryBinding.INSTANCE);
        }
        return COMMAND_HINT;
    }

    /**
     * 是否仅包含 v.roaming 赋值
     */
    public static boolean hasOnlyRoamingAssignment(String expression) {
        return ROAMING_ASSIGNMENT_PATTERN.matcher(expression).find();
    }
}
