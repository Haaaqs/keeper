package me.ihaq.keeper;

import me.ihaq.keeper.data.ConfigFile;
import me.ihaq.keeper.data.ConfigValue;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Keeper {

    private static final Map<Object, ConfigurationFile> OBJECTS = new HashMap<>();
    private final JavaPlugin plugin;

    /**
     * @param plugin instance of your plugin
     */
    public Keeper(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * The objects that contains fields that need to be loaded from the config.
     *
     * @param objects the objects you want to register
     * @return instance of this class so you can build
     */
    public Keeper register(Object... objects) {

        Arrays.stream(objects)
                .filter(obj -> obj.getClass().isAnnotationPresent(ConfigFile.class))
                .forEach(obj -> OBJECTS.put(
                        obj,
                        new ConfigurationFile(
                                plugin,
                                obj.getClass().getAnnotation(ConfigFile.class).value()
                        )
                ));

        // adding the default config values and saving the config
        OBJECTS.forEach((k, v) -> save(k, v, false));

        return this;
    }

    /**
     * @return instance of this class so you can build
     */
    public Keeper load() {
        OBJECTS.forEach((object, v) ->
                Arrays.stream(object.getClass().getDeclaredFields())
                        .filter(field -> field.isAnnotationPresent(ConfigValue.class))
                        .forEach(field -> {

                            Object value = v.getConfiguration().get(
                                    field.getAnnotation(ConfigValue.class).value()
                            );

                            if (value == null) {
                                return;
                            }

                            if (value instanceof String) {
                                value = ChatColor.translateAlternateColorCodes('&', (String) value);
                            }

                            try {
                                field.setAccessible(true);
                                field.set(object, value);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }

                        }));
        return this;
    }

    /**
     * Saves the config with updated values.
     *
     * @return instance of this class so you can build
     */
    public Keeper save() {
        OBJECTS.forEach((k, v) -> save(k, v, true));
        return this;
    }

    /**
     * Reloads the config.
     *
     * @return instance of this class so you can build
     */
    public Keeper reload() {
        save();
        load();
        return this;
    }

    /**
     * Save a config file.
     */
    private static void save(@NotNull Object object, ConfigurationFile file, boolean override) {
        file.load();

        Arrays.stream(object.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ConfigValue.class))
                .forEach(field -> {
                    try {
                        field.setAccessible(true);

                        String path = field.getAnnotation(ConfigValue.class).value();
                        Object value = field.get(object);

                        if (value instanceof String) {
                            value = ((String) value).replaceAll("" + ChatColor.COLOR_CHAR, "&");
                        }

                        if (override || file.getConfiguration().get(path) == null) {
                            System.out.println("Setting " + path + " to " + value.toString());
                            file.getConfiguration().set(path, value);
                        }

                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });

        file.save();
    }

}