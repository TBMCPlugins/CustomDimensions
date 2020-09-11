package buttondevteam.customdimensions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Lifecycle;
import net.minecraft.server.v1_16_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.WorldType;
import org.bukkit.craftbukkit.v1_16_R2.CraftServer;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class CustomDimensions extends JavaPlugin implements Listener {
	@Override
	public void onEnable() {
		//Bukkit.getPluginManager().registerEvents(this, this);
		getLogger().info("Loading custom dimensions...");
		try {
			load();
		} catch (Exception e) {
			e.printStackTrace();
		}
		getLogger().info("Finished loading custom dimensions!");
	}

	private void load() throws Exception {
		var console = ((CraftServer) Bukkit.getServer()).getServer();
		var field = console.getClass().getSuperclass().getDeclaredField("saveData");
		field.setAccessible(true);
		var saveData = (SaveData) field.get(console);
		GeneratorSettings mainGenSettings = saveData.getGeneratorSettings();
		RegistryMaterials<WorldDimension> dimensionRegistry = mainGenSettings.d();

		var dimIterator = dimensionRegistry.d().iterator();

		var mainWorld = Bukkit.getWorlds().get(0);

		var convertable = Convertable.a(Bukkit.getWorldContainer().toPath());

		while (dimIterator.hasNext()) {
			Map.Entry<ResourceKey<WorldDimension>, WorldDimension> dimEntry = dimIterator.next();
			ResourceKey<WorldDimension> dimKey = dimEntry.getKey();

			if (dimKey != WorldDimension.OVERWORLD //The default dimensions are already loaded
					&& dimKey != WorldDimension.THE_NETHER
					&& dimKey != WorldDimension.THE_END) {
				ResourceKey<World> worldKey = ResourceKey.a(IRegistry.L, dimKey.a());
				DimensionManager dimensionmanager = dimEntry.getValue().b();
				ChunkGenerator chunkgenerator = dimEntry.getValue().c();
				var name = dimKey.a().getKey();
				getLogger().info("Loading " + name);
				var session = convertable.new ConversionSession(name, dimKey) { //The original session isn't prepared for custom dimensions
					@Override
					public File a(ResourceKey<World> resourcekey) {
						return new File(this.folder.toFile(), "custom");
					}
				};
				MinecraftServer.convertWorld(session);

				//Load world settings or create default values
				RegistryReadOps<NBTBase> registryreadops = RegistryReadOps.a(DynamicOpsNBT.a, console.dataPackResources.h(), console.customRegistry);
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
							EnumDifficulty.EASY, false, new GameRules(), console.datapackconfiguration);
					worlddata = new WorldDataServer(worldSettings, dimGenSettings, Lifecycle.stable());
				}

				worlddata.checkName(name);
				worlddata.a(console.getServerModName(), console.getModded().isPresent());
				if (console.options.has("forceUpgrade")) {
					net.minecraft.server.v1_16_R2.Main.convertWorld(session, DataConverterRegistry.a(),
							console.options.has("eraseCache"), () -> true,
							worlddata.getGeneratorSettings().d().d().stream()
									.map((entry2) -> ResourceKey.a(IRegistry.K, entry2.getKey().a()))
									.collect(ImmutableSet.toImmutableSet()));
				}

				List<MobSpawner> spawners = ImmutableList.of(new MobSpawnerPhantom(), new MobSpawnerPatrol(), new MobSpawnerCat(), new VillageSiege(), new MobSpawnerTrader(worlddata));

				ResourceKey<DimensionManager> dimManResKey = ResourceKey.a(IRegistry.K, dimKey.a());
				//Replace existing dimension manager, correctly setting the ID up (which is -1 for default worlds...)
				((RegistryMaterials<DimensionManager>) console.customRegistry.a()).a(OptionalInt.empty(), dimManResKey, dimensionmanager, Lifecycle.stable());

				var worldloadlistener = console.worldLoadListenerFactory.create(11);

				WorldServer worldserver = new WorldServer(console, console.executorService, session,
						worlddata, worldKey, dimensionmanager, worldloadlistener, chunkgenerator,
						false, //isDebugWorld
						BiomeManager.a(worlddata.getGeneratorSettings().getSeed()), //Biome seed
						spawners, false, org.bukkit.World.Environment.NORMAL, null);

				if (Bukkit.getWorld(name.toLowerCase(Locale.ENGLISH)) == null) {
					getLogger().warning("Failed to load custom dimension " + name);
				} else {
					console.initWorld(worldserver, worlddata, worlddata, worlddata.getGeneratorSettings());
					worldserver.setSpawnFlags(true, true);
					console.worldServer.put(worldserver.getDimensionKey(), worldserver);
					Bukkit.getPluginManager().callEvent(new WorldInitEvent(worldserver.getWorld()));
					console.loadSpawn(worldserver.getChunkProvider().playerChunkMap.worldLoadListener, worldserver);
					Bukkit.getPluginManager().callEvent(new WorldLoadEvent(worldserver.getWorld()));
				}
			}
		}
	}
}
