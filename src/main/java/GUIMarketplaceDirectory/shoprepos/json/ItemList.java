package GUIMarketplaceDirectory.shoprepos.json;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import GUIMarketplaceDirectory.utils.MyChatColor;

@JsonInclude(Include.NON_NULL)
public class ItemList {
    public interface BlockBuilder {
        BlockData getBlockData(String string);
        PlayerProfile createPlayerProfile(UUID uniqueId, String name);  
    }

    private Integer price;
    private String qty;
    private String name, customName;
    private int stackSize;
    private String customType;
    private ObjectNode extraInfo;

    private Boolean outOfStock;
    private LocalDateTime outOfStockSince;
    private String outOfStockBy;

    //runtime variables for displaying in inventory
    @JsonIgnore
    private ObjectMapper mapper = new ObjectMapper();
    @JsonIgnore
    private ItemStack item;
    @JsonIgnore
    private BlockBuilder blockBuilder;
    @JsonIgnore
    private List<Integer> warnings = new ArrayList<>();


    public ItemList() {
    }

    public ItemList(String itemName, String qty, int price, BlockBuilder blockBuilder) {
        this.name = itemName;
        this.stackSize = 1;
        this.qty = qty;
        this.price = price;
        this.customName = "";
        this.customType = "";
        this.blockBuilder = blockBuilder;
        updateItemStack(blockBuilder);
    }

    public ItemList(ItemStack item) {
        processItemStack(item);
        this.stackSize = 1;
        this.qty = "";
        this.price = 0;
    }

    public ItemList(ItemStack item, BlockBuilder blockBuilder) {
        processItemStack(item);
        this.blockBuilder = blockBuilder;
        updateItemStack(blockBuilder);
    }

