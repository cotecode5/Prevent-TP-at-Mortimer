package com.example;

import com.google.inject.Provides;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.api.Client;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
    name = "Prevent TP at Mortimer",
    description = "Exhaustive Text Filter: Blocks every single travel, cape, jewelry, item, and spell name in OSRS inside Mortimer's cave",
    tags = {"ironman", "teleport", "mortimer", "guard", "wyrmscraig", "cape", "ardougne", "block", "stop"}
)
public class PreventTpAtMortimerPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(PreventTpAtMortimerPlugin.class);

    @Inject
    private Client client;

    @Inject
    private PreventTpAtMortimer config;

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

        if (client.getLocalPlayer() == null || client.getLocalPlayer().getWorldLocation().getRegionID() != WYRMSCRAIG_CAVERN_REGION_ID)
        {
            return;
        }

        // Check both what the action is (option) and what item/spell is being clicked (target)
        String option = event.getMenuOption().toLowerCase();
        String target = event.getMenuTarget().toLowerCase();

        // 1. ALL ITEM ACTION VERBS, DEVICES & FAILSAPES
        boolean isTeleportVerb = option.contains("teleport") 
            || option.contains("tele")
            || option.contains("rub")          // All Jewelry (Glory, Wealth, Duelling, Combat, Games, Passage)
            || option.contains("break")        // All Teleport Tablets
            || option.contains("pod")          // Royal seed pod
            || option.contains("ectophial")    // Ectophial
            || option.contains("communion")    // Cam Torum bone/prayer devices
            || option.contains("destination")  // Chronicle / Teleport Matrices / Books
            || option.contains("minigame")     // Minigame/Grouping teleports
            || option.contains("poh")
            || option.contains("house")
            || target.contains("scroll")       // Blocks every single Teleport Scroll in the game
            || target.contains("tablet")       // Backup for any tablet text
            || target.contains("cloak")        // Ardougne Cloaks
            || target.contains("cape")         // All Skillcapes / Achievement Capes
            || target.contains("pendant")      // Digsite pendant / Strung rabbit foot variations
            || target.contains("talisman")     // Xeric's Talisman / Bloodkoch Talisman
            || target.contains("ring")         // Ring of Wealth / Duelling / Elements / Endurance
            || target.contains("amulet")       // Glory / Nature / Bounty
            || target.contains("necklace")     // Passage / Games / Anguish
            || target.contains("bracelet");    // Combat bracelet

        // 2. EXHAUSTIVE CITIES, VILLAGES, DUNGEONS, AND AREA DESTINATIONS
        boolean isTeleportLocation = option.contains("ardougne") || target.contains("ardougne")
            || option.contains("monastery") || target.contains("monastery") // Ardougne cloak monastery option
            || option.contains("farm") || target.contains("farm")           // Ardougne cloak farm patch option
            || option.contains("lumbridge") || target.contains("lumbridge")
            || option.contains("varrock") || target.contains("varrock")
            || option.contains("falador") || target.contains("falador")
            || option.contains("camelot") || target.contains("camelot")
            || option.contains("seers") || target.contains("seers")
            || option.contains("yanille") || target.contains("yanille")
            || option.contains("watchtower") || target.contains("watchtower")
            || option.contains("trollheim") || target.contains("trollheim")
            || option.contains("kourend") || target.contains("kourend")
            || option.contains("prifddinas") || target.contains("prifddinas")
            || option.contains("catherby") || target.contains("catherby")
            || option.contains("kharyrllr") || target.contains("kharyrllr")
            || option.contains("senntisten") || target.contains("senntisten")
            || option.contains("dareeyak") || target.contains("dareeyak")
            || option.contains("carallllar") || target.contains("carallllar")
            || option.contains("annakarl") || target.contains("annakarl")
            || option.contains("ghorrock") || target.contains("ghorrock")
            || option.contains("lassar") || target.contains("lassar")
            || option.contains("paddewwa") || target.contains("paddewwa")
            || option.contains("miscellania") || target.contains("miscellania")
            || option.contains("grand exchange") || target.contains("grand exchange")
            || option.contains("glory") || target.contains("glory")
            || option.contains("passage") || target.contains("passage")
            || option.contains("combat") || target.contains("combat")
            || option.contains("champions") || target.contains("champions")
            || option.contains("myth") || target.contains("myth")
            || option.contains("guild") || target.contains("guild")         // Crafting/Fishing/Cooking guilds etc
            || option.contains("max") || target.contains("max")             // Max cape features
            || option.contains("construction") || target.contains("construction")
            || option.contains("rimmington") || target.contains("rimmington")
            || option.contains("taverley") || target.contains("taverley")
            || option.contains("pollnivneach") || target.contains("pollnivneach")
            || option.contains("hosidius") || target.contains("hosidius")
            || option.contains("barbarian") || target.contains("barbarian")
            || option.contains("outpost") || target.contains("outpost")
            || option.contains("burthorpe") || target.contains("burthorpe")
            || option.contains("corporeal") || target.contains("corporeal")
            || option.contains("wintertodt") || target.contains("wintertodt")
            || option.contains("castle") || target.contains("castle")
            || option.contains("ferox") || target.contains("ferox")
            || option.contains("edgeville") || target.contains("edgeville")
            || option.contains("karamja") || target.contains("karamja")
            || option.contains("draynor") || target.contains("draynor")
            || option.contains("al kharid") || target.contains("al kharid")
            || option.contains("digsite") || target.contains("digsite")
            || option.contains("nardah") || target.contains("nardah")
            || option.contains("bandit") || target.contains("bandit")
            || option.contains("zul-andra") || target.contains("zul-andra")
            || option.contains("vorkath") || target.contains("vorkath")
            || option.contains("rellekka") || target.contains("rellekka")
            || option.contains("mos le'harmless") || target.contains("mos le'harmless")
            || option.contains("brimhaven") || target.contains("brimhaven")
            || option.contains("uzzer") || target.contains("uzzer")
            || option.contains("marim") || target.contains("marim")
            || option.contains("morytania") || target.contains("morytania")
            || option.contains("burgh") || target.contains("burgh")
            || option.contains("fenkenstrain") || target.contains("fenkenstrain")
            || option.contains("canifis") || target.contains("canifis")
            || option.contains("breeleen") || target.contains("breeleen")
            || option.contains("civitas") || target.contains("civitas")     // Varlamore
            || option.contains("aldarin") || target.contains("aldarin")     // Varlamore
            || option.contains("cam torum") || target.contains("cam torum") // Varlamore
            || option.contains("salvage") || target.contains("salvage")
            || option.contains("tzhaar") || target.contains("tzhaar")
            || option.contains("mor ul rek") || target.contains("mor ul rek")
            || option.contains("jaldraocht") || target.contains("jaldraocht")
            || option.contains("apes atoll") || target.contains("apes atoll")
            || option.contains("weiss") || target.contains("weiss");

        // 3. SPELLBOOK TEXT FILTER (Standard, Ancients, Lunars, Arceuus spell clicks)
        boolean matchesSpellbookTeleport = option.contains("cast") && 
            (target.contains("teleport") || target.contains("home") || target.contains("tele") || target.contains("poh") || target.contains("brollop"));

        // Kill the event immediately if a item action word, specific location keyword, or teleport spell is selected
        if (isTeleportVerb || isTeleportLocation || matchesSpellbookTeleport)
        {
            event.consume();
            client.addChatMessage(net.runelite.api.ChatMessageType.GAMEMESSAGE, "", "Teleport completely blocked by Prevent TP at Mortimer! Don't leave the boat behind.", null);
        }
    }

    @Provides
    PreventTpAtMortimer provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PreventTpAtMortimer.class);
    }
}
