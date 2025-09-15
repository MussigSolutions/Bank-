package com.bankqtyhotkeys;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("bankqtyhotkeys")
public interface BankQuantityHotkeysConfig extends Config
{
    @ConfigItem(
        keyName = "enableShift",
        name = "Shift → 1",
        description = "Hold Shift to switch withdraw quantity to 1",
        position = 0
    )
    default boolean enableShift() { return true; }

    @ConfigItem(
        keyName = "enableCtrl",
        name = "Ctrl → All",
        description = "Hold Ctrl to switch withdraw quantity to All",
        position = 1
    )
    default boolean enableCtrl() { return true; }

    @ConfigItem(
        keyName = "enableAlt",
        name = "Alt → X",
        description = "Hold Alt to switch withdraw quantity to X (last used)",
        position = 2
    )
    default boolean enableAlt() { return true; }

    @ConfigItem(
        keyName = "restoreOnRelease",
        name = "Restore on release",
        description = "Return to your last manually selected quantity when the modifier is released",
        position = 3
    )
    default boolean restoreOnRelease() { return true; }
}
