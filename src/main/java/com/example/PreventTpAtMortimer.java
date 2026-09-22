package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("preventtpatmortimer")
public interface PreventTpAtMortimerConfig extends Config
{
    @ConfigItem(
        keyName = "enableGuard",
        name = "Enable Teleport Guard",
        description = "Blocks teleports inside Wyrmscraig Cavern to save UIM boat placement"
    )
    default boolean enableGuard()
    {
        return true;
    }
}
