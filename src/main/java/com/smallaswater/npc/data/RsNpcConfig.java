package com.smallaswater.npc.data;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.registry.EntityRegistry;
import cn.nukkit.registry.Registries;
import cn.nukkit.utils.Config;
import com.smallaswater.npc.RsNPC;
import com.smallaswater.npc.entitys.EntityRsNPC;
import com.smallaswater.npc.entitys.EntityRsNPCCustomEntity;
import com.smallaswater.npc.utils.ConfigUtils;
import com.smallaswater.npc.utils.Utils;
import com.smallaswater.npc.utils.exception.RsNpcConfigLoadException;
import com.smallaswater.npc.utils.exception.RsNpcLoadException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * @author lt_name
 */
@Slf4j
public class RsNpcConfig {

    public static final String NPC_CONFIG_VERSION_KEY = "ConfigVersion";

    private final Config config;
    private final String name;
    @Setter
    private String showName;

    @Setter
    @Getter
    private boolean nameTagAlwaysVisible;

    private final String levelName;
    private final Location location;

    private final ItemData itemData;

    @Setter
    @Getter
    private String skinName;
    @Setter
    private Skin skin;

    @Getter
    private int networkId;

    @Setter
    @Getter
    private float scale;

    @Setter
    private boolean lookAtThePlayer;

    @Setter
    private boolean enableEmote;
    private final ArrayList<String> emoteIDs = new ArrayList<>();
    @Setter
    private int showEmoteInterval;

    @Setter
    @Getter
    private boolean canProjectilesTrigger;

    private final ArrayList<String> cmds = new ArrayList<>();
    private final ArrayList<String> messages = new ArrayList<>();

    @Getter
    private final double baseMoveSpeed;

    @Getter
    private final ArrayList<Vector3> route = new ArrayList<>();
    @Setter
    @Getter
    private boolean enablePathfinding;

    @Getter
    private final double whirling;

    @Setter
    @Getter
    private boolean enabledDialogPages;
    @Setter
    @Getter
    private String dialogPagesName;

    // 自定义实体
    private boolean enableCustomEntity;
    private String customEntityIdentifier;
    private int customEntitySkinId;

    //自定义碰撞大小
    @Getter
    private boolean enableCustomCollisionSize;
    @Getter
    private float customCollisionSizeWidth;
    @Getter
    private float customCollisionSizeLength;
    @Getter
    private float customCollisionSizeHeight;

    private EntityRsNPC entityRsNpc;

