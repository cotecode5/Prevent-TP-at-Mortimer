package com.example;

import com.google.inject.Provides;
import javax.inject.Inject;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Widget;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
    name = "Prevent TP at Mortimer",
    description = "Prevents teleport and travel actions while inside Wyrmscraig Cavern",
    tags = {
        "teleport",
        "mortimer",
        "wyrmscraig",
        "boat",
        "cave",
        "safety"
    }
)
public class PreventTpAtMortimerPlugin extends Plugin
{
    private static final Logger log =
        LoggerFactory.getLogger(PreventTpAtMortimerPlugin.class);

    private static final int WYRMSCRAIG_CAVERN_REGION_ID = 5463;

    @Inject
    private Client client;

    @Inject
    private PreventTpAtMortimer config;

    @Override
    protected void startUp()
    {
        log.info("Prevent TP at Mortimer started!");
    }

    @Override
    protected void shutDown()
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

        if (!isInsideWyrmscraigCavern())
        {
            return;
        }

        if (!isTeleportAction(event))
        {
            return;
        }

        log.info(
            "BLOCKED TELEPORT: action={}, option='{}', target='{}', id={}, itemId={}, itemOp={}, param0={}, param1={}",
            event.getMenuAction(),
            event.getMenuOption(),
            event.getMenuTarget(),
            event.getId(),
            event.getItemId(),
            event.getItemOp(),
            event.getParam0(),
            event.getParam1()
        );

        event.consume();

