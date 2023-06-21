package me.PSK1103.GUIMarketplaceDirectory.shoprepos.json;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.TropicalFish;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class ItemList {

    public interface BlockBuilder {
        BlockData getBlockData(String string);
        PlayerProfile createPlayerProfile(UUID uniqueId, String name);  
    }

    ItemStack item;
    int price;
    String qty;
    String name, customName;
    String customType;
    BlockBuilder blockBuilder;
    Map<String, Object> extraInfo;

    ItemList() {
    }

    ItemList(String itemName, String qty, int price, BlockBuilder blockBuilder) {
        this.name = itemName;
        this.qty = qty;
        this.price = price;
        this.customName = "";
        this.customType = "";
        this.blockBuilder = blockBuilder;
        this.extraInfo = new HashMap<>(0);
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

    ItemList(String itemName, ItemMeta meta, BlockBuilder blockBuilder) {
        this.name = itemName;
        this.customName = "";
        this.blockBuilder = blockBuilder;
        this.extraInfo = new HashMap<>(0);
        item = new ItemStack(Material.getMaterial(itemName));
        item.setItemMeta(meta);
        if (meta.hasDisplayName())
            //this.customName = ((TextComponent) meta.displayName()).content();
            this.customName = meta.getDisplayName();
        qty = "";
        price = 0;
    }

    /* 
     * Custom Items such as heads, potions, tipped arrows, rockets, banners, shulkers, enchanted books and enchants
     */
    public static ItemStack getCustomItem(ItemStack item, String customType, Map<String, Object> extraInfo, BlockBuilder blockBuilder) {
        switch (customType) {
            case "head" -> {
                SkullMeta skullmeta = (SkullMeta) item.getItemMeta();
                if(extraInfo.containsKey("name") && !(extraInfo.get("name").toString().equals("null")) && extraInfo.containsKey("profileId")){
                    skullmeta.setOwnerProfile(blockBuilder.createPlayerProfile(UUID.fromString(extraInfo.get("profileId").toString()), extraInfo.get("name").toString()));
                }
                else if(extraInfo.containsKey("name") && !(extraInfo.get("name").toString().equals("null"))) {
                    skullmeta.setOwner(extraInfo.get("name").toString());
                }
                
                PlayerProfile playerProfile = skullmeta.getOwnerProfile();
                if(extraInfo.containsKey("skin") && !(extraInfo.get("skin").toString().equals("null"))) {
                    try {
                        PlayerTextures playerTextures = playerProfile.getTextures();
                        URL skinUrl = new URL(extraInfo.get("skin").toString());
                        playerTextures.setSkin(skinUrl);
                        playerProfile.setTextures(playerTextures);
                        
                    } catch (MalformedURLException e) {
                        //e.printStackTrace();
                    }
                }
                skullmeta.setOwnerProfile(playerProfile);
                item.setItemMeta(skullmeta);
            }
            case "potion", "tippedArrow" -> {
                PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
                Object integer1 = extraInfo.get("effect");
                PotionData base = new PotionData(PotionType.values()[integer1 instanceof String ? Integer.parseInt(integer1.toString()) : integer1 instanceof Integer ? Integer.parseInt(integer1.toString()) : Double.valueOf(integer1.toString()).intValue()], (Boolean) extraInfo.get("extended"), (Boolean) extraInfo.get("upgraded"));
                potionMeta.setBasePotionData(base);
                item.setItemMeta(potionMeta);
            }
            case "rocket" -> {
                FireworkMeta fireworkMeta = (FireworkMeta) item.getItemMeta();
                try{
                    fireworkMeta.setPower(Integer.valueOf(extraInfo.get("flight").toString()));
                }catch(NumberFormatException e){
                    fireworkMeta.setPower(1);
                }
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
                        meta.setDisplayName(String.valueOf(content.get("customName")));
                    }
                    if (content.containsKey("customType")) {
                        getCustomItem(itemStack, content.get("customType").toString(), (Map<String, Object>) content.get("extraInfo"), blockBuilder);
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
            case "axolotl" -> {
                AxolotlBucketMeta axolotlMeta = (AxolotlBucketMeta) item.getItemMeta();
                axolotlMeta.setVariant(Axolotl.Variant.valueOf(extraInfo.get("type").toString()));
                item.setItemMeta(axolotlMeta);
            } 
            case "writtenBook" -> {
                BookMeta writtenBookMeta = (BookMeta) item.getItemMeta();
                if (extraInfo.containsKey("author")) {
                    writtenBookMeta.setAuthor(extraInfo.get("author").toString());
                }
                if (extraInfo.containsKey("generation")) {
                    writtenBookMeta.setGeneration(BookMeta.Generation.valueOf(extraInfo.get("generation").toString()));
                }
                if (extraInfo.containsKey("title")) {
                    writtenBookMeta.setTitle(extraInfo.get("title").toString());
                }
                item.setItemMeta(writtenBookMeta);
            }
            case "crossbow" -> {
                CrossbowMeta CrossbowMeta = (CrossbowMeta) item.getItemMeta();
                ItemStack arrow = new ItemStack(Material.valueOf(extraInfo.get("loaded").toString()));
                if (extraInfo.containsKey("tipped") || arrow.getType() == Material.TIPPED_ARROW) {
                    PotionMeta arrowMeta = (PotionMeta) arrow.getItemMeta();
                    arrowMeta.setColor(Color.fromRGB(Integer.valueOf(extraInfo.get("tipped").toString())));
                    arrow.setItemMeta(arrowMeta);
                }
                CrossbowMeta.addChargedProjectile(arrow);
                item.setItemMeta(CrossbowMeta);
            } 
            case "leatherArmor" -> {
                LeatherArmorMeta LeatherArmorMeta = (LeatherArmorMeta) item.getItemMeta();
                LeatherArmorMeta.setColor(Color.fromRGB(Integer.valueOf(extraInfo.get("color").toString())));
                item.setItemMeta(LeatherArmorMeta);
            }
            case "armor" -> {
                ArmorMeta armorMeta = (ArmorMeta) item.getItemMeta();
                if(extraInfo.containsKey("trimPattern") && extraInfo.containsKey("trimMaterial")) {
                    TrimPattern trimPattern;
                    switch (extraInfo.get("trimPattern").toString()) {
                        case "minecraft:coast":
                            trimPattern = TrimPattern.COAST;
                            break;
                        case "minecraft:dune":
                            trimPattern = TrimPattern.DUNE;
                            break;
                        case "minecraft:eye":
                            trimPattern = TrimPattern.EYE;
                            break;
                        case "minecraft:host":
                            trimPattern = TrimPattern.HOST;
                            break;
                        case "minecraft:raiser":
                            trimPattern = TrimPattern.RAISER;
                            break;
                        case "minecraft:rib":
                            trimPattern = TrimPattern.RIB;
                            break;
                        case "minecraft:sentry":
                            trimPattern = TrimPattern.SENTRY;
                            break;
                        case "minecraft:shaper":
                            trimPattern = TrimPattern.SHAPER;
                            break;
                        case "minecraft:silence":
                            trimPattern = TrimPattern.SILENCE;
                            break;
                        case "minecraft:snout":
                            trimPattern = TrimPattern.SNOUT;
                            break;
                        case "minecraft:spire":
                            trimPattern = TrimPattern.SPIRE;
                            break;
                        case "minecraft:tide":
                            trimPattern = TrimPattern.TIDE;
                            break;
                        case "minecraft:vex":
                            trimPattern = TrimPattern.VEX;
                            break;
                        case "minecraft:ward":
                            trimPattern = TrimPattern.WARD;
                            break;
                        case "minecraft:wayfinder":
                            trimPattern = TrimPattern.WAYFINDER;
                            break;
                        case "minecraft:wild":
                            trimPattern = TrimPattern.WILD;
                            break;
                        default:
                            trimPattern = TrimPattern.COAST;
                            break;
                    }
                    TrimMaterial trimMaterial;
                    switch (extraInfo.get("trimMaterial").toString()) {
                        case "minecraft:amethyst":
                            trimMaterial = TrimMaterial.AMETHYST;
                            break;
                        case "minecraft:copper":
                            trimMaterial = TrimMaterial.COPPER;
                            break;
                        case "minecraft:diamond":
                            trimMaterial = TrimMaterial.DIAMOND;
                            break;
                        case "minecraft:emerald":
                            trimMaterial = TrimMaterial.EMERALD;
                            break;
                        case "minecraft:gold":
                            trimMaterial = TrimMaterial.GOLD;
                            break;
                        case "minecraft:iron":
                            trimMaterial = TrimMaterial.IRON;
                            break;
                        case "minecraft:lapis":
                            trimMaterial = TrimMaterial.LAPIS;
                            break;
                        case "minecraft:netherite":
                            trimMaterial = TrimMaterial.NETHERITE;
                            break;
                        case "minecraft:quartz":
                            trimMaterial = TrimMaterial.QUARTZ;
                            break;
                        case "minecraft:redstone":
                            trimMaterial = TrimMaterial.REDSTONE;
                            break;
                        default:
                            trimMaterial = TrimMaterial.AMETHYST;
                            break;
                    }
                    armorMeta.setTrim(new ArmorTrim(trimMaterial, trimPattern));
                    item.setItemMeta(armorMeta);
                }
                if(extraInfo.containsKey("color")) {
                    ColorableArmorMeta colorableArmorMeta = (ColorableArmorMeta) item.getItemMeta();
                    colorableArmorMeta.setColor(Color.fromRGB(Integer.valueOf(extraInfo.get("color").toString())));
                    item.setItemMeta(colorableArmorMeta);
                }
                
            }
            case "filledMap" -> {
                MapMeta mapMeta = (MapMeta) item.getItemMeta();
                mapMeta.setMapId(Integer.valueOf(extraInfo.get("id").toString()));
                item.setItemMeta(mapMeta);
            }
            case "goatHorn" -> {
                MusicInstrumentMeta goatHornMeta = (MusicInstrumentMeta) item.getItemMeta();
                goatHornMeta.setInstrument(MusicInstrument.getByKey(NamespacedKey.fromString(extraInfo.get("instrument").toString())));
                item.setItemMeta(goatHornMeta);
            }
            case "suspiciousStew" -> {
                SuspiciousStewMeta suspiciousStewMeta = (SuspiciousStewMeta) item.getItemMeta();
                suspiciousStewMeta.addCustomEffect(new PotionEffect(PotionEffectType.getByName(extraInfo.get("effect").toString()), 1, 1), true);
                item.setItemMeta(suspiciousStewMeta);
            }
            case "tropicalFishBucket" -> {
                TropicalFishBucketMeta tropicalFishBucketMeta = (TropicalFishBucketMeta) item.getItemMeta();
                tropicalFishBucketMeta.setBodyColor(DyeColor.valueOf(extraInfo.get("color").toString()));
                tropicalFishBucketMeta.setPattern(TropicalFish.Pattern.valueOf(extraInfo.get("pattern").toString()));
                tropicalFishBucketMeta.setPatternColor(DyeColor.valueOf(extraInfo.get("patternColor").toString()));
                item.setItemMeta(tropicalFishBucketMeta);
            }
            case "decoratedPot" -> {
                //((BlockDataMeta) item.getItemMeta()).setBlockData(blockBuilder.getBlockData(extraInfo.get("shards").toString()));
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
        //meta.displayName(Component.text(customName));
        meta.setDisplayName(customName);
        item.setItemMeta(meta);
    }

    public void setExtraInfo(Map<String, Object> extraInfo, String customType) {
        this.extraInfo = extraInfo;
        this.customType = customType;
        this.item = getCustomItem(item, customType, extraInfo, blockBuilder);
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

