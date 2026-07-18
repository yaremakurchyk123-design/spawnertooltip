# Spawner Tooltip HolyWorld — порт под 1.20.1

Это перенос вашего мода `spawner-tooltip-1_0_0.jar` (изначально собран под
Fabric 1.21.4) на Minecraft **1.20.1**.

## Что изменилось при переносе

1.21.4 использует систему **Data Components** — данные предмета читались через
`DataComponentTypes.CUSTOM_DATA`. В 1.20.1 такой системы ещё нет, поэтому
кастомные теги (`gs_marker`, `gs_sword_item`, `gs_kills_total`,
`gs_kills_remaining`, `gs_mob_type`, `gs_total_eggs`) теперь читаются напрямую
из классического NBT предмета через `ItemStack#getNbt()`.

Также обновлена сигнатура события тултипа:
`net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback` в 1.20.1 имеет
параметры `(ItemStack stack, TooltipContext context, List<Text> lines)` —
без `TooltipType`, который появился только в 1.21.

Вся остальная логика (переводы названий мобов/чар, разбор JSON меча, римские
цифры для уровней чар, форматирование чисел, цвета текста) перенесена без
изменений.

⚠️ **Важно**: если серверный плагин, который записывает эти NBT-теги на
1.20.1, кладёт их не в корень тега предмета, а во вложенный подтег (например
`PublicBukkitValues` при работе через NBT-API/PersistentDataContainer на
Spigot/Paper), нужно поправить чтение в
`SpawnerTooltipHandler.appendInfo()` — сначала получить вложенный
`NbtCompound`, а потом уже читать из него `gs_*` ключи. Пришлите пример NBT
предмета (`/give` + `F3+H` или NBT explorer), и я поправлю точный путь.

## Структура проекта

```
spawnertooltip-1.20.1/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── LICENSE_spawner-tooltip
└── src/main/
    ├── java/com/example/spawnertooltip/
    │   ├── SpawnerTooltipClient.java
    │   └── SpawnerTooltipHandler.java
    └── resources/
        ├── fabric.mod.json
        └── ico.png
```

## Сборка

Нужен установленный JDK 17+ и интернет-доступ (Gradle скачает Fabric Loom,
Minecraft 1.20.1, маппинги Yarn и Fabric API — из песочницы, где собирался
этот ответ, скачать их нельзя, поэтому jar нужно собрать у себя).

```bash
cd spawnertooltip-1.20.1
./gradlew build
```

(Если нет `gradlew` — запустите `gradle wrapper` один раз, имея Gradle 8+
установленным, либо откройте проект в IntelliJ IDEA с плагином Fabric —
он сам сгенерирует wrapper.)

Готовый jar появится в `build/libs/spawnertooltip-1.0.0.jar`.

## Установка

Скопируйте собранный jar в папку `mods` клиента Fabric 1.20.1
(Fabric Loader ≥0.15.11, Fabric API для 1.20.1).
