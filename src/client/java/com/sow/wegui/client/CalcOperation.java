package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import net.minecraft.client.Minecraft;

import java.text.DecimalFormat;
import java.util.List;

/**
 * //calc 的直接 API 实现。
 */
public final class CalcOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        if (values.isEmpty() || values.get(0).isBlank()) {
            error(mc, "请指定表达式");
            return false;
        }

        String input = values.get(0).trim();
        try {
            Expression expression = Expression.compile(input);
            int timeout = 3000;
            try {
                var player = WeOperationHelper.requirePlayer(mc);
                if (player != null) {
                    var session = WeOperationHelper.requireSession(player);
                    if (session != null) {
                        timeout = session.getTimeout();
                    }
                }
            } catch (Exception ignored) {
            }
            double result = expression.evaluate(new double[]{}, timeout);
            String formatted = Double.isNaN(result) ? "NaN" : new DecimalFormat("#,##0.#####").format(result);
            feedback(mc, input + " = " + formatted);
            return true;
        } catch (ExpressionException e) {
            error(mc, "表达式错误: " + e.getMessage());
            return false;
        }
    }
}
