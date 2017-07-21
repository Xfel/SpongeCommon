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
package org.spongepowered.common.command.parameter;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.token.CommandArgs;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.ArgumentParseException;
import org.spongepowered.api.command.parameter.managed.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class SpongeParameterBuilder implements Parameter.Builder {

    @Nullable private Text key;
    @Nullable private ValueParameter valueParameter;
    @Nullable private ValueParser parser;
    @Nullable private ValueCompleter completer;
    @Nullable private ValueUsage usage;
    private final List<ValueParameterModifier> modifiers = Lists.newArrayList();
    @Nullable private String permission;

    @Override
    public Parameter.Builder key(Text key) {
        Preconditions.checkNotNull(key);
        this.key = key;
        return this;
    }

    @Override
    public Parameter.Builder parser(ValueParser parser) {
        Preconditions.checkNotNull(parser);
        if (parser instanceof ValueParameter) {
            this.valueParameter = (ValueParameter) parser;
            this.parser = null;
        } else {
            this.parser = parser;
            this.valueParameter = null;
        }
        return this;
    }

    @Override
    public Parameter.Builder suggestions(@Nullable ValueCompleter completer) {
        this.completer = completer;
        return this;
    }

    @Override
    public Parameter.Builder usage(@Nullable ValueUsage usage) {
        this.usage = usage;
        return this;
    }

    @Override
    public Parameter.Builder addModifiers(ValueParameterModifier... modifiers) {
        this.modifiers.addAll(Arrays.asList(modifiers));
        return this;
    }

    @Override
    public Parameter.Builder addModifiers(List<ValueParameterModifier> modifiers) {
        this.modifiers.addAll(modifiers);
        return this;
    }

    @Override
    public Parameter.Builder addModifierToBeginning(ValueParameterModifier modifier) {
        this.modifiers.add(0, modifier);
        return this;
    }

    @Override
    public Parameter.Builder permission(@Nullable String permission) {
        this.permission = permission;
        return this;
    }

    @Override
    public Parameter build() {
        Preconditions.checkState(this.key != null, "key must be set");
        Preconditions.checkState(this.valueParameter != null || this.parser != null, "Either PlayerValueParameter or onParse must be set");

        List<ValueParameterModifier> modifiersToUse = Lists.newArrayList(this.modifiers);
        if (this.permission != null) {
            modifiersToUse.add(0, new PermissionModifier(this.permission));
        }

        if (this.valueParameter != null && this.usage == null && this.completer == null) {
            return new SpongeParameter(this.key, modifiersToUse, this.valueParameter);
        }

        ValueParser parserToUse;
        ValueCompleter completerToUse;
        ValueUsage usageToUse;

        if (this.valueParameter != null) {
            parserToUse = this.valueParameter;
            completerToUse = this.completer == null ? this.valueParameter : this.completer;
            usageToUse = this.usage == null ? this.valueParameter : this.usage;
        } else {
            parserToUse = this.parser;
            completerToUse = this.completer == null ? (cs, args, cec) -> new ArrayList<>() : this.completer;
            usageToUse = this.usage == null ? (t, cs) -> t : this.usage;
        }

        return new SpongeParameter(this.key, modifiersToUse, new SpongeValueParameter(parserToUse, completerToUse, usageToUse));
    }

    @Override
    public Parameter.Builder from(Parameter value) {
        if (!(value instanceof SpongeParameter)) {
            throw new IllegalArgumentException("value must be a SpongeParameter");
        }
        reset();

        ((SpongeParameter) value).populateBuilder(this);
        return this;
    }

    @Override
    public Parameter.Builder reset() {
        this.key = null;
        this.valueParameter = null;
        this.parser = null;
        this.completer = null;
        this.usage = null;
        this.modifiers.clear();
        this.permission = null;
        return this;
    }

    private static class SpongeValueParameter implements ValueParameter {

        private final ValueParser parser;
        private final ValueCompleter completer;
        private final ValueUsage usage;

        private SpongeValueParameter(
                ValueParser parser,
                ValueCompleter completer,
                ValueUsage usage) {
            this.parser = parser;
            this.completer = completer;
            this.usage = usage;
        }

        @Override
        public Optional<Object> getValue(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
            return this.parser.getValue(source, args, context);
        }

        @Override
        public List<String> complete(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
            return this.completer.complete(source, args, context);
        }

        @Override
        public Text getUsage(Text key, CommandSource source) {
            return this.usage.getUsage(key, source);
        }

    }

    private static class PermissionModifier implements ValueParameterModifier {

        private final String permission;

        private PermissionModifier(String permission) {
            this.permission = permission;
        }

        @Override
        public void onParse(Text key, CommandSource source, CommandArgs args, CommandContext context, ParsingContext parsingContext)
                throws ArgumentParseException {
            if (source.hasPermission(this.permission)) {
                parsingContext.next();
            }
        }

        @Override
        public List<String> complete(CommandSource source, CommandArgs args, CommandContext context, List<String> currentCompletions)
                throws ArgumentParseException {
            return Lists.newArrayList();
        }

        @Override
        public Text getUsage(Text key, CommandSource source, Text currentUsage) {
            if (source.hasPermission(this.permission)) {
                return currentUsage;
            }

            return Text.EMPTY;
        }
    }

}
