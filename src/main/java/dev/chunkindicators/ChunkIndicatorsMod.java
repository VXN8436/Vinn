package dev.chunkindicators;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkIndicatorsMod implements ModInitializer {
    public static final String MOD_ID = "chunk_indicators";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static IndicatorConfig config;

    @Override
    public void onInitialize() {
        config = IndicatorConfig.load();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (config.enabled) {
                IndicatorScanner.tick(server, config);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("chunkindicators")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("enabled").executes(ctx -> setEnabled(ctx.getSource(), true)))
                .then(Commands.literal("disabled").executes(ctx -> setEnabled(ctx.getSource(), false)))
                .then(Commands.literal("status").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "Chunk Indicators: " + (config.enabled ? "Enabled" : "Disabled")), false);
                    return 1;
                }))
                .then(Commands.literal("reload").executes(ctx -> {
                    config = IndicatorConfig.load();
                    IndicatorScanner.clearCache();
                    ctx.getSource().sendSuccess(() -> Component.literal("Chunk Indicators config reloaded."), true);
                    return 1;
                }))
            )
        );

        LOGGER.info("Chunk Indicators loaded ({}).", config.enabled ? "Enabled" : "Disabled");
    }

    private static int setEnabled(net.minecraft.commands.CommandSourceStack source, boolean value) {
        config.enabled = value;
        config.save();
        if (!value) {
            IndicatorScanner.clearCache();
        }
        source.sendSuccess(() -> Component.literal(
            "Chunk Indicators: " + (value ? "Enabled" : "Disabled")), true);
        return 1;
    }
}
