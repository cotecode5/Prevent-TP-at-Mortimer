package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("preventtpatmortimer")
public interface PreventTpAtMortimer extends Config
{
    @ConfigItem(
        keyName = "enableGuard",
        name = "Enable Teleport Guard",
        description = "Blocks teleport and travel actions inside Wyrmscraig Cavern"
    )
    default boolean enableGuard()
    {
        return true;
    }

    @ConfigItem(
        keyName = "debugLogging",
        name = "Enable Debug Logging",
        description = "Logs detailed menu and widget information while inside Wyrmscraig Cavern"
    )
    default boolean debugLogging()
    {
        return false;
    }
}
