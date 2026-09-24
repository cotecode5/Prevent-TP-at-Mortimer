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

        if (config.debugLogging())
        {
            debugMenuAction(event);
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
            "Teleport blocked — don't leave your boat behind!",
            null
        );
    }

    private void debugMenuAction(MenuOptionClicked event)
    {
        Widget widget = event.getWidget();

        log.info(
            "DEBUG MENU: action={}, option='{}', target='{}', id={}, itemId={}, itemOp={}, param0={}, param1={}, widget={}",
            event.getMenuAction(),
            clean(event.getMenuOption()),
            clean(event.getMenuTarget()),
            event.getId(),
            event.getItemId(),
            event.getItemOp(),
            event.getParam0(),
            event.getParam1(),
            describeWidget(widget)
        );

        if (widget == null)
        {
            return;
        }

        String[] actions = widget.getActions();

        if (actions == null)
        {
            log.info("DEBUG WIDGET: no widget actions");
            return;
        }

        StringBuilder actionList = new StringBuilder();

        for (int i = 0; i < actions.length; i++)
        {
            if (actions[i] == null)
            {
                continue;
            }

            if (actionList.length() > 0)
            {
                actionList.append(" | ");
            }

            actionList
                .append(i)
                .append(": ")
                .append(clean(actions[i]));
        }

        log.info(
            "DEBUG WIDGET ACTIONS: {}",
            actionList
        );
    }

    private String describeWidget(Widget widget)
    {
        if (widget == null)
        {
            return "null";
        }

        return "id="
            + widget.getId()
            + ", type="
            + widget.getType()
            + ", itemId="
            + widget.getItemId()
            + ", itemQuantity="
            + widget.getItemQuantity()
            + ", index="
            + widget.getIndex()
            + ", text='"
            + clean(widget.getText())
            + "'";
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
         * These actions are never teleport actions.
         *
         * This check is deliberately FIRST so a teleport-capable cape,
         * jewelry item, or other equipment can always be equipped or worn.
         */
        if (isEquipmentAction(option))
        {
            return false;
        }

        /*
         * Explicit teleport wording on the actual clicked option/target.
         */
        if (isExplicitTeleportText(option)
            || isExplicitTeleportText(target))
        {
            return true;
        }

        if (isTeleportItemAction(event, option, target))
        {
            return true;
        }

        if (isTeleportWidgetAction(event, option, target))
        {
            return true;
        }

        if (isTravelWorldAction(
            event.getMenuAction(),
            option,
            target))
        {
            return true;
        }

        return false;
    }

    private boolean isEquipmentAction(String option)
    {
        return option.equals("wear")
            || option.equals("wield")
            || option.equals("equip");
    }

    private boolean isTeleportItemAction(
        MenuOptionClicked event,
        String option,
        String target)
    {
        if (!event.isItemOp())
        {
            return false;
        }

        /*
         * Item operation 2 is the normal Wear/Wield/Equip operation.
         * Never block it.
         */
        if (event.getItemOp() == 2)
        {
            return false;
        }

        /*
         * Other equipment actions are also harmless.
         */
        if (isEquipmentAction(option))
        {
            return false;
        }

        /*
         * Teleport items that explicitly identify themselves.
         */
        if (isExplicitTeleportText(option)
            || isExplicitTeleportText(target))
        {
            return true;
        }

        /*
         * Common teleport-item activation actions.
         */
        if (option.equals("rub")
            || option.equals("break")
            || option.equals("activate")
            || option.equals("operate"))
        {
            return looksLikeTeleportItem(target);
        }

        /*
         * Teleport-capable capes/cloaks can use custom menu options
         * such as "monastery teleport" or "farm teleport".
         */
        if (looksLikeCapeOrCloak(target))
        {
            return isCapeTeleportOption(option);
        }

        return false;
    }

    private boolean isTeleportWidgetAction(
        MenuOptionClicked event,
        String option,
        String target)
    {
        MenuAction action = event.getMenuAction();

        /*
         * Never block actual equipment actions, regardless of what
         * other actions happen to exist on the same widget.
         */
        if (isEquipmentAction(option))
        {
            return false;
        }

        /*
         * Spell/widget targeting actions.
         */
        if (action == MenuAction.WIDGET_TARGET
            || action == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT
            || action == MenuAction.WIDGET_TARGET_ON_NPC
            || action == MenuAction.WIDGET_TARGET_ON_PLAYER
            || action == MenuAction.WIDGET_TARGET_ON_GROUND_ITEM
            || action == MenuAction.WIDGET_TARGET_ON_WIDGET)
        {
            if (looksLikeTeleportSpell(target)
                || isExplicitTeleportText(option)
                || isExplicitTeleportText(target))
            {
                return true;
            }
        }

        if (!isWidgetAction(action))
        {
            return false;
        }

        /*
         * Cape teleport actions can be CC_OP or CC_OP_LOW_PRIORITY.
         * Check the actual clicked option, never the entire widget's
         * available action list.
         */
        if (looksLikeCapeOrCloak(target)
            && isCapeTeleportOption(option))
        {
            return true;
        }

        /*
         * Normal spell casting.
         */
        if (option.equals("cast"))
        {
            return looksLikeTeleportSpell(target);
        }

        /*
         * Explicit teleport wording on the actual clicked action.
         */
        if (isExplicitTeleportText(option)
            || isExplicitTeleportText(target))
        {
            return true;
        }

        return false;
    }

    private boolean looksLikeCapeOrCloak(String target)
    {
        if (target == null || target.isEmpty())
        {
            return false;
        }

        return target.contains("cape")
            || target.contains("cloak");
    }

    private boolean isCapeTeleportOption(String option)
    {
        if (option == null || option.isEmpty())
        {
            return false;
        }

        return option.contains("teleport")
            || option.contains("monastery")
            || option.contains("farm")
            || option.contains("guild")
            || option.contains("house")
            || option.contains("tele")
            || option.contains("destination")
            || option.contains("travel");
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
            case WORLD_ENTITY_FIRST_OPTION:
            case WORLD_ENTITY_SECOND_OPTION:
            case WORLD_ENTITY_THIRD_OPTION:
            case WORLD_ENTITY_FOURTH_OPTION:
            case WORLD_ENTITY_FIFTH_OPTION:
                return isTravelWord(option)
                    || isTravelWord(target)
                    || isExplicitTeleportText(option)
                    || isExplicitTeleportText(target);

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
            || target.contains("ferox")
            || target.contains("cemetery")
            || target.contains("barrows")
            || target.contains("corporeal")
            || target.contains("paddewwa")
            || target.contains("canifis")
            || target.contains("battlefield");
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
