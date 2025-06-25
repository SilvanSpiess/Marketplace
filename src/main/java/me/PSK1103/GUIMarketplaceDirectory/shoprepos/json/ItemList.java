package me.PSK1103.GUIMarketplaceDirectory.shoprepos.json;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.TropicalFish;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import me.PSK1103.GUIMarketplaceDirectory.shoprepos.json.ItemList;
import me.PSK1103.GUIMarketplaceDirectory.utils.MyChatColor;

public class ItemList {

    public interface BlockBuilder {
        BlockData getBlockData(String string);
        PlayerProfile createPlayerProfile(UUID uniqueId, String name);  
    }

    ItemStack item;
    int price;
    String qty;
    String name, customName;
    public String customType;
    BlockBuilder blockBuilder;
    public Map<String, Object> extraInfo;

    public ItemList() {
    }

    public ItemList(String itemName, String qty, int price, BlockBuilder blockBuilder) {
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
            lore.add(Component.text(MyChatColor.GRAY + "Price hidden or variable"));
        }
        else if(price == 0) {
            lore.add(Component.text(MyChatColor.GREEN + "Free!"));
        }
        else {
            lore.add(Component.text("§6" + qtyString + " §ffor §3" + price + " diamond" + (price == 1 ? "" : "s")));
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, /*ItemFlag.HIDE_ENCHANTS,*/ ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
    }

    public ItemList(String itemName, ItemMeta meta, BlockBuilder blockBuilder) {
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
            case "potion", "tippedArrow" -> {//TODO
                PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
                PotionType potiontype = PotionType.valueOf(extraInfo.get("effect").toString());
                potionMeta.setBasePotionType(potiontype);
                //Integer effectID = (Integer) extraInfo.get("effect");
                //Boolean extendedInfo = (Boolean) extraInfo.get("extended"); 
                //Boolean upgradedInfo = (Boolean) extraInfo.get("upgraded");
                
                //PotionData base = new PotionData(PotionType.values()[integer1 instanceof String ? Integer.parseInt(integer1.toString()) : integer1 instanceof Integer ? Integer.parseInt(integer1.toString()) : Double.valueOf(integer1.toString()).intValue()], (Boolean) extraInfo.get("extended"), (Boolean) extraInfo.get("upgraded"));
                //potionMeta.setBasePotionData(base);
                item.setItemMeta(potionMeta);
            }
            case "ominousBottle" -> {
                OminousBottleMeta ominousBottleMeta = (OminousBottleMeta) item.getItemMeta();
                if(extraInfo.containsKey("amplifier"))
                    ominousBottleMeta.setAmplifier(Integer.valueOf(extraInfo.get("amplifier").toString()));
                item.setItemMeta(ominousBottleMeta);
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
                //enchants.forEach((enchant, integer) -> esm.addStoredEnchant(new EnchantmentWrapper(enchant), integer instanceof String ? Integer.parseInt(integer.toString()) : integer instanceof Integer ? Integer.parseInt(integer.toString()) : Double.valueOf(integer.toString()).intValue(), false));
                enchants.forEach((enchant, integer) -> esm.addEnchant(Enchantment.getByName(enchant), integer instanceof String ? Integer.parseInt(integer.toString()) : integer instanceof Integer ? Integer.parseInt(integer.toString()) : Double.valueOf(integer.toString()).intValue(), true));
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
                if(extraInfo.containsKey("loaded")) {
                    ItemStack arrow = new ItemStack(Material.valueOf(extraInfo.get("loaded").toString()));
                    if (extraInfo.containsKey("tipped") || arrow.getType() == Material.TIPPED_ARROW) {
                        PotionMeta arrowMeta = (PotionMeta) arrow.getItemMeta();
                        arrowMeta.setColor(Color.fromRGB(Integer.valueOf(extraInfo.get("tipped").toString())));
                        arrow.setItemMeta(arrowMeta);
                    }
                    CrossbowMeta.addChargedProjectile(arrow);
                }
                item.setItemMeta(CrossbowMeta);
            }
            case "leatherArmor" -> {
                if(extraInfo.containsKey("color")) {
                    LeatherArmorMeta LeatherArmorMeta = (LeatherArmorMeta) item.getItemMeta();
                    LeatherArmorMeta.setColor(Color.fromRGB(Integer.valueOf(extraInfo.get("color").toString())));
                    item.setItemMeta(LeatherArmorMeta);
                }
            }
            case "wolfArmor" -> {                
                if(extraInfo.containsKey("color")) {
                    ColorableArmorMeta colorableArmorMeta = (ColorableArmorMeta) item.getItemMeta();
                    colorableArmorMeta.setColor(Color.fromRGB(Integer.valueOf(extraInfo.get("color").toString())));
                    item.setItemMeta(colorableArmorMeta);
                }
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
            //Map<Enchantment,Integer> enchants = new HashMap<>();
            //codedEnchants.forEach((enchant, integer) -> enchants.put(new EnchantmentWrapper(enchant), integer instanceof String ? Integer.parseInt(integer.toString()) : integer instanceof Integer ? Integer.parseInt(integer.toString()) : Double.valueOf(integer.toString()).intValue()));
            //item.addEnchantments(enchants);
            ItemMeta itemMeta = item.getItemMeta();
            codedEnchants.forEach((enchant, integer) -> itemMeta.addEnchant(Enchantment.getByName(enchant), integer instanceof String ? Integer.parseInt(integer.toString()) : integer instanceof Integer ? Integer.parseInt(integer.toString()) : Double.valueOf(integer.toString()).intValue(), true));
            item.setItemMeta(itemMeta);  
        }
        return item;
    }

