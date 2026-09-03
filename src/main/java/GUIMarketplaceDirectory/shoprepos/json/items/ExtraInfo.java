package GUIMarketplaceDirectory.shoprepos.json.items;


import org.bukkit.*;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.TropicalFish;
import org.bukkit.inventory.meta.BookMeta.Generation;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import org.bukkit.entity.Axolotl.Variant;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.BannerPatternInfo;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.FireWorkEffectInfo;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.ShulkerContentInfo;

@JsonInclude(Include.NON_NULL)
public class ExtraInfo {
    private String name;                              // for heads
    private String profileId;                         // for heads
    private String skin;                              // for heads
    private PotionType potionType;                    // for potions and tipped arrows TODO used to be named effect
    private Integer amplifier;                        // for ominous potions
    private Integer flight;                           // for rockets
    private List<FireWorkEffectInfo> effects;         // for rockets
    private List<BannerPatternInfo> patterns;         // for banners
    private Map<Enchantment, Integer> storedEnchants; // for enchanted books
    private Variant type;                             // for axolotl bucket
    private String author;                            // for written book
    private Generation generation;                    // for written book
    private String title;                             // for written book
    private ItemList loaded;                          // for crossbow
    private Integer color;                            // for wolf armour, leather armour
    private TrimMaterial trimMaterial;                // for armour
    private TrimPattern trimPattern;                  // for armour
    private Integer id;                               // for maps
    private MusicInstrument instrument;               // for goat horns
    private PotionEffectType effect;                  // for suspicous stew
    private DyeColor fishColor;                       // for tropical fish bucked TODO used to be called color
    private TropicalFish.Pattern fishPattern;         // for tropical fish bucket TODO used to be called pattern
    private DyeColor fishPatternColor;                // for tropical fish bucked TODO used to be called patternColor
    private List<ShulkerContentInfo> contents;        // for shulker contents

    private Map<Enchantment, Integer> enchants;       // for any item

    public ExtraInfo() {}

    public String getName() { return this.name; }
    public void setName(String name) { this.name = name; }
    public String getProfileId() { return this.profileId; }
    public void setProfileId(String profileId) { this.profileId = profileId; }
    public String getSkin() { return this.skin; }
    public void setSkin(String skin) { this.skin = skin; }
    public PotionType getPotionType() { return this.potionType; }
    public void setPotionType(PotionType potionType) { this.potionType = potionType; }
    public Integer getAmplifier() { return this.amplifier; }
    public void setAmplifier(int amplifier) { this.amplifier = amplifier; }
    public Integer getFlight() { return this.flight; }
    public void setFlight(int flight) { this.flight = flight; }
    public List<FireWorkEffectInfo> getEffects() { return this.effects; }
    public void setEffects(List<FireWorkEffectInfo> effects) { this.effects = effects; }
    public List<BannerPatternInfo> getPatterns() { return this.patterns; }
    public void setPatterns(List<BannerPatternInfo> patterns) { this.patterns = patterns; }
    public Map<Enchantment,Integer> getStoredEnchants() { return this.storedEnchants; }
    public void setStoredEnchants(Map<Enchantment,Integer> storedEnchants) { this.storedEnchants = storedEnchants; }
    public Variant getType() { return this.type; }
    public void setType(Variant type) { this.type = type; }
    public String getAuthor() { return this.author; }
    public void setAuthor(String author) { this.author = author; }
    public Generation getGeneration() { return this.generation; }
    public void setGeneration(Generation generation) { this.generation = generation; }
    public String getTitle() { return this.title; }
    public void setTitle(String title) { this.title = title; }
    public ItemList getLoaded() { return this.loaded; }
    public void setLoaded(ItemList loaded) { this.loaded = loaded; }
    public Integer getColor() { return this.color; }
    public void setColor(int color) { this.color = color; }
    public TrimMaterial getTrimMaterial() { return this.trimMaterial; }
    public void setTrimMaterial(TrimMaterial trimMaterial) { this.trimMaterial = trimMaterial; }
    public TrimPattern getTrimPattern() { return this.trimPattern; }
    public void setTrimPattern(TrimPattern trimPattern) { this.trimPattern = trimPattern; }
    public Integer getId() { return this.id; }
    public void setId(int id) { this.id = id; }
    public MusicInstrument getInstrument() { return this.instrument; }
    public void setInstrument(MusicInstrument instrument) { this.instrument = instrument; }
    public PotionEffectType getEffect() { return this.effect; }
    public void setEffect(PotionEffectType effect) { this.effect = effect; }
    public DyeColor getFishColor() { return this.fishColor; }
    public void setFishColor(DyeColor fishColor) { this.fishColor = fishColor; }
    public TropicalFish.Pattern getFishPattern() { return this.fishPattern; }
    public void setFishPattern(TropicalFish.Pattern fishPattern) { this.fishPattern = fishPattern; }
    public DyeColor getFishPatternColor() { return this.fishPatternColor; }
    public void setFishPatternColor(DyeColor fishPatternColor) { this.fishPatternColor = fishPatternColor; }
    public List<ShulkerContentInfo> getContents() { return this.contents; }
    public void setContents(List<ShulkerContentInfo> contents) { this.contents = contents; }
    public Map<Enchantment,Integer> getEnchants() { return this.enchants; }
    public void setEnchants(Map<Enchantment,Integer> enchants) { this.enchants = enchants; }