        client.addChatMessage(
            ChatMessageType.GAMEMESSAGE,
            "",
            "Teleport blocked! You are inside Wyrmscraig Cavern.",
            null
        );
    }

    private boolean isInsideWyrmscraigCavern()
    {
        if (client.getLocalPlayer() == null)
        {
            return false;
        }

        return client.getLocalPlayer()
            .getWorldLocation()
            .getRegionID() == WYRMSCRAIG_CAVERN_REGION_ID;
    }

    private boolean isTeleportAction(MenuOptionClicked event)
    {
        String option = normalize(event.getMenuOption());
        String target = normalize(event.getMenuTarget());

        /*
         * ------------------------------------------------------------
         * 1. DIRECT TELEPORT / TRAVEL TEXT
         * ------------------------------------------------------------
         *
         * This catches things such as:
         *
         * Teleport
         * Tele
         * Teleport to...
         * Travel
         * Minigame teleport
         * Home
         */
        if (containsTeleportText(option) || containsTeleportText(target))
        {
            return true;
        }

        /*
         * ------------------------------------------------------------
         * 2. INVENTORY ITEM OPERATIONS
         * ------------------------------------------------------------
         *
         * RuneLite explicitly exposes whether this menu entry is
         * an item operation and which item generated it.
         */
        if (event.isItemOp())
        {
            return isTeleportItemOperation(event, option, target);
        }

        /*
         * ------------------------------------------------------------
         * 3. WIDGET / INTERFACE ACTIONS
         * ------------------------------------------------------------
         *
         * Spellbook, minigame interface, jewellery interfaces,
         * equipment interfaces, etc.
         */
        if (isWidgetAction(event.getMenuAction()))
        {
            return isTeleportWidgetAction(event, option, target);
        }

        /*
         * ------------------------------------------------------------
         * 4. GENERAL FALLBACK
         * ------------------------------------------------------------
         */
        return false;
    }

    private boolean isTeleportItemOperation(
        MenuOptionClicked event,
        String option,
        String target)
    {
        /*
         * Directly named teleport operations.
         */
        if (containsTeleportText(option) || containsTeleportText(target))
        {
            return true;
        }

        /*
         * These are common activation verbs used by teleport items.
         *
         * We only consider them for actual item operations.
         */
        if (option.equals("rub")
            || option.equals("break")
            || option.equals("tele")
            || option.equals("teleport")
            || option.equals("activate"))
        {
            return looksLikeTeleportItem(target);
        }

        return false;
    }

    private boolean isTeleportWidgetAction(
        MenuOptionClicked event,
        String option,
        String target)
    {
        /*
         * A widget action containing an explicit teleport/travel
         * destination is almost certainly a teleport action.
         */
        if (containsTeleportText(option) || containsTeleportText(target))
        {
            return true;
        }

        Widget widget = event.getWidget();

        if (widget == null)
        {
            return false;
        }

        /*
         * Inspect the actions belonging to the clicked widget.
         *
         * This is considerably more useful than assuming param1
         * is always a widget ID.
         */
        String[] actions = widget.getActions();

        if (actions != null)
        {
            for (String action : actions)
            {
                if (action == null)
                {
                    continue;
                }

                if (containsTeleportText(normalize(action)))
                {
                    return true;
                }
            }
        }

        /*
         * Spellbook actions commonly arrive as "Cast".
         *
         * If the target identifies a teleport spell, block it.
         */
        if (option.equals("cast"))
        {
            return looksLikeTeleportSpell(target);
        }

        return false;
    }

    private boolean isWidgetAction(MenuAction action)
    {
        switch (action)
        {
            case CC_OP:
            case CC_OP_LOW_PRIORITY:
            case WIDGET_FIRST_OPTION:
            case WIDGET_SECOND_OPTION:
            case WIDGET_THIRD_OPTION:
            case WIDGET_FOURTH_OPTION:
            case WIDGET_FIFTH_OPTION:
            case WIDGET_TARGET:
            case WIDGET_TYPE_1:
            case WIDGET_TYPE_4:
            case WIDGET_TYPE_5:
                return true;

            default:
                return false;
        }
    }

    private boolean containsTeleportText(String text)
    {
        if (text == null || text.isEmpty())
        {
            return false;
        }

        return text.contains("teleport")
            || text.equals("tele")
            || text.contains("tele ")
            || text.startsWith("tele ")
            || text.contains("minigame")
            || text.contains("travel")
            || text.contains("destination")
            || text.equals("home")
            || text.contains("home teleport")
            || text.contains("to house")
            || text.contains("poh")
            || text.contains("fairy ring")
            || text.contains("spirit tree")
            || text.contains("transport");
    }

    private boolean looksLikeTeleportSpell(String target)
    {
        if (target == null || target.isEmpty())
        {
            return false;
        }

        return target.contains("teleport")
            || target.contains("tele")
            || target.contains("home")
            || target.contains("house")
            || target.contains("to target")
            || target.contains("bake pie");
    }

    private boolean looksLikeTeleportItem(String target)
    {
        if (target == null || target.isEmpty())
        {
            return false;
        }

        /*
         * Item-name fallback.
         *
         * This is intentionally much smaller than the previous
         * giant list. We are looking for classes of items that
         * actually provide teleport functionality.
         */
        return target.contains("teleport")
            || target.contains("tablet")
            || target.contains("scroll")
            || target.contains("seed pod")
            || target.contains("ectophial")
            || target.contains("glory")
            || target.contains("wealth")
            || target.contains("duelling")
            || target.contains("passage")
            || target.contains("games necklace")
            || target.contains("combat bracelet")
            || target.contains("skills necklace")
            || target.contains("slayer ring")
            || target.contains("xeric")
            || target.contains("digsite pendant")
            || target.contains("construction cape")
            || target.contains("max cape")
            || target.contains("skillcape")
            || target.contains("achievement diary cape")
            || target.contains("quest point cape")
            || target.contains("music cape")
            || target.contains("arceuus")
            || target.contains("book");
    }

    private String normalize(String text)
    {
        if (text == null)
        {
            return "";
        }

        return text
            .toLowerCase()
            .replaceAll("<[^>]*>", "")
            .trim();
    }

    @Provides
    PreventTpAtMortimer provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PreventTpAtMortimer.class);
    }
}
