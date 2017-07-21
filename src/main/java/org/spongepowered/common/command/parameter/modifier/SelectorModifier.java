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
package org.spongepowered.common.command.parameter.modifier;

import static org.spongepowered.api.util.SpongeApiTranslationHelper.t;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.parameter.ArgumentParseException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.managed.ParsingContext;
import org.spongepowered.api.command.parameter.managed.ValueParameterModifier;
import org.spongepowered.api.command.parameter.token.CommandArgs;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.selector.Selector;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class SelectorModifier implements ValueParameterModifier {

    private final Collection<Class<? extends Entity>> entityClasses;
    private final boolean onlyOne;
    private final boolean strict;

    public SelectorModifier(Collection<Class<? extends Entity>> entityClasses, boolean onlyOne, boolean strict) {
        this.entityClasses = entityClasses;
        this.onlyOne = onlyOne;
        this.strict = strict;
    }

    @Override
    public void onParse(Text key, CommandSource source, CommandArgs args, CommandContext context, ParsingContext parsingContext)
            throws ArgumentParseException {
        String arg = args.peek();
        if (arg.startsWith("@")) { // Possibly a selector
            try {
                Set<Entity> entities = Selector.parse(args.next()).resolve(source);
                Iterator<Entity> entityIterator = entities.iterator();
                while (entityIterator.hasNext()) {
                    Entity entity = entityIterator.next();
                    if (this.entityClasses.contains(entity.getClass())) {
                        if (this.strict) {
                            throw args.createError(t("The selector returned entities that are not valid for this argument."));
                        } else {
                            entityIterator.remove();
                        }
                    }
                }

                if (entities.isEmpty()) {
                    throw args.createError(t("The selector returned no entities."));
                }

                if (this.onlyOne && entities.size() != 1) {
                    throw args.createError(t("The selector returned more than one entity when only one should have been returned."));
                }

                context.putEntry(key, entities);
            } catch (IllegalArgumentException ex) {
                throw args.createError(Text.of(ex.getMessage()));
            }
        } else {
            parsingContext.next();
        }
    }
}
