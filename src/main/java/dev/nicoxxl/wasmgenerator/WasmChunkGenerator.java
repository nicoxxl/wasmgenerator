package dev.nicoxxl.wasmgenerator;

import static io.github.kawamuray.wasmtime.WasmValType.I32;
import static io.github.kawamuray.wasmtime.WasmValType.I64;

import io.github.kawamuray.wasmtime.*;
import io.github.kawamuray.wasmtime.Module;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

public class WasmChunkGenerator extends ChunkGenerator {
    private Store store;
    private Instance instance;
    private WasmFunctions.Function7<Long, Long, Long, Long, Long, Long, Long, Integer> main;
    private Lock lock;

    public WasmChunkGenerator() throws Exception {
        this.lock = new ReentrantLock();
        this.store = Store.withoutData();
        Engine engine = store.engine();
        Linker linker = new Linker(engine);
        Func abortFunc = WasmFunctions.wrap(store, I32, I32, I32, I32, (a, b, c, d) -> {
            System.out.println("abort " + a + b + c + d);
        });

        linker.define("env", "abort", Extern.fromFunc(abortFunc));
        Module module = Module.fromFile(engine, "./generator.wasm");
        ArrayList<Extern> externsList = new ArrayList<Extern>();
        for (Object externItemObj : linker.externs(store)) {
            ExternItem externItem = (ExternItem) externItemObj;
            externsList.add(externItem.extern());
        }

        this.instance = new Instance(store, module, externsList);

        Func entryPoint = (Func) this.instance.getFunc(store, "main").get();
        this.main = WasmFunctions.func(store, entryPoint, I64, I64, I64, I64, I64, I64, I64, I32);
    }

    public String callGen(long sx, long ex, long sy, long ey, long sz, long ez, long seed) throws Exception {
        this.lock.lock();
        try {
            int stringPointer = this.main.call(sx, ex, sy, ey, sz, ez, seed);
            ByteBuffer memory = ((Memory) this.instance.getMemory(this.store, "memory").get()).buffer(this.store);
            memory.order(ByteOrder.LITTLE_ENDIAN);
            boolean failed = true;
            int rtId;
            try {
                rtId = memory.getInt(stringPointer - 8);
                failed = false;
            } finally {
                if (failed) {
                    System.out.println("memory.position() = " + memory.position());
                    System.out.println("memory.limit() =    " + memory.limit());
                    System.out.println("memory.capacity() = " + memory.capacity());
                    System.out.println("stringPointer = " + stringPointer);
                }
            }
            if (rtId != 2) {
                throw new Exception("Wrong type :" + rtId);
            }
            int rtSize = memory.getInt(stringPointer - 4);
            ByteArrayOutputStream rawString = new ByteArrayOutputStream();
            for (int i = 0; i < rtSize; i++) {
                rawString.write(memory.get(i + stringPointer));
            }
            String blocks = rawString.toString(StandardCharsets.UTF_16LE);
            return blocks;
        } finally {
            this.lock.unlock();
        }
    }

    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        int ystep = 64;
        for (int sy = chunkData.getMinHeight(); sy < chunkData.getMaxHeight(); sy += ystep) {
            int ey = Math.min(sy + ystep, chunkData.getMaxHeight());
            int[] blocks;
            Material[] materialMap;
            try {
                String rawData = this.callGen(
                        chunkX * 16L,
                        (chunkX + 1) * 16L,
                        sy,
                        ey,
                        chunkZ * 16L,
                        (chunkZ + 1) * 16L,
                        0
                );
                System.out.println("rawData.length = " + rawData.length());

                String[] rawBlockMapAndBlocks = rawData.split("\\|");
                // System.out.println("rawBlockMapAndBlocks[0].length() = " + rawBlockMapAndBlocks[0].length() + ", rawBlockMapAndBlocks[1].length() = " + rawBlockMapAndBlocks[1].length());
                String[] blockMap = rawBlockMapAndBlocks[0].split(";");
                blocks = (int[]) Arrays.stream(rawBlockMapAndBlocks[1].split(";")).mapToInt(Integer::parseInt).toArray();
                materialMap = Stream.of(blockMap).map(Material::matchMaterial).toArray(Material[]::new);
                // System.out.println("materialMap.length = " + materialMap.length);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            int i = 0;
            for (int x = 0; x < 16; x++) {
                for (int y = sy; y < ey; y++) {
                    for (int z = 0; z < 16; z++) {
                        int blockIdx = blocks[i];
                        var material = materialMap[blockIdx];
                        chunkData.setBlock(x, y, z, material);
                        i++;
                    }
                }
            }
        }
    }
}
