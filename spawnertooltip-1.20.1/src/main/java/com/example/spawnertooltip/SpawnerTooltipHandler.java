package com.example.spawnertooltip;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Builds the extra tooltip lines shown for custom spawner items.
 *
 * Original (1.21.4) version read a "gs_marker" flag and related fields from
 * the DataComponentTypes.CUSTOM_DATA component of the ItemStack. In 1.20.1
 * there is no component system yet, so the same keys are read directly from
 * the ItemStack's classic NBT compound (ItemStack#getNbt()). If the server
 * plugin that writes this data nests it under a sub-tag on 1.20.1 instead of
 * the item root tag, adjust ROOT_TAG_KEY below (see comment near appendInfo).
 */
public final class SpawnerTooltipHandler {

    private static final int COLOR_HEADER = 0x55FFFF;
    private static final int COLOR_LABEL = 0xAAAAAA;
    private static final int COLOR_VALUE = 0xFFD500;
    private static final int COLOR_SWORD_NAME = 0x55FFFF;
    private static final int COLOR_ENCHANT = 0xD580FF;
    private static final int COLOR_ENCHANT_LVL = 0xB266FF;
    private static final int COLOR_ERROR = 0xFF5555;
    private static final int COLOR_MUTED = 0x555555;

    private static final Pattern BROKEN_HEX =
            Pattern.compile("\u00a7x(?:\u00a7[0-9a-fA-F]){0,5}(\u00a7[0-9a-fA-F])?(?!\u00a7[0-9a-fA-F])");

    private static final Map<String, String> RU_MOB_NAMES = Map.ofEntries(
            Map.entry("MAGMA_CUBE", "Магма-куб"),
            Map.entry("ZOMBIE", "Зомби"),
            Map.entry("SKELETON", "Скелет"),
            Map.entry("CREEPER", "Крипер"),
            Map.entry("SPIDER", "Паук"),
            Map.entry("ENDERMAN", "Эндермен"),
            Map.entry("BLAZE", "Блейз"),
            Map.entry("WITCH", "Ведьма"),
            Map.entry("PIGLIN", "Пиглин"),
            Map.entry("WITHER_SKELETON", "Визер-скелет"),
            Map.entry("SLIME", "Слизень"),
            Map.entry("PHANTOM", "Фантом"),
            Map.entry("CHICKEN", "Курица"),
            Map.entry("PIG", "Свинья"),
            Map.entry("HOGLIN", "Хоглин")
    );

    private static final Map<String, String> RU_ENCHANT_NAMES = Map.ofEntries(
            Map.entry("sharpness", "Острота"),
            Map.entry("smite", "Небесная кара"),
            Map.entry("bane_of_arthropods", "Бич членистоногих"),
            Map.entry("knockback", "Отдача"),
            Map.entry("fire_aspect", "Заговор огня"),
            Map.entry("looting", "Добыча"),
            Map.entry("sweeping", "Разящий клинок"),
            Map.entry("unbreaking", "Прочность"),
            Map.entry("mending", "Починка"),
            Map.entry("vanishing_curse", "Проклятие утраты"),
            Map.entry("binding_curse", "Проклятие несъёмности"),
            Map.entry("efficiency", "Эффективность"),
            Map.entry("fortune", "Удача"),
            Map.entry("silk_touch", "Шёлковое касание"),
            Map.entry("protection", "Защита"),
            Map.entry("fire_protection", "Огнеупорность"),
            Map.entry("blast_protection", "Взрывоустойчивость"),
            Map.entry("projectile_protection", "Защита от снарядов"),
            Map.entry("feather_falling", "Невесомость"),
            Map.entry("respiration", "Подводное дыхание"),
            Map.entry("aqua_affinity", "Подводник"),
            Map.entry("depth_strider", "Подводная ходьба"),
            Map.entry("frost_walker", "Ледоход"),
            Map.entry("power", "Сила"),
            Map.entry("punch", "Ударная волна"),
            Map.entry("flame", "Воспламенение"),
            Map.entry("infinity", "Бесконечность"),
            Map.entry("luck_of_the_sea", "Везучий рыбак"),
            Map.entry("lure", "Приманка"),
            Map.entry("critical-enchant-custom", "Критический"),
            Map.entry("destroyer-enchant-custom", "Разрушитель"),
            Map.entry("rich-enchant-custom", "Богач"),
            Map.entry("mob-farmer-enchant", "Фармер"),
            Map.entry("EVOKER", "Вызыватель")
    );

    private static final Map<String, String> SWORD_TYPE_NAMES = Map.of(
            "minecraft:diamond_sword", "Алмазный меч",
            "minecraft:netherite_sword", "Незеритовый меч",
            "minecraft:iron_sword", "Железный меч",
            "minecraft:wooden_sword", "Деревянный меч",
            "minecraft:golden_sword", "Золотой меч"
    );

