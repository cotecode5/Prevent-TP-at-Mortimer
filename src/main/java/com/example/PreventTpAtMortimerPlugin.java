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
        "travel"
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
         * 1. Directly named teleport/travel actions.
         */
        if (isExplicitTeleportText(option)
            || isExplicitTeleportText(target))
        {
            return true;
        }

        /*
         * 2. Inventory/item actions.
         *
         * This catches things such as:
         * Rub glory
         * Rub ring
         * Break tablet
         * Activate seed pod
         * etc.
         */
        if (event.isItemOp())
        {
            return isTeleportItemAction(event, option, target);
        }

        /*
         * 3. Spellbook/interface actions.
         */
        if (isWidgetAction(event.getMenuAction()))
        {
            return isTeleportWidgetAction(event, option, target);
        }

        /*
         * 4. A few travel actions can be attached directly to
         * game objects/NPCs rather than items or widgets.
         */
        return isTravelWorldAction(event.getMenuAction(), option, target);
    }

    private boolean isTeleportItemAction(
        MenuOptionClicked event,
        String option,
        String target)
    {
        /*
         * Explicit teleport wording always wins.
         */
        if (isExplicitTeleportText(option)
            || isExplicitTeleportText(target))
        {
            return true;
        }

        /*
         * These are the common OSRS operations used by teleport items.
         *
         * We deliberately do NOT block every item operation because
         * that would also prevent normal actions such as Eat, Drink,
         * Wield, Wear, Drop, etc.
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
         * Spellbook teleports generally appear as:
         *
         * Cast -> [spell name]
         */
        if (option.equals("cast"))
        {
            return looksLikeTeleportSpell(target);
        }

        /*
         * Check the actual widget's available actions.
         * This gives us another layer of protection when the visible
         * menu option itself doesn't contain the word teleport.
         */
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
        /*
         * Only inspect actual world/object/NPC interaction actions.
         * This avoids blocking unrelated interface clicks.
         */
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
                return isTravelWord(option)
                    || isTravelWord(target);

            default:
                return false;
        }
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

    private boolean isExplicitTeleportText(String text)
    {
        if (text == null || text.isEmpty())
        {
            return false;
        }

        return text.contains("teleport")
            || text.equals("tele")
            || text.startsWith("tele ")
            || text.contains(" teleport")
            || text.contains("home teleport")
            || text.contains("minigame teleport")
            || text.contains("destination")
            || text.contains("to house")
            || text.equals("poh")
            || text.contains("fairy ring")
            || text.contains("spirit tree")
            || text.contains("transport");
    }

    private boolean isTravelWord(String text)
    {
        if (text == null || text.isEmpty())
        {
            return false;
        }

        return text.equals("travel")
            || text.startsWith("travel ")
            || text.equals("transport")
            || text.startsWith("transport ")
            || text.equals("teleport")
            || text.equals("tele")
            || text.equals("leave")
            || text.equals("depart")
            || text.equals("destination");
    }

    private boolean looksLikeTeleportSpell(String target)
    {
        if (target == null || target.isEmpty())
        {
            return false;
        }

        return target.contains("teleport")
            || target.contains("tele ")
            || target.equals("tele")
            || target.contains("home")
            || target.contains("house")
            || target.contains("varrock")
            || target.contains("lumbridge")
            || target.contains("falador")
            || target.contains("camelot")
            || target.contains("ardougne")
            || target.contains("watchtower")
            || target.contains("trollheim")
            || target.contains("kourend")
            || target.contains("edgeville")
            || target.contains("kharyrll")
            || target.contains("senntisten")
            || target.contains("barbarian")
            || target.contains("ape atoll")
            || target.contains("carrallangar")
            || target.contains("ferox");
    }

    private boolean looksLikeTeleportItem(String target)
    {
        if (target == null || target.isEmpty())
        {
            return false;
        }

        return target.contains("teleport")
            || target.contains("tablet")
            || target.contains("scroll")
            || target.contains("seed pod")
            || target.contains("ectophial")
            || target.contains("amulet of glory")
            || target.contains("ring of wealth")
            || target.contains("ring of dueling")
            || target.contains("ring of duel")
            || target.contains("games necklace")
            || target.contains("combat bracelet")
            || target.contains("skills necklace")
            || target.contains("slayer ring")
            || target.contains("ring of the elements")
            || target.contains("xeric")
            || target.contains("digsite pendant")
            || target.contains("construction cape")
            || target.contains("max cape")
            || target.contains("skillcape")
            || target.contains("achievement diary cape")
            || target.contains("quest point cape")
            || target.contains("music cape")
            || target.contains("arceuus")
            || target.contains("book of the dead")
            || target.contains("book of dead")
            || target.contains("occult altar")
            || target.contains("royal seed pod");
    }

    private String clean(String text)
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
