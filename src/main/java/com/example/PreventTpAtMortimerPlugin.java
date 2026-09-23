package com.example;

import com.google.inject.Provides;
import javax.inject.Inject;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
    name = "Prevent TP at Mortimer",
    description = "Blocks teleport and travel actions inside Wyrmscraig Cavern",
    tags = {
        "teleport",
        "mortimer",
        "wyrmscraig",
        "boat",
        "cave",
        "safety",
        "travel"
    }
)
public class PreventTpAtMortimerPlugin extends Plugin
{
    private static final Logger log =
        LoggerFactory.getLogger(PreventTpAtMortimerPlugin.class);

    /*
     * Wyrmscraig Cavern is located in RuneLite region 10374.
     *
     * The cavern entrance, Mortimer area, and farthest reachable
     * normal cavern area were all confirmed to be in this region.
     *
     * The small quest-only area in region 10375 is intentionally
     * excluded because it is not part of the normal cavern area
     * used after unlocking the island.
     */
    private static final int WYRMSCRAIG_CAVERN_REGION_ID = 10374;

    @Inject
    private Client client;

    @Inject
    private PreventTpAtMortimer config;

    @Override
    protected void startUp()
    {
        log.info("Prevent TP at Mortimer started");
    }

    @Override
    protected void shutDown()
    {
        log.info("Prevent TP at Mortimer stopped");
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

        if (!isTeleportOrTravelAction(event))
        {
            return;
        }

        String option = clean(event.getMenuOption());
        String target = clean(event.getMenuTarget());

        log.info(
            "BLOCKED TRAVEL ACTION: action={}, option='{}', target='{}', id={}, itemId={}, itemOp={}, param0={}, param1={}",
            event.getMenuAction(),
            option,
            target,
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
            "Teleport/travel blocked - you are inside Wyrmscraig Cavern.",
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

    private boolean isTeleportOrTravelAction(MenuOptionClicked event)
    {
        String option = clean(event.getMenuOption());
        String target = clean(event.getMenuTarget());

        /*
         * Catch explicit teleport/travel wording first.
         */
        if (isExplicitTeleportText(option)
            || isExplicitTeleportText(target))
        {
            return true;
        }

        /*
         * Item actions such as Rub, Break, Activate, etc.
         */
        if (event.isItemOp())
        {
            return isTeleportItemAction(event, option, target);
        }

        /*
         * Widget actions such as spellbook/clickable interface
         * teleport actions.
         */
        if (isWidgetAction(event.getMenuAction()))
        {
            return isTeleportWidgetAction(event, option, target);
        }

        /*
         * World-object/NPC travel actions.
         */
        return isTravelWorldAction(event.getMenuAction(), option, target);
    }

    private boolean isTeleportItemAction(
        MenuOptionClicked event,
        String option,
        String target)
    {
        if (isExplicitTeleportText(option)
            || isExplicitTeleportText(target))
        {
            return true;
        }

        /*
         * Common teleport-item interactions.
         *
         * We intentionally inspect the target text instead of
         * relying only on item IDs, so new/unknown teleport items
         * can still be caught when their names identify them.
         */
        if (option.equals("rub")
            || option.equals("break")
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
         * Spellbook cast actions.
         */
        if (option.equals("cast"))
        {
            return looksLikeTeleportSpell(target);
        }

        Widget widget = event.getWidget();

        if (widget == null)
        {
            return false;
        }

        String[] actions = widget.getActions();

        if (actions == null)
        {
            return false;
        }

        /*
         * Check all actions belonging to the widget.
         * This helps catch interface-based teleport options
         * whose visible menu text may not itself contain
         * "teleport".
         */
        for (String action : actions)
        {
            if (action == null)
            {
                continue;
            }

            if (isExplicitTeleportText(clean(action)))
            {
                return true;
            }
        }

        return false;
    }

    private boolean isTravelWorldAction(
        MenuAction action,
        String option,
        String target)
    {
        switch (action)
        {
            case GAME_OBJECT_FIRST_OPTION:
            case GAME_OBJECT_SECOND_OPTION:
            case GAME_OBJECT_THIRD_OPTION:
            case GAME_OBJECT_FOURTH_OPTION:
            case GAME_OBJECT_FIFTH_OPTION:
            case NPC_FIRST_OPTION:
            case NPC_SECOND_OPTION:
            case NPC_THIRD_OPTION:
            case NPC_FOURTH_OPTION:
            case NPC_FIFTH_OPTION:
                return isTravelWord(option
```
