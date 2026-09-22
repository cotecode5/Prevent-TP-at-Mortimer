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
    description = "Prevents all accidental teleports, capes, and cloaks inside Mortimer's cave to protect boat placement fees",
    tags = {"uim", "teleport", "mortimer", "guard", "wyrmscraig", "cape", "ardougne"}
)
public class PreventTpAtMortimerPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private PreventTpAtMortimerConfig config;

    // Region ID 5463 maps explicitly to Wyrmscraig Cavern
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

        // Comprehensive catch-all for text triggers used by spells, scroll books, jewelry, tabs, and capes
        boolean isTeleport = option.contains("teleport") 
            || option.contains("tele")
            || option.contains("monastery")   // Catches Ardougne Cloak monastery teleports
            || option.contains("ardougne")    // Catches Ardougne explicitly
            || option.contains("kandarin")    // Catches diary gear teleports
            || option.contains("royal seed pod")
            || option.contains("ectophial")
            || (option.contains("cast") && (target.contains("teleport") || target.contains("home")));

        if (isTeleport)
        {
            // Stop the action from sending to the game server completely
            event.consume();
            client.addChatMessage(net.runelite.api.ChatMessageType.GAMEMESSAGE, "", "Teleport blocked by Prevent TP at Mortimer! Your boat is safe.", null);
        }
    }

    @Provides
    PreventTpAtMortimerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PreventTpAtMortimerConfig.class);
    }
}