    public RsNpcConfig(@NonNull String name, @NonNull Config config) throws RsNpcConfigLoadException, RsNpcLoadException {
        this.config = config;
        this.name = name;

        try {
            this.showName = config.getString("name");
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `name` error! Please check the config file!", e);
        }

        try {
            this.nameTagAlwaysVisible = config.getBoolean("nameTagAlwaysVisible", true);
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `nameTagAlwaysVisible` error! Please check the config file!", e);
        }

        try {
            HashMap<String, Object> map = config.get("spawn_point", new HashMap<>());
            String oldLevelName = (String) map.get("level");
            if (oldLevelName.endsWith("Dim0")) {
                this.levelName = oldLevelName.split(" ")[0];
            }
            else {
                this.levelName = oldLevelName;
            }
//            this.levelName = (String) map.get("level");
            Level level = Server.getInstance().getLevelByName(this.levelName);
            if (level == null) {
                throw new RsNpcLoadException("world doesnt exist：" + this.levelName + "Unable to load NPCs for the world");
            }
            this.location = new Location(
                    Utils.toDouble(map.get("x")),
                    Utils.toDouble(map.get("y")),
                    Utils.toDouble(map.get("z")),
                    Utils.toDouble(map.getOrDefault("yaw", 0D)),
                    0,
                    level
            );
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `spawn_point` error! Please check the config file!", e);
        }

        ItemData itemDataCache;
        try {
            itemDataCache = ItemData.of(config);
        } catch (Exception e) {
            itemDataCache = ItemData.empty();
            throw new RsNpcConfigLoadException("NPC config `handheldItem,headItem,chestItem...` error! Please check the config file!", e);
        }
        this.itemData = itemDataCache;

        try {
            this.skinName = config.getString("skin", "private_steve");
            if (!RsNPC.getInstance().getSkins().containsKey(this.skinName)) {
                RsNPC.getInstance().getLogger().warning("NPC: " + this.name + " Skin: " + this.skinName + " doesnt exist！switch to default skin！");
            }
            this.skin = RsNPC.getInstance().getSkinByName(this.skinName);
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `skin` error! Please check the config file!", e);
        }

        try {
            this.setNetworkId(config.getInt("networkId", -1));
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `networkId` error! Please check the config file!", e);
        }

        try {
            this.scale = (float) Utils.toDouble(config.get("scale", 1));
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `scale` error! Please check the config file!", e);
        }

        try {
            this.lookAtThePlayer = config.getBoolean("lookPlayer", true);
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `lookPlayer` error! Please check the config file!", e);
        }

        try {
            this.enableEmote = config.getBoolean("emoji.enable");
            this.emoteIDs.addAll(config.getStringList("emoji.id"));
            this.showEmoteInterval = config.getInt("emoji.interval", 10);
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `emoji` error! Please check the config file!", e);
        }

        try {
            this.canProjectilesTrigger = config.getBoolean("allow_projectile_trigger", true);
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `allow_projectile_trigger` error! Please check the config file!", e);
        }

        try {
            if (config.exists("click_command")) {
                if (!(config.get("click_command") instanceof List)) {
                    throw new RuntimeException("The content read by the `click_command` is not of List type! Please check if your config format is correct!");
                }
                this.cmds.addAll(config.getStringList("click_command"));
            }
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `click_command` error! Please check the config file!", e);
        }

        try {
            if (config.exists("send_message")) {
                if (!(config.get("send_message") instanceof List)) {
                    throw new RuntimeException("The content read by the `send_message` is not of List type! Please check if your config format is correct!");
                }
                this.messages.addAll(config.getStringList("send_message"));
            }
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `send_message` error! Please check the config file!", e);
        }

        try {
            this.baseMoveSpeed = Utils.toDouble(config.get("basic_speed", 1.0D));
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `basic_speed` error! Please check the config file!", e);
        }

        try {
            for (String string : config.getStringList("route")) {
                String[] s = string.split(":");
                this.route.add(new Vector3(Double.parseDouble(s[0]),
                        Double.parseDouble(s[1]),
                        Double.parseDouble(s[2])));
            }
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `route` error! Please check the config file!", e);
        }

        try {
            this.enablePathfinding = config.getBoolean("enable_assisted_pathfinding", true);
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `enable_assisted_pathfinding` error! Please check the config file!", e);
        }

        try {
            this.whirling = Utils.toDouble(config.get("rotate", 0.0));
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `rotate` error! Please check the config file!", e);
        }

        try {
            this.enabledDialogPages = RsNPC.getInstance().getDialogManager() != null && config.getBoolean("dialog.enable");
            this.dialogPagesName = config.getString("dialog.page", "demo");
            if (RsNPC.getInstance().getDialogManager().getDialogConfig(this.dialogPagesName) == null) {
                RsNPC.getInstance().getLogger().warning("NPC config `dialog.page` options failed to load! There is no name named" + this.dialogPagesName + "page.");
            }
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config dialog failed to load! Please check the configuration file", e);
        }

        try {
            this.enableCustomEntity = this.config.getBoolean("CustomEntity.enable", false);
            this.customEntityIdentifier = this.config.getString("CustomEntity.identifier", "RsNPC:Demo");
            this.customEntitySkinId = this.config.getInt("CustomEntity.skinId", 0);
            if (this.enableCustomEntity && Registries.ENTITY.getEntityNetworkId(this.customEntityIdentifier) == 0) {
                Registries.ENTITY.registerCustomEntity(RsNPC.getInstance(),
                        new EntityRegistry.CustomEntityDefinition(this.customEntityIdentifier, "", false, true),
                        EntityRsNPC.class);
            }
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `CustomEntity` failed to load! Please check the config file!", e);
        }

        try {
            this.enableCustomCollisionSize = this.config.getBoolean("CustomCollisionSize.enable", false);
            this.customCollisionSizeWidth = (float) this.config.getDouble("CustomCollisionSize.width", 0.6);
            this.customCollisionSizeLength = (float) this.config.getDouble("CustomCollisionSize.length", 0.6);
            this.customCollisionSizeHeight = (float) this.config.getDouble("CustomCollisionSize.height", 1.8);
        } catch (Exception e) {
            throw new RsNpcConfigLoadException("NPC config `CustomCollisionSize` failed to load! Please check the config file！", e);
        }

        //更新配置文件
        this.save();
        ConfigUtils.addDescription(this.config, RsNPC.getInstance().getNpcConfigDescription());
    }

