package com.elfmcys.ysm.client.animation.molang;

import com.elfmcys.ysm.geckolib3.core.molang.MolangParser;
import com.elfmcys.ysm.geckolib3.core.molang.builtin.MathBinding;
import com.elfmcys.ysm.geckolib3.core.molang.builtin.QueryBinding;
import com.elfmcys.ysm.geckolib3.core.molang.value.IValue;
import com.elfmcys.ysm.molang.parser.ParseException;
import org.apache.commons.lang3.concurrent.ConcurrentException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

public class CustomMolangParser {
    private static final ConcurrentLinkedQueue<MolangParser> PARSER_POOL = new ConcurrentLinkedQueue<>();
    private static final Map<String, Object> EXTRA_BINDING = new HashMap<>();
    private static final Map<String, Object> COMMAND_HINT = new HashMap<>();
    private static final Pattern ROAMING_ASSIGNMENT_PATTERN = Pattern.compile("^([;\\s]*(v|variable)\\.roaming\\.[A-Za-z0-9_]+\\s*=[^;]+[;\\s]*)+$", Pattern.CASE_INSENSITIVE);

    public static MolangParser rentInstance() {
        MolangParser parser = PARSER_POOL.poll();
        if (parser == null) {
            return createMolangParser();
        } else {
            return parser;
        }
    }

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
            try {
                EXTRA_BINDING.put("ysm", YSMBinding.INSTANCE.get());
                EXTRA_BINDING.put("ctrl", CtrlBinding.INSTANCE.get());
                EXTRA_BINDING.put("tlm", TLMBinding.INSTANCE.get());
                EXTRA_BINDING.put("args", UserFunctionArgument.INSTANCE);
            } catch (ConcurrentException e) {
                throw new RuntimeException(e);
            }
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