    private void processItemStack(ItemStack itemStack) {
        warnings.clear();
        //
        this.name = itemStack.getType().getKey().getKey().toUpperCase();
        if (itemStack.getItemMeta().hasDisplayName()) this.customName = itemStack.getItemMeta().getDisplayName();
        //extraInfo, customType
        if (name.contains("SHULKER_BOX")) {
            if (itemStack.getItemMeta() instanceof BlockStateMeta im) {
                if (im.getBlockState() instanceof ShulkerBox shulker) {

                    List<ItemList> contents = new ArrayList<>(27);

                    for (int i = 0; i < 27; i++) {
                        ItemStack itemStack1 = shulker.getSnapshotInventory().getItem(i);
                        if (itemStack1 == null || itemStack1.getType() == Material.AIR)
                            continue;
                        ItemList itemList1 = new ItemList(itemStack1);
                        itemList1.setStackSize(itemStack1.getAmount());
                        contents.add(itemList1);
                    }
                    ObjectNode extraInfo = mapper.createObjectNode();
                    extraInfo.put("contents", mapper.valueToTree(contents));
                    this.extraInfo = extraInfo;
                    this.customType = "shulker";
                }
            }
        } else if (itemStack.getType() == Material.PLAYER_HEAD) {
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            ObjectNode extraInfo = mapper.createObjectNode();
            OfflinePlayer whoSkull = skullMeta.getOwningPlayer();
            if(whoSkull != null) {
                extraInfo.put("name", skullMeta.getOwningPlayer().getName());
            }
            if(skullMeta.getOwnerProfile() != null && 
                skullMeta.getOwnerProfile().getTextures() != null && 
                skullMeta.getOwnerProfile().getTextures().getSkin() != null) {
                    extraInfo.put("skin", skullMeta.getOwnerProfile().getTextures().getSkin().toString());
                    extraInfo.put("profileId", skullMeta.getOwnerProfile().getUniqueId().toString());
            }
            this.extraInfo = extraInfo;
            this.customType = "head";
        } else if (name.contains("POTION")) {
            PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
            PotionType potionType = potionMeta.getBasePotionType();
            ObjectNode potionData = mapper.createObjectNode();
            potionData.put("effect", potionType.toString());
            this.extraInfo = potionData;
            this.customType = "potion";
        } else if (name.contains("OMINOUS_BOTTLE")) {
            OminousBottleMeta ominousBottleMeta = (OminousBottleMeta) itemStack.getItemMeta(); 
            ObjectNode ominousBottleData = mapper.createObjectNode();
            if(ominousBottleMeta.hasAmplifier())
                ominousBottleData.put("amplifier", Integer.toString(ominousBottleMeta.getAmplifier()));
            this.extraInfo = ominousBottleData;
            this.customType = "ominousBottle";
        } else if (name.contains("FIREWORK_ROCKET")) {
            FireworkMeta rocketMeta = (FireworkMeta) itemStack.getItemMeta();
            ArrayNode effects = mapper.createArrayNode();
            rocketMeta.getEffects().forEach(fireworkEffect -> {
                ObjectNode effect = effects.addObject();
                effect.put("type", fireworkEffect.getType().toString());
                effect.put("flicker", fireworkEffect.hasFlicker());
                effect.put("trail", fireworkEffect.hasTrail());
                ArrayNode colors = effect.putArray("colors");
                ArrayNode fadeColors = effect.putArray("fadeColors");
                fireworkEffect.getColors().forEach(color -> colors.add(color.asRGB()));
                fireworkEffect.getFadeColors().forEach(fadeColor -> fadeColors.add(fadeColor.asRGB()));
                effects.add(effect);
            });
            ObjectNode fireworksData = mapper.createObjectNode();
            fireworksData.put("flight", Integer.toString(rocketMeta.getPower()));
            fireworksData.put("effects", effects);
            this.extraInfo = fireworksData;
            this.customType = "rocket";
        } else if (name.contains("TIPPED_ARROW")) {//TODO
            PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
            PotionType potionType = potionMeta.getBasePotionType();
            ObjectNode data = mapper.createObjectNode();
            //data.put("effect", Integer.valueOf(potionType.getType().ordinal()).toString());
            //data.put("upgraded", potionType.isUpgraded());
            //data.put("extended", potionType.isExtended());
            data.put("effect", potionType.toString());
            this.extraInfo = data;
            this.customType = "tippedArrow";
        } else if (name.endsWith("BANNER")) {
            BannerMeta bannerMeta = (BannerMeta) itemStack.getItemMeta();
            ArrayNode patterns = mapper.createArrayNode();
            bannerMeta.getPatterns().forEach(pattern -> {
                ObjectNode patternData = patterns.addObject();
                patternData.put("color", pattern.getColor().name().toUpperCase());
                patternData.put("type", pattern.getPattern().name().toUpperCase());
            });
            ObjectNode extraInfo = mapper.createObjectNode();
            extraInfo.put("patterns", patterns);
            this.extraInfo = extraInfo;
            this.customType = "banner";
        } else if(itemStack.getType() == Material.ENCHANTED_BOOK) {
            ObjectNode storedEnchants = mapper.createObjectNode();
            ((EnchantmentStorageMeta) itemStack.getItemMeta()).getStoredEnchants().forEach((enchantment, integer) -> storedEnchants.put(enchantment.getKey().getKey(),integer.toString()));
            ObjectNode extraInfo = mapper.createObjectNode();
            extraInfo.put("storedEnchants",storedEnchants);
            this.extraInfo = extraInfo;
            this.customType = "enchantedBook";
        } else if(itemStack.getType() == Material.AXOLOTL_BUCKET) {
            AxolotlBucketMeta axolotlMeta = (AxolotlBucketMeta) itemStack.getItemMeta();
            ObjectNode extraInfo = mapper.createObjectNode();
            extraInfo.put("type", axolotlMeta.getVariant().toString());
            this.extraInfo = extraInfo;
            this.customType = "axolotl";
        } else if(itemStack.getType() == Material.WRITABLE_BOOK || itemStack.getType() == Material.WRITTEN_BOOK) {
            BookMeta writtenBookMeta = (BookMeta) itemStack.getItemMeta();
            ObjectNode extraInfo = mapper.createObjectNode();
            if (writtenBookMeta.hasAuthor()) {
                extraInfo.put("author", writtenBookMeta.getAuthor());
            }
            if (writtenBookMeta.hasGeneration()) {
                extraInfo.put("generation", writtenBookMeta.getGeneration().toString());
            }
            if (writtenBookMeta.hasTitle()) {
                extraInfo.put("title", writtenBookMeta.getTitle());
            }
            this.extraInfo = extraInfo;
            this.customType = "writtenBook";
        } else if(itemStack.getType() == Material.CROSSBOW) {
            CrossbowMeta crossbowMeta = (CrossbowMeta) itemStack.getItemMeta();
            ObjectNode extraInfo = mapper.createObjectNode();
            if (!crossbowMeta.getChargedProjectiles().isEmpty()) {
                extraInfo.put("loaded", crossbowMeta.getChargedProjectiles().get(0).getType().toString());
                if (crossbowMeta.getChargedProjectiles().get(0).getType() == Material.TIPPED_ARROW) {
                    extraInfo.put("tipped", Integer.toString(((PotionMeta) crossbowMeta.getChargedProjectiles().get(0).getItemMeta()).getColor().asRGB()));
                }
            }                       
            this.extraInfo = extraInfo;
            this.customType = "crossbow";
        } else if(itemStack.getType() == Material.WOLF_ARMOR) {            
            ColorableArmorMeta colorableArmorMeta = (ColorableArmorMeta) itemStack.getItemMeta();
            ObjectNode extraInfo = mapper.createObjectNode(); 
            if(colorableArmorMeta.getColor() != null) {
                extraInfo.put("color", Integer.toString(((ColorableArmorMeta) itemStack.getItemMeta()).getColor().asRGB()));
            }
            this.extraInfo = extraInfo;
            this.customType = "wolfArmor";
        } else if(name.contains("BOOTS") || name.contains("LEGGINGS") || name.contains("CHESTPLATE") || name.contains("HELMET")) {
            ArmorMeta armorMeta = (ArmorMeta) itemStack.getItemMeta();
            ObjectNode extraInfo = mapper.createObjectNode();                
            if(armorMeta.getTrim() != null) {
                extraInfo.put("trimMaterial", armorMeta.getTrim().getMaterial().getKey().toString());
                extraInfo.put("trimPattern", armorMeta.getTrim().getPattern().getKey().toString());
            }
            if(name.contains("LEATHER")) {
                extraInfo.put("color", Integer.toString(((ColorableArmorMeta) itemStack.getItemMeta()).getColor().asRGB()));
            }
            this.extraInfo = extraInfo;
            this.customType = "armor";
        } else if(itemStack.getType() == Material.FILLED_MAP) {
            MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
            ObjectNode extraInfo = mapper.createObjectNode();
            extraInfo.put("id", Integer.toString(mapMeta.getMapId()));
            this.extraInfo = extraInfo;
            this.customType = "filledMap";
        } else if(itemStack.getType() == Material.GOAT_HORN) {
            MusicInstrumentMeta goatHornMeta = (MusicInstrumentMeta) itemStack.getItemMeta();
            ObjectNode extraInfo = mapper.createObjectNode();
            extraInfo.put("instrument", goatHornMeta.getInstrument().getKey().toString());
            this.extraInfo = extraInfo;
            this.customType = "goatHorn";
        } else if(itemStack.getType() == Material.SUSPICIOUS_STEW) {
            SuspiciousStewMeta suspiciousStewMeta = (SuspiciousStewMeta) itemStack.getItemMeta();
            ObjectNode extraInfo = mapper.createObjectNode();
            extraInfo.put("effect", suspiciousStewMeta.getCustomEffects().get(0).getType().getName());
            this.extraInfo = extraInfo;
            this.customType = "suspiciousStew";
        } else if(itemStack.getType() == Material.TROPICAL_FISH_BUCKET) {
            TropicalFishBucketMeta tropicalFishBucketMeta = (TropicalFishBucketMeta) itemStack.getItemMeta();
            ObjectNode extraInfo = mapper.createObjectNode();
            extraInfo.put("color", tropicalFishBucketMeta.getBodyColor().toString());
            extraInfo.put("pattern", tropicalFishBucketMeta.getPattern().toString());
            extraInfo.put("patternColor", tropicalFishBucketMeta.getPatternColor().toString());
            this.extraInfo = extraInfo;
            this.customType = "tropicalFishBucket";
        }

        Map<Enchantment,Integer> enchants = itemStack.getEnchantments();
        if(!enchants.isEmpty()) {
            Map<String, Object> extraInfo;
            if(this.extraInfo == null) this.extraInfo = mapper.createObjectNode();

            ObjectNode codedEnchants = mapper.createObjectNode();
            Iterator<Map.Entry<Enchantment,Integer>> enchantIterator = enchants.entrySet().iterator();
            while (enchantIterator.hasNext()) {
                Map.Entry<Enchantment,Integer> enchant = enchantIterator.next();
                codedEnchants.put(enchant.getKey().getKey().getKey() , enchant.getValue().toString());
            }
            this.extraInfo.put("enchants", codedEnchants);
        }
    }

