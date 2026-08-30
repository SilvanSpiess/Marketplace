package GUIMarketplaceDirectory.shoprepos.json.items;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.AxolotlBucketMeta;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ColorableArmorMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.inventory.meta.OminousBottleMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.inventory.meta.TropicalFishBucketMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.BannerPatternInfo;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.FireWorkEffectInfo;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.ShulkerContentInfo;

@JsonInclude(Include.NON_NULL)
public class ItemList {
    public interface BlockBuilder {
        BlockData getBlockData(String string);
        PlayerProfile createPlayerProfile(UUID uniqueId, String name);  
    }

    protected String name, customName;
    protected int stackSize;
    protected String customType;
    protected ExtraInfo extraInfo;

    //runtime variables for displaying in inventory
    @JsonIgnore
    protected ItemStack item;
    @JsonIgnore
    protected BlockBuilder blockBuilder;
    @JsonIgnore
    protected List<String> addWarnings = new ArrayList<>();


    public ItemList() {
    }

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public ItemList(String itemName, BlockBuilder blockBuilder) {
        this.name = itemName;
        this.stackSize = 1;
        this.customName = "";
        this.customType = "";
        this.blockBuilder = blockBuilder;
        updateItemStack(blockBuilder);
    }

    public ItemList(ItemStack item) {
        processItemStack(item);
        this.stackSize = 1;
    }

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public ItemList(ItemStack item, BlockBuilder blockBuilder) {
        processItemStack(item);
        this.blockBuilder = blockBuilder;
        updateItemStack(blockBuilder);
    }

