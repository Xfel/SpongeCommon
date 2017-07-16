package org.spongepowered.common.command.parameters;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.parameters.ArgumentParseException;
import org.spongepowered.api.command.parameters.Parameter;
import org.spongepowered.common.command.parameters.tokenized.SpongeSingleArg;
import org.spongepowered.common.command.parameters.tokenized.SpongeTokenizedArgs;
import org.spongepowered.common.command.specification.SpongeCommandExecutionContext;
import org.spongepowered.lwts.runner.LaunchWrapperTestRunner;

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
        SpongeTokenizedArgs args = new SpongeTokenizedArgs(Lists.newArrayList(new SpongeSingleArg("test", 0, 4)), "test");

        // This context
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();

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
        SpongeTokenizedArgs args = new SpongeTokenizedArgs(Lists.newArrayList(new SpongeSingleArg("1", 0, 1)), "1");

        // This context
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();

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
        SpongeTokenizedArgs args = new SpongeTokenizedArgs(Lists.newArrayList(new SpongeSingleArg("a1", 0, 1)), "a1");

        // This context
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();

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
        SpongeTokenizedArgs args = new SpongeTokenizedArgs(Lists.newArrayList(new SpongeSingleArg("a1", 0, 1)), "a1");

        // This context
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();

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
        SpongeTokenizedArgs args = new SpongeTokenizedArgs(Lists.newArrayList(new SpongeSingleArg("a1", 0, 1)), "a1");

        // This context
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();

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
        SpongeTokenizedArgs args = new SpongeTokenizedArgs(Lists.newArrayList(new SpongeSingleArg("a1", 0, 1)), "a1");

        // This context
        SpongeCommandExecutionContext context = new SpongeCommandExecutionContext();

        // And this source
        CommandSource source = Mockito.mock(CommandSource.class);

        // Parse
        parameter.parse(source, args, context);

        // The context should contain the argument in the context.
        Assert.assertFalse(context.hasAny("test"));

        // Also check we haven't iterated
        Assert.assertTrue(args.hasNext());

    }

}