    /* 
    Making the item stack based on available data 
    */
    public void updateItemStack(BlockBuilder blockBuilder) {
        this.item = makeItemStack(blockBuilder);
    }

    private ItemStack makeItemStack(BlockBuilder blockBuilder) {
        ItemStack item = new ItemStack(Material.getMaterial(this.name));
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

        if (price > 0 && !qtyString.isEmpty()) {
            lore.add(Component.text("§6" + qtyString + " §ffor §3" + price + " diamond" + (price == 1 ? "" : "s")));
        } else if(price < 0) {
            this.price = -1;
            lore.add(Component.text(MyChatColor.GRAY + "Price hidden or variable"));
        } else if(price == 0) {
            lore.add(Component.text(MyChatColor.GREEN + "Free!"));
        }
        meta.lore(lore);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, /*ItemFlag.HIDE_ENCHANTS,*/ ItemFlag.HIDE_UNBREAKABLE);
        if (this.customName != null && !this.customName.isEmpty())
            meta.setDisplayName(this.customName);
        item.setItemMeta(meta);
        if (this.customType != null && this.extraInfo != null && !this.extraInfo.isEmpty()) 
            addInfoToCustomItem(item, this.customType, extraInfo, blockBuilder);
        
        return item;
    }
    
    @JsonIgnore
    private ItemStack addInfoToCustomItem(ItemStack item, String customType, ObjectNode extraInfo, BlockBuilder blockBuilder) {
        // Custom Items such as heads, potions, tipped arrows, rockets, banners, shulkers, enchanted books and enchants
        switch (customType) {
            case "head" -> {
                SkullMeta skullmeta = (SkullMeta) item.getItemMeta();
                if(extraInfo.has("name") && !(extraInfo.get("name").textValue().equals("null")) && extraInfo.has("profileId")){
                    skullmeta.setOwnerProfile(blockBuilder.createPlayerProfile(UUID.fromString(extraInfo.get("profileId").textValue()), extraInfo.get("name").textValue()));
                }
                else if(extraInfo.has("name") && !(extraInfo.get("name").textValue().equals("null"))) {
                    skullmeta.setOwner(extraInfo.get("name").textValue());
                }
                
                PlayerProfile playerProfile = skullmeta.getOwnerProfile();
                if(extraInfo.has("skin") && !(extraInfo.get("skin").textValue().equals("null"))) {
                    try {
                        PlayerTextures playerTextures = playerProfile.getTextures();
                        URL skinUrl = new URL(extraInfo.get("skin").textValue());
                        playerTextures.setSkin(skinUrl);
                        playerProfile.setTextures(playerTextures);
                        
                    } catch (MalformedURLException e) {
                        //e.printStackTrace();
                    }
                }
                skullmeta.setOwnerProfile(playerProfile);
                item.setItemMeta(skullmeta);
            }
            case "potion", "tippedArrow" -> { //TODO
                PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
                PotionType potiontype = PotionType.valueOf(extraInfo.get("effect").textValue());
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
                if(extraInfo.has("amplifier"))
                    ominousBottleMeta.setAmplifier(Integer.valueOf(extraInfo.get("amplifier").textValue()));
                item.setItemMeta(ominousBottleMeta);
            }
            case "rocket" -> {
                FireworkMeta fireworkMeta = (FireworkMeta) item.getItemMeta();
                try{
                    fireworkMeta.setPower(Integer.valueOf(extraInfo.get("flight").textValue()));
                }catch(NumberFormatException e){
                    fireworkMeta.setPower(1);
                }
                ArrayNode effects = (ArrayNode) extraInfo.get("effects"); //TODO
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
                ArrayNode patterns = (ArrayNode) extraInfo.get("patterns"); //TODO
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
                ArrayNode contents = (ArrayNode) extraInfo.get("contents");
                List<ItemStack> items = new ArrayList<>();
                contents.forEach(content -> {
                    try {
                        ItemList contentList = mapper.treeToValue(content, ItemList.class);
                        items.add(contentList.getItem(blockBuilder));
                        //ItemList x = (ItemList) content;
                        //items.add(x.getItem(blockBuilder));
                    } catch (IllegalArgumentException | JsonProcessingException ex) {
                        System.getLogger(ItemList.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                    }
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
                axolotlMeta.setVariant(Axolotl.Variant.valueOf(extraInfo.get("type").textValue()));
                item.setItemMeta(axolotlMeta);
            } 
            case "writtenBook" -> {
                BookMeta writtenBookMeta = (BookMeta) item.getItemMeta();
                if (extraInfo.has("author")) {
                    writtenBookMeta.setAuthor(extraInfo.get("author").textValue());
                }
                if (extraInfo.has("generation")) {
                    writtenBookMeta.setGeneration(BookMeta.Generation.valueOf(extraInfo.get("generation").textValue()));
                }
                if (extraInfo.has("title")) {
                    writtenBookMeta.setTitle(extraInfo.get("title").textValue());
                }
                item.setItemMeta(writtenBookMeta);
            }
            case "crossbow" -> {
                CrossbowMeta CrossbowMeta = (CrossbowMeta) item.getItemMeta();
                if(extraInfo.has("loaded")) {
                    ItemStack arrow = new ItemStack(Material.valueOf(extraInfo.get("loaded").textValue()));
                    if (extraInfo.has("tipped") || arrow.getType() == Material.TIPPED_ARROW) {
                        PotionMeta arrowMeta = (PotionMeta) arrow.getItemMeta();
                        arrowMeta.setColor(Color.fromRGB(Integer.valueOf(extraInfo.get("tipped").textValue())));
                        arrow.setItemMeta(arrowMeta);
                    }
                    CrossbowMeta.addChargedProjectile(arrow);
                }
                item.setItemMeta(CrossbowMeta);
            }
            case "leatherArmor" -> {
                if(extraInfo.has("color")) {
                    LeatherArmorMeta LeatherArmorMeta = (LeatherArmorMeta) item.getItemMeta();
                    LeatherArmorMeta.setColor(Color.fromRGB(Integer.valueOf(extraInfo.get("color").textValue())));
                    item.setItemMeta(LeatherArmorMeta);
                }
            }
            case "wolfArmor" -> {                
                if(extraInfo.has("color")) {
                    ColorableArmorMeta colorableArmorMeta = (ColorableArmorMeta) item.getItemMeta();
                    colorableArmorMeta.setColor(Color.fromRGB(Integer.valueOf(extraInfo.get("color").textValue())));
                    item.setItemMeta(colorableArmorMeta);
                }
            }
            case "armor" -> {
                ArmorMeta armorMeta = (ArmorMeta) item.getItemMeta();
                if(extraInfo.has("trimPattern") && extraInfo.has("trimMaterial")) {
                    TrimPattern trimPattern;
                    switch (extraInfo.get("trimPattern").textValue()) {
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
                    switch (extraInfo.get("trimMaterial").textValue()) {
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
                if(extraInfo.has("color")) {
                    ColorableArmorMeta colorableArmorMeta = (ColorableArmorMeta) item.getItemMeta();
                    colorableArmorMeta.setColor(Color.fromRGB(Integer.valueOf(extraInfo.get("color").textValue())));
                    item.setItemMeta(colorableArmorMeta);
                }                
            }
            case "filledMap" -> {
                MapMeta mapMeta = (MapMeta) item.getItemMeta();
                mapMeta.setMapId(Integer.valueOf(extraInfo.get("id").textValue()));
                item.setItemMeta(mapMeta);
            }
            case "goatHorn" -> {
                MusicInstrumentMeta goatHornMeta = (MusicInstrumentMeta) item.getItemMeta();
                goatHornMeta.setInstrument(MusicInstrument.getByKey(NamespacedKey.fromString(extraInfo.get("instrument").textValue())));
                item.setItemMeta(goatHornMeta);
            }
            case "suspiciousStew" -> {
                SuspiciousStewMeta suspiciousStewMeta = (SuspiciousStewMeta) item.getItemMeta();
                suspiciousStewMeta.addCustomEffect(new PotionEffect(PotionEffectType.getByName(extraInfo.get("effect").textValue()), 1, 1), true);
                item.setItemMeta(suspiciousStewMeta);
            }
            case "tropicalFishBucket" -> {
                TropicalFishBucketMeta tropicalFishBucketMeta = (TropicalFishBucketMeta) item.getItemMeta();
                tropicalFishBucketMeta.setBodyColor(DyeColor.valueOf(extraInfo.get("color").textValue()));
                tropicalFishBucketMeta.setPattern(TropicalFish.Pattern.valueOf(extraInfo.get("pattern").textValue()));
                tropicalFishBucketMeta.setPatternColor(DyeColor.valueOf(extraInfo.get("patternColor").textValue()));
                item.setItemMeta(tropicalFishBucketMeta);
            }
            case "decoratedPot" -> {
                //((BlockDataMeta) item.getItemMeta()).setBlockData(blockBuilder.getBlockData(extraInfo.get("shards").textValue()));
            }
        }
        if (extraInfo.has("enchants")) {
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

    //getters and setters
    public int getPrice() {
        if (price == null) return 0;
        else return price;
    }

    public void setPrice(int price) {
        this.price = price;
        if (this.item != null && this.blockBuilder != null) updateItemStack(blockBuilder);
    }

    public String getQty() {
        if (qty == null) return "";
        else return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
        if (this.item != null && this.blockBuilder != null) updateItemStack(blockBuilder);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (this.item != null && this.blockBuilder != null) updateItemStack(blockBuilder);
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
        if (this.item != null && this.blockBuilder != null) updateItemStack(blockBuilder);
    }

    public int getStackSize() {
        return stackSize;
    }

    public void setStackSize(int stackSize) {
        this.stackSize = stackSize;
        if (this.item != null && this.blockBuilder != null) updateItemStack(blockBuilder);
    }

    public String getCustomType() {
        return customType;
    }

    public void setCustomType(String customType) {
        this.customType = customType;
        if (this.item != null && this.blockBuilder != null) updateItemStack(blockBuilder);
    }

    public ObjectNode getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(ObjectNode extraInfo) {
        this.extraInfo = extraInfo;
        if (this.item != null && this.blockBuilder != null) updateItemStack(blockBuilder);
    }



    @JsonIgnore
    public ItemStack getItem(BlockBuilder blockBuilder) {
        if (this.item == null) {
            this.item = makeItemStack(blockBuilder);
            this.blockBuilder = blockBuilder;
        }
        return this.item;
    }

    public List<Integer> getWarnings() {
        return warnings;
    }
}

