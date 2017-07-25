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
package org.spongepowered.test;

import com.google.common.collect.Lists;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.managed.ChildExceptionBehaviors;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

/**
 * This adds sample commands that will generally print messages to the executor
 * of the command. It tests various parameters and other functions of the
 * SpongeManagedCommand.
 */
@Plugin(id = "standardcommands", name = "StandardCommands", description = "A plugin to test the function of the Command.Builder")
public class StandardCommandsTest {

    private static Text textKey = Text.of("text");
    private static Text playerKey = Text.of("player");

    @Listener
    public void onInit(GameInitializationEvent event) {
        Sponge.getCommandManager().register(this, Command.builder().setExecutor((source, context) -> {
            source.sendMessage(Text.of("No parameter command."));
            return CommandResult.success();
        }).build(), "noparam");

        Sponge.getCommandManager().register(this, Command.builder()
                .setPermission("sponge.test.permission")
                .setExecutor((source, context) -> {
                    source.sendMessage(Text.of("You have the permission \"sponge.test.permission\"."));
                    return CommandResult.success();
        }).build(), "permission");

        Sponge.getCommandManager().register(this, Command.builder()
                .parameters(Parameter.builder().setKey(textKey).remainingRawJoinedStrings().build())
                .setShortDescription(Text.of("Repeats what you say to the command."))
                .setExecutor((source, context) -> {
                    source.sendMessage(Text.of("Simon says: ", context.getOneUnchecked(textKey)));
                    return CommandResult.success();
                }).build(), "simonsays");

        Sponge.getCommandManager().register(this, Command.builder()
                .parameters(Parameter.builder().setKey(textKey).choices("wisely", "poorly").build())
                .setShortDescription(Text.of("Repeats what you say to the command, from a choice of \"wisely\" and \"poorly\"."))
                .setExecutor((source, context) -> {
                    source.sendMessage(Text.of("You chose ", context.getOneUnchecked(textKey)));
                    return CommandResult.success();
                }).build(), "choose");

        Sponge.getCommandManager().register(this, Command.builder()
                .parameters(Parameter.builder().setKey(textKey).string().optional().build())
                .setShortDescription(Text.of("Repeats the one word you say to the command, if you add that parameter."))
                .setExecutor((source, context) -> {
                    source.sendMessage(Text.of("You chose ", context.<String>getOne(textKey).orElse("nothing")));
                    return CommandResult.success();
                }).build(), "chooseoptional");

        Sponge.getCommandManager().register(this, Command.builder()
                .parameters(Parameter.builder().setKey(textKey).allOf().string().build())
                .setShortDescription(Text.of("Repeats the words you say to the command, one at a time."))
                .setExecutor((source, context) -> {
                    context.getAll(textKey).forEach(x -> source.sendMessage(Text.of("You chose ", x)));
                    return CommandResult.success();
                }).build(), "chooseall");

        Sponge.getCommandManager().register(this, Command.builder()
                .parameters(
                        Parameter.builder().setKey(playerKey).playerOrSource().string().build(),
                        Parameter.builder().setKey(textKey).allOf().string()
                                .setSuggestions(((source, args, context) -> Lists.newArrayList("spam", "bacon", "eggs")))
                                .setUsage(((key, source) -> Text.of("Words to send")))
                                .build()
                )
                .setShortDescription(Text.of("Repeats the words you say to the command, one at a time, to the specified player, but with helpful "
                        + "suggestions and a custom usage text."))
                .setExecutor((source, context) -> {
                    Player player = context.<Player>getOne(playerKey).orElseThrow(() -> new CommandException(Text.of("No player was specified")));
                    context.getAll(textKey).forEach(x -> player.sendMessage(Text.of(source.getName(), " chose ", x)));
                    return CommandResult.success();
                }).build(), "chooseplayer");

        Sponge.getCommandManager().register(this, Command.builder()
                .setShortDescription(Text.of("A command that only has a subcommand"))
                .child(Command.builder().setExecutor((source, context) -> {
                    source.sendMessage(Text.of("Child executed"));
                    return CommandResult.success();
                }).build(), "child").build(), "subwithchildonly");

        Sponge.getCommandManager().register(this, Command.builder()
                .setShortDescription(Text.of("A command that has a subcommand as well as a base command"))
                .child(Command.builder().setExecutor((source, context) -> {
                    source.sendMessage(Text.of("Child executed"));
                    return CommandResult.success();
                })
                .setExecutor(((source, context) -> {
                    source.sendMessage(Text.of("Base executed"));
                    return CommandResult.success();
                }))
                .build(), "child").build(), "subwithchildandbase");

        Sponge.getCommandManager().register(this, Command.builder()
                .setShortDescription(Text.of("A command that throws exceptions from the child and base, but throws the first one it finds."))
                .setChildExceptionBehavior(ChildExceptionBehaviors.RETHROW)
                .child(Command.builder().setExecutor((source, context) -> {
                    throw new CommandException(Text.of("Child"));
                })
                .setExecutor(((source, context) -> {
                    throw new CommandException(Text.of("Base"));
                }))
                .build(), "child").build(), "exception1");

        Sponge.getCommandManager().register(this, Command.builder()
                .setShortDescription(Text.of("A command that throws exceptions from the child and base, but stacks them."))
                .setChildExceptionBehavior(ChildExceptionBehaviors.STORE)
                .child(Command.builder().setExecutor((source, context) -> {
                    throw new CommandException(Text.of("Child"));
                })
                .setExecutor(((source, context) -> {
                    throw new CommandException(Text.of("Base"));
                }))
                .build(), "child").build(), "exception2");

        Sponge.getCommandManager().register(this, Command.builder()
                .setShortDescription(Text.of("A command that throws exceptions from the child and base, but suppresses child exceptions."))
                .setChildExceptionBehavior(ChildExceptionBehaviors.SUPPRESS)
                .child(Command.builder().setExecutor((source, context) -> {
                    throw new CommandException(Text.of("Child"));
                })
                .setExecutor(((source, context) -> {
                    throw new CommandException(Text.of("Base"));
                }))
                .build(), "child").build(), "exception3");
    }

}
