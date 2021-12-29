package net.hoz.netapi.common.paper;

import cloud.commandframework.CommandManager;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.hoz.netapi.client.config.DataConfig;
import net.hoz.netapi.client.service.NetLangProvider;
import net.hoz.netapi.client.lang.NLang;
import net.hoz.netapi.common.command.NetCommandExceptions;
import net.hoz.netapi.common.module.NetCommonModule;
import org.bukkit.command.CommandException;
import org.bukkit.plugin.Plugin;
import org.screamingsandals.lib.bukkit.command.PaperScreamingCloudManager;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.sender.CommandSenderWrapper;

@Slf4j
@RequiredArgsConstructor
public class PaperCommonModule extends AbstractModule {
    private final Plugin owner;
    private final DataConfig clientConfig;
    private final String commandName;

    @SneakyThrows
    @Override
    protected void configure() {
        bind(Plugin.class).toInstance(owner);
        install(new NetCommonModule(clientConfig));
    }

    @Singleton
    @Provides
    public MinecraftHelp<CommandSenderWrapper> buildMinecraftHelp(CommandManager<CommandSenderWrapper> manager) {
        final var help = new MinecraftHelp<>("/" + commandName + " help", input -> input, manager);
        //TODO
        //help.setMessageProvider();
        return help;
    }

    @Singleton
    @Provides
    public CommandManager<CommandSenderWrapper> buildCommandManager(NetLangProvider netLangProvider) throws Exception {

        final var manager = new PaperScreamingCloudManager(owner,
                AsynchronousCommandExecutionCoordinator.<CommandSenderWrapper>newBuilder().build());

        manager.registerExceptionHandler(CommandException.class,
                ((commandSender, e) -> {
                    log.warn("Exception occurred while executing command! {}", e.getMessage(), e);
                    Message.of(NLang.COMMON_INTERNAL_ERROR).send(commandSender);
                }));

        new MinecraftExceptionHandler<CommandSenderWrapper>()
                .withHandler(MinecraftExceptionHandler.ExceptionType.INVALID_SENDER,
                        NetCommandExceptions.INVALID_SENDER)
                .withHandler(MinecraftExceptionHandler.ExceptionType.INVALID_SYNTAX,
                        NetCommandExceptions.INVALID_SYNTAX)
                .withHandler(MinecraftExceptionHandler.ExceptionType.NO_PERMISSION,
                        NetCommandExceptions.NO_PERMISSIONS)
                .withHandler(MinecraftExceptionHandler.ExceptionType.ARGUMENT_PARSING,
                        NetCommandExceptions.ARGUMENT_PARSING)
                .withHandler(MinecraftExceptionHandler.ExceptionType.COMMAND_EXECUTION,
                        NetCommandExceptions.COMMAND_EXECUTION)
                .apply(manager, input -> input);

        return manager;
    }
}
