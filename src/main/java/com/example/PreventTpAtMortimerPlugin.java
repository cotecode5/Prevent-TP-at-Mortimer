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
    description = "Safe Text Filter: Blocks travel, cape, jewelry, item, and spell names inside Mortimer's cave without interfering with standard items",
    tags = {"ironman", "teleport", "mortimer", "guard", "wyrmscraig", "cape", "ardougne", "block", "stop"}
)
public class PreventTpAtMortimerPlugin extends Plugin
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

        // 1. SAFE TRAVEL VERBS & BASELINES (Items, Caps, Pods, Tabs)
        boolean matchesItemTeleportText = option.contains("teleport") 
            || option.contains("tele")
            || option.contains("rub")         // Jewelry commands (Glory, Duelling, Games, Wealth)
            || option.contains("break")       // Teleport tabs
            || option.contains("pod")         // Royal seed pod
            || option.contains("ectophial")   // Ectophial
            || option.contains("communion")   // Cam Torum options
            || option.contains("destination") // Chronicle / Matrix books
            || option.contains("poh");

        // 2. EXHAUSTIVE CITIES AND AREA DESTINATIONS (No single rune words or raw elements)
        boolean matchesCityText = option.contains("ardougne")   // Ardougne Cloak / Spells
            || option.contains("monastery")   // Ardougne Cloak Monastery
            || option.contains("farm")        // Ardougne Cloak Patch
            || option.contains("lumbridge")
            || option.contains("varrock")
            || option.contains("falador")
            || option.contains("camelot")
            || option.contains("seers")
            || option.contains("yanille")
            || option.contains("watchtower")
            || option.contains("trollheim")
            || option.contains("kourend")
            || option.contains("prifddinas")
            || option.contains("catherby")
            || option.contains("kharyrllr")
            || option.contains("senntisten")
            || option.contains("dareeyak")
            || option.contains("carallllar")
            || option.contains("annakarl")
            || option.contains("ghorrock")
            || option.contains("lassar")
            || option.contains("paddewwa")
            || option.contains("miscellania")
            || option.contains("grand exchange")
            || option.contains("glory")
            || option.contains("passage")
            || option.contains("combat")
            || option.contains("champions")
            || option.contains("myth")
            || option.contains("guild")       // All Skill/Guild Capes
            || option.contains("max")         // Max Cape submenus
            || option.contains("construction")
            || option.contains("rimmington")
            || option.contains("taverley")
            || option.contains("pollnivneach")
            || option.contains("hosidius")
            || option.contains("barbarian")
            || option.contains("outpost")
            || option.contains("burthorpe")
            || option.contains("corporeal")
            || option.contains("wintertodt")
            || option.contains("castle")      // Ring of Duelling
            || option.contains("ferox")       // Ring of Duelling
            || option.contains("edgeville")
            || option.contains("karamja")
            || option.contains("draynor")
            || option.contains("al kharid")
            || option.contains("digsite")
            || option.contains("nardah")
            || option.contains("bandit")
            || option.contains("zul-andra")
            || option.contains("vorkath")
            || option.contains("rellekka")
            || option.contains("mos le'harmless")
            || option.contains("brimhaven")
            || option.contains("uzzer")
            || option.contains("marim")
            || option.contains("morytania")
            || option.contains("burgh")
            || option.contains("fenkenstrain")
            || option.contains("canifis")
            || option.contains("breeleen")
            || option.contains("civitas")     // Varlamore
            || option.contains("aldarin")     // Varlamore
            || option.contains("cam torum")   // Varlamore
            || option.contains("salvage")
            || option.contains("tzhaar")
            || option.contains("mor ul rek")
            || option.contains("jaldraocht")
            || option.contains("apes atoll")
            || option.contains("weiss");

        // 3. SPELLBOOK TEXT FILTER (Standard, Ancients, Lunars, Arceuus spell clicks)
        boolean matchesSpellbookTeleport = option.contains("cast") && 
            (target.contains("teleport") || target.contains("home") || target.contains("tele") || target.contains("poh") || target.contains("brollop"));

        // Block everything instantly if a keyword or travel spell triggers
        if (matchesItemTeleportText || matchesCityText || matchesSpellbookTeleport)
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
