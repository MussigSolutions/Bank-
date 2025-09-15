package com.bankqtyhotkeys;

import com.google.inject.Provides;
import java.awt.event.KeyEvent;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
        name = "Bank Quantity Hotkeys",
        description = "Hold Shift/Ctrl/Alt to temporarily switch bank withdraw quantity to 1 / All / X",
        tags = {"bank", "quantity", "hotkeys", "withdraw"}
)
public class BankQuantityHotkeysPlugin extends Plugin implements KeyListener
{
    enum Quantity { ONE, ALL, X }

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private KeyManager keyManager;
    @Inject private BankQuantityHotkeysConfig config;

    // We remember the user's last baseline (the button they manually selected) so we can restore on key release
    @Getter
    private Quantity baseline = Quantity.ALL;

    // Track whether we are currently overriding due to a held modifier
    private boolean overrideActive = false;
    private Quantity overrideWas = null;

    @Provides
    BankQuantityHotkeysConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(BankQuantityHotkeysConfig.class);
    }

    @Override
    protected void startUp()
    {
        keyManager.registerKeyListener(this);
        log.debug("Bank Quantity Hotkeys started");
    }

    @Override
    protected void shutDown()
    {
        keyManager.unregisterKeyListener(this);
        log.debug("Bank Quantity Hotkeys stopped");
    }

    private boolean bankOpen()
    {
        return client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER) != null;
    }

    private void clickBankQuantity(Quantity q)
    {
        final WidgetInfo target = switch (q)
        {
            case ONE -> WidgetInfo.BANK_QUANTITY_ONE;
            case ALL -> WidgetInfo.BANK_QUANTITY_ALL;
            case X   -> WidgetInfo.BANK_QUANTITY_X;
        };

        clientThread.invoke(() ->
        {
            Widget w = client.getWidget(target);
            if (w == null || w.isHidden())
            {
                return;
            }

            // "Op1" on a bank quantity button: CC_OP with op=1
            client.invokeMenuAction(
                "", "", 1,
                MenuAction.CC_OP.getId(),
                w.getId(), -1
            );
        });
    }

    private void applyOverrideIfNeeded(boolean shift, boolean ctrl, boolean alt)
    {
        if (!bankOpen())
        {
            return;
        }

        Quantity desired = null;
        if (shift && config.enableShift()) desired = Quantity.ONE;
        else if (ctrl && config.enableCtrl()) desired = Quantity.ALL;
        else if (alt && config.enableAlt()) desired = Quantity.X;

        if (desired != null)
        {
            if (!overrideActive)
            {
                overrideActive = true;
                overrideWas = baseline; // remember what we’ll restore to
            }
            clickBankQuantity(desired);
        }
        else if (overrideActive)
        {
            // Restore previous selection when the modifier is released
            overrideActive = false;
            if (overrideWas != null && config.restoreOnRelease())
            {
                clickBankQuantity(overrideWas);
            }
            overrideWas = null;
        }
    }

    // Listen for the player manually clicking a bank quantity button to keep "baseline" in sync
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked e)
    {
        if (e.getWidget() == null) return;
        int id = e.getWidget().getId();

        if (id == WidgetInfo.BANK_QUANTITY_ONE.getId()) baseline = Quantity.ONE;
        else if (id == WidgetInfo.BANK_QUANTITY_ALL.getId()) baseline = Quantity.ALL;
        else if (id == WidgetInfo.BANK_QUANTITY_X.getId()) baseline = Quantity.X;
        // Note: If you want to track 5/10 too, add them similarly and extend the enum if desired.
    }

    // --- KeyListener ---

    @Override
    public void keyTyped(KeyEvent e) { /* not used */ }

    @Override
    public void keyPressed(KeyEvent e)
    {
        // Use live modifier state so this also works when the key repeats
        boolean shift = (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0;
        boolean ctrl  = (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK)  != 0;
        boolean alt   = (e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK)   != 0;
        applyOverrideIfNeeded(shift, ctrl, alt);
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        boolean shift = (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0;
        boolean ctrl  = (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK)  != 0;
        boolean alt   = (e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK)   != 0;
        applyOverrideIfNeeded(shift, ctrl, alt);
    }
}
