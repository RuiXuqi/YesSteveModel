package com.elfmcys.yesstevemodel.client.animation.molang;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.MolangParser;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.parser.ParseException;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ObjectBinding;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

// Native Access
public class CustomMolangParser {
    private static final ConcurrentLinkedQueue<MolangParser> PARSER_POOL = new ConcurrentLinkedQueue<>();
    private static final Map<String, ObjectBinding> EXTRA_BINDING = Object2ReferenceMaps.singleton("ysm", YSMBinding.INSTANCE);

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
            return parser.parseExpressionUnsafe(expression);
        } finally {
            returnInstance(parser);
        }
    }

    private static MolangParser createMolangParser() {
        return new MolangParser(EXTRA_BINDING);
    }
}
