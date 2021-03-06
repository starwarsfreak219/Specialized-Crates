package me.ztowne13.customcrates.crates.options.particles;

import me.ztowne13.customcrates.SpecializedCrates;
import me.ztowne13.customcrates.crates.CrateSettings;
import me.ztowne13.customcrates.interfaces.files.FileSettings;
import me.ztowne13.customcrates.interfaces.logging.StatusLoggerEvent;
import me.ztowne13.customcrates.utils.ChatUtils;
import me.ztowne13.customcrates.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Builder;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FireworkData {
    private final SpecializedCrates instance;
    private CrateSettings crateSettings;

    private String unLoaded;
    private String id;

    private Builder effect;
    private int power = 1;

    private List<String> colors = new ArrayList<>();
    private List<String> fadeColors = new ArrayList<>();
    private boolean trail = false;
    private boolean flicker = false;
    private FireworkEffect.Type feType = FireworkEffect.Type.BALL_LARGE;

    public FireworkData(SpecializedCrates instance, CrateSettings crateSettings) {
        this.instance = instance;
        this.crateSettings = crateSettings;
        this.id = UUID.randomUUID().toString().substring(0, 8);
    }

    public boolean loadFromFirework(ItemStack stack) {
        FireworkMeta fm = (FireworkMeta) stack.getItemMeta();
        setEffect(FireworkEffect.builder());
        power = fm.getPower();
        for (FireworkEffect ef : fm.getEffects()) {
            flicker = ef.hasFlicker();
            effect.flicker(flicker);

            trail = ef.hasTrail();
            effect.trail(trail);

            for (Color c : ef.getColors()) {
                colors.add(c.asRGB() + "");
                effect.withColor(c);
            }

            for (Color c : ef.getFadeColors()) {
                fadeColors.add(c.asRGB() + "");
                effect.withFade(c);
            }

            if (ef.getType() != null) {
                feType = ef.getType();
            }
            effect.with(feType);
        }

        if (colors.isEmpty()) {
            return false;
        }

        unLoaded = asString();
        return true;
    }

    public void load(String s) {
        String[] args = ChatUtils.stripFromWhitespace(s).split(FileSettings.SPLITTER_1);
        setEffect(FireworkEffect.builder());

        unLoaded = s;

        try {
            String[] splitArgs0 = args[0].split(FileSettings.SPLITTER_2);
            for (String colorUnParsed : splitArgs0) {
                if (colorUnParsed.equalsIgnoreCase("")) {
                    continue;
                }

                try {
                    if (Utils.getColorFromString(colorUnParsed) != null) {
                        Color c = Utils.getColorFromString(colorUnParsed);
                        colors.add(colorUnParsed);
                        getEffect().withColor(c);
                    } else {
                        Color c = Color.fromRGB(Integer.parseInt(colorUnParsed));
                        colors.add(colorUnParsed);
                        getEffect().withColor(c);
                    }
                } catch (Exception exc) {
                    StatusLoggerEvent.FIREWORK_DATA_INVALIDCOLOR
                            .log(getCrateSettings().getCrate(), s, colorUnParsed, "color");
                }
            }

            for (String colorUnParsed : args[1].split(";")) {
                if (colorUnParsed.equalsIgnoreCase("")) {
                    continue;
                }

                try {
                    if (Utils.getColorFromString(colorUnParsed) != null) {
                        Color c = Utils.getColorFromString(colorUnParsed);
                        fadeColors.add(colorUnParsed);
                        getEffect().withFade(c);
                    } else {
                        Color c = Color.fromRGB(Integer.parseInt(colorUnParsed));
                        fadeColors.add(colorUnParsed);
                        getEffect().withFade(c);
                    }
                } catch (Exception exc) {
                    StatusLoggerEvent.FIREWORK_DATA_INVALIDCOLOR
                            .log(getCrateSettings().getCrate(), s, colorUnParsed, "fade");
                }
            }

            String cause = args[2] + " is not true / false.";

            try {
                Boolean b = Boolean.valueOf(args[2].toLowerCase());
                getEffect().trail(b);
                trail = b;

                cause = "Improperly formatted FLICKER";
                cause = args[3] + " is not true / false.";

                b = Boolean.valueOf(args[3].toLowerCase());
                getEffect().flicker(b);
                flicker = b;

                cause = "Improperly formatted TYPE";
                cause = args[4] + " is not a valid Firework Effect Type.";

                FireworkEffect.Type ft = FireworkEffect.Type.valueOf(args[4].toUpperCase());
                getEffect().with(ft);
                feType = ft;

                cause = "Improperly formatted POWER";
                cause = args[5] + " is not a valid number / power.";
                setPower(Integer.valueOf(args[5]));

                StatusLoggerEvent.FIREWORK_DATA_SUCCESS.log(getCrateSettings().getCrate(), s);
            } catch (Exception exc) {
                StatusLoggerEvent.FIREWORK_DATA_PARTIALSUCCESS.log(getCrateSettings().getCrate(), s, cause);
            }
        } catch (Exception exc) {
            StatusLoggerEvent.FIREWORK_DATA_FAILURE.log(getCrateSettings().getCrate(), s);
        }

    }

    public void play(Location l) {
        final Firework fw = (Firework) l.getWorld().spawnEntity(l, EntityType.FIREWORK);
        FireworkMeta fm = fw.getFireworkMeta();
        fw.setCustomName("scf");
        fw.setCustomNameVisible(false);
        fm.addEffect(getEffect().build());
        fm.setPower(getPower());
        fw.setFireworkMeta(fm);

        if (getPower() == 0) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(instance, fw::detonate, 2);
        }
    }

    // Formatted color;color, fade color;fade color, trail?, flicker?, effect type, power
    public String asString() {
        String serializedFw = "";
        for (String color : getColors()) {
            serializedFw = color + ";";
        }

        if (getColors().isEmpty()) {
            serializedFw += ";";
        }
        serializedFw = serializedFw.substring(0, serializedFw.length() - 1) + ", ";

        for (String color : getFadeColors()) {
            serializedFw = color + ";";
        }

        if (getFadeColors().isEmpty()) {
            serializedFw += ";";
        }

        serializedFw = serializedFw.substring(0, serializedFw.length() - 1);
        serializedFw += ", " + isTrail();
        serializedFw += ", " + isFlicker();
        serializedFw += ", " + getFeType().name();
        serializedFw += ", " + getPower();

        return serializedFw;
    }

    public boolean equals(FireworkData fd) {
        return fd.toString().equalsIgnoreCase(toString());
    }

    public String toString() {
        return unLoaded;
    }

    public CrateSettings getCrateSettings() {
        return crateSettings;
    }

    public void setCrateSettings(CrateSettings crateSettings) {
        this.crateSettings = crateSettings;
    }

    public Builder getEffect() {
        return effect;
    }

    public void setEffect(Builder effect) {
        this.effect = effect;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public boolean isTrail() {
        return trail;
    }

    public void setTrail(boolean trail) {
        this.trail = trail;
    }

    public boolean isFlicker() {
        return flicker;
    }

    public void setFlicker(boolean flicker) {
        this.flicker = flicker;
    }

    public FireworkEffect.Type getFeType() {
        return feType;
    }

    public void setFeType(FireworkEffect.Type feType) {
        this.feType = feType;
    }

    public List<String> getColors() {
        return colors;
    }

    public void setColors(List<String> colors) {
        this.colors = colors;
    }

    public List<String> getFadeColors() {
        return fadeColors;
    }

    public void setFadeColors(List<String> fadeColors) {
        this.fadeColors = fadeColors;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
