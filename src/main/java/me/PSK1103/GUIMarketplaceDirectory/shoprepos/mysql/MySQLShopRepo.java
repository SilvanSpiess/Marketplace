package me.PSK1103.GUIMarketplaceDirectory.shoprepos.mysql;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTListCompound;
import me.PSK1103.GUIMarketplaceDirectory.database.SQLDatabase;
import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;
import me.PSK1103.GUIMarketplaceDirectory.utils.CoreProtectLookup;
import me.PSK1103.GUIMarketplaceDirectory.utils.Metrics;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Matcher;

class ItemList {
    ItemStack item;
    int price;
    String qty;
    String name, customName;
    String customType;
    JSONObject extraInfo;

    ItemList(String itemName, String qty, int price) {
        this.name = itemName;
        this.qty = qty;
        this.price = price;
        this.customName = "";
        this.customType = "";
        this.extraInfo = new JSONObject();
        item = new ItemStack(Material.getMaterial(itemName));
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>(2);
        String qtyString = "";
        String[] parts = qty.split(":");
        if (Integer.parseInt(parts[0]) > 0)
            qtyString = parts[0] + " shulker";
        else if (Integer.parseInt(parts[1]) > 0)
            qtyString = parts[1] + " stack";
        else if (Integer.parseInt(parts[2]) > 0)
            qtyString = parts[2];

        else return;

        if(price < 0) {
            this.price = -1;
            lore.add(Component.text(ChatColor.GRAY + "Price hidden or variable"));
        }
        else if(price == 0) {
            lore.add(Component.text(ChatColor.GREEN + "Free!"));
        }
        else {
            lore.add(Component.text(ChatColor.translateAlternateColorCodes('&', "&6" + qtyString + " &ffor &3" + price + " diamond" + (price == 1 ? "" : "s"))));
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, /*ItemFlag.HIDE_ENCHANTS,*/ ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
    }

    ItemList(String itemName, ItemMeta meta) {
        this.name = itemName;
        this.customName = "";
        this.extraInfo = new JSONObject();
        item = new ItemStack(Material.getMaterial(itemName));
        item.setItemMeta(meta);
        if (meta.hasDisplayName())
            this.customName = ((TextComponent) meta.displayName()).content();
        qty = "";
        price = 0;
    }

    public static ItemStack getCustomItem(ItemStack item, String customType, Map<String, Object> extraInfo) {
        switch (customType) {
            case "head" -> {
                NBTItem head = new NBTItem(item);
                NBTCompound skull = head.addCompound("SkullOwner");
                skull.setString("Name", extraInfo.get("name").toString());
                skull.setString("Id", UUID.randomUUID().toString());
                NBTListCompound texture = skull.addCompound("Properties").getCompoundList("textures").addCompound();
                texture.setString("Signature", extraInfo.get("signature").toString());
                texture.setString("Value", extraInfo.get("value").toString());
                item = head.getItem();
            }
            case "potion", "tippedArrow" -> {
                PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
                Object integer1 = extraInfo.get("effect");
                PotionData base = new PotionData(PotionType.values()[integer1 instanceof String ? Integer.parseInt(integer1.toString()) : integer1 instanceof Integer ? Integer.parseInt(integer1.toString()) : Double.valueOf(integer1.toString()).intValue()], (Boolean) extraInfo.get("extended"), (Boolean) extraInfo.get("upgraded"));
                potionMeta.setBasePotionData(base);
                item.setItemMeta(potionMeta);
            }
            case "rocket" -> {
                NBTItem rocket = new NBTItem(item);
                NBTCompound fl = rocket.addCompound("Fireworks");
                double flno = (Double) extraInfo.get("flight");
                fl.setByte("Flight", (byte) flno);
                item = rocket.getItem();
                FireworkMeta fireworkMeta = (FireworkMeta) item.getItemMeta();
                List<Object> effects = (List<Object>) extraInfo.get("effects");
                if (effects != null && effects.size() > 0) {
                    List<FireworkEffect> fireworkEffects = new ArrayList<>();
                    effects.forEach(o -> {
                        Map<String, Object> effect = ((Map<String, Object>) o);
                        List<Color> colors = new ArrayList<>();
                        List<Color> fadeColors = new ArrayList<>();
                        ((List<Double>) effect.get("colors")).forEach(aDouble -> colors.add(Color.fromRGB(aDouble.intValue())));
                        ((List<Double>) effect.get("fadeColors")).forEach(aDouble -> fadeColors.add(Color.fromRGB(aDouble.intValue())));
                        FireworkEffect fireworkEffect = FireworkEffect.builder()
                                .flicker((Boolean) effect.get("flicker"))
                                .trail((Boolean) effect.get("trail"))
                                .with(FireworkEffect.Type.valueOf(effect.get("type").toString()))
                                .withColor(colors)
                                .withFade(fadeColors)
                                .build();
                        fireworkEffects.add(fireworkEffect);
                    });
                    fireworkMeta.addEffects(fireworkEffects);
                    item.setItemMeta(fireworkMeta);
                }
            }
            case "banner" -> {
                BannerMeta bannerMeta = (BannerMeta) item.getItemMeta();
                List<Object> patterns = (List<Object>) extraInfo.get("patterns");
                List<Pattern> bannerPatterns = new ArrayList<>();
                patterns.forEach(o -> {
                    Map<String, Object> pattern = (Map<String, Object>) o;
                    Pattern bannerPattern = new Pattern(DyeColor.valueOf(pattern.get("color").toString()), PatternType.valueOf(pattern.get("type").toString()));
                    bannerPatterns.add(bannerPattern);
                });
                bannerMeta.setPatterns(bannerPatterns);
                item.setItemMeta(bannerMeta);
            }
            case "shulker" -> {
                List<Map<String, Object>> contents = (List<Map<String, Object>>) extraInfo.get("contents");
                List<ItemStack> items = new ArrayList<>();
                contents.forEach(content -> {
                    ItemStack itemStack = new ItemStack(Material.valueOf(content.get("name").toString()), Double.valueOf(content.get("quantity").toString()).intValue());
                    if (content.containsKey("customName")) {
                        ItemMeta meta = itemStack.getItemMeta();
                        meta.displayName(Component.text(content.get("customName").toString()));
                    }
                    if (content.containsKey("customType")) {
                        getCustomItem(itemStack, content.get("customType").toString(), (Map<String, Object>) content.get("extraInfo"));
                    }

                    items.add(itemStack);

                });
                BlockStateMeta blockStateMeta = (BlockStateMeta) item.getItemMeta();
                ShulkerBox shulkerBox = (ShulkerBox) blockStateMeta.getBlockState();
                shulkerBox.getInventory().setContents(items.toArray(new ItemStack[0]));
                shulkerBox.update(true, false);
                blockStateMeta.setBlockState(shulkerBox);
                item.setItemMeta(blockStateMeta);
            }
            case "enchantedBook" -> {
                Map<String, Object> enchants = (Map<String, Object>) extraInfo.get("storedEnchants");
                EnchantmentStorageMeta esm = (EnchantmentStorageMeta) item.getItemMeta();
                enchants.forEach((enchant, integer) -> esm.addStoredEnchant(new EnchantmentWrapper(enchant), integer instanceof String ? Integer.parseInt(integer.toString()) : integer instanceof Integer ? Integer.parseInt(integer.toString()) : Double.valueOf(integer.toString()).intValue(), false));
                item.setItemMeta(esm);
            }
        }
        if (extraInfo.containsKey("enchants")) {
            Map<String,Object> codedEnchants = (Map<String, Object>) extraInfo.get("enchants");
            Map<Enchantment,Integer> enchants = new HashMap<>();
            codedEnchants.forEach((enchant, integer) -> enchants.put(new EnchantmentWrapper(enchant), integer instanceof String ? Integer.parseInt(integer.toString()) : integer instanceof Integer ? Integer.parseInt(integer.toString()) : Double.valueOf(integer.toString()).intValue()));
            item.addEnchantments(enchants);
        }
        return item;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(customName));
        item.setItemMeta(meta);
    }

