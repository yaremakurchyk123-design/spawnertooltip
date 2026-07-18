package com.example.spawnertooltip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

/**
 * Client entrypoint. Registers the tooltip callback that appends
 * custom spawner information (sword, enchants, kills, mob type, eggs)
 * to item tooltips.
 *
 * Ported from 1.21.4 (item data components) to 1.20.1 (classic ItemStack NBT).
 */
public class SpawnerTooltipClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // In 1.20.1 the fabric-api client tooltip event has the signature
        // (ItemStack stack, TooltipContext context, List<Text> lines) - no TooltipType param yet.
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> SpawnerTooltipHandler.appendInfo(stack, lines));
    }
}
