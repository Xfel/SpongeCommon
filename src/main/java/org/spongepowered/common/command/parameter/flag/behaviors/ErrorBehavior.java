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
package org.spongepowered.common.command.parameter.flag.behaviors;

import static org.spongepowered.common.util.SpongeCommonTranslationHelper.t;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.ArgumentParseException;
import org.spongepowered.api.command.parameter.flag.UnknownFlagBehavior;
import org.spongepowered.api.command.parameter.token.CommandArgs;

public class ErrorBehavior implements UnknownFlagBehavior {

    @Override
    public void parse(CommandSource source, CommandArgs args, CommandContext context, CommandArgs.Snapshot tokenizedArgsPreviousState,
            CommandContext.Snapshot contextPreviousState, String flag) throws ArgumentParseException {
        args.setState(tokenizedArgsPreviousState);
        throw args.createError(t("%s is not a valid flag", flag));
    }

    @Override
    public String getId() {
        return "sponge:error";
    }

    @Override
    public String getName() {
        return "Throw error on unknown flag";
    }

}
