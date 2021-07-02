package buttondevteam.customdimensions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Callables;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;

import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.*;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.level.progress.WorldLoadListener;
import net.minecraft.util.datafix.DataConverterRegistry;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.npc.MobSpawnerCat;
import net.minecraft.world.entity.npc.MobSpawnerTrader;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.MobSpawner;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.dimension.WorldDimension;
import net.minecraft.world.level.levelgen.GeneratorSettings;
import net.minecraft.world.level.levelgen.MobSpawnerPatrol;
import net.minecraft.world.level.levelgen.MobSpawnerPhantom;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.Convertable.ConversionSession;
import net.minecraft.world.level.storage.SaveData;
import net.minecraft.world.level.storage.WorldDataServer;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.server.packs.resources.IResourceManager;

import org.bukkit.Bukkit;
import org.bukkit.WorldType;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

public class CustomDimensions extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		getLogger().info("Loading custom dimensions...");
		try {
			load();
		} catch (Exception e) {
			e.printStackTrace();
		}
		getLogger().info("Finished loading custom dimensions!");
	}

	private void load() throws Exception {
		DedicatedServer console = ((CraftServer) Bukkit.getServer()).getServer();
		System.out.println("******************************************");
		for (Field fie : console.getClass().getSuperclass().getDeclaredFields()) {
			System.out.println(fie.getName());
		}
		Field field = console.getClass().getSuperclass().getDeclaredField("saveData");
		field.setAccessible(true);
		SaveData saveData = (SaveData) field.get(console);
		GeneratorSettings mainGenSettings = saveData.getGeneratorSettings();
		RegistryMaterials<WorldDimension> dimensionRegistry = mainGenSettings.d();

		org.bukkit.World mainWorld = Bukkit.getWorlds().get(0);

		Convertable convertable = Convertable.a(Bukkit.getWorldContainer().toPath());

		if (!getConfig().contains("ignored")) {
			getConfig().set("ignored", Lists.newArrayList("single_biome"));
			saveConfig();
		}
		List<String> ignored = getConfig().getStringList("ignored");
		int allCount = -3, loadedCount = 0, ignoredCount = 0; //-3: overworld, nether, end
		for (Entry<ResourceKey<WorldDimension>, WorldDimension> dimEntry : dimensionRegistry.d()) {
			allCount++;
			if (ignored.contains(dimEntry.getKey().a().getKey())) {
				getLogger().info(dimEntry.getKey() + " is on the ignore list");
				ignoredCount++;
				continue;
			}
			try {
				if (loadDimension(dimEntry.getKey(), dimEntry.getValue(), convertable, console, mainWorld))
					loadedCount++;
			} catch (Exception e) {
				getLogger().warning("Failed to load dimension " + dimEntry.getKey());
				e.printStackTrace();
			}
		}

	}

	private boolean loadDimension(ResourceKey<WorldDimension> dimKey, WorldDimension dimension,
	                              Convertable convertable, DedicatedServer console, org.bukkit.World mainWorld) throws IOException {
		if (dimKey == WorldDimension.a //The default dimensions are already loaded
				|| dimKey == WorldDimension.b
				|| dimKey == WorldDimension.c
				|| dimKey == WorldDimension.d)
			return false;
		ResourceKey<World> worldKey = ResourceKey.a(IRegistry.Q, dimKey.a());
		DimensionManager dimensionmanager = dimension.b();
		ChunkGenerator chunkgenerator = dimension.c();
		String name = getConfig().getString("worldNames." + dimKey.a());
		if (name == null)
			name = dimKey.a().getKey();
		if (Bukkit.getWorld(name) != null) {
			getLogger().info(name + " already loaded");
			return false;
		}
		getLogger().info("Loading " + name);
		ConversionSession session = convertable.new ConversionSession(name, dimKey) { //The original session isn't prepared for custom dimensions
			@Override
			public File a(ResourceKey<World> resourcekey) {
				return new File(this.c.toFile(), "custom");
			}
		};
		MinecraftServer.convertWorld(session);

		//Load world settings or create default values
		RegistryReadOps<NBTBase> registryreadops = RegistryReadOps.a((DynamicOps<NBTBase>) DynamicOpsNBT.a, console.aC.i(), console.getCustomRegistry());
		WorldDataServer worlddata = (WorldDataServer) session.a(registryreadops, console.datapackconfiguration);
		if (worlddata == null) {
			Properties properties = new Properties();
			properties.put("level-seed", Objects.toString(mainWorld.getSeed()));
			properties.put("generate-structures", Objects.toString(true));
			properties.put("level-type", WorldType.NORMAL.getName());
			GeneratorSettings dimGenSettings = GeneratorSettings.a(console.getCustomRegistry(), properties);
			WorldSettings worldSettings = new WorldSettings(name,
					EnumGamemode.getById(Bukkit.getDefaultGameMode().getValue()),
					false, //Hardcore
					EnumDifficulty.a, false, new GameRules(), console.datapackconfiguration);
			worlddata = new WorldDataServer(worldSettings, dimGenSettings, Lifecycle.stable());
		}

		worlddata.checkName(name);
		worlddata.a(console.getServerModName(), console.getModded().isPresent());
		if (console.options.has("forceUpgrade")) {
			net.minecraft.server.Main.convertWorld(session, DataConverterRegistry.a(),
					console.options.has("eraseCache"), () -> true,
					worlddata.getGeneratorSettings().d().d().stream()
							.map((entry2) -> ResourceKey.a(IRegistry.P, entry2.getKey().a()))
							.collect(ImmutableSet.toImmutableSet()));
		}

		List<MobSpawner> spawners = ImmutableList.of(new MobSpawnerPhantom(), new MobSpawnerPatrol(), new MobSpawnerCat(), new VillageSiege(), new MobSpawnerTrader(worlddata));

		
		ResourceKey<DimensionManager> dimManResKey = ResourceKey.a(IRegistry.P, dimKey.a());

		RegistryMaterials<DimensionManager> dimRegistry = new RegistryMaterials<DimensionManager>(null, null); //((RegistryMaterials<DimensionManager>));
		{
			MinecraftKey key = dimRegistry.getKey(dimensionmanager);
			if (key == null) { //The loaded manager is different - different dimension type
				//Replace existing dimension manager, correctly setting the ID up (which is -1 for default worlds...)
				dimRegistry.a(OptionalInt.empty(), dimManResKey, dimensionmanager, Lifecycle.stable());
			}
		}

		WorldLoadListener worldloadlistener = console.L.create(11);
		WorldServer worldserver = new WorldServer(console, console.aA, session,
				worlddata, worldKey, dimensionmanager, worldloadlistener, chunkgenerator,
				false, //isDebugWorld
				BiomeManager.a(worlddata.getGeneratorSettings().getSeed()), //Biome seed
				spawners,
				true, //Update world time
				org.bukkit.World.Environment.NORMAL, null);

		if (Bukkit.getWorld(name.toLowerCase(Locale.ENGLISH)) == null) {
			getLogger().warning("Failed to load custom dimension " + name);
			return false;
		} else {
			console.initWorld(worldserver, worlddata, worlddata, worlddata.getGeneratorSettings());
			worldserver.setSpawnFlags(true, true);
			console.R.put(worldserver.getDimensionKey(), worldserver);
			Bukkit.getPluginManager().callEvent(new WorldInitEvent(worldserver.getWorld()));
			console.loadSpawn(worldserver.getChunkProvider().a.z, worldserver);
			Bukkit.getPluginManager().callEvent(new WorldLoadEvent(worldserver.getWorld()));
			return true;
		}
	}
}