    public void setExtraInfo(JSONObject extraInfo, String customType) {
        this.extraInfo = extraInfo;
        this.customType = customType;
        this.item = getCustomItem(item, customType, extraInfo);
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public void setPrice(int price) {
        this.price = price;
        ItemMeta meta = item.getItemMeta();
        List<String> l = new ArrayList<>(2);
        String qtyString = "";
        String[] parts = qty.split(":");
        if (Integer.parseInt(parts[0]) > 0)
            qtyString = parts[0] + " shulker";
        else if (Integer.parseInt(parts[1]) > 0)
            qtyString = parts[1] + " stack";
        else if (Integer.parseInt(parts[2]) > 0)
            qtyString = parts[2];

        else return;

        if(price < 0) {
            this.price = -1;
            l.add(ChatColor.GRAY + "Price hidden or variable");
        }
        else if(price == 0) {
            l.add(ChatColor.GREEN + "Free!");
        }
        else {
            l.add(ChatColor.translateAlternateColorCodes('&', "&6" + qtyString + " &ffor &3" + price + " diamond" + (price == 1 ? "" : "s")));
        }
        /*List<String> oldLore = meta.getLore();
        if(oldLore!=null)
            l.addAll(oldLore);*/
        List<Component> lore = new ArrayList<>();
        l.forEach(s -> lore.add(Component.text(s)));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, /*ItemFlag.HIDE_ENCHANTS,*/ ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
    }

    public ItemStack getItem() {
        return item;
    }
}

public class MySQLShopRepo implements ShopRepo {
    private final GUIMarketplaceDirectory plugin;
    private static final EnumSet<Material> materialsWithoutTextures = EnumSet.noneOf(Material.class);
    private final Map<String, String> shopOperatingUsers = new HashMap<>();
    private final Map<String, String> itemOperatingUsers = new HashMap<>();
    private final JSONParser parser = new JSONParser();

    static {
        materialsWithoutTextures.addAll(Arrays.asList(Material.LAVA, Material.WATER, Material.BUBBLE_COLUMN,Material.PISTON_HEAD,
                Material.MOVING_PISTON,Material.AIR,Material.ATTACHED_MELON_STEM, Material.ATTACHED_PUMPKIN_STEM));
    }

    private final Logger logger;

    public MySQLShopRepo( GUIMarketplaceDirectory plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        if (plugin.getCustomConfig().bstatsEnabled())
            addShopCountMetric();
    }

