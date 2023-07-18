package dev.nicoxxl.wasmgenerator;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public final class WasmGenerator extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        try {
            return new WasmChunkGenerator();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
