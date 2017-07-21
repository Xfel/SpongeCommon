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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.parameter.ArgumentParseException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.common.command.parameter.token.SpongeCommandArgs;
import org.spongepowered.common.command.parameter.token.SpongeSingleArg;
import org.spongepowered.common.command.managed.SpongeCommandContext;
import org.spongepowered.lwts.runner.LaunchWrapperTestRunner;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@RunWith(LaunchWrapperTestRunner.class)
public class ParameterTest {

    @Test(expected = IllegalStateException.class)
    public void testThatNoKeyFails() {

        // When given this parameter with no key, exception
        Parameter.builder().string().build();
    }

    @Test
    public void testThatStandardParameterCanBeParsed() throws Exception {

        // With this parameter
        Parameter parameter = Parameter.builder().key("test").string().build();

        // This tokenized args
        SpongeCommandArgs args = new SpongeCommandArgs(Lists.newArrayList(new SpongeSingleArg("test", 0, 4)), "test");

        // This context
        SpongeCommandContext context = new SpongeCommandContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

        // The context should contain the argument in the context.
        Assert.assertEquals("test", context.<String>getOneUnchecked("test"));

    }

    @Test
    public void testThatIntegerCanBeParsed() throws Exception {

        // With this parameter
        Parameter parameter = Parameter.builder().key("test").integer().build();

        // This tokenized args
        SpongeCommandArgs args = new SpongeCommandArgs(Lists.newArrayList(new SpongeSingleArg("1", 0, 1)), "1");

        // This context
        SpongeCommandContext context = new SpongeCommandContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

        // Get the result
        int res = context.<Integer>getOneUnchecked("test");

        // The context should contain the argument in the context.
        Assert.assertEquals(1, res);

    }

    @Test(expected = ArgumentParseException.class)
    public void testThatIntegerWillThrowIfTheInputIsntAnInteger() throws Exception {

        // With this parameter
        Parameter parameter = Parameter.builder().key("test").integer().build();

        // This tokenized args
        SpongeCommandArgs args = new SpongeCommandArgs(Lists.newArrayList(new SpongeSingleArg("a1", 0, 1)), "a1");

        // This context
        SpongeCommandContext context = new SpongeCommandContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

    }

    @Test
    public void testThatCustomValueParserWorks() throws Exception {

        // With this parameter
        Parameter parameter = Parameter.builder().key("test").parser((s, a, c) -> Optional.of(a.next())).build();

        // This tokenized args
        SpongeCommandArgs args = new SpongeCommandArgs(Lists.newArrayList(new SpongeSingleArg("a1", 0, 1)), "a1");

        // This context
        SpongeCommandContext context = new SpongeCommandContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

        // The context should contain the argument in the context.
        Assert.assertEquals("a1", context.<String>getOneUnchecked("test"));

    }

    @Test(expected = ArgumentParseException.class)
    public void testThatCustomParserBreaksIfNothingIsReturned() throws Exception {

        // With this parameter (it's optional for a reason!)
        Parameter parameter = Parameter.builder().key("test").parser((s, a, c) -> Optional.empty()).build();

        // This tokenized args
        SpongeCommandArgs args = new SpongeCommandArgs(Lists.newArrayList(new SpongeSingleArg("a1", 0, 1)), "a1");

        // This context
        SpongeCommandContext context = new SpongeCommandContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

    }

    @Test
    public void testThatCustomParserCanReturnNothingIfOptional() throws Exception {

        // With this parameter (it's optional for a reason!)
        Parameter parameter = Parameter.builder().key("test").optionalWeak().parser((s, a, c) -> Optional.empty()).build();

        // This tokenized args
        SpongeCommandArgs args = new SpongeCommandArgs(Lists.newArrayList(new SpongeSingleArg("a1", 0, 1)), "a1");

        // This context
        SpongeCommandContext context = new SpongeCommandContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

        // The context should not contain the argument in the context.
        Assert.assertFalse(context.hasAny("test"));

        // Also check we haven't iterated
        Assert.assertTrue(args.hasNext());

    }