    public static class FireWorkEffectInfo {
        private FireworkEffect.Type type;
        private boolean flicker;
        private boolean trail;
        private List<Integer> colors; 
        private List<Integer> fadeColors;

        public FireWorkEffectInfo() {}
        public FireWorkEffectInfo(FireworkEffect.Type type, boolean flicker, boolean trail, List<Integer> colors, List<Integer> fadeColors) {
            this.type = type;
            this.flicker = flicker;
            this.trail = trail;
            this.colors = colors;
            this.fadeColors = fadeColors;
        }

        public FireworkEffect.Type getType() { return this.type; }
        public void setType(FireworkEffect.Type type) { this.type = type; }
        public boolean isFlicker() { return this.flicker; }
        public boolean getFlicker() { return this.flicker; }
        public void setFlicker(boolean flicker) { this.flicker = flicker; }
        public boolean isTrail() { return this.trail; }
        public boolean getTrail() { return this.trail; }
        public void setTrail(boolean trail) { this.trail = trail; }
        public List<Integer> getColors() { return this.colors; }
        public void setColors(List<Integer> colors) { this.colors = colors; }
        public List<Integer> getFadeColors() { return this.fadeColors; }
        public void setFadeColors(List<Integer> fadeColors) { this.fadeColors = fadeColors; }
    }

    public static class BannerPatternInfo {
        private DyeColor color;
        private PatternType type;

        public BannerPatternInfo() {}
        public BannerPatternInfo(DyeColor color, PatternType type) {
            this.color = color;
            this.type = type;
        }

        public DyeColor getColor() { return this.color; }
        public void setColor(DyeColor color) { this.color = color; }
        public PatternType getType() { return this.type; }
        public void setType(PatternType type) { this.type = type; }
    }

    public static class ShulkerContentInfo {
        private int invSlot;
        private ItemList item;

        public ShulkerContentInfo() {}
        public ShulkerContentInfo(int invSlot, ItemList item) {
            this.invSlot = invSlot;
            this.item = item;
        }

        public Integer getInvSlot() { return this.invSlot; }
        public void setInvSlot(int invSlot) { this.invSlot = invSlot; }
        public ItemList getItem() { return this.item; }
        public void setItem(ItemList item) { this.item = item; }
    }

    /* ---------------------------------------------------------------------------------------------------
        Serialization & deserialization
    --------------------------------------------------------------------------------------------------- */

