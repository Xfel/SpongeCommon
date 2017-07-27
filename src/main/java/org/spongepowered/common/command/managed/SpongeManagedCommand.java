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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spongepowered.common.util.SpongeCommonTranslationHelper.t;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandMessageFormatting;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.dispatcher.Dispatcher;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.ArgumentParseException;
import org.spongepowered.api.command.parameter.token.InputTokenizer;
import org.spongepowered.api.command.managed.ChildExceptionBehavior;
import org.spongepowered.api.command.managed.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.flag.Flags;
import org.spongepowered.api.command.parameter.token.CommandArgs;
import org.spongepowered.common.command.SpongeCommandMapping;
import org.spongepowered.common.command.parameter.flag.NoFlags;
import org.spongepowered.common.command.parameter.token.SpongeCommandArgs;
import org.spongepowered.common.command.managed.childexception.ChildCommandException;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpongeManagedCommand implements Command, Dispatcher {

    private final Parameter parameters;
    private final Map<String, CommandMapping> mappings = Maps.newHashMap();
    private final TreeSet<String> primaryAliases = Sets.newTreeSet();
    private final ChildExceptionBehavior childExceptionBehavior;
    private final InputTokenizer inputTokenizer;
    private final Flags flags;
    private final CommandExecutor executor;
    @Nullable private final String permission;
    private final Function<CommandSource, Optional<Text>> shortDescription;
    private final Function<CommandSource, Optional<Text>> extendedDescription;
    private final boolean requirePermissionForChildren;

    SpongeManagedCommand(Iterable<Parameter> parameters,
            Map<String, Command> children,
            ChildExceptionBehavior childExceptionBehavior,
            InputTokenizer inputTokenizer,
            Flags flags,
            CommandExecutor executor, @Nullable String permission,
            Function<CommandSource, Optional<Text>> shortDescription,
            Function<CommandSource, Optional<Text>> extendedDescription,
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
            Map<Command, List<String>> intermediate = children.entrySet().stream()
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
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        // Step one, create the CommandArgs and CommandContext
        SpongeCommandArgs args = new SpongeCommandArgs(this.inputTokenizer.tokenize(arguments, true), arguments);
        SpongeCommandContext context = new SpongeCommandContext();
        return processInternal(source, args, context);
    }

    public CommandResult processInternal(CommandSource source, CommandArgs args, SpongeCommandContext context)
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
                Command cmd = optionalChild.get().getCommand();
                context.setCurrentCommand(subCommand);
                try {
                    if (cmd instanceof SpongeManagedCommand) {
                        SpongeManagedCommand childSpec = (SpongeManagedCommand) cmd;
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
        SpongeCommandArgs args = new SpongeCommandArgs(this.inputTokenizer.tokenize(arguments, true), arguments);
        CommandContext context = new SpongeCommandContext(null, true, targetPosition);
        return ImmutableList.copyOf(this.parameters.complete(source, args, context));
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return this.permission == null || source.hasPermission(this.permission);
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource source) {
        return this.shortDescription.apply(source);
    }

    public Optional<Text> getExtendedDescription(CommandSource source) {
        return this.extendedDescription.apply(source);
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
            Command child = this.mappings.get(x).getCommand();
            return child instanceof SpongeManagedCommand && ((SpongeManagedCommand) child).requirePermissionForChildren || child.testPermission(source);
        }).collect(Collectors.toList());

        if (aliasesToDisplay.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(t("Subcommands: ").toBuilder().color(TextColors.RED).append(Text.of(String.join(", ", aliasesToDisplay))).build());
    }

    void populateBuilder(SpongeCommandBuilder builder) {
        builder.setChildExceptionBehavior(this.childExceptionBehavior)
               .setShortDescription(this.shortDescription)
               .setExtendedDescription(this.extendedDescription)
               .setExecutor(this.executor)
               .setFlags(this.flags)
               .setInputTokenizer(this.inputTokenizer)
               .parameters(this.parameters)
               .setPermission(this.permission)
               .setRequirePermissionForChildren(this.requirePermissionForChildren);

        this.mappings.forEach((alias, mapping) -> builder.child(mapping.getCommand(), mapping.getAllAliases()));
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
    public void populateContext(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
        // First, the flags.
        this.flags.parse(source, args, context);
        this.parameters.parse(source, args, context);
    }

    public CommandExecutor getExecutor() {
        return this.executor;
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
        if (mapping != null && (source == null || mapping.getCommand().testPermission(source))) {
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
