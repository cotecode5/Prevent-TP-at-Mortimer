package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("preventtpatmortimer")
public interface ExampleConfig extends Config
{
    @ConfigItem(
        keyName = "enableGuard",
        name = "Enable Teleport Guard",
        description = "Blocks all item/cape teleports inside Wyrmscraig Cavern to save boat placement"
    )
    default boolean enableGuard()
    {
        return true;
    }
}