    public static class EnchantmentSerializer extends JsonSerializer<Enchantment> {
        @Override
        public void serialize(Enchantment value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.getKey().getKey());
        }
    }
    public static class EnchantmentKeySerializer extends JsonSerializer<Enchantment> {
        @Override
        public void serialize(Enchantment value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeFieldName(value.getKey().getKey());
        }
    }
    public static class EnchantmentDeserializer extends JsonDeserializer<Enchantment> {
        @Override
        public Enchantment deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            // return Enchantment.getByName(p.getValueAsString());
            // return Registry.ENCHANTMENT.get(new NamespacedKey(NamespacedKey.MINECRAFT, p.getValueAsString()));
            Enchantment enchantment = Registry.ENCHANTMENT.get(
                NamespacedKey.minecraft(p.getValueAsString())
            );

            if (enchantment == null) {
                throw ctxt.weirdKeyException(
                    Enchantment.class,
                    p.getValueAsString(),
                    "Unknown enchantment"
                );
            }

            return enchantment;
        }
    }
    public static class EnchantmentKeyDeserializer extends KeyDeserializer {
        @Override
        public Enchantment deserializeKey(String key, DeserializationContext ctxt) throws IOException {
            // return Registry.ENCHANTMENT.get( new NamespacedKey(NamespacedKey.MINECRAFT, key) );
            Enchantment enchantment = Registry.ENCHANTMENT.get(
                NamespacedKey.minecraft(key)
            );

            if (enchantment == null) {
                throw ctxt.weirdKeyException(
                    Enchantment.class,
                    key,
                    "Unknown enchantment"
                );
            }

            return enchantment;
        }
    }

    public static class PotionEffectTypeSerializer extends JsonSerializer<PotionEffectType> {
        @Override
        public void serialize(PotionEffectType value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null || value.getKey() == null) gen.writeNull();
            else gen.writeString(value.getKey().getKey());
        }
    }

    public static class PotionEffectTypeDeserializer extends JsonDeserializer<PotionEffectType> {
        @Override
        public PotionEffectType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            //NamespacedKey key = NamespacedKey.fromString(p.getValueAsString());
            return Registry.MOB_EFFECT.get(new NamespacedKey(NamespacedKey.BUKKIT, p.getValueAsString().toLowerCase()));
        }
    }

    //MusicInstrument
    public static class MusicInstrumentSerializer extends JsonSerializer<MusicInstrument> {
        @Override
        public void serialize(MusicInstrument value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null || value.getKey() == null) gen.writeNull();
            else gen.writeString(value.getKey().getKey());
        }
    }

    public static class MusicInstrumentDeserializer extends JsonDeserializer<MusicInstrument> {
        @Override
        public MusicInstrument deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            //NamespacedKey key = NamespacedKey.fromString(p.getValueAsString());
            return Registry.INSTRUMENT.get(NamespacedKey.fromString(p.getValueAsString().toLowerCase()));
        }
    }

    //DyeColor
    public static class DyeColorSerializer extends JsonSerializer<DyeColor> {
        @Override
        public void serialize(DyeColor value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.name());
        }
    }

    public static class DyeColorDeserializer extends JsonDeserializer<DyeColor> {
        @Override
        public DyeColor deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return DyeColor.valueOf(p.getValueAsString());
        }
    }

    // banner PatternType
    public static class PatternTypeSerializer extends JsonSerializer<PatternType> {
        @Override
        public void serialize(PatternType value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.getIdentifier());
        }
    }

    public static class PatternTypeDeserializer extends JsonDeserializer<PatternType> {
        @Override
        public PatternType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return PatternType.getByIdentifier(p.getValueAsString());
        }
    }

    // armour TrimPattern
    public static class TrimPatternSerializer extends JsonSerializer<TrimPattern> {
        @Override
        public void serialize(TrimPattern value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.getKey().getKey()); //getKeyOrNull maybe
        }
    }

    public static class TrimPatternDeserializer extends JsonDeserializer<TrimPattern> {
        @Override
        public TrimPattern deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return Registry.TRIM_PATTERN.get(NamespacedKey.fromString(p.getValueAsString()));
        }
    }
    
    // armour trim material
    public static class TrimMaterialSerializer extends JsonSerializer<TrimMaterial> {
        @Override
        public void serialize(TrimMaterial value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.getKey().getKey());
        }
    }

    public static class TrimMaterialDeserializer extends JsonDeserializer<TrimMaterial> {
        @Override
        public TrimMaterial deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return Registry.TRIM_MATERIAL.get(NamespacedKey.fromString(p.getValueAsString()));
        }
    }

    // local date time
    public static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.toString());
        }
    }

    public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return LocalDateTime.parse(p.getValueAsString());
        }
    }
}

