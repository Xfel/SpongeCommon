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
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandExecutionResult;
import org.spongepowered.api.command.parameters.Parameter;
import org.spongepowered.api.command.specification.ChildExceptionBehaviors;
import org.spongepowered.api.command.specification.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

/**
 * This adds sample commands that will generally print messages to the executor
 * of the command. It tests various parameters and other functions of the
 * CommandSpec.
 */
@Plugin(id = "standardcommands", name = "StandardCommands", description = "A plugin to test the function of the CommandSpec")
public class StandardCommandsTest {

    private static Text textKey = Text.of("text");
    private static Text playerKey = Text.of("player");

    @Listener
    public void onInit(GameInitializationEvent event) {
        Sponge.getCommandManager().register(this, CommandSpec.builder().executor((source, context) -> {
            source.sendMessage(Text.of("No parameter command."));
            return CommandExecutionResult.success();
        }).build(), "noparam");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .permission("sponge.test.permission")
                .executor((source, context) -> {
                    source.sendMessage(Text.of("You have the permission \"sponge.test.permission\"."));
                    return CommandExecutionResult.success();
        }).build(), "permission");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .parameters(Parameter.builder().key(textKey).remainingRawJoinedStrings().build())
                .description(Text.of("Repeats what you say to the command."))
                .executor((source, context) -> {
                    source.sendMessage(Text.of("Simon says: ", context.getOneUnchecked(textKey)));
                    return CommandExecutionResult.success();
                }).build(), "simonsays");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .parameters(Parameter.builder().key(textKey).choices("wisely", "poorly").build())
                .description(Text.of("Repeats what you say to the command, from a choice of \"wisely\" and \"poorly\"."))
                .executor((source, context) -> {
                    source.sendMessage(Text.of("You chose ", context.getOneUnchecked(textKey)));
                    return CommandExecutionResult.success();
                }).build(), "choose");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .parameters(Parameter.builder().key(textKey).string().optional().build())
                .description(Text.of("Repeats the one word you say to the command, if you add that parameter."))
                .executor((source, context) -> {
                    source.sendMessage(Text.of("You chose ", context.<String>getOne(textKey).orElse("nothing")));
                    return CommandExecutionResult.success();
                }).build(), "chooseoptional");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .parameters(Parameter.builder().key(textKey).allOf().string().build())
                .description(Text.of("Repeats the words you say to the command, one at a time."))
                .executor((source, context) -> {
                    context.getAll(textKey).forEach(x -> source.sendMessage(Text.of("You chose ", x)));
                    return CommandExecutionResult.success();
                }).build(), "chooseall");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .parameters(
                        Parameter.builder().key(playerKey).playerOrSource().string().build(),
                        Parameter.builder().key(textKey).allOf().string()
                                .suggestions(((source, args, context) -> Lists.newArrayList("spam", "bacon", "eggs")))
                                .usage(((key, source) -> Text.of("Words to send")))
                                .build()
                )
                .description(Text.of("Repeats the words you say to the command, one at a time, to the specified player, but with helpful "
                        + "suggestions and a custom usage text."))
                .executor((source, context) -> {
                    Player player = context.<Player>getOne(playerKey).orElseThrow(() -> new CommandException(Text.of("No player was specified")));
                    context.getAll(textKey).forEach(x -> player.sendMessage(Text.of(source.getName(), " chose ", x)));
                    return CommandExecutionResult.success();
                }).build(), "chooseplayer");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("A command that only has a subcommand"))
                .addChild(CommandSpec.builder().executor((source, context) -> {
                    source.sendMessage(Text.of("Child executed"));
                    return CommandExecutionResult.success();
                }).build(), "child").build(), "subwithchildonly");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("A command that has a subcommand as well as a base command"))
                .addChild(CommandSpec.builder().executor((source, context) -> {
                    source.sendMessage(Text.of("Child executed"));
                    return CommandExecutionResult.success();
                })
                .executor(((source, context) -> {
                    source.sendMessage(Text.of("Base executed"));
                    return CommandExecutionResult.success();
                }))
                .build(), "child").build(), "subwithchildandbase");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("A command that throws exceptions from the child and base, but throws the first one it finds."))
                .childExceptionBehavior(ChildExceptionBehaviors.RETHROW)
                .addChild(CommandSpec.builder().executor((source, context) -> {
                    throw new CommandException(Text.of("Child"));
                })
                .executor(((source, context) -> {
                    throw new CommandException(Text.of("Base"));
                }))
                .build(), "child").build(), "exception1");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("A command that throws exceptions from the child and base, but stacks them."))
                .childExceptionBehavior(ChildExceptionBehaviors.STORE)
                .addChild(CommandSpec.builder().executor((source, context) -> {
                    throw new CommandException(Text.of("Child"));
                })
                .executor(((source, context) -> {
                    throw new CommandException(Text.of("Base"));
                }))
                .build(), "child").build(), "exception2");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("A command that throws exceptions from the child and base, but suppresses child exceptions."))
                .childExceptionBehavior(ChildExceptionBehaviors.SUPPRESS)
                .addChild(CommandSpec.builder().executor((source, context) -> {
                    throw new CommandException(Text.of("Child"));
                })
                .executor(((source, context) -> {
                    throw new CommandException(Text.of("Base"));
                }))
                .build(), "child").build(), "exception3");
    }

}
