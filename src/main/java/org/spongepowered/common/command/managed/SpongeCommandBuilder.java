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
package org.spongepowered.common.command.managed;

import static org.spongepowered.common.util.SpongeCommonTranslationHelper.t;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.parameter.token.InputTokenizer;
import org.spongepowered.api.command.parameter.token.InputTokenizers;
import org.spongepowered.api.command.managed.ChildExceptionBehavior;
import org.spongepowered.api.command.managed.ChildExceptionBehaviors;
import org.spongepowered.api.command.managed.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.flag.Flags;
import org.spongepowered.common.command.parameter.flag.NoFlags;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SpongeCommandBuilder implements Command.Builder {

    private static final CommandExecutor SUBCOMMAND_ONLY_EXECUTOR = (s, c) -> {
        // TODO: Make this better!
        throw new CommandException(t("This command requires a subcommand."));
    };

    private List<Parameter> parameters = Lists.newArrayList();
    private final Map<String, Command> children = Maps.newHashMap();
    private ChildExceptionBehavior behavior = ChildExceptionBehaviors.SUPPRESS;
    private InputTokenizer inputTokenizer = InputTokenizers.LENIENT_QUOTED_STRING;
    @Nullable private CommandExecutor executor = null;
    @Nullable private Flags flags = null;
    @Nullable private String permission = null;
    @Nullable private Text shortDescription = null;
    @Nullable private Text extendedDescription = null;
    private boolean requirePermissionForChildren = true;

    @Override
    public Command.Builder child(Command child, Iterable<String> keys) {

        keys.forEach(x ->
                Preconditions.checkArgument(!this.children.containsKey(x.toLowerCase(Locale.ENGLISH)),
                        "No two children can have the same key. Keys are case insensitive."));

        // Do this separately so we don't just put a few keys in.
        keys.forEach(x -> this.children.put(x.toLowerCase(Locale.ENGLISH), child));
        return this;
    }

    @Override
    public Command.Builder setRequirePermissionForChildren(boolean required) {
        this.requirePermissionForChildren = required;
        return this;
    }

    @Override
    public Command.Builder setChildExceptionBehavior(ChildExceptionBehavior exceptionBehavior) {
        this.behavior = exceptionBehavior;
        return this;
    }

    @Override
    public Command.Builder setShortDescription(@Nullable Text description) {
        this.shortDescription = description;
        return this;
    }

    @Override
    public Command.Builder setExecutor(CommandExecutor executor) {
        this.executor = executor;
        return this;
    }

    @Override
    public Command.Builder setExtendedDescription(@Nullable Text extendedDescription) {
        this.extendedDescription = extendedDescription;
        return this;
    }

    @Override
    public Command.Builder setFlags(Flags flags) {
        this.flags = flags;
        return this;
    }

    @Override
    public Command.Builder parameter(Parameter parameter) {
        this.parameters.add(parameter);
        return this;
    }

    @Override
    public Command.Builder setPermission(@Nullable String permission) {
        this.permission = permission;
        return this;
    }

    @Override
    public Command.Builder setInputTokenizer(InputTokenizer tokenizer) {
        this.inputTokenizer = tokenizer;
        return this;
    }

    @Override
    public Command build() {

        Preconditions.checkState(!this.children.isEmpty() || this.executor != null,
                "The command must have an executor or at least one child command.");
        return new SpongeManagedCommand(
                this.parameters,
                this.children,
                this.behavior,
                this.inputTokenizer,
                this.flags == null ? NoFlags.INSTANCE : this.flags,
                this.executor == null ? SUBCOMMAND_ONLY_EXECUTOR : this.executor,
                this.permission,
                this.shortDescription,
                this.extendedDescription,
                this.requirePermissionForChildren
        );
    }

    @Override
    public Command.Builder from(Command value) {
        if (!(value instanceof SpongeManagedCommand)) {
            throw new IllegalArgumentException("value must be a SpongeCommand");
        }

        reset();
        ((SpongeManagedCommand) value).populateBuilder(this);
        return this;
    }

    @Override
    public Command.Builder reset() {
        this.parameters = ImmutableList.of();
        this.children.clear();
        this.behavior = ChildExceptionBehaviors.RETHROW;
        this.inputTokenizer = InputTokenizers.LENIENT_QUOTED_STRING;
        this.executor = null;
        this.flags = null;
        this.permission = null;
        this.shortDescription = null;
        this.extendedDescription = null;
        this.requirePermissionForChildren = true;

        return this;
    }

}