    @Override
    public String addShopAsOwner(String name, String desc, String owner, String uuid, String loc, String displayItem) {
        Material material = Material.matchMaterial(displayItem);
        if(material != null && !materialsWithoutTextures.contains(material)) {
            displayItem = "WRITTEN_BOOK";
        }
        return SQLDatabase.createShopAsOwner(name, desc, owner, uuid, loc, displayItem);
    }

    @Override
    public String addShop(String name, String desc, String owner, String uuid, String loc, String displayItem) {
        Material material = Material.matchMaterial(displayItem);
        if(material != null && !materialsWithoutTextures.contains(material)) {
            displayItem = "WRITTEN_BOOK";
        }
        String res = SQLDatabase.createShop(name, desc, owner, uuid, loc, displayItem);
        if (res != null)
            shopOperatingUsers.put(uuid, res);
        return res;
    }

    @Override
    public String getOwner(String key) {
        return SQLDatabase.getOwner(Integer.parseInt(key));
    }

    @Override
    public boolean getIsInitOwner(String uuid) {
        if (shopOperatingUsers.containsKey(uuid))
            return SQLDatabase.getIsInitOwner(uuid);
        return false;
    }

    @Override
    public void stopInitOwner(String uuid) {
        SQLDatabase.finishShopTransaction(uuid);
        shopOperatingUsers.remove(uuid);
    }

    @Override
    public int startAddingOwner(String uuid, String key) {
        if (shopOperatingUsers.containsKey(uuid) || shopOperatingUsers.containsValue(key))
            return 0;
        SQLDatabase.startAddingOwner(uuid, Integer.parseInt(key));
        shopOperatingUsers.put(uuid, key);
        return 1;
    }

    @Override
    public int startSettingDisplayItem(String uuid, String key) {
        if (shopOperatingUsers.containsKey(uuid) || shopOperatingUsers.containsValue(key))
            return 0;
        SQLDatabase.startSettingDisplayItem(uuid, Integer.parseInt(key));
        shopOperatingUsers.put(uuid, key);
        return 1;
    }

    public int startSettingLookupRadius(String uuid, String key) {
        if (shopOperatingUsers.containsKey(uuid) || shopOperatingUsers.containsValue(key))
            return 0;
        if(!plugin.getCustomConfig().useCoreProtect())
            return 0;
        SQLDatabase.startSettingLookupRadius(uuid, Integer.parseInt(key));
        shopOperatingUsers.put(uuid, key);
        return 1;
    }

    @Override
    public int startRemovingShop(String uuid, String key) {
        SQLDatabase.startRejectingShop(uuid, Integer.parseInt(key), false);
        shopOperatingUsers.put(uuid, key);
        return 1;
    }

    @Override
    public boolean getIsEditingShop(String uuid, String key) {
        return shopOperatingUsers.containsKey(uuid) || shopOperatingUsers.containsValue(key);
    }

    @Override
    public boolean getIsAddingOwner(String key) {
        return shopOperatingUsers.containsValue(key) && SQLDatabase.isAddingOwner(SQLDatabase.getEditType(Integer.parseInt(key)));
    }

    @Override
    public boolean getIsUserAddingOwner(String uuid) {
        return shopOperatingUsers.containsKey(uuid) && SQLDatabase.isAddingOwner(SQLDatabase.getEditType(uuid));
    }

    @Override
    public void addOwner(String uuid, OfflinePlayer player) {
        SQLDatabase.addOwner(uuid, player.getUniqueId().toString(), player.getName());
        shopOperatingUsers.remove(uuid);
    }

    @Override
    public void setDisplayItem(String uuid, String materialName) {
        SQLDatabase.setDisplayItem(uuid, materialName);
        shopOperatingUsers.remove(uuid);
    }

    public void setLookupRadius(String uuid, int radius) {
        SQLDatabase.setLookupRadius(Integer.parseInt(shopOperatingUsers.get(uuid)), radius);
        shopOperatingUsers.remove(uuid);
    }

    @Override
    public void saveShops() {
        // no need for this in db based storage
    }

    @Override
    public boolean isShopUnderEditOrAdd(String key) {
        return shopOperatingUsers.containsValue(key) || itemOperatingUsers.containsValue(key);
    }

