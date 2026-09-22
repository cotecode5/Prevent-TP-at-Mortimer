package com.example;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
    name = "Prevent TP at Mortimer",
    description = "Blocks every single spell, cape, jewelry piece, pod, and tab teleport in OSRS via comprehensive text scanning inside Mortimer's cave",
    tags = {"ironman", "teleport", "mortimer", "guard", "wyrmscraig", "cape", "ardougne", "stop", "block"}
)
public class ExamplePlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ExampleConfig config;

    // Region ID 5463 maps explicitly to Wyrmscraig Cavern where Mortimer stands
    private static final int WYRMSCRAIG_CAVERN_REGION_ID = 5463;

    @Override
    protected void startUp() throws Exception
    {
        log.info("Prevent TP at Mortimer started!");
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("Prevent TP at Mortimer stopped!");
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (!config.enableGuard())
        {
            return;
        }

        // Only run check if player exists and is physically standing inside Wyrmscraig Cavern
        if (client.getLocalPlayer() == null || client.getLocalPlayer().getWorldLocation().getRegionID() != WYRMSCRAIG_CAVERN_REGION_ID)
        {
            return;
        }

        String option = event.getMenuOption().toLowerCase();
        String target = event.getMenuTarget().toLowerCase();

        // 1. EXTENSIVE TRAVEL TEXT FILTER (Capes, Worn Gear, Inventory Items, Tabs, Jewelry, Scrolls)
        boolean matchesItemTeleportText = option.contains("teleport") 
            || option.contains("tele")
            || option.contains("monastery")   // Ardougne Cloak monastery
            || option.contains("farm")        // Ardougne Cloak patch
            || option.contains("ardougne")    // Ardougne direct
            || option.contains("rub")         // Jewelry (Glory, Wealth, Duelling, Games, Passage)
            || option.contains("break")       // Teleport tabs
            || option.contains("pod")         // Royal seed pod
            || option.contains("ectophial")   // Ectophial
            || option.contains("communion")   // Cam Torum options
            || option.contains("destination") // Chronicle / Teleport matrices
            || option.contains("digsite")     // Digsite pendant
            || option.contains("xenorias")    // Xeric's talisman
            || option.contains("xoanian")     // Bloodkoch talisman
            || option.contains("miscellania") // Ring of wealth / Capes
            || option.contains("grand exchange")
            || option.contains("glory")
            || option.contains("passage")
            || option.contains("combat")
            || option.contains("champions")   // Chronicle / Capes
            || option.contains("myth")        // Myths cape
            || option.contains("guild")       // Skills/Crafting/Cooking/Fishing capes
            || option.contains("max")         // Max cape options
            || option.contains("construction")// Con cape
            || option.contains("house")       // House tabs / Capes
            || option.contains("rimmington")
            || option.contains("taverley")
            || option.contains("pollnivneach")
            || option.contains("hosidius")
            || option.contains("prifddinas")
            || option.contains("barbarian")   // Games neck
            || option.contains("outpost")     // Games neck
            || option.contains("burthorpe")   // Games neck
            || option.contains("corporeal")   // Games neck
            || option.contains("wintertodt")  // Games neck
            || option.contains("castle")      // Ring of duelling
            || option.contains("ferox")       // Ring of duelling
            || option.contains("poh");

        // 2. SPELLBOOK TEXT FILTER (Standard, Ancients, Lunars, Arceuus spell clicks)
        // Checks if you are choosing a "Cast" action on an icon that has the word "teleport" or "home" in its name
        boolean matchesSpellbookTeleport = option.contains("cast") && 
            (target.contains("teleport") || target.contains("home") || target.contains("tele") || target.contains("poh") || target.contains("brollop"));

        // If ANY travel action or travel spell is clicked, block it instantly
        if (matchesItemTeleportText || matchesSpellbookTeleport)
        {
            event.consume();
            client.addChatMessage(net.runelite.api.ChatMessageType.GAMEMESSAGE, "", "Teleport blocked by Prevent TP at Mortimer! Don't leave the boat behind.", null);
        }
    }

    @Provides
    ExampleConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ExampleConfig.class);
    }
}
