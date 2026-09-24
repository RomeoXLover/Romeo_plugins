package com.romeo.fabric;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public final class RomeoFabricMod implements DedicatedServerModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("RomeoFabricMod");

    @Override
    public void onInitializeServer() {
        LOGGER.info("Romeo Fabric server mod enabled.");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            try {
                Class<?> commandManagerClass = Class.forName("net.minecraft.server.command.CommandManager");
                Method literalMethod = commandManagerClass.getMethod("literal", String.class);
                Object literalBuilder = literalMethod.invoke(null, "testplugin");

                Class<?> literalArgumentBuilderClass = Class.forName("com.mojang.brigadier.builder.LiteralArgumentBuilder");
                Method executesMethod = literalArgumentBuilderClass.getMethod("executes", Command.class);
                executesMethod.invoke(literalBuilder, (Command<Object>) context -> {
                    Object source = context.getSource();
                    try {
                        Method sendFeedback = source.getClass().getMethod("sendFeedback", Runnable.class, boolean.class);
                        sendFeedback.invoke(source, (Runnable) () -> {
                        }, false);
                    } catch (ReflectiveOperationException ignored) {
                        try {
                            Method sendMessage = source.getClass().getMethod("sendMessage", String.class);
                            sendMessage.invoke(source, "Romeo Fabric mod is active.");
                        } catch (ReflectiveOperationException ignored2) {
                            // Ignore runtime compatibility differences across versions.
                        }
                    }
                    return 1;
                });

                Class<?> commandNodeClass = Class.forName("com.mojang.brigadier.tree.CommandNode");
                Method registerMethod = dispatcher.getClass().getMethod("register", commandNodeClass);
                registerMethod.invoke(dispatcher, literalBuilder);
            } catch (ReflectiveOperationException e) {
                LOGGER.warn("Could not register testplugin command through reflection.", e);
            }
        });
    }
}