    @Override
    public int initItemAddition(String uuid, String key, String name, ItemStack itemStack) {
        int res = 1;
        itemOperatingUsers.put(uuid, key);

        ItemList item = new ItemList(name, itemStack.getItemMeta());

        if (name.contains("SHULKER_BOX")) {
            if (itemStack.getItemMeta() instanceof BlockStateMeta im) {
                if (im.getBlockState() instanceof ShulkerBox shulker) {

                    List<Map<String, Object>> contents = new ArrayList<>();

                    for (int i = 0; i < 27; i++) {
                        ItemStack itemStack1 = shulker.getSnapshotInventory().getItem(i);
                        if (itemStack1 == null || itemStack1.getType() == Material.AIR)
                            continue;
                        String n = itemStack1.getType().getKey().getKey().toUpperCase(Locale.ROOT);

                        Map<String, Object> content = new HashMap<>();
                        content.put("name", itemStack1.getType().getKey().getKey().toUpperCase());
                        content.put("quantity", itemStack1.getAmount());

                        if (itemStack1.getType() == Material.PLAYER_HEAD) {
                            NBTItem nbtItem = new NBTItem(itemStack1);
                            NBTCompound skullOwner = nbtItem.getCompound("SkullOwner");
                            if (skullOwner != null) {
                                Map<String, Object> skullData = new HashMap<>();
                                skullData.put("name", skullOwner.getString("Name"));
                                skullData.put("value", skullOwner.getCompound("Properties").getCompoundList("textures").get(0).getString("Value"));
                                skullData.put("signature", skullOwner.getCompound("Properties").getCompoundList("textures").get(0).getString("Signature"));
                                content.put("extraInfo", skullData);
                                content.put("customType", "head");
                                item.customType = "head";
                            } else res = 2;
                        } else if (n.contains("POTION")) {
                            PotionMeta potionMeta = (PotionMeta) itemStack1.getItemMeta();
                            Map<String, Object> data = new HashMap<>();
                            PotionData potionType = potionMeta.getBasePotionData();
                            data.put("effect", Integer.valueOf(potionType.getType().ordinal()).toString());
                            data.put("upgraded", potionType.isUpgraded());
                            data.put("extended", potionType.isExtended());
                            content.put("extraInfo", data);
                            content.put("customType", "potion");
                        } else if (n.contains("FIREWORK_ROCKET")) {
                            FireworkMeta rocketMeta = (FireworkMeta) itemStack1.getItemMeta();
                            List<Object> effects = new ArrayList<>();
                            rocketMeta.getEffects().forEach(fireworkEffect -> {
                                Map<String, Object> effect = new HashMap<>();
                                effect.put("type", fireworkEffect.getType());
                                effect.put("flicker", fireworkEffect.hasFlicker());
                                effect.put("trail", fireworkEffect.hasTrail());
                                List<Integer> colors = new ArrayList<>();
                                List<Integer> fadeColors = new ArrayList<>();
                                fireworkEffect.getColors().forEach(color -> colors.add(color.asRGB()));
                                fireworkEffect.getFadeColors().forEach(fadeColor -> fadeColors.add(fadeColor.asRGB()));
                                effect.put("colors", colors);
                                effect.put("fadeColors", fadeColors);
                                effects.add(effect);
                            });
                            NBTItem nbtItem = new NBTItem(itemStack1);
                            Map<String, Object> fireworksData = new HashMap<>();
                            fireworksData.put("flight", nbtItem.getCompound("Fireworks").getByte("Flight"));
                            fireworksData.put("effects", effects);
                            content.put("extraInfo", fireworksData);
                            content.put("customType", "rocket");
                        } else if (n.contains("TIPPED_ARROW")) {
                            PotionMeta potionMeta = (PotionMeta) itemStack1.getItemMeta();
                            Map<String, Object> data = new HashMap<>();
                            PotionData potionType = potionMeta.getBasePotionData();
                            data.put("effect", Integer.valueOf(potionType.getType().ordinal()).toString());
                            data.put("upgraded", potionType.isUpgraded());
                            data.put("extended", potionType.isExtended());
                            content.put("extraInfo", data);
                            content.put("customType", "tippedArrow");
                        } else if (n.contains("BANNER")) {
                            BannerMeta bannerMeta = (BannerMeta) itemStack1.getItemMeta();
                            List<Object> patterns = new ArrayList<>();
                            bannerMeta.getPatterns().forEach(pattern -> {
                                Map<String, Object> patternData = new HashMap<>();
                                patternData.put("color", pattern.getColor().name().toUpperCase());
                                patternData.put("type", pattern.getPattern().name().toUpperCase());
                                patterns.add(patternData);
                            });
                            Map<String, Object> info = new HashMap<>();
                            info.put("patterns", patterns);
                            content.put("extraInfo", info);
                            content.put("customType", "banner");
                        } else if(itemStack1.getType() == Material.ENCHANTED_BOOK) {
                            Map<String,String> storedEnchants = new HashMap<>();
                            ((EnchantmentStorageMeta) itemStack.getItemMeta()).getStoredEnchants().forEach((enchantment, integer) -> storedEnchants.put(enchantment.getKey().getKey(),integer.toString()));
                            item.extraInfo = new JSONObject();
                            item.extraInfo.put("storedEnchants",storedEnchants);
                            item.customType = "enchantedBook";
                        }

                        contents.add(content);
                    }

                    item.extraInfo = new JSONObject();
                    item.extraInfo.put("contents", contents);
                    item.customType = "shulker";

                }
            }
        } else if (itemStack.getType() == Material.PLAYER_HEAD) {
            NBTItem nbtItem = new NBTItem(itemStack);
            NBTCompound skullOwner = nbtItem.getCompound("SkullOwner");
            if (skullOwner != null) {
                JSONObject skullData = new JSONObject();
                skullData.put("name", skullOwner.getString("Name"));
                skullData.put("value", skullOwner.getCompound("Properties").getCompoundList("textures").get(0).getString("Value"));
                skullData.put("signature", skullOwner.getCompound("Properties").getCompoundList("textures").get(0).getString("Signature"));
                item.extraInfo = skullData;
                item.customType = "head";
            } else res = 2;
        } else if (name.contains("POTION")) {
            PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
            JSONObject data = new JSONObject();
            PotionData potionType = potionMeta.getBasePotionData();
            data.put("effect", Integer.valueOf(potionType.getType().ordinal()).toString());
            data.put("upgraded", potionType.isUpgraded());
            data.put("extended", potionType.isExtended());
            item.extraInfo = data;
            item.customType = "potion";
        } else if (name.contains("FIREWORK_ROCKET")) {
            FireworkMeta rocketMeta = (FireworkMeta) itemStack.getItemMeta();
            List<Object> effects = new ArrayList<>();
            rocketMeta.getEffects().forEach(fireworkEffect -> {
                Map<String, Object> effect = new HashMap<>();
                effect.put("type", fireworkEffect.getType());
                effect.put("flicker", fireworkEffect.hasFlicker());
                effect.put("trail", fireworkEffect.hasTrail());
                List<Integer> colors = new ArrayList<>();
                List<Integer> fadeColors = new ArrayList<>();
                fireworkEffect.getColors().forEach(color -> colors.add(color.asRGB()));
                fireworkEffect.getFadeColors().forEach(fadeColor -> fadeColors.add(fadeColor.asRGB()));
                effect.put("colors", colors);
                effect.put("fadeColors", fadeColors);
                effects.add(effect);
            });
            NBTItem nbtItem = new NBTItem(itemStack);
            JSONObject fireworksData = new JSONObject();
            fireworksData.put("flight", nbtItem.getCompound("Fireworks").getByte("Flight"));
            fireworksData.put("effects", effects);
            item.extraInfo = fireworksData;
            item.customType = "rocket";
        } else if (name.contains("TIPPED_ARROW")) {
            PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
            JSONObject data = new JSONObject();
            PotionData potionType = potionMeta.getBasePotionData();
            data.put("effect", Integer.valueOf(potionType.getType().ordinal()).toString());
            data.put("upgraded", potionType.isUpgraded());
            data.put("extended", potionType.isExtended());
            item.extraInfo = data;
            item.customType = "tippedArrow";
        } else if (name.endsWith("BANNER")) {
            BannerMeta bannerMeta = (BannerMeta) itemStack.getItemMeta();
            List<Object> patterns = new ArrayList<>();
            bannerMeta.getPatterns().forEach(pattern -> {
                Map<String, Object> patternData = new HashMap<>();
                patternData.put("color", pattern.getColor().name().toUpperCase());
                patternData.put("type", pattern.getPattern().name().toUpperCase());
                patterns.add(patternData);
            });
            item.extraInfo = new JSONObject();
            item.extraInfo.put("patterns", patterns);
            item.customType = "banner";
        }
        else if(itemStack.getType() == Material.ENCHANTED_BOOK) {
            Map<String,String> storedEnchants = new HashMap<>();
            ((EnchantmentStorageMeta) itemStack.getItemMeta()).getStoredEnchants().forEach((enchantment, integer) -> storedEnchants.put(enchantment.getKey().getKey(),integer.toString()));
            item.extraInfo = new JSONObject();
            item.extraInfo.put("storedEnchants",storedEnchants);
            item.customType = "enchantedBook";
        }

        Map<Enchantment,Integer> enchants = itemStack.getEnchantments();
        if(!enchants.isEmpty()) {
            if(item.extraInfo==null)
                item.extraInfo = new JSONObject();
            Map<String,String> codedEnchants = new HashMap<>();
            enchants.forEach((enchantment, integer) -> codedEnchants.put(enchantment.getKey().getKey(),integer.toString()));
            item.extraInfo.put("enchants",codedEnchants);
        }
        SQLDatabase.startItemAddition(Integer.parseInt(key), uuid, item.name, item.customName, item.customType, item.qty, item.price, item.extraInfo);
        return res;
    }

