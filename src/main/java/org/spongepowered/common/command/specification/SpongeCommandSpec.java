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
package org.spongepowered.common.command.specification;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spongepowered.common.util.SpongeCommonTranslationHelper.t;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandExecutionResult;
import org.spongepowered.api.command.CallableCommand;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandMessageFormatting;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.dispatcher.Dispatcher;
import org.spongepowered.api.command.parameters.CommandExecutionContext;
import org.spongepowered.api.command.parameters.ArgumentParseException;
import org.spongepowered.api.command.parameters.tokens.InputTokenizer;
import org.spongepowered.api.command.specification.ChildExceptionBehavior;
import org.spongepowered.api.command.specification.CommandExecutor;
import org.spongepowered.api.command.specification.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.command.parameters.Parameter;
import org.spongepowered.api.command.parameters.flags.Flags;
import org.spongepowered.api.command.parameters.tokens.TokenizedArgs;
import org.spongepowered.common.command.SpongeCommandMapping;
import org.spongepowered.common.command.parameters.flags.NoFlags;
import org.spongepowered.common.command.parameters.tokenized.SpongeTokenizedArgs;
import org.spongepowered.common.command.specification.childexception.ChildCommandException;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SpongeCommandSpec implements CommandSpec, Dispatcher {

    private final Parameter parameters;
    private final Map<String, CommandMapping> mappings = Maps.newHashMap();
    private final TreeSet<String> primaryAliases = Sets.newTreeSet();
    private final ChildExceptionBehavior childExceptionBehavior;
    private final InputTokenizer inputTokenizer;
    private final Flags flags;
    private final CommandExecutor executor;
    @Nullable private final String permission;
    @Nullable private final Text shortDescription;
    @Nullable private final Text extendedDescription;
    private final boolean requirePermissionForChildren;

    SpongeCommandSpec(Iterable<Parameter> parameters,
            Map<String, CallableCommand> children,
            ChildExceptionBehavior childExceptionBehavior,
            InputTokenizer inputTokenizer,
            Flags flags,
            CommandExecutor executor, @Nullable String permission,
            @Nullable Text shortDescription,
            @Nullable Text extendedDescription,
            boolean requirePermissionForChildren) {
        this.parameters = Parameter.seq(parameters);
        this.childExceptionBehavior = childExceptionBehavior;
        this.inputTokenizer = inputTokenizer;
        this.flags = flags;
        this.executor = executor;
        this.permission = permission;
        this.shortDescription = shortDescription;
        this.extendedDescription = extendedDescription;
        this.requirePermissionForChildren = requirePermissionForChildren;

        if (!children.isEmpty()) {
            // Register the commands
            Map<CallableCommand, List<String>> intermediate = children.entrySet().stream()
                    .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
            intermediate.forEach((lowLevel, aliases) -> {
                SpongeCommandMapping spongeCommandMapping;
                if (aliases.size() == 1) {
                    spongeCommandMapping = new SpongeCommandMapping(lowLevel, aliases.get(0));
                } else {
                    spongeCommandMapping = new SpongeCommandMapping(lowLevel, aliases.get(0), aliases.subList(1, aliases.size()));
                }

                this.primaryAliases.add(aliases.get(0));
                aliases.forEach(x -> this.mappings.put(x, spongeCommandMapping));
            });
        }
    }

    @Override
    public CommandExecutionResult process(CommandSource source, String arguments) throws CommandException {
        // Step one, create the TokenizedArgs and CommandExecutionContext
        SpongeTokenizedArgs args = new SpongeTokenizedArgs(this.inputTokenizer.tokenize(arguments, true), arguments);
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();
        return processInternal(source, args, context);
    }

    private CommandExecutionResult processInternal(CommandSource source, SpongeTokenizedArgs args, SpongeCommandExecutionContext context)
            throws CommandException {
        if (this.requirePermissionForChildren) {
            checkPermission(source);
        }

        ChildCommandException childException = null;

        // Step two, children. If we have any, we parse them now.
        if (!this.mappings.isEmpty() && args.hasNext()) {
            Object argsState = args.getState();
            Object contextState = context.getState();
            String subCommand = args.next().toLowerCase(Locale.ENGLISH);
            Optional<? extends CommandMapping> optionalChild = get(subCommand.toLowerCase(Locale.ENGLISH));
            if (optionalChild.isPresent()) {
                // Get the command
                CallableCommand cmd = optionalChild.get().getCallable();
                context.setCurrentCommand(subCommand);
                try {
                    if (cmd instanceof SpongeCommandSpec) {
                        SpongeCommandSpec childSpec = (SpongeCommandSpec) cmd;
                        return childSpec.processInternal(source, args, context);
                    } else {
                        return cmd.process(source, args.rawArgsFromCurrentPosition());
                    }
                } catch (CommandException ex) {
                    // This might still rethrow, depends on the selected behavior
                    CommandException eex = this.childExceptionBehavior.onChildCommandError(ex).orElse(null);
                    if (eex instanceof ChildCommandException) {
                        childException = (ChildCommandException) eex;
                    } else {
                        childException = new ChildCommandException(subCommand, eex, null);
                    }
                }
            }

            // Reset the state.
            args.setState(argsState);
            context.setState(contextState);
        }


        // Step three, this command
        if (!this.requirePermissionForChildren) {
            checkPermission(source);
        }

        populateContext(source, args, context);

        try {
            return this.executor.execute(source, context);
        } catch (CommandException ex) {
            if (childException != null) {
                throw new ChildCommandException(
                        context.getCurrentCommand().orElseGet(() ->
                                Sponge.getCommandManager().getPrimaryAlias(this).orElse("")), ex, childException);
            }

            // Rethrow
            throw ex;
        }
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition)
            throws CommandException {
        checkNotNull(source, "source");
        SpongeTokenizedArgs args = new SpongeTokenizedArgs(this.inputTokenizer.tokenize(arguments, true), arguments);
        CommandExecutionContext context = new SpongeCommandExecutionContext(null, true, targetPosition);
        return ImmutableList.copyOf(this.parameters.complete(source, args, context));
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return this.permission == null || source.hasPermission(this.permission);
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource source) {
        return Optional.ofNullable(this.shortDescription);
    }

    @Override
    public Optional<Text> getExtendedDescription(CommandSource source) {
        return Optional.ofNullable(this.extendedDescription);
    }

    @Override
    public Optional<Text> getHelp(CommandSource source) {
        checkNotNull(source, "source");
        Text.Builder builder = Text.builder();
        this.getShortDescription(source).ifPresent((a) -> builder.append(a, Text.NEW_LINE));
        builder.append(getUsage(source));
        this.getExtendedDescription(source).ifPresent((a) -> builder.append(Text.NEW_LINE, a));
        return Optional.of(builder.build());
    }

    @Override
    public Text getUsage(CommandSource source) {
        Text paramUsage = parameterUsageText(source);
        Text.Builder builder;
        if (paramUsage.isEmpty()) {
            builder = t("No parameters.").toBuilder().color(TextColors.RED);
        } else {
            builder = t("Usage: ").toBuilder().color(TextColors.RED).append(paramUsage);
        }

        subcommandUsageText(source).ifPresent(x -> builder.append(Text.NEW_LINE).append(t("Subcommands: ")).append(x));
        return builder.build();
    }

    private Text parameterUsageText(CommandSource source) {
        if (this.flags instanceof NoFlags) {
            return this.parameters.getUsage(source);
        }

        return Text.joinWith(CommandMessageFormatting.SPACE_TEXT, this.flags.getUsage(source), this.parameters.getUsage(source));
    }

    private Optional<Text> subcommandUsageText(CommandSource source) {
        List<String> aliasesToDisplay = this.primaryAliases.stream().filter(x -> {
            CallableCommand child = this.mappings.get(x).getCallable();
            return child instanceof SpongeCommandSpec && ((SpongeCommandSpec) child).requirePermissionForChildren || child.testPermission(source);
        }).collect(Collectors.toList());

        if (aliasesToDisplay.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(t("Subcommands: ").toBuilder().color(TextColors.RED).append(Text.of(String.join(", ", aliasesToDisplay))).build());
    }

    void populateBuilder(SpongeCommandSpecBuilder builder) {
        builder.childExceptionBehavior(this.childExceptionBehavior)
               .description(this.shortDescription)
               .extendedDescription(this.extendedDescription)
               .executor(this.executor)
               .flags(this.flags)
               .inputTokenizer(this.inputTokenizer)
               .parameters(this.parameters)
               .permission(this.permission)
               .requirePermissionForChildren(this.requirePermissionForChildren);

        this.mappings.forEach((alias, mapping) -> builder.addChild(mapping.getCallable(), mapping.getAllAliases()));
    }

    private void checkPermission(CommandSource source) throws CommandPermissionException {
        if (!testPermission(source)) {
            throw new CommandPermissionException();
        }
    }

    /**
     * Process this command with existing arguments and context objects.
     *
     * @param source The source to populate the context with
     * @param args The arguments to process with
     * @param context The context to put data in
     * @throws ArgumentParseException if an invalid argument is provided
     */
    private void populateContext(CommandSource source, TokenizedArgs args, CommandExecutionContext context) throws ArgumentParseException {
        // First, the flags.
        this.flags.parse(source, args, context);
        this.parameters.parse(source, args, context);
    }

    @Override
    public Set<? extends CommandMapping> getCommands() {
        return ImmutableSet.copyOf(this.mappings.values());
    }

    @Override
    public Set<String> getPrimaryAliases() {
        return ImmutableSet.copyOf(this.primaryAliases);
    }

    @Override
    public Set<String> getAliases() {
        return ImmutableSet.copyOf(this.mappings.keySet());
    }

    @Override
    public Optional<? extends CommandMapping> get(String alias) {
        return get(alias, null);
    }

    @Override
    public Optional<? extends CommandMapping> get(String alias, @Nullable CommandSource source) {
        // No need to use a disambiguator, we don't allow multiple children with the same name.
        CommandMapping mapping = this.mappings.get(alias.toLowerCase(Locale.ENGLISH));
        if (mapping != null && (source == null || mapping.getCallable().testPermission(source))) {
            return Optional.of(mapping);
        }

        return Optional.empty();
    }

    @Override
    public Set<? extends CommandMapping> getAll(String alias) {
        return get(alias).map(ImmutableSet::of).orElseGet(ImmutableSet::of);
    }

    @Override
    public Multimap<String, CommandMapping> getAll() {
        ImmutableMultimap.Builder<String, CommandMapping> im = ImmutableMultimap.builder();
        this.mappings.forEach(im::put);
        return im.build();
    }

    @Override
    public boolean containsAlias(String alias) {
        return this.mappings.containsKey(alias.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public boolean containsMapping(CommandMapping mapping) {
        return this.mappings.containsValue(mapping);
    }

}