    public void save() {
        this.config.set("name", this.showName);

        this.config.set("nameTagAlwaysVisible", this.nameTagAlwaysVisible);

        HashMap<String, Object> map = this.config.get("spawn_point", new HashMap<>());
        map.put("level", this.levelName);
        map.put("x", this.location.getX());
        map.put("y", this.location.getY());
        map.put("z", this.location.getZ());
        map.put("yaw", this.location.getYaw());
        this.config.set("spawn_point", map);

        Objects.requireNonNullElseGet(this.itemData, ItemData::empty).save(this.config);

        this.config.set("skin", this.skinName);

        this.config.set("networkId", this.networkId);

        this.config.set("scale", this.scale);

        this.config.set("lookPlayer", this.lookAtThePlayer);

        this.config.set("emoji.enable", this.enableEmote);
        this.config.set("emoji.id", this.emoteIDs);
        this.config.set("emoji.interval", this.showEmoteInterval);

        this.config.set("allow_projectile_trigger", this.canProjectilesTrigger);

        this.config.set("click_command", this.cmds);
        this.config.set("send_message", this.messages);

        this.config.set("basic_speed", this.baseMoveSpeed);

        ArrayList<String> list = new ArrayList<>();
        for (Vector3 vector3 : this.route) {
            list.add(vector3.getX() + ":" + vector3.getY() + ":" + vector3.getZ());
        }
        this.config.set("route", list);
        this.config.set("enable_assisted_pathfinding", this.enablePathfinding);

        this.config.set("rotate", this.whirling);

        this.config.set("dialog.enable", this.enabledDialogPages);
        this.config.set("dialog.page", this.dialogPagesName);

        this.config.set("CustomEntity.enable", this.enableCustomEntity);
        this.config.set("CustomEntity.identifier", this.customEntityIdentifier);
        this.config.set("CustomEntity.skinId", this.customEntitySkinId);

        this.config.set("CustomCollisionSize.enable", this.enableCustomCollisionSize);
        this.config.set("CustomCollisionSize.width", this.customCollisionSizeWidth);
        this.config.set("CustomCollisionSize.length", this.customCollisionSizeLength);
        this.config.set("CustomCollisionSize.height", this.customCollisionSizeHeight);

        this.config.save();
    }

    public void checkEntity() {
        this.location.setLevel(Server.getInstance().getLevelByName(this.levelName));
        if (this.location.getLevel() == null || this.location.getLevel().getProvider() == null) {
            RsNPC.getInstance().getLogger().error("level: " + this.levelName + " cant load the NPC: " + this.name);
            return;
        }
        if (this.location.getChunk() != null &&
                this.location.getChunk().isLoaded() &&
                !this.location.getLevel().getPlayers().isEmpty()) {
            if (this.entityRsNpc == null || this.entityRsNpc.isClosed()) {
                CompoundTag nbt = Entity.getDefaultNBT(location)
                        .putString("rsnpcName", this.name)
                        .putCompound("Skin", (new CompoundTag())
                                .putByteArray("Data", this.skin.getSkinData().data)
                                .putString("ModelId", this.skin.getSkinId()));
                if (this.enableCustomEntity && this.customEntityIdentifier != null) {
                    nbt.putInt("skinId", this.customEntitySkinId);
                    this.entityRsNpc = new EntityRsNPCCustomEntity(this.location.getChunk(), nbt, this);
                    EntityRsNPCCustomEntity entityRsNPC = (EntityRsNPCCustomEntity) this.entityRsNpc;
                    entityRsNPC.setIdentifier(this.customEntityIdentifier);
                }
                else {
                    this.entityRsNpc = new EntityRsNPC(this.location.getChunk(), nbt, this);
                    this.entityRsNpc.setSkin(this.getSkin());
                }
                this.entityRsNpc.setScale(this.scale);
                this.entityRsNpc.spawnToAll();
            }
            if (this.getRoute().isEmpty()) {
                this.entityRsNpc.setPosition(this.location);
            }
            if (!this.lookAtThePlayer) {
                this.entityRsNpc.setRotation(this.location.yaw, this.location.pitch);
            }
            this.entityRsNpc.setNameTag(this.showName /*VariableManage.stringReplace(null, this.showName, this)*/);
        }
    }

