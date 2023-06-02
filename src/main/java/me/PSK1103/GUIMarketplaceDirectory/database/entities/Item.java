package me.PSK1103.GUIMarketplaceDirectory.database.entities;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTListCompound;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "item")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Transient
    ItemStack item;

    int price;

    String qty;

    String name;

    @Column(name = "custom_name")
    String customName;

    @Column(name = "custom_type")
    String customType;

    @Transient
    Map<String, Object> extraInfo;

    @Column(name = "extra_info")
    String extraInfoRaw;

    @PrePersist
    private void prePersist() {
        extraInfoRaw = new Gson().toJson(extraInfo);
    }

    @PostLoad
    private void postLoad() {
        extraInfo = new Gson().fromJson(
                extraInfoRaw, new TypeToken<HashMap<String, Object>>() {}.getType()
        );
        item = new ItemStack(Material.getMaterial(name));
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
        if(extraInfo != null && !extraInfo.isEmpty()) {
            item = getCustomItem(item, customType, extraInfo);
        }
    }

    private ItemStack getCustomItem(ItemStack item, String customType, Map<String, Object> extraInfo) {
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
                PotionData base = new PotionData(PotionType.valueOf(extraInfo.get("effect").toString()), (Boolean) extraInfo.get("extended"), (Boolean) extraInfo.get("upgraded"));
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
}