    @Override
    public void initShopOwnerAddition(String uuid) {
        SQLDatabase.initShopOwnerAddition(uuid);
    }

    @Override
    public int getEditType(String uuid) {
        if (!shopOperatingUsers.containsKey(uuid))
            return -1;
        return SQLDatabase.getEditType(uuid);
    }

    @Override
    public void setQty(String qty, String uuid) {
        SQLDatabase.setQty(qty, uuid);
    }

    @Override
    public void setPrice(int price, String uuid) {
        itemOperatingUsers.remove(uuid);
        SQLDatabase.setPrice(price, uuid);
    }

    @Override
    public boolean isAddingItem(String uuid) {
        return itemOperatingUsers.containsKey(uuid);
    }

    @Override
    public void stopEditing(String uuid) {
        SQLDatabase.cancelItemAddition(uuid);
        itemOperatingUsers.remove(uuid);
    }

    @Override
    public boolean isShopOwner(String uuid, String key) {
        return SQLDatabase.isOwner(uuid, Integer.parseInt(key));
    }

    @Override
    public void approveShop(String key) {
        SQLDatabase.approveShop(Integer.parseInt(key));
    }

    @Override
    public void rejectShop(String uuid) {
        SQLDatabase.removeShop(uuid);
        shopOperatingUsers.remove(uuid);
    }

    @Override
    public void cancelRejectShop(String uuid) {
        SQLDatabase.finishShopTransaction(uuid);
        shopOperatingUsers.remove(uuid);
    }

    @Override
    public boolean isShopRejecting(String key) {
        return shopOperatingUsers.containsValue(key) && SQLDatabase.isRejectingShop(SQLDatabase.getEditType(Integer.parseInt(key)));
    }

    @Override
    public boolean isUserRejectingShop(String uuid) {
        return shopOperatingUsers.containsKey(uuid) && SQLDatabase.isRejectingShop(SQLDatabase.getEditType(uuid));
    }