    public Config getConfig() {
        return this.config;
    }

    public String getName() {
        return this.name;
    }

    public String getShowName() {
        return this.showName;
    }

    public Location getLocation() {
        return this.location;
    }

    public Item getHand() {
        return this.itemData.getHand();
    }

    public void setHand(Item item) {
        this.itemData.hand = item;
        this.itemData.handString = Utils.item2String(item);
    }

    public Item[] getArmor() {
        return this.itemData.getArmor();
    }

    public void setArmor(Item[] items) {
        this.itemData.armor = items;
        for (int i = 0; i < items.length; i++) {
            this.itemData.armorString[i] = Utils.item2String(items[i]);
        }
    }

    public Skin getSkin() {
        return this.skin;
    }

    public boolean isLookAtThePlayer() {
        return this.lookAtThePlayer;
    }

    public boolean isEnableEmote() {
        return this.enableEmote;
    }

    public ArrayList<String> getEmoteIDs() {
        return this.emoteIDs;
    }

    public int getShowEmoteInterval() {
        return this.showEmoteInterval;
    }

    public ArrayList<String> getCmds() {
        return this.cmds;
    }

    public ArrayList<String> getMessages() {
        return this.messages;
    }

    public EntityRsNPC getEntityRsNpc() {
        return this.entityRsNpc;
    }

    public void setNetworkId(int networkId) {
        if (networkId <= 0) {
            networkId = -1;
        }
        this.networkId = networkId;
    }

    @EqualsAndHashCode(of = {"handString", "armorString"})
    public static class ItemData {

        private String handString = "minecraft:air:0";
        private String[] armorString = new String[]{"minecraft:air:0", "minecraft:air:0", "minecraft:air:0", "minecraft:air:0"};
        private Item hand;
        private Item[] armor = new Item[4];

        public static ItemData of(Config config) {
            ItemData itemData = new ItemData();

            itemData.handString = config.getString("handheldItem", "");
            itemData.armorString[0] = config.getString("headItem");
            itemData.armorString[1] = config.getString("chestItem");
            itemData.armorString[2] = config.getString("legItem");
            itemData.armorString[3] = config.getString("footItem");

            return itemData;
        }

        public static ItemData empty() {
            return new ItemData();
        }

        public void save(Config config) {
            config.set("handheldItem", this.handString);
            config.set("headItem", this.armorString[0]);
            config.set("chestItem", this.armorString[1]);
            config.set("legItem", this.armorString[2]);
            config.set("footItem", this.armorString[3]);
        }

        public Item getHand() {
            if (this.hand == null || this.hand.isNull()) {
                String string = this.handString;
                if (string.trim().isEmpty()) {
                    string = "minecraft:air:0";
                }
                String[] split = string.split(":");
                try {
                    if (split.length == 3) {
                        this.hand = Item.get(split[0] + ":" + split[1], 0, Integer.parseInt(split[2]));
                    }
                    else {
                        this.hand = Item.get(split[0] + ":" + split[1]);
                    }
                } catch (Exception e) {
                    this.hand = Item.get(Block.INFO_UPDATE);
                    RsNPC.getInstance().getLogger().warning("NPC config `handheldItem` " + string + " error! Please check the config file!");
                }
            }
            return this.hand;
        }

        public Item[] getArmor() {
            for (int i = 0; i < this.armor.length; i++) {
                if (this.armor[i] == null || this.armor[i].isNull()) {
                    String string = this.armorString[i];
                    if (string.trim().isEmpty()) {
                        string = "minecraft:air:0";
                    }
                    String[] split = string.split(":");
                    try {
                        if (split.length == 3) {
                            this.armor[i] = Item.get(split[0] + ":" + split[1], 0, Integer.parseInt(split[2]));
                        }
                        else {
                            this.armor[i] = Item.get(split[0] + ":" + split[1]);
                        }
                    } catch (Exception e) {
                        this.armor[i] = Item.get(Block.INFO_UPDATE);
                        RsNPC.getInstance().getLogger().warning("NPC config `chestItem...` " + string + " error! Please check the config file!");
                    }
                }
            }
            return this.armor;
        }

    }

}