    private SpawnerTooltipHandler() {
    }

    public static void appendInfo(ItemStack stack, List<Text> lines) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains("gs_marker")) {
            return;
        }

        lines.add(divider());
        lines.add(colored("\u2699 Данные спавнера", COLOR_HEADER, true));

        if (nbt.contains("gs_sword_item")) {
            appendSwordInfo(nbt.getString("gs_sword_item"), lines);
        }

        long total = nbt.getLong("gs_kills_total");
        long remaining = nbt.getLong("gs_kills_remaining");
        if (total > 0L) {
            long killed = Math.max(0L, Math.min(total, total - remaining));
            int percent = (int) Math.round(killed * 100.0 / total);
            lines.add(blank());
            lines.add(statLine("Убито мобов",
                    formatNumber(killed) + " / " + formatNumber(total) + "  (" + percent + "%)"));
        }

        if (nbt.contains("gs_mob_type")) {
            String raw = nbt.getString("gs_mob_type");
            lines.add(statLine("Тип моба", RU_MOB_NAMES.getOrDefault(raw, raw)));
        }

        if (nbt.contains("gs_total_eggs")) {
            lines.add(statLine("Вставлено яиц", String.valueOf(nbt.getInt("gs_total_eggs"))));
        }

        lines.add(divider());
    }

    private static void appendSwordInfo(String rawJson, List<Text> lines) {
        if (rawJson == null || rawJson.isBlank()) {
            return;
        }

        JsonObject sword;
        try {
            sword = JsonParser.parseString(rawJson).getAsJsonObject();
        } catch (Exception e) {
            lines.add(blank());
            lines.add(colored("Ошибка чтения данных меча", COLOR_ERROR, false));
            return;
        }

        lines.add(blank());
        lines.add(colored("\u2694 Меч: ", COLOR_MUTED, false)
                .append(colored(swordName(sword), COLOR_SWORD_NAME, false)));

        if (sword.has("enchants") && sword.get("enchants").isJsonObject()) {
            JsonObject enchants = sword.getAsJsonObject("enchants");
            lines.add(colored("  Чары:", COLOR_LABEL, false));
            for (String key : enchants.keySet()) {
                lines.add(enchantLine(key, enchants.get(key)));
            }
        }
    }

    private static Text enchantLine(String key, JsonElement value) {
        try {
            int level = value.getAsInt();
            return colored("   \u2022 ", COLOR_MUTED, false)
                    .append(colored(formatEnchantName(key), COLOR_ENCHANT, false))
                    .append(colored(" " + toRoman(level), COLOR_ENCHANT_LVL, false));
        } catch (Exception e) {
            return colored("   \u2022 " + key + " (ошибка чтения уровня)", COLOR_ERROR, false);
        }
    }

    private static MutableText colored(String text, int rgb, boolean bold) {
        return Text.literal(text).styled(style -> style.withColor(TextColor.fromRgb(rgb)).withBold(bold));
    }

    private static Text statLine(String label, String value) {
        return colored("\u258d " + label + ": ", COLOR_LABEL, false)
                .append(colored(value, COLOR_VALUE, false));
    }

    private static Text divider() {
        return Text.literal("                                ")
                .styled(style -> style.withColor(TextColor.fromRgb(COLOR_MUTED)).withStrikethrough(true));
    }

    private static Text blank() {
        return Text.literal("");
    }

    private static String swordName(JsonObject sword) {
        try {
            if (sword.has("name")) {
                String name = cleanupName(sword.get("name").getAsString());
                if (!name.isBlank()) {
                    return name;
                }
            }
        } catch (Exception ignored) {
        }
        if (sword.has("type")) {
            return SWORD_TYPE_NAMES.getOrDefault(sword.get("type").getAsString(), "Меч");
        }
        return "Неизвестный меч";
    }

    private static String cleanupName(String s) {
        return BROKEN_HEX.matcher(s).replaceAll("").trim();
    }

    private static String formatEnchantName(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String translated = RU_ENCHANT_NAMES.get(path);
        if (translated != null) {
            return translated;
        }
        String pretty = path.replace("-enchant-custom", "").replace('-', ' ').replace('_', ' ');
        return capitalize(pretty);
    }

    private static String capitalize(String s) {
        if (s.isBlank()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String toRoman(int number) {
        if (number <= 0 || number > 20) {
            return String.valueOf(number);
        }
        String[] romans = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
                "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};
        return romans[number];
    }

    private static String formatNumber(long n) {
        return String.format("%,d", n).replace(',', ' ');
    }
}
