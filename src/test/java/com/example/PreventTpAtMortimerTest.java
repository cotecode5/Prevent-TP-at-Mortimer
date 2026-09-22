package com.example;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PreventTpAtMortimerTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(PreventTpAtMortimerPlugin.class);
        RuneLite.main(args);
    }
}