    @Override
    public void addShopToRejectQueue(String uuid, String key) {
        shopOperatingUsers.put(uuid, key);
        SQLDatabase.startRejectingShop(uuid, Integer.parseInt(key), true);
    }

    @Override
    public void removeShop(String uuid) {
        SQLDatabase.removeShop(uuid);
        shopOperatingUsers.remove(uuid);
    }

    @Override
    public void cancelRemoveShop(String uuid) {
        SQLDatabase.finishShopTransaction(uuid);
        shopOperatingUsers.remove(uuid);
    }

    @Override
    public boolean isShopRemoving(String key) {
        return shopOperatingUsers.containsValue(key) && SQLDatabase.isRemovingShop(SQLDatabase.getEditType(Integer.parseInt(key)));
    }

    @Override
    public boolean isUserRemovingShop(String uuid) {
        return shopOperatingUsers.containsKey(uuid) && SQLDatabase.isRemovingShop(SQLDatabase.getEditType(uuid));
    }

    @Override
    public void addShopToRemoveQueue(String uuid, String key) {
        shopOperatingUsers.put(uuid, key);
        SQLDatabase.startRejectingShop(uuid, Integer.parseInt(key), true);
    }

    @Override
    public List<Map<String, String>> getShopDetails() {
        return SQLDatabase.getShopDetails(1);
    }

    @Override
    public List<Map<String, String>> getPendingShopDetails() {
        return SQLDatabase.getShopDetails(0);
    }

