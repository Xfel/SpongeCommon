/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.command.parameters.factories;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.spongepowered.api.command.parameters.spec.ValueParameterModifier;
import org.spongepowered.api.command.parameters.spec.factories.ValueParameterModifierFactory;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.common.command.parameters.modifiers.DefaultValueModifier;
import org.spongepowered.common.command.parameters.modifiers.DefaultValueSuppplierModifier;
import org.spongepowered.common.command.parameters.modifiers.RepeatedModifier;
import org.spongepowered.common.command.parameters.modifiers.SelectorModifier;

import java.util.Collection;
import java.util.function.Supplier;

public class SpongeValueParameterModifierFactory implements ValueParameterModifierFactory {

    @Override
    public ValueParameterModifier repeated(int times) {
        return new RepeatedModifier(times);
    }

    @Override
    public ValueParameterModifier selector(Collection<Class<? extends Entity>> supportedEntities, boolean requireOne, boolean strict) {
        Preconditions.checkNotNull(supportedEntities, "supportedEntities");
        Preconditions.checkArgument(!supportedEntities.isEmpty(), "supportedEntities must not be zero length");
        return new SelectorModifier(Lists.newArrayList(supportedEntities), requireOne, strict);
    }

    @Override
    public ValueParameterModifier defaultValue(Object defaultValue) {
        return new DefaultValueModifier(defaultValue);
    }

    @Override
    public ValueParameterModifier defaultValueSupplier(Supplier<Object> defaultValueSupplier) {
        return new DefaultValueSuppplierModifier(defaultValueSupplier);
    }

}