    public static Map<String, Object> stackToMap(ItemStack itemStack, List<Integer> res) {
        String name = itemStack.getType().getKey().getKey().toUpperCase();
        Map<String, Object> item = new HashMap<>();
        //extraInfo, customType
        if (name.contains("SHULKER_BOX")) {
            if (itemStack.getItemMeta() instanceof BlockStateMeta) {
                BlockStateMeta im = (BlockStateMeta) itemStack.getItemMeta();
                if (im.getBlockState() instanceof ShulkerBox) {
                    ShulkerBox shulker = (ShulkerBox) im.getBlockState();

                    List<Map<String, Object>> contents = new ArrayList<>(27);

                    for (int i = 0; i < 27; i++) {
                        ItemStack itemStack1 = shulker.getSnapshotInventory().getItem(i);
                        if (itemStack1 == null || itemStack1.getType() == Material.AIR)
                            continue;
                        
                        String n = itemStack1.getType().getKey().getKey().toUpperCase(Locale.ROOT);

                        Map<String, Object> content = stackToMap(itemStack1, res);
                        content.put("name", itemStack1.getType().getKey().getKey().toUpperCase());
                        content.put("quantity", itemStack1.getAmount());
                        contents.add(content);
                    }
                    Map<String, Object> extraInfo = new HashMap<>();
                    extraInfo.put("contents", contents);
                    item.put("extraInfo", extraInfo);
                    item.put("customType", "shulker");
                }
            }
        } else if (itemStack.getType() == Material.PLAYER_HEAD) {
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            Map<String, Object> extraInfo = new HashMap<>();
            OfflinePlayer whoSkull = skullMeta.getOwningPlayer();
            if(whoSkull != null){
                extraInfo.put("name", skullMeta.getOwningPlayer().getName());
            }
            if(skullMeta.getOwnerProfile() != null && 
                skullMeta.getOwnerProfile().getTextures() != null && 
                skullMeta.getOwnerProfile().getTextures().getSkin() != null) {
                    extraInfo.put("skin", skullMeta.getOwnerProfile().getTextures().getSkin().toString());
                    extraInfo.put("profileId", skullMeta.getOwnerProfile().getUniqueId().toString());
            }
            item.put("extraInfo", extraInfo);
            item.put("customType", "head");
        } else if (name.contains("POTION")) {
            PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
            PotionType potionType = potionMeta.getBasePotionType();
            Map<String, Object> potionData = new HashMap<>();
            potionData.put("effect", potionType.toString());
            item.put("extraInfo", potionData);
            item.put("customType","potion");
        } else if (name.contains("OMINOUS_BOTTLE")) {
            OminousBottleMeta ominousBottleMeta = (OminousBottleMeta) itemStack.getItemMeta(); 
            Map<String, Object> ominousBottleData = new HashMap<>();
            if(ominousBottleMeta.hasAmplifier())
                ominousBottleData.put("amplifier", Integer.toString(ominousBottleMeta.getAmplifier()));
            item.put("extraInfo", ominousBottleData);
            item.put("customType","ominousBottle");
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
            Map<String, Object> fireworksData = new HashMap<>();
            fireworksData.put("flight", Integer.toString(rocketMeta.getPower()));
            fireworksData.put("effects", effects);
            item.put("extraInfo", fireworksData);
            item.put("customType", "rocket");
        } else if (name.contains("TIPPED_ARROW")) {//TODO
            PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
            PotionType potionType = potionMeta.getBasePotionType();
            Map<String, Object> data = new HashMap<>();
            //data.put("effect", Integer.valueOf(potionType.getType().ordinal()).toString());
            //data.put("upgraded", potionType.isUpgraded());
            //data.put("extended", potionType.isExtended());
            data.put("effect", potionType.toString());
            item.put("extraInfo", data);
            item.put("customType", "tippedArrow");
        } else if (name.endsWith("BANNER")) {
            BannerMeta bannerMeta = (BannerMeta) itemStack.getItemMeta();
            List<Object> patterns = new ArrayList<>();
            bannerMeta.getPatterns().forEach(pattern -> {
                Map<String, Object> patternData = new HashMap<>();
                patternData.put("color", pattern.getColor().name().toUpperCase());
                patternData.put("type", pattern.getPattern().name().toUpperCase());
                patterns.add(patternData);
            });
            Map<String, Object> extraInfo = new HashMap<>();
            extraInfo.put("patterns", patterns);
            item.put("extraInfo", extraInfo);
            item.put("customType", "banner");
        } else if(itemStack.getType() == Material.ENCHANTED_BOOK) {
            Map<String,String> storedEnchants = new HashMap<>();
            ((EnchantmentStorageMeta) itemStack.getItemMeta()).getStoredEnchants().forEach((enchantment, integer) -> storedEnchants.put(enchantment.getKey().getKey(),integer.toString()));
            Map<String, Object> extraInfo = new HashMap<>();
            extraInfo.put("storedEnchants",storedEnchants);
            item.put("extraInfo", extraInfo);
            item.put("customType", "enchantedBook");
        } else if(itemStack.getType() == Material.AXOLOTL_BUCKET) {
            AxolotlBucketMeta axolotlMeta = (AxolotlBucketMeta) itemStack.getItemMeta();
            Map<String, Object> extraInfo = new HashMap<>();
            extraInfo.put("type", axolotlMeta.getVariant().toString());
            item.put("extraInfo", extraInfo);
            item.put("customType", "axolotl");
        } else if(itemStack.getType() == Material.WRITABLE_BOOK || itemStack.getType() == Material.WRITTEN_BOOK) {
            BookMeta writtenBookMeta = (BookMeta) itemStack.getItemMeta();
            Map<String, Object> extraInfo = new HashMap<>();
            if (writtenBookMeta.hasAuthor()) {
                extraInfo.put("author", writtenBookMeta.getAuthor());
            }
            if (writtenBookMeta.hasGeneration()) {
                extraInfo.put("generation", writtenBookMeta.getGeneration().toString());
            }
            if (writtenBookMeta.hasTitle()) {
                extraInfo.put("title", writtenBookMeta.getTitle());
            }
            item.put("extraInfo", extraInfo);
            item.put("customType", "writtenBook");
        } else if(itemStack.getType() == Material.CROSSBOW) {
            CrossbowMeta crossbowMeta = (CrossbowMeta) itemStack.getItemMeta();
            Map<String, Object> extraInfo = new HashMap<>();
            if (crossbowMeta.getChargedProjectiles().size() > 0) {
                extraInfo.put("loaded", crossbowMeta.getChargedProjectiles().get(0).getType().toString());
                if (crossbowMeta.getChargedProjectiles().get(0).getType() == Material.TIPPED_ARROW) {
                    extraInfo.put("tipped", Integer.toString(((PotionMeta) crossbowMeta.getChargedProjectiles().get(0)).getColor().asRGB()));
                }
            }                       
            item.put("extraInfo", extraInfo);
            item.put("customType", "crossbow");
        } else if(itemStack.getType() == Material.WOLF_ARMOR) {            
            ColorableArmorMeta colorableArmorMeta = (ColorableArmorMeta) itemStack.getItemMeta();
            Map<String, Object> extraInfo = new HashMap<>(); 
            if(colorableArmorMeta.getColor() != null) {
                extraInfo.put("color", Integer.toString(((ColorableArmorMeta) itemStack.getItemMeta()).getColor().asRGB()));
            }
            item.put("extraInfo", extraInfo);
            item.put("customType", "wolfArmor");
        } else if(name.contains("BOOTS") || name.contains("LEGGINGS") || name.contains("CHESTPLATE") || name.contains("HELMET")) {
            ArmorMeta armorMeta = (ArmorMeta) itemStack.getItemMeta();
            Map<String, Object> extraInfo = new HashMap<>();                
            if(armorMeta.getTrim() != null) {
                extraInfo.put("trimMaterial", armorMeta.getTrim().getMaterial().getKey().toString());
                extraInfo.put("trimPattern", armorMeta.getTrim().getPattern().getKey().toString());
            }
            if(name.contains("LEATHER")) {
                extraInfo.put("color", Integer.toString(((ColorableArmorMeta) itemStack.getItemMeta()).getColor().asRGB()));
            }
            item.put("extraInfo", extraInfo);
            item.put("customType", "armor");
        } else if(itemStack.getType() == Material.FILLED_MAP) {
            MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
            Map<String, Object> extraInfo = new HashMap<>();
            extraInfo.put("id", Integer.toString(mapMeta.getMapId()));
            item.put("extraInfo", extraInfo);
            item.put("customType", "filledMap");
        } else if(itemStack.getType() == Material.GOAT_HORN) {
            MusicInstrumentMeta goatHornMeta = (MusicInstrumentMeta) itemStack.getItemMeta();
            Map<String, Object> extraInfo = new HashMap<>();
            extraInfo.put("instrument", goatHornMeta.getInstrument().getKey().toString());
            item.put("extraInfo", extraInfo);
            item.put("customType", "goatHorn");
        } else if(itemStack.getType() == Material.SUSPICIOUS_STEW) {
            SuspiciousStewMeta suspiciousStewMeta = (SuspiciousStewMeta) itemStack.getItemMeta();
            Map<String, Object> extraInfo = new HashMap<>();
            extraInfo.put("effect", suspiciousStewMeta.getCustomEffects().get(0).getType().getName());
            item.put("extraInfo", extraInfo);
            item.put("customType", "suspiciousStew");
        } else if(itemStack.getType() == Material.TROPICAL_FISH_BUCKET) {
            TropicalFishBucketMeta tropicalFishBucketMeta = (TropicalFishBucketMeta) itemStack.getItemMeta();
            Map<String, Object> extraInfo = new HashMap<>();
            extraInfo.put("color", tropicalFishBucketMeta.getBodyColor().toString());
            extraInfo.put("pattern", tropicalFishBucketMeta.getPattern().toString());
            extraInfo.put("patternColor", tropicalFishBucketMeta.getPatternColor().toString());
            item.put("extraInfo", extraInfo);
            item.put("customType", "tropicalFishBucket");
        }

        Map<Enchantment,Integer> enchants = itemStack.getEnchantments();
        if(!enchants.isEmpty()) {
            Map<String, Object> extraInfo;
            if(item.containsKey("extraInfo")) {
                extraInfo = (Map<String, Object>) item.get("extraInfo");
            }                
            else
                extraInfo = new HashMap<>();
            item.put("extraInfo", extraInfo);
            Map<String,String> codedEnchants = new HashMap<>();
            Iterator<Map.Entry<Enchantment,Integer>> enchantIterator = enchants.entrySet().iterator();
            while (enchantIterator.hasNext()) {
                Map.Entry<Enchantment,Integer> enchant = enchantIterator.next();
                codedEnchants.put(enchant.getKey().getKey().getKey() , enchant.getValue().toString());
            }
            extraInfo.put("enchants", codedEnchants);
            item.put("extraInfo", extraInfo);
        }
        return item;
    }

    public List<Integer> storeExtraInfo(ItemStack itemStack) {
        List<Integer> errorTracker = new LinkedList<>();
        Map<String, Object> extraItemInfo = ItemList.stackToMap(itemStack, errorTracker);
        if (extraItemInfo.containsKey("extraInfo") && extraItemInfo.containsKey("customType")) {
            this.extraInfo = (Map<String, Object>) extraItemInfo.get("extraInfo");
            this.customType = extraItemInfo.get("customType").toString();
        } else if (extraItemInfo.containsKey("extraInfo")) {
            this.extraInfo = (Map<String, Object>) extraItemInfo.get("extraInfo");
        }
        return errorTracker;
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
            l.add(MyChatColor.GRAY + "Price hidden or variable");
        }
        else if(price == 0) {
            l.add(MyChatColor.GREEN + "Free!");
        }
        else {
            l.add("§6" + qtyString + " §ffor §3" + price + " diamond" + (price == 1 ? "" : "s"));
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