    @Test
    public void testAllOf() throws Exception {

        // With this parameter
        Parameter parameter = Parameter.builder().key("test").allOf().string().build();

        // This tokenized args
        SpongeCommandArgs args = new SpongeCommandArgs(Lists.newArrayList(
                new SpongeSingleArg("a1", 0, 1),
                new SpongeSingleArg("a2", 2, 3),
                new SpongeSingleArg("a3", 4, 5)
        ), "a1 a2 a3");

        // This context
        SpongeCommandContext context = new SpongeCommandContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

        // The context should contain the argument in the context.
        Assert.assertTrue(context.hasAny("test"));

        // Test that they are all there
        Collection<String> result = context.getAll("test");
        Assert.assertEquals(3, result.size());
        Assert.assertTrue(result.contains("a1"));
        Assert.assertTrue(result.contains("a2"));
        Assert.assertTrue(result.contains("a3"));

        // Also check we have fully iterated
        Assert.assertFalse(args.hasNext());

    }

    @Test
    public void testRepeat() throws Exception {

        // With this parameter
        Parameter parameter = Parameter.builder().key("test").repeated(2).string().build();

        // This tokenized args
        SpongeCommandArgs args = new SpongeCommandArgs(Lists.newArrayList(
                new SpongeSingleArg("a1", 0, 1),
                new SpongeSingleArg("a2", 2, 3),
                new SpongeSingleArg("a3", 4, 5)
        ), "a1 a2 a3");

        // This context
        SpongeCommandContext context = new SpongeCommandContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

        // The context should contain the argument in the context.
        Assert.assertTrue(context.hasAny("test"));

        // Test that they are all there
        Collection<String> result = context.getAll("test");
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains("a1"));
        Assert.assertTrue(result.contains("a2"));
        Assert.assertFalse(result.contains("a3"));