    @Override
    public List<Object> getShopInv(String key) {
        List<Map<String,Object>> inv = SQLDatabase.getInv(Integer.parseInt(key));
        List<Object> data = new ArrayList<>();
        List<ItemStack> inventory = new ArrayList<>();
        List<Integer> itemIds = new ArrayList<>();

        for (Map<String, Object> itemObj : inv) {
            ItemList item = new ItemList(itemObj.get("name").toString(), itemObj.get("qty").toString(), Integer.parseInt(itemObj.get("price").toString()));
            if (itemObj.get("customName") != null)
                item.setCustomName(itemObj.get("customName").toString());
            if (itemObj.get("extraInfo") != null) {
                try {
                    JSONObject extraData = ((JSONObject) parser.parse(itemObj.get("extraInfo").toString()));
                    item.setExtraInfo(extraData, itemObj.getOrDefault("customType", "").toString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }
            ItemMeta meta = item.item.getItemMeta();
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : meta.lore();
            lore.add(Component.text("Right click to find a better deal"));
            meta.lore(lore);
            item.item.setItemMeta(meta);
            inventory.add(item.item);
            itemIds.add(Integer.valueOf(itemObj.get("id").toString()));
        }
        data.add(inventory);
        data.add(itemIds);
        return data;
    }

    @Override
    public void findBetterAlternative(Player player, String key, int pos) {
        Map<String, Object> itemObj = SQLDatabase.getItem(pos);
        ItemList item = new ItemList(itemObj.get("name").toString(), itemObj.get("qty").toString(), Integer.parseInt(itemObj.get("price").toString()));
        if (itemObj.get("customName") != null)
            item.setCustomName(itemObj.get("customName").toString());
        if (itemObj.get("extraInfo") != null) {
            try {
                JSONObject extraData = ((JSONObject) parser.parse(itemObj.get("extraInfo").toString()));
                item.setExtraInfo(extraData, itemObj.getOrDefault("customType", "").toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        double value = 0;
        if(item.price<=0) {
            value = Integer.MAX_VALUE;
        }
        else {
            String[] parts1 = itemObj.get("qty").toString().split(":");
            if (Integer.parseInt(parts1[0]) > 0)
                value = Double.parseDouble(parts1[0]) * 1728;
            else if (Integer.parseInt(parts1[1]) > 0)
                value = Double.parseDouble(parts1[1]) * 64;
            else if (Integer.parseInt(parts1[2]) > 0)
                value = Double.parseDouble(parts1[2]);
            value /= item.price;
        }

        List<Map<String,Object>> res = SQLDatabase.getAlternatives(item.name);
        boolean found = false;
        for (Map<String, Object> resObj : res) {
            ItemList item1 = new ItemList(resObj.get("name").toString(), resObj.get("qty").toString(), Integer.parseInt(resObj.get("price").toString()));
            if (resObj.get("customName") != null)
                item1.setCustomName(itemObj.get("customName").toString());
            if (resObj.get("extraInfo") != null) {
                try {
                    JSONObject extraData = ((JSONObject) parser.parse(itemObj.get("extraInfo").toString()));
                    item1.setExtraInfo(extraData, itemObj.getOrDefault("customType", "").toString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            if(plugin.getCustomConfig().filterAlternatives()) {
                if (((item.item.getType() == Material.POTION && item1.item.getType() == Material.POTION) || (item.item.getType() == Material.LINGERING_POTION && item1.item.getType() == Material.LINGERING_POTION) || (item.item.getType() == Material.TIPPED_ARROW && item1.item.getType() == Material.TIPPED_ARROW)) && ((PotionMeta) item.item.getItemMeta()).getBasePotionData().getType().ordinal() != ((PotionMeta) item1.item.getItemMeta()).getBasePotionData().getType().ordinal())
                    continue;
                if (item.item.getType() == Material.ENCHANTED_BOOK && item.extraInfo.containsKey("storedEnchants") && item1.item.getType() == Material.ENCHANTED_BOOK && item1.extraInfo.containsKey("storedEnchants") && ((EnchantmentStorageMeta) item.item.getItemMeta()).getStoredEnchants().keySet().stream().noneMatch(enchantment -> ((EnchantmentStorageMeta) item1.item.getItemMeta()).getStoredEnchants().containsKey(enchantment)))
                    continue;
            }
                double val = 0;
            if (item1.price <= 0)
                val = Integer.MAX_VALUE;
            else {
                String[] parts = item1.qty.split(":");
                if (Integer.parseInt(parts[0]) > 0)
                    val = Double.parseDouble(parts[0]) * 1728;
                else if (Integer.parseInt(parts[1]) > 0)
                    val = Double.parseDouble(parts[1]) * 64;
                else if (Integer.parseInt(parts[2]) > 0)
                    val = Double.parseDouble(parts[2]);
                val /= item1.price;
            }
            if (val > value) {
                player.sendMessage(ChatColor.GOLD + resObj.get("shop_name").toString() + ChatColor.WHITE + " has a better deal: " + ((TextComponent) item1.getItem().lore().get(0)).content());
                found = true;
            }
        }
        if (!found) {
            player.sendMessage("No better alternatives found");
        }
    }

    @Override
    public String getShopName(String key) {
        return SQLDatabase.getShopName(Integer.parseInt(key));
    }

    @Override
    public List<Map<String, String>> getRefinedShopsByName(String searchKey) {
        return SQLDatabase.filterShopsByName(searchKey);
    }

    @Override
    public List<ItemStack> getMatchingItems(String key, String itemName) {
        List<Map<String, Object>> res = SQLDatabase.getMatchingItems(Integer.parseInt(key), itemName);
        List<ItemStack> items = new ArrayList<>();
        for (Map<String, Object> itemObj : res) {
            ItemList item = new ItemList(itemObj.get("name").toString(), itemObj.get("qty").toString(), Integer.parseInt(itemObj.get("price").toString()));
            if (itemObj.get("customName") != null)
                item.setCustomName(itemObj.get("customName").toString());
            if (itemObj.get("extraInfo") != null) {
                try {
                    JSONObject extraData = ((JSONObject) parser.parse(itemObj.get("extraInfo").toString()));
                    item.setExtraInfo(extraData, itemObj.getOrDefault("customType", "").toString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            items.add(item.item);
        }
        return items;
    }

    @Override
    public void removeMatchingItems(String key, String itemName) {
        SQLDatabase.removeMatchingItems(Integer.parseInt(key), itemName);
    }

    @Override
    public void removeItem(String key, ItemStack item) {
        List<Map<String,Object>> res = SQLDatabase.getMatchingItems(Integer.parseInt(key), item.getType().getKey().getKey());
        for (Map<String, Object> itemObj : res) {
            ItemList item1 = new ItemList(itemObj.get("name").toString(), itemObj.get("qty").toString(), Integer.parseInt(itemObj.get("price").toString()));
            if (((TextComponent) item.getItemMeta().lore().get(0)).content().equals(((TextComponent) item1.item.getItemMeta().lore().get(0)).content())) {
                SQLDatabase.removeItem(Integer.parseInt(itemObj.get("id").toString()));
            }
        }
    }

    @Override
    public List<Map<String, String>> getRefinedShopsByPlayer(String searchKey) {
        return SQLDatabase.filterShopsByPlayer(searchKey);
    }

    @Override
    public Map<String, Object> findItem(String searchKey) {
        List<ItemStack> items = new ArrayList<>();
        List<Map<String,String>> shopIds = new ArrayList<>();
        List<Map<String,Object>> res = SQLDatabase.findItem(searchKey);
        for (Map<String, Object> itemObj : res) {
            String shopName = itemObj.get("shop_name").toString(), shopId = itemObj.get("shop_id").toString(), shopLoc = itemObj.get("shop_loc").toString();
            ItemList item = new ItemList(itemObj.get("name").toString(), itemObj.get("qty").toString(), Integer.parseInt(itemObj.get("price").toString()));
            if (itemObj.get("customName") != null)
                item.setCustomName(itemObj.get("customName").toString());
            if (itemObj.get("extraInfo") != null) {
                try {
                    JSONObject extraData = ((JSONObject) parser.parse(itemObj.get("extraInfo").toString()));
                    item.setExtraInfo(extraData, itemObj.getOrDefault("customType", "").toString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            ItemStack itemToAdd = item.item.clone();
            ItemMeta meta = itemToAdd.getItemMeta();
            List<Component> lore = meta.lore() != null ? meta.lore() : new ArrayList<>();
            lore.add(Component.text(ChatColor.GREEN + "From " + shopName));
            lore.add(Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,plugin.getCustomConfig().getDefaultShopLocColor() + shopLoc)));
            lore.add(Component.text(ChatColor.YELLOW + "Right-click to view this shop"));
            meta.lore(lore);
            itemToAdd.setItemMeta(meta);
            items.add(itemToAdd);
            Map<String,String> shopData = new HashMap<>();
            shopData.put("name",shopName);
            shopData.put("id",shopId);
            shopIds.add(shopData);
        }
        Map<String,Object> searchResults = new HashMap<>();
        searchResults.put("items",items);
        searchResults.put("shops",shopIds);
        return searchResults;
    }

    private void addShopCountMetric() {
        plugin.getMetrics().addCustomChart(new Metrics.SingleLineChart("shop_items", SQLDatabase::getItemCount));
        plugin.getMetrics().addCustomChart(new Metrics.SingleLineChart("shops", SQLDatabase::getShopCount));
    }

    public void migrateJSONShops() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File shopFile = plugin.getShops();
            try {
                JSONParser parser = new JSONParser();
                assert shopFile != null;
                JSONObject data = (JSONObject) parser.parse(new FileReader(shopFile));
                JSONArray shopJSONs = ((JSONArray) data.get("shops"));
                JSONArray pShopJSONs = ((JSONArray) data.get("pendingShops"));
                for (Object json : shopJSONs) {
                    JSONObject shopJSON = (JSONObject) json;
                    String uuid = shopJSON.get("uuid").toString();
                    String owner = shopJSON.get("owner").toString();
                    String name = shopJSON.get("name").toString();
                    String desc = shopJSON.get("desc").toString();
                    String loc = shopJSON.get("loc").toString();
                    String displayItem = shopJSON.containsKey("displayItem") ? shopJSON.get("displayItem").toString() : null;
                    int ownerId = SQLDatabase.addPlayer(uuid, owner);
                    int shopId = SQLDatabase.insertShop(ownerId, name, desc, loc, displayItem, 1);
                    if (shopJSON.containsKey("owners")) {
                        Map<String, String> owners = new Gson().fromJson(shopJSON.get("owners").toString(),
                                new TypeToken<HashMap<String, String>>() {
                                }.getType());

                        owners.forEach((u, n) ->  {
                            int oid = SQLDatabase.addPlayer(u,n);
                            SQLDatabase.addToPlayerShopBridge(oid, shopId);
                        });
                    }
                    JSONArray itemsArray = ((JSONArray) shopJSON.get("items"));
                    for (Object o : itemsArray) {
                        try {
                            JSONObject itemJSON = ((JSONObject) o);
                            ItemList item = new ItemList(itemJSON.get("name").toString(), itemJSON.get("qty").toString(), Integer.parseInt(itemJSON.get("price").toString()));
                            if (itemJSON.get("customName") != null)
                                item.setCustomName(itemJSON.get("customName").toString());
                            if (itemJSON.containsKey("extraInfo")) {
                                JSONObject extraData = ((JSONObject) itemJSON.get("extraInfo"));
                                item.setExtraInfo(extraData, itemJSON.getOrDefault("customType","").toString());
                            }
                            SQLDatabase.insertItem(shopId, item.name, item.customName, item.customType, item.qty, item.price, item.extraInfo);
                        } catch (ClassCastException | NullPointerException e) {
                            if (e instanceof ClassCastException)
                                logger.severe("Malformed shops.json, cannot add item");
                            if (e instanceof NullPointerException)
                                logger.warning("Key value(s) missing, item won't be created");
                            e.printStackTrace();
                        }
                    }
                }
            } catch (ParseException | IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void lookupShop(Player player, String key) {
        String timeString = plugin.getCustomConfig().getLookupTime();
        java.util.regex.Pattern timePattern = java.util.regex.Pattern.compile("(\\d+[m|h|s|d|M])");
        Matcher timeMatcher = timePattern.matcher(timeString);
        int time = 0;
        try {
            while(timeMatcher.find()) {
                String type = timeMatcher.group(2);
                int value = Integer.parseInt(timeMatcher.group(1));
                time += value *
                        switch (type) {
                            case "s" -> 1;
                            case "m" -> 60;
                            case "h" -> 3600;
                            case "d" -> 86400;
                            case "M" -> 3592000;
                            default -> throw new IllegalStateException("Unexpected value: " + type);
                        };
            }
        }
        catch (Exception ignored) {
            time = 604800;
        }
        String loc = SQLDatabase.getShopLoc(Integer.parseInt(key));
        Matcher locMatcher = java.util.regex.Pattern.compile("(-?\\d+),(-?\\d+)").matcher(loc);
        int x,y,z;
        if(locMatcher.find()) {
            x = Integer.parseInt(locMatcher.group(1));
            z = Integer.parseInt(locMatcher.group(2));
        }
        else {
            x = player.getLocation().getBlockX();
            z = player.getLocation().getBlockZ();
        }
        y = player.getLocation().getBlockY();
        Location location = new Location(player.getWorld(), x, y, z);
        int radius = SQLDatabase.getLookupRadius(Integer.parseInt(key));
//        int interactions = plugin.getCustomConfig().getInteractions();
        List<String> owners = SQLDatabase.getShopOwners(Integer.parseInt(key));
        new CoreProtectLookup(plugin).lookup(player, owners, location, time, radius);
    }

    @Override
    public void lookupAllShops(Player player) {
        Map<Integer, String> shopIds = SQLDatabase.getAllShopIds();
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        shopIds.forEach((id, name) -> {
            class LookupThread implements Runnable{
                @Override
                public void run() {
                    player.sendMessage(ChatColor.GREEN + "For " + ChatColor.GOLD + name);
                    lookupShop(player, id.toString());
                }
            }
            executorService.submit(new LookupThread());
        });
        executorService.shutdown();
    }
}
