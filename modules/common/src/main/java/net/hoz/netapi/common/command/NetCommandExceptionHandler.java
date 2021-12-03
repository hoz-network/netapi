package net.hoz.netapi.common.command;

import cloud.commandframework.exceptions.InvalidCommandSenderException;
import cloud.commandframework.exceptions.InvalidSyntaxException;
import lombok.extern.slf4j.Slf4j;
import net.hoz.netapi.client.lang.NLang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.sender.CommandSenderWrapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.BiFunction;

@Slf4j
public class NetCommandExceptionHandler {

    public BiFunction<CommandSenderWrapper, Exception, Component> INVALID_SYNTAX =
            (sender, e) -> Message.of(NLang.COMMON_COMMANDS_INVALID_SYNTAX)
                    .placeholder("syntax", ((InvalidSyntaxException) e).getCorrectSyntax())
                    .resolvePrefix()
                    .getForJoined(sender);

    public BiFunction<CommandSenderWrapper, Exception, Component> INVALID_SENDER =
            (sender, e) -> Message.of(NLang.COMMON_COMMANDS_INVALID_SENDER)
                    .placeholder("valid-sender", ((InvalidCommandSenderException) e).getRequiredSender().getSimpleName())
                    .resolvePrefix()
                    .getForJoined(sender);

    public BiFunction<CommandSenderWrapper, Exception, Component> NO_PERMISSIONS =
            (sender, e) -> Message.of(NLang.COMMON_COMMANDS_NO_PERMISSIONS)
                    .resolvePrefix()
                    .getForJoined(sender);

    public BiFunction<CommandSenderWrapper, Exception, Component> ARGUMENT_PARSING =
            (sender, e) -> Message.of(NLang.COMMON_COMMANDS_INVALID_ARGUMENT)
                    .placeholder("argument", e.getCause().getMessage())
                    .resolvePrefix()
                    .getForJoined(sender);

    public BiFunction<CommandSenderWrapper, Exception, Component> COMMAND_EXECUTION =
            (sender, e) -> {
                final var cause = e.getCause();
                cause.printStackTrace();

                final var writer = new StringWriter();
                cause.printStackTrace(new PrintWriter(writer));
                final var stackTrace = writer.toString().replaceAll("\t", "    ");
                final var hover = HoverEvent.showText(
                        Component.text()
                                .append(Component.text(e.getMessage()))
                                .append(Component.newline())
                                .append(Message.of(NLang.COMMON_INTERNAL_ERROR).getFor(sender))
                );

                final var click = ClickEvent.copyToClipboard(stackTrace);
                return Component.text()
                        .append(Message.of(NLang.COMMON_INTERNAL_ERROR)
                                .resolvePrefix()
                                .getFor(sender))
                        .hoverEvent(hover)
                        .clickEvent(click)
                        .build();
            };
}