        // Also check we have fully iterated
        Assert.assertTrue(args.hasNext());

    }

    @Test(expected = ArgumentParseException.class)
    public void testRepeatFailsIfThereAreNotEnoughEntries() throws Exception {

        // With this parameter
        Parameter parameter = Parameter.builder().key("test").repeated(4).string().build();

        // This tokenized args
        SpongeCommandArgs args = new SpongeCommandArgs(Lists.newArrayList(
                new SpongeSingleArg("a1", 0, 1),
                new SpongeSingleArg("a2", 2, 3),
                new SpongeSingleArg("a3", 4, 5)
        ), "a1 a2 a3");

        // This context
        SpongeCommandContext context = new SpongeCommandContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

    }

    @Test
    public void testFirstParsingChoosingFirstBranch() throws Exception {

        // With this parameter
        Parameter parameter = Parameter.firstOf(
                Parameter.builder().key("test").repeated(3).string().build(),
                Parameter.builder().key("test2").string().build()
        );

        // This tokenized args
        SpongeCommandArgs args = new SpongeCommandArgs(Lists.newArrayList(
                new SpongeSingleArg("a1", 0, 1),
                new SpongeSingleArg("a2", 2, 3),
                new SpongeSingleArg("a3", 4, 5)
        ), "a1 a2 a3");

        // This context
        SpongeCommandContext context = new SpongeCommandContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

        Assert.assertTrue(context.hasAny("test"));
        Assert.assertFalse(context.hasAny("test2"));

        // Test that they are all there
        Collection<String> result = context.getAll("test");
        Assert.assertEquals(3, result.size());
    }

    @Test
    public void testFirstParsingChoosingSecondBranch() throws Exception {

        // With this parameter
        Parameter parameter = Parameter.firstOf(
                Parameter.builder().key("test").repeated(4).string().build(),
                Parameter.builder().key("test2").string().build()
        );

        // This tokenized args
        SpongeCommandArgs args = new SpongeCommandArgs(Lists.newArrayList(
                new SpongeSingleArg("a1", 0, 1),
                new SpongeSingleArg("a2", 2, 3),
                new SpongeSingleArg("a3", 4, 5)
        ), "a1 a2 a3");

        // This context
        SpongeCommandContext context = new SpongeCommandContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

        Assert.assertFalse(context.hasAny("test"));
        Assert.assertTrue(context.hasAny("test2"));

        Assert.assertEquals("a1", context.getOneUnchecked("test2"));
    }

    @Test
    public void testSeq() throws Exception {

        // With this parameter
        Parameter parameter = Parameter.seq(
                Parameter.builder().key("test").string().build(),
                Parameter.builder().key("test2").string().build()
        );

        // This tokenized args
        SpongeCommandArgs args = new SpongeCommandArgs(Lists.newArrayList(
                new SpongeSingleArg("a1", 0, 1),
                new SpongeSingleArg("a2", 2, 3)
        ), "a1 a2");

        // This context
        SpongeCommandContext context = new SpongeCommandContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

        Assert.assertTrue(context.hasAny("test"));
        Assert.assertTrue(context.hasAny("test2"));

        Assert.assertEquals("a1", context.getOneUnchecked("test"));
        Assert.assertEquals("a2", context.getOneUnchecked("test2"));
    }

    @Test
    public void testChoices() throws Exception {

        Map<String, String> choices = ImmutableMap.of("a", "one", "b", "two");

        // With this parameter
        Parameter parameter =
                Parameter.seq(
                        Parameter.builder().key("test").choices(choices).build(),
                        Parameter.builder().key("test2").choices(choices).build()
        );


        // This tokenized args
        SpongeCommandArgs args = new SpongeCommandArgs(Lists.newArrayList(
                new SpongeSingleArg("a", 0, 0),
                new SpongeSingleArg("b", 2, 2)
        ), "a b");

        // This context
        SpongeCommandContext context = new SpongeCommandContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

        Assert.assertEquals("one", context.getOneUnchecked("test"));
        Assert.assertEquals("two", context.getOneUnchecked("test2"));

    }

    private enum TestEnum {
        ONE, TWO, RED
    }


    @Test
    public void testEnum() throws Exception {

        // With this parameter
        Parameter parameter = Parameter.builder().key("test").enumValue(TestEnum.class).build();

        // This tokenized args
        SpongeCommandArgs args = new SpongeCommandArgs(Lists.newArrayList(
                new SpongeSingleArg("one", 0, 0),
                new SpongeSingleArg("TwO", 2, 2),
                new SpongeSingleArg("RED", 4, 4)
        ), "one TwO RED");

        // This context
        SpongeCommandContext context = new SpongeCommandContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

        Assert.assertEquals(TestEnum.ONE, context.getOneUnchecked("test"));

        // Reset context, parse, repeat
        context = new SpongeCommandContext();
        parameter.parse(source, args, context);

        Assert.assertEquals(TestEnum.TWO, context.getOneUnchecked("test"));

        // Reset context, parse, repeat
        context = new SpongeCommandContext();
        parameter.parse(source, args, context);

        Assert.assertEquals(TestEnum.RED, context.getOneUnchecked("test"));

    }

    @Test
    public void testRemainingJoinedStrings() throws Exception {

        // With this parameter
        Parameter parameter = Parameter.builder().key("test").remainingJoinedStrings().build();

        // This tokenized args
        SpongeCommandArgs args = new SpongeCommandArgs(Lists.newArrayList(new SpongeSingleArg("test test test2", 0, 4)),
                "test test test2");

        // This context
        SpongeCommandContext context = new SpongeCommandContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

        // The context should contain the argument in the context.
        Assert.assertEquals("test test test2", context.<String>getOneUnchecked("test"));

    }
}
