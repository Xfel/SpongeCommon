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
package org.spongepowered.common.command.parameters;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.parameters.Parameter;
import org.spongepowered.api.command.parameters.flags.Flags;
import org.spongepowered.common.command.TestParameter;
import org.spongepowered.common.command.parameters.flags.SpongeFlagsBuilder;
import org.spongepowered.common.command.parameters.flags.behaviors.AcceptNonValueBehavior;
import org.spongepowered.common.command.parameters.flags.behaviors.AcceptValueBehavior;
import org.spongepowered.common.command.parameters.flags.behaviors.IgnoreBehavior;
import org.spongepowered.common.command.parameters.flags.behaviors.SkipBehavior;
import org.spongepowered.common.command.parameters.tokenized.SpongeTokenizedArgs;
import org.spongepowered.common.command.parameters.tokenized.tokenizers.SpaceSplitInputTokenizer;
import org.spongepowered.common.command.specification.SpongeCommandExecutionContext;
import org.spongepowered.lwts.runner.LaunchWrapperTestRunner;

@RunWith(LaunchWrapperTestRunner.class)
public class FlagsTest {

    private final Parameter DUMMY = new TestParameter();

    @Test
    public void testShortFlag() throws Exception {

        Flags flags = new SpongeFlagsBuilder().flag("a").build();

        SpongeTokenizedArgs args = new SpongeTokenizedArgs(new SpaceSplitInputTokenizer().tokenize("-a", false), "-a");
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();
        CommandSource source = Mockito.mock(CommandSource.class);

        flags.parse(source, args, context);
        Assert.assertTrue(context.<Boolean>getOneUnchecked("a"));
    }

    @Test
    public void testShortFlags() throws Exception {

        Flags flags = new SpongeFlagsBuilder().flag("a").flag("b").build();

        SpongeTokenizedArgs args = new SpongeTokenizedArgs(new SpaceSplitInputTokenizer().tokenize("-ab", false), "-ab");
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();
        CommandSource source = Mockito.mock(CommandSource.class);

        flags.parse(source, args, context);
        Assert.assertTrue(context.<Boolean>getOneUnchecked("a"));
        Assert.assertTrue(context.<Boolean>getOneUnchecked("b"));
    }

    @Test
    public void testUnknownBehaviorAddValue() throws Exception {

        Flags flags = new SpongeFlagsBuilder().flag("a").flag("b").setUnknownShortFlagBehavior(new AcceptValueBehavior()).build();

        SpongeTokenizedArgs args = new SpongeTokenizedArgs(new SpaceSplitInputTokenizer().tokenize("-c hello", false), "-c hello");
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();
        CommandSource source = Mockito.mock(CommandSource.class);

        flags.parse(source, args, context);
        Assert.assertEquals("hello", context.<String>getOneUnchecked("c"));
    }

    @Test
    public void testUnknownBehaviorSkip() throws Exception {

        Flags flags = new SpongeFlagsBuilder().flag("a").flag("b").setUnknownShortFlagBehavior(new SkipBehavior()).build();

        SpongeTokenizedArgs args = new SpongeTokenizedArgs(new SpaceSplitInputTokenizer().tokenize("-ca hello", false), "-ca hello");
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();
        CommandSource source = Mockito.mock(CommandSource.class);

        flags.parse(source, args, context);
        Assert.assertFalse(context.hasAny("c"));
        Assert.assertTrue(context.hasAny("a"));
    }

    @Test
    public void testUnknownBehaviorIgnore() throws Exception {

        Flags flags = new SpongeFlagsBuilder().flag("a").flag("b").setUnknownShortFlagBehavior(new IgnoreBehavior()).build();

        SpongeTokenizedArgs args = new SpongeTokenizedArgs(new SpaceSplitInputTokenizer().tokenize("-c hello", false), "-c hello");
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();
        CommandSource source = Mockito.mock(CommandSource.class);

        flags.parse(source, args, context);
        Assert.assertFalse(context.<Boolean>getOne("c").isPresent());
    }

    @Test
    public void testUnknownBehaviorAddValueShortFlagJustReturnsTrueWhenTheWrongWayAround() throws Exception {

        Flags flags = new SpongeFlagsBuilder().flag("a").flag("b").setUnknownShortFlagBehavior(new AcceptValueBehavior()).build();

        SpongeTokenizedArgs args = new SpongeTokenizedArgs(new SpaceSplitInputTokenizer().tokenize("-ca hello", false), "-ca hello");
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();
        CommandSource source = Mockito.mock(CommandSource.class);

        flags.parse(source, args, context);
        Assert.assertTrue(context.<Boolean>getOneUnchecked("c"));
        Assert.assertTrue(context.<Boolean>getOneUnchecked("a"));
    }

    @Test
    public void testUnknownBehaviorAddValueShortFlag() throws Exception {

        Flags flags = new SpongeFlagsBuilder().flag("a").flag("b").setUnknownShortFlagBehavior(new AcceptValueBehavior()).build();

        SpongeTokenizedArgs args = new SpongeTokenizedArgs(new SpaceSplitInputTokenizer().tokenize("-c hello", false), "-c hello");
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();
        CommandSource source = Mockito.mock(CommandSource.class);

        flags.parse(source, args, context);
        Assert.assertTrue(context.hasAny("c"));
        Assert.assertEquals("hello", context.<String>getOneUnchecked("c"));
    }

    @Test
    public void testUnknownBehaviorAddNoValue() throws Exception {

        Flags flags = new SpongeFlagsBuilder().flag("a").flag("b").setUnknownShortFlagBehavior(new AcceptNonValueBehavior()).build();

        SpongeTokenizedArgs args = new SpongeTokenizedArgs(new SpaceSplitInputTokenizer().tokenize("-c hello", false), "-c hello");
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();
        CommandSource source = Mockito.mock(CommandSource.class);

        flags.parse(source, args, context);
        Assert.assertTrue(context.<Boolean>getOneUnchecked("c"));
    }

    @Test
    public void testLongFlag() throws Exception {

        Flags flags = new SpongeFlagsBuilder().flag("a").flag("b").flag("-ab").build();

        SpongeTokenizedArgs args = new SpongeTokenizedArgs(new SpaceSplitInputTokenizer().tokenize("--ab", false), "--ab");
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();
        CommandSource source = Mockito.mock(CommandSource.class);

        flags.parse(source, args, context);
        Assert.assertTrue(context.<Boolean>getOneUnchecked("ab"));
        Assert.assertFalse(context.hasAny("a"));
        Assert.assertFalse(context.hasAny("b"));
    }

    @Test
    public void testValueFlag() throws Exception {

        // Prepare the parameter
        Flags flags = new SpongeFlagsBuilder().valueFlag(this.DUMMY, "-ab").build();

        SpongeTokenizedArgs args = new SpongeTokenizedArgs(new SpaceSplitInputTokenizer().tokenize("--ab hello", false), "--ab hello");
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();
        CommandSource source = Mockito.mock(CommandSource.class);

        flags.parse(source, args, context);
        Assert.assertEquals("hello", context.<String>getOneUnchecked("test"));
        Assert.assertTrue(context.<Boolean>getOneUnchecked("ab"));
    }

}
