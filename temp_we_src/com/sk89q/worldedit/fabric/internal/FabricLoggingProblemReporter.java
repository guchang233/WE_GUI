/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.fabric.internal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.class_8942;

public final class FabricLoggingProblemReporter implements class_8942, AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();

    public static <T> T with(Supplier<String> contextSupplier, Function<class_8942, T> consumer) {
        try (var problemReporter = new FabricLoggingProblemReporter(contextSupplier)) {
            return consumer.apply(problemReporter);
        }
    }

    FabricLoggingProblemReporter(Supplier<String> contextSupplier) {
        this.contextSupplier = contextSupplier;
    }

    private final class_8943 delegate = new class_8943();
    private final Supplier<String> contextSupplier;

    @Override
    public class_8942 method_54946(class_11336 child) {
        return delegate.method_54946(child);
    }

    @Override
    public void method_54947(class_11337 problem) {
        delegate.method_54947(problem);
    }

    @Override
    public void close() {
        if (!delegate.method_71349()) {
            LOGGER.warn("Problems were reported during {}: {}", contextSupplier.get(), delegate.method_71351());
        }
    }
}
