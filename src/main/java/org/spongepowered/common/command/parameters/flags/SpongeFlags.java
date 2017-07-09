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
package org.spongepowered.common.command.parameters.flags;

import static org.spongepowered.common.util.SpongeCommonTranslationHelper.t;

import org.spongepowered.api.command.CommandMessageFormatting;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.parameters.ArgumentParseException;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.command.parameters.CommandExecutionContext;
import org.spongepowered.api.command.parameters.Parameter;
import org.spongepowered.api.command.parameters.flags.Flags;
import org.spongepowered.api.command.parameters.flags.UnknownFlagBehavior;
import org.spongepowered.api.command.parameters.tokens.TokenizedArgs;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpongeFlags implements Flags {

    private final List<String> primaryFlags;
    private final Map<String, Parameter> flags;
    private final UnknownFlagBehavior shortUnknown;
    private final UnknownFlagBehavior longUnknown;
    private final boolean anchorFlags;

    SpongeFlags(List<String> primaryFlags, Map<String, Parameter> flags,
            UnknownFlagBehavior shortUnknown, UnknownFlagBehavior longUnknown, boolean anchorFlags) {
        this.primaryFlags = primaryFlags;
        this.flags = flags;
        this.shortUnknown = shortUnknown;
        this.longUnknown = longUnknown;
        this.anchorFlags = anchorFlags;
    }

    @Override
    public void parse(CommandSource source, TokenizedArgs args, CommandExecutionContext context) throws ArgumentParseException {
        if (args.hasPrevious() && !this.anchorFlags || !args.hasNext() || !args.peek().startsWith("-")) {
            return; // Nothing to parse, move along.
        }

        // Avoiding APE
        Object tokenizedPreviousState = args.getState();
        Object contextPreviousState = args.getState();
        String next = args.next();
        if (next.startsWith("--")) {
            parseLong(next, source, args, context, tokenizedPreviousState, contextPreviousState);
        } else {
            parseShort(next, source, args, context, tokenizedPreviousState, contextPreviousState);
        }
    }

    private void parseShort(String flag, CommandSource source, TokenizedArgs args, CommandExecutionContext context, Object tokenizedPreviousState,
            Object contextPreviousState) throws ArgumentParseException {
        char[] shortFlags = flag.substring(1).toLowerCase(Locale.ENGLISH).toCharArray();

        // -abc is parsed as -a -b -c
        // Note that if we have -abc [blah], a and b MUST NOT try to parse the next value. This is why we have the
        // PreventIteratorMovementTokenizedArgs class, which will throw an error in those scenarioes.
        // -c is allowed to have a value.
        PreventIteratorMovementTokenizedArgs nonMoving = new PreventIteratorMovementTokenizedArgs(args);
        for (int i = 0; i < shortFlags.length; i++) {
            TokenizedArgs argsToUse = i == shortFlags.length - 1 ? args : nonMoving;
            Parameter param = this.flags.get(String.valueOf(shortFlags[i]));
            if (param == null) {
                this.shortUnknown.parse(source, argsToUse, context, tokenizedPreviousState, contextPreviousState);
            } else {
                param.parse(source, argsToUse, context);
            }
        }
    }

    private void parseLong(String flag, CommandSource source, TokenizedArgs args, CommandExecutionContext context, Object tokenizedPreviousState,
            Object contextPreviousState) throws ArgumentParseException {
        String longFlag = flag.substring(2).toLowerCase(Locale.ENGLISH);
        Parameter param = this.flags.get(longFlag);
        if (param == null) {
            this.longUnknown.parse(source, args, context, tokenizedPreviousState, contextPreviousState);
        } else {
            param.parse(source, args, context);
        }
    }

    @Override
    public Text getUsage(CommandSource src) {
        return Text.joinWith(CommandMessageFormatting.SPACE_TEXT, this.primaryFlags.stream()
                .map(this.flags::get).map(x -> x.getUsage(src)).filter(x -> !x.isEmpty()).collect(Collectors.toList()));
    }

    @Override
    public boolean isAnchored() {
        return this.anchorFlags;
    }

    void populateBuilder(SpongeFlagsBuilder builder) {
        builder.updateFlags(this.primaryFlags, this.flags)
                .setUnknownShortFlagBehavior(this.shortUnknown)
                .setUnknownLongFlagBehavior(this.longUnknown)
                .setAnchorFlags(this.anchorFlags);
    }

    private class PreventIteratorMovementTokenizedArgs implements TokenizedArgs {

        private final TokenizedArgs args;

        PreventIteratorMovementTokenizedArgs(TokenizedArgs args) {
            this.args = args;
        }

        @Override
        public boolean hasNext() {
            return this.args.hasNext();
        }

        @Override
        public String next() throws ArgumentParseException {
            throw createValueError();
        }

        @Override
        public Optional<String> nextIfPresent() {
            return Optional.empty();
        }

        @Override
        public String peek() throws ArgumentParseException {
            return this.args.peek();
        }

        @Override
        public boolean hasPrevious() {
            return this.args.hasPrevious();
        }

        @Override
        public String previous() throws ArgumentParseException {
            throw createValueError();
        }

        @Override
        public List<String> getAll() {
            return this.args.getAll();
        }

        @Override
        public int getCurrentRawPosition() {
            return this.args.getCurrentRawPosition();
        }

        @Override
        public String getRaw() {
            return this.args.getRaw();
        }

        @Override
        public Object getState() {
            return this.args.getState();
        }

        @Override
        public void setState(Object state) {
            // noop
        }

        @Override
        public ArgumentParseException createError(Text message) {
            return this.args.createError(message);
        }

        @Override
        public ArgumentParseException createError(Text message, Throwable inner) {
            return this.args.createError(message, inner);
        }

        private ArgumentParseException createValueError() {
            return createError(t("Short flags that are not at the end of a group cannot have a value."));
        }
    }
}
