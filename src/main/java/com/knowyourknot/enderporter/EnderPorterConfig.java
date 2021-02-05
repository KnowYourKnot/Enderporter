package com.knowyourknot.enderporter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.oroarmor.config.Config;
import com.oroarmor.config.ConfigItem;
import com.oroarmor.config.ConfigItemGroup;

import net.fabricmc.loader.api.FabricLoader;

public class EnderPorterConfig extends Config {
    protected static final ConfigItemGroup MAIN_GROUP = new ConfigRoot();
    protected static final ConfigItemGroup[] MAIN_GROUP_ARRAY = {MAIN_GROUP};
    protected static final List<ConfigItemGroup> CONFIG = Arrays.asList(MAIN_GROUP_ARRAY);

    public EnderPorterConfig() {
        super(CONFIG, new File(FabricLoader.getInstance().getConfigDir().toFile(), "enderporter.json"), "enderporter");
        readConfigFromFile();
        saveConfigToFile();
    }

    public static class ConfigRoot extends ConfigItemGroup {
        public static final ConfigItem<Boolean> allowFreeTravel = new ConfigItem<>("allow_free_travel", false, "config.enderporter.allow_free_travel");
        public static final ConfigItem<Integer> blocksPerPearl = new ConfigItem<>("blocks_per_pearl", 30, "config.enderporter.blocks_per_pearl");
        public static final ConfigItem<Integer> travelLimit = new ConfigItem<>("travel_limit", -1, "config.enderporter.travel_limit");
        public static final ConfigItem<Integer> porterChargeTime = new ConfigItem<>("porter_charge_time", 20, "config.enderporter.porter_charge_time");
        public static final ConfigItem<Boolean> allowInterdimensionalTravel = new ConfigItem<>("allow_interdimensional_travel", true, "config.enderporter.allow_interdimensional_travel");
        public static final ConfigItem<Boolean> allowTeleportToVoid = new ConfigItem<>("allow_teleport_to_void", true, "config.enderporter.allow_teleport_to_void");
        protected static final ConfigItem<?>[] ROOT_CONFIG_ARRAY = {allowFreeTravel, blocksPerPearl, travelLimit, porterChargeTime, allowInterdimensionalTravel, allowTeleportToVoid};

        public ConfigRoot() {
            super(Arrays.asList(ROOT_CONFIG_ARRAY), "enderporter_config");
        }

    }
    
}