    private void processItemStack(ItemStack itemStack) {
        addWarnings.clear();
        //
        this.name = itemStack.getType().getKey().getKey().toUpperCase();
        if (itemStack.getItemMeta().hasDisplayName()) this.customName = itemStack.getItemMeta().getDisplayName();
        //extraInfo, customType
        if (name.contains("SHULKER_BOX")) {
            if (itemStack.getItemMeta() instanceof BlockStateMeta im) {
                if (im.getBlockState() instanceof ShulkerBox shulker) {
                    this.customType = "shulker";
                    this.extraInfo = new ExtraInfo();
                    List<ShulkerContentInfo> contents = new ArrayList<>(27);

                    for (int i = 0; i < 27; i++) {
                        ItemStack itemStack1 = shulker.getSnapshotInventory().getItem(i);
                        if (itemStack1 == null || itemStack1.getType() == Material.AIR)
                            continue;
                        ItemList itemList1 = new ItemList(itemStack1);
                        itemList1.setStackSize(itemStack1.getAmount());
                        
                        contents.add(new ExtraInfo.ShulkerContentInfo(i, itemList1));
                    }
                    this.extraInfo.setContents(contents);
                }
            }
        } else if (itemStack.getType() == Material.PLAYER_HEAD) {
            this.customType = "head";
            this.extraInfo = new ExtraInfo();
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            OfflinePlayer whoSkull = skullMeta.getOwningPlayer();
            if(whoSkull != null) {
                extraInfo.setName(skullMeta.getOwningPlayer().getName());
            }
            if(skullMeta.getOwnerProfile() != null && 
                skullMeta.getOwnerProfile().getTextures() != null && 
                skullMeta.getOwnerProfile().getTextures().getSkin() != null) {
                    extraInfo.setSkin(skullMeta.getOwnerProfile().getTextures().getSkin().toString());
                    extraInfo.setProfileId(skullMeta.getOwnerProfile().getUniqueId().toString());
            }
        } else if (name.contains("POTION")) {
            this.customType = "potion";
            this.extraInfo = new ExtraInfo();
            PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
            PotionType potionType = potionMeta.getBasePotionType();
            extraInfo.setPotionType(potionType);
        } else if (name.contains("OMINOUS_BOTTLE")) {
            this.customType = "ominousBottle";
            this.extraInfo = new ExtraInfo();
            OminousBottleMeta ominousBottleMeta = (OminousBottleMeta) itemStack.getItemMeta(); 
            if(ominousBottleMeta.hasAmplifier())
                extraInfo.setAmplifier(ominousBottleMeta.getAmplifier());
        } else if (name.contains("FIREWORK_ROCKET")) {
            this.customType = "rocket";
            this.extraInfo = new ExtraInfo();
            FireworkMeta rocketMeta = (FireworkMeta) itemStack.getItemMeta();
            extraInfo.setEffects(rocketMeta.getEffects().stream()
                .map(fireworkEffect -> {
                    return new FireWorkEffectInfo(
                        fireworkEffect.getType(),
                        fireworkEffect.hasFlicker(),
                        fireworkEffect.hasTrail(),
                        fireworkEffect.getColors().stream().map(Color::asRGB).toList(),
                        fireworkEffect.getFadeColors().stream().map(Color::asRGB).toList()
                    );
                }).toList());
            if (rocketMeta.hasPower()) extraInfo.setFlight(rocketMeta.getPower());
        } else if (name.contains("TIPPED_ARROW")) {//TODO
            this.customType = "tippedArrow";
            this.extraInfo = new ExtraInfo();
            PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
            PotionType potionType = potionMeta.getBasePotionType();
            extraInfo.setPotionType(potionType);
        } else if (name.endsWith("BANNER")) {
            this.customType = "banner";
            this.extraInfo = new ExtraInfo();
            BannerMeta bannerMeta = (BannerMeta) itemStack.getItemMeta();
            extraInfo.setPatterns(bannerMeta.getPatterns().stream()
                .map(pattern -> {
                    return new BannerPatternInfo(
                        pattern.getColor(),
                        pattern.getPattern()
                    );
                })
                .toList());
        } else if(itemStack.getType() == Material.ENCHANTED_BOOK) {
            this.customType = "enchantedBook";
            this.extraInfo = new ExtraInfo();
            EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta) itemStack.getItemMeta();
            extraInfo.setStoredEnchants(enchantmentStorageMeta.getStoredEnchants());
        } else if(itemStack.getType() == Material.AXOLOTL_BUCKET) {
            this.customType = "axolotl";
            this.extraInfo = new ExtraInfo();
            AxolotlBucketMeta axolotlMeta = (AxolotlBucketMeta) itemStack.getItemMeta();
            extraInfo.setType(axolotlMeta.getVariant());
        } else if(itemStack.getType() == Material.WRITABLE_BOOK || itemStack.getType() == Material.WRITTEN_BOOK) {
            this.customType = "writtenBook";
            this.extraInfo = new ExtraInfo();
            BookMeta writtenBookMeta = (BookMeta) itemStack.getItemMeta();
            if (writtenBookMeta.hasAuthor()) 
                extraInfo.setAuthor(writtenBookMeta.getAuthor());
            if (writtenBookMeta.hasGeneration()) 
                extraInfo.setGeneration(writtenBookMeta.getGeneration());
            if (writtenBookMeta.hasTitle())
                extraInfo.setTitle(writtenBookMeta.getTitle());
        } else if(itemStack.getType() == Material.CROSSBOW) {
            this.customType = "crossbow";
            this.extraInfo = new ExtraInfo();
            CrossbowMeta crossbowMeta = (CrossbowMeta) itemStack.getItemMeta();
            if (!crossbowMeta.getChargedProjectiles().isEmpty()) {
                extraInfo.setLoaded(new ItemList(crossbowMeta.getChargedProjectiles().get(0)));
            }                       
        } else if(itemStack.getType() == Material.WOLF_ARMOR) {            
            this.customType = "wolfArmor";
            this.extraInfo = new ExtraInfo();
            ColorableArmorMeta colorableArmorMeta = (ColorableArmorMeta) itemStack.getItemMeta();
            if(colorableArmorMeta.getColor() != null) {
                extraInfo.setColor(colorableArmorMeta.getColor().asRGB());
            }
        } else if(name.contains("BOOTS") || name.contains("LEGGINGS") || name.contains("CHESTPLATE") || name.contains("HELMET")) {
            this.customType = "armor";
            this.extraInfo = new ExtraInfo();
            ArmorMeta armorMeta = (ArmorMeta) itemStack.getItemMeta();          
            if(armorMeta.getTrim() != null) {
                extraInfo.setTrimMaterial(armorMeta.getTrim().getMaterial());
                extraInfo.setTrimPattern(armorMeta.getTrim().getPattern());
            }
            if(name.contains("LEATHER")) {
                ColorableArmorMeta colorableArmorMeta = (ColorableArmorMeta) itemStack.getItemMeta();
                extraInfo.setColor(colorableArmorMeta.getColor().asRGB());
            }
        } else if(itemStack.getType() == Material.FILLED_MAP) {
            this.customType = "filledMap";
            this.extraInfo = new ExtraInfo();
            MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
            extraInfo.setId(mapMeta.getMapId());
        } else if(itemStack.getType() == Material.GOAT_HORN) {
            this.customType = "goatHorn";
            this.extraInfo = new ExtraInfo();
            MusicInstrumentMeta goatHornMeta = (MusicInstrumentMeta) itemStack.getItemMeta();
            extraInfo.setInstrument(goatHornMeta.getInstrument());
        } else if(itemStack.getType() == Material.SUSPICIOUS_STEW) {
            this.customType = "suspiciousStew";
            this.extraInfo = new ExtraInfo();
            SuspiciousStewMeta suspiciousStewMeta = (SuspiciousStewMeta) itemStack.getItemMeta();
            extraInfo.setEffect(suspiciousStewMeta.getCustomEffects().get(0).getType());
        } else if(itemStack.getType() == Material.TROPICAL_FISH_BUCKET) {
            this.customType = "tropicalFishBucket";
            this.extraInfo = new ExtraInfo();
            TropicalFishBucketMeta tropicalFishBucketMeta = (TropicalFishBucketMeta) itemStack.getItemMeta();
            if (tropicalFishBucketMeta.hasBodyColor())
                extraInfo.setFishColor(tropicalFishBucketMeta.getBodyColor());
            if (tropicalFishBucketMeta.hasPattern())
                extraInfo.setFishPattern(tropicalFishBucketMeta.getPattern());
            if (tropicalFishBucketMeta.hasPatternColor())
                extraInfo.setFishPatternColor(tropicalFishBucketMeta.getPatternColor());
        }

        Map<Enchantment,Integer> enchants = itemStack.getEnchantments();
        if(!enchants.isEmpty()) {
            if(this.extraInfo == null) this.extraInfo = new ExtraInfo();
            extraInfo.setEnchants(enchants.entrySet().stream().filter(enchant -> {
                if (!(enchant.getValue() >= enchant.getKey().getStartLevel() && enchant.getValue() <= enchant.getKey().getMaxLevel())) {
                    addWarnings.add("The enchanted item you're trying to add has illegal enchants on it. You may continue adding, however these enchants will not be seen within your shop window.");
                    return false;
                }
                return true;
            }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
        }
    }

    /* 
    Making the item stack based on available data 
    */
    public void updateItemStack(BlockBuilder blockBuilder) {
        this.item = makeItemStack(blockBuilder);
    }

    protected ItemStack makeItemStack(BlockBuilder blockBuilder) {
        ItemStack item = new ItemStack(Material.getMaterial(this.name));
        ItemMeta meta = item.getItemMeta();

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        if (this.customName != null && !this.customName.isEmpty())
            meta.setDisplayName(this.customName);
        item.setItemMeta(meta);
        if (this.customType != null && this.extraInfo != null)
            addInfoToCustomItem(item, this.customType, extraInfo, blockBuilder);
        
        return item;
    }
    
    @JsonIgnore
    private ItemStack addInfoToCustomItem(ItemStack item, String customType, ExtraInfo extraInfo, BlockBuilder blockBuilder) {
        // Custom Items such as heads, potions, tipped arrows, rockets, banners, shulkers, enchanted books and enchants
        switch (customType) {
            case "head" -> {
                SkullMeta skullmeta = (SkullMeta) item.getItemMeta();
                if (extraInfo.getName() != null && !extraInfo.getName().isEmpty()
                 && extraInfo.getProfileId() != null && extraInfo.getProfileId().isEmpty()){
                    skullmeta.setOwnerProfile(blockBuilder.createPlayerProfile(UUID.fromString(extraInfo.getProfileId()), extraInfo.getName()));
                }
                else if (extraInfo.getName() != null && !extraInfo.getName().isEmpty() 
                 && !(extraInfo.getName().equals("null"))) {
                    skullmeta.setOwner(extraInfo.getName());
                }
                
                PlayerProfile playerProfile = skullmeta.getOwnerProfile();
                if(extraInfo.getSkin() != null && !extraInfo.getSkin().isEmpty() && !(extraInfo.getSkin().equals("null"))) {
                    try {
                        PlayerTextures playerTextures = playerProfile.getTextures();
                        URL skinUrl = new URL(extraInfo.getSkin());
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
                potionMeta.setBasePotionType(extraInfo.getPotionType());
                item.setItemMeta(potionMeta);
            }
            case "ominousBottle" -> {
                OminousBottleMeta ominousBottleMeta = (OminousBottleMeta) item.getItemMeta();
                if(extraInfo.getAmplifier() != null)
                    ominousBottleMeta.setAmplifier(Integer.valueOf(extraInfo.getAmplifier()));
                item.setItemMeta(ominousBottleMeta);
            }
            case "rocket" -> {
                FireworkMeta fireworkMeta = (FireworkMeta) item.getItemMeta();
                if (extraInfo.getFlight() != null) {
                    try{
                        fireworkMeta.setPower(extraInfo.getFlight());
                    } catch(NumberFormatException e){
                        fireworkMeta.setPower(1);
                    }
                }
                List<FireWorkEffectInfo> effects = extraInfo.getEffects();
                if (effects != null && effects.size() > 0) {
                    List<FireworkEffect> fireworkEffects = new ArrayList<>();
                    effects.forEach(fireWorkEffectInfo -> {
                        List<Color> colors = new ArrayList<>();
                        List<Color> fadeColors = new ArrayList<>();
                        fireWorkEffectInfo.getColors().forEach(color -> colors.add(Color.fromRGB(color.intValue())));
                        fireWorkEffectInfo.getFadeColors().forEach(fadeColor -> fadeColors.add(Color.fromRGB(fadeColor.intValue())));
                        FireworkEffect fireworkEffect = FireworkEffect.builder()
                                .flicker(fireWorkEffectInfo.getFlicker())
                                .trail(fireWorkEffectInfo.getTrail())
                                .with(fireWorkEffectInfo.getType())
                                .withColor(colors)
                                .withFade(fadeColors)
                                .build();
                        fireworkEffects.add(fireworkEffect);
                    });
                    fireworkMeta.addEffects(fireworkEffects);
                }
                item.setItemMeta(fireworkMeta);
            }
            case "banner" -> {
                BannerMeta bannerMeta = (BannerMeta) item.getItemMeta();
                List<BannerPatternInfo> patterns = extraInfo.getPatterns();
                List<Pattern> bannerPatterns = new ArrayList<>();
                patterns.forEach(bannerPatternInfo -> {
                    Pattern bannerPattern = new Pattern(bannerPatternInfo.getColor(), bannerPatternInfo.getType());
                    bannerPatterns.add(bannerPattern);
                });
                bannerMeta.setPatterns(bannerPatterns);
                item.setItemMeta(bannerMeta);
            }
            case "shulker" -> {
                List<ShulkerContentInfo> contents = extraInfo.getContents();
                ItemStack[] items = new ItemStack[27];
                contents.forEach(content -> {
                    try {
                        ItemList contentList = content.getItem();
                        items[content.getInvSlot()] = contentList.getItem(blockBuilder);
                    } catch (IllegalArgumentException ex) {
                        System.getLogger(ItemList.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                    }
                });
                BlockStateMeta blockStateMeta = (BlockStateMeta) item.getItemMeta();
                ShulkerBox shulkerBox = (ShulkerBox) blockStateMeta.getBlockState();
                shulkerBox.getInventory().setContents(items);
                shulkerBox.update(true, false);
                blockStateMeta.setBlockState(shulkerBox);
                item.setItemMeta(blockStateMeta);
            }
            case "enchantedBook" -> {
                Map<Enchantment, Integer> enchants = extraInfo.getStoredEnchants();
                EnchantmentStorageMeta esm = (EnchantmentStorageMeta) item.getItemMeta();
                
                enchants.forEach((enchant, integer) -> esm.addStoredEnchant(enchant, integer , true));
                item.setItemMeta(esm);
            }
            case "axolotl" -> {
                AxolotlBucketMeta axolotlMeta = (AxolotlBucketMeta) item.getItemMeta();
                axolotlMeta.setVariant(extraInfo.getType());
                item.setItemMeta(axolotlMeta);
            } 
            case "writtenBook" -> {
                BookMeta writtenBookMeta = (BookMeta) item.getItemMeta();
                if (!extraInfo.getAuthor().isEmpty()) {
                    writtenBookMeta.setAuthor(extraInfo.getAuthor());
                }
                if (extraInfo.getGeneration() != null) {
                    writtenBookMeta.setGeneration(extraInfo.getGeneration());
                }
                if (!extraInfo.getTitle().isEmpty()) {
                    writtenBookMeta.setTitle(extraInfo.getTitle());
                }
                item.setItemMeta(writtenBookMeta);
            }
            case "crossbow" -> {
                CrossbowMeta CrossbowMeta = (CrossbowMeta) item.getItemMeta();
                if(extraInfo.getLoaded() != null) {
                    ItemStack arrow = extraInfo.getLoaded().getItem(blockBuilder);
                    CrossbowMeta.addChargedProjectile(arrow);
                }
                item.setItemMeta(CrossbowMeta);
            }
            case "leatherArmor" -> {
                if(extraInfo.getColor() != null) {
                    LeatherArmorMeta LeatherArmorMeta = (LeatherArmorMeta) item.getItemMeta();
                    LeatherArmorMeta.setColor(Color.fromRGB(extraInfo.getColor()));
                    item.setItemMeta(LeatherArmorMeta);
                }
            }
            case "wolfArmor" -> {                
                if(extraInfo.getColor() != null) {
                    ColorableArmorMeta colorableArmorMeta = (ColorableArmorMeta) item.getItemMeta();
                    colorableArmorMeta.setColor(Color.fromRGB(extraInfo.getColor()));
                    item.setItemMeta(colorableArmorMeta);
                }
            }
            case "armor" -> {
                ArmorMeta armorMeta = (ArmorMeta) item.getItemMeta();
                if(extraInfo.getTrimPattern() != null && extraInfo.getTrimMaterial() != null) {
                    TrimPattern trimPattern = extraInfo.getTrimPattern();
                    TrimMaterial trimMaterial = extraInfo.getTrimMaterial();                    
                    armorMeta.setTrim(new ArmorTrim(trimMaterial, trimPattern));
                    item.setItemMeta(armorMeta);
                }
                if(extraInfo.getColor() != null) {
                    ColorableArmorMeta colorableArmorMeta = (ColorableArmorMeta) item.getItemMeta();
                    colorableArmorMeta.setColor(Color.fromRGB(extraInfo.getColor()));
                    item.setItemMeta(colorableArmorMeta);
                }                
            }
            case "filledMap" -> {
                MapMeta mapMeta = (MapMeta) item.getItemMeta();
                mapMeta.setMapId(extraInfo.getId());
                item.setItemMeta(mapMeta);
            }
            case "goatHorn" -> {
                MusicInstrumentMeta goatHornMeta = (MusicInstrumentMeta) item.getItemMeta();
                goatHornMeta.setInstrument(extraInfo.getInstrument());
                item.setItemMeta(goatHornMeta);
            }
            case "suspiciousStew" -> {
                SuspiciousStewMeta suspiciousStewMeta = (SuspiciousStewMeta) item.getItemMeta();
                if (extraInfo.getEffect() != null)
                    suspiciousStewMeta.addCustomEffect(new PotionEffect(extraInfo.getEffect(), 1, 1), true);
                item.setItemMeta(suspiciousStewMeta);
            }
            case "tropicalFishBucket" -> {
                TropicalFishBucketMeta tropicalFishBucketMeta = (TropicalFishBucketMeta) item.getItemMeta();
                tropicalFishBucketMeta.setBodyColor(extraInfo.getFishColor());
                tropicalFishBucketMeta.setPattern(extraInfo.getFishPattern());
                tropicalFishBucketMeta.setPatternColor(extraInfo.getFishPatternColor());
                item.setItemMeta(tropicalFishBucketMeta);
            }
            case "decoratedPot" -> {
                //((BlockDataMeta) item.getItemMeta()).setBlockData(blockBuilder.getBlockData(extraInfo.get("shards").textValue()));
            }
        }
        if (extraInfo.getEnchants() != null) {
            Map<Enchantment,Integer> enchants = extraInfo.getEnchants();
            ItemMeta itemMeta = item.getItemMeta();
            enchants.forEach((enchant, integer) -> itemMeta.addEnchant(enchant, integer, true));
            item.setItemMeta(itemMeta);
        }
        return item;
    }

    //getters and setters
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

    public ExtraInfo getExtraInfo() {
        return this.extraInfo;
    }

    public void setExtraInfo(ExtraInfo extraInfo) {
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

    @JsonIgnore
    public List<String> getAddWarnings() {
        return addWarnings;
    }
}