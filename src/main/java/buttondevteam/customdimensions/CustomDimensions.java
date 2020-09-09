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

	public void load() throws Exception {
		getLogger().info("Loading...");
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

			if (dimKey != WorldDimension.OVERWORLD
					&& dimKey != WorldDimension.THE_NETHER
					&& dimKey != WorldDimension.THE_END) {
				System.out.println("First check");
				for (var dimMan : console.f.a()) {
					try {
						System.out.println("Found dim man for key: " + console.f.a().getKey(dimMan));
						System.out.println("Resource key: " + console.f.a().c(dimMan));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				ResourceKey<World> worldKey = ResourceKey.a(IRegistry.L, dimKey.a());
				DimensionManager dimensionmanager = dimEntry.getValue().b();
				ChunkGenerator chunkgenerator = dimEntry.getValue().c(); //TODO: Shade
				var name = dimKey.a().getKey();
				var session = convertable.new ConversionSession(name, dimKey) { //The original session isn't prepared for custom dimensions
					@Override
					public File a(ResourceKey<World> resourcekey) {
						return new File(this.folder.toFile(), "custom");
					}
				};
				MinecraftServer.convertWorld(session);

				//Load world settings or create default values
				RegistryReadOps<NBTBase> registryreadops = RegistryReadOps.a(DynamicOpsNBT.a, console.dataPackResources.h(), console.f);
				WorldDataServer worlddata = (WorldDataServer) session.a(registryreadops, console.datapackconfiguration);
				if (worlddata == null) {
					Properties properties = new Properties();
					properties.put("level-seed", Objects.toString(mainWorld.getSeed()));
					properties.put("generate-structures", Objects.toString(true));
					properties.put("level-type", WorldType.NORMAL.getName());
					GeneratorSettings dimGenSettings = GeneratorSettings.a(console.aX(), properties);
					WorldSettings worldSettings = new WorldSettings(name,
							EnumGamemode.getById(Bukkit.getDefaultGameMode().getValue()),
							false, //Hardcore
							EnumDifficulty.EASY, false, new GameRules(), console.datapackconfiguration);
					worlddata = new WorldDataServer(worldSettings, dimGenSettings, Lifecycle.stable());
				}

				//Create a custom WorldDataServer that is aware that it's a dimension and delegates most calls to the main world
				var data = DimensionWorldData.create(worlddata, name);

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

				//in bf but not in bi
				System.out.println("Second check");
				ResourceKey<DimensionManager> dimManKey = null;
				int lastID = -1;
				for (var dimMan : console.f.a()) {
					try {
						System.out.println("Found dim man for key: " + console.f.a().getKey(dimMan));
						var key = console.f.a().c(dimMan);
						System.out.println("Resource key: " + key);
						int id = console.f.a().a(dimensionmanager);
						System.out.println("ID: " + id);
						if (id > lastID) lastID = id;
						if (key.isPresent() && key.get().a().getKey().equals(name)) {
							//Register dimension manager in registry
							//var originalManagerID = console.f.a().a(dimensionmanager); - -1
							//System.out.println("Replacing dimension manager with ID " + originalManagerID);
							dimManKey = key.get();
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				if (dimManKey != null) {
					ResourceKey<DimensionManager> dimManResKey = ResourceKey.a(IRegistry.K, dimKey.a());
					System.out.println("Replacing with " + dimManResKey);
					((RegistryMaterials<DimensionManager>) console.f.a()).a(OptionalInt.empty(), dimManKey, dimensionmanager, Lifecycle.stable());
				}

				//in bf but not in bi
				System.out.println("Third check");
				for (var dimMan : console.f.a()) {
					try {
						System.out.println("Found dim man for key: " + console.f.a().getKey(dimMan));
						System.out.println("Resource key: " + console.f.a().c(dimMan));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				var worldloadlistener = console.worldLoadListenerFactory.create(11);

				//RegistryMaterials<WorldDimension> registrymaterials = worlddata.getGeneratorSettings().d();
				//WorldDimension worlddimension = (WorldDimension) registrymaterials2.a(actualDimension);
				//Use the main world's dimension data

				WorldServer worldserver = new WorldServer(console, console.executorService, session,
						data, worldKey, dimensionmanager, worldloadlistener, chunkgenerator,
						false, //isDebugWorld
						BiomeManager.a(mainWorld.getSeed()), //Biome seed
						spawners, false, org.bukkit.World.Environment.NORMAL, null);
				data.world = worldserver; //Mocked world data
				worlddata.world = worldserver; //Inner world data

				//((CraftWorld) mainWorld).getHandle().getWorldBorder().a(new IWorldBorderListener.a(worldserver.getWorldBorder()));

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
		getLogger().info("Loading finished!");
	}

	/*private WorldServer createWorldServer(WorldDataMutable worldData, String name) throws Exception {
		var obj = new ObjenesisStd();
		var ws = obj.newInstance(WorldServer.class);
		ws.spigotConfig = new SpigotWorldConfig(name);
		//ws.generator = gen;
		var field = ws.getClass().getDeclaredField("world");
		field.setAccessible(true);
		field.set(ws, new CraftWorld(ws, null, org.bukkit.World.Environment.NORMAL));
		ws.ticksPerAnimalSpawns = ws.getServer().getTicksPerAnimalSpawns();
		ws.ticksPerMonsterSpawns = ws.getServer().getTicksPerMonsterSpawns();
		...
	}*/

	/*public World load(String name) {
		File folder = new File(Bukkit.getWorldContainer(), name);
		World world = Bukkit.getWorld(name);
		if (world != null) {
			return world;
		} else if (folder.exists() && !folder.isDirectory()) {
			throw new IllegalArgumentException("File exists with the name '" + name + "' and isn't a folder");
		} else {
			ResourceKey<WorldDimension> actualDimension = ResourceKey.a(IRegistry.M, new MinecraftKey(name));

			Convertable.ConversionSession worldSession;
			try {
				worldSession = Convertable.a(Bukkit.getWorldContainer().toPath()).c(name, actualDimension);
			} catch (IOException var21) {
				throw new RuntimeException(var21);
			}

			var server = (CraftServer) Bukkit.getServer();
			var console = server.getServer();

			MinecraftServer.convertWorld(worldSession);
			RegistryReadOps<NBTBase> registryreadops = RegistryReadOps.a(DynamicOpsNBT.a, console.dataPackResources.h(), console.f);
			WorldDataServer worlddata = (WorldDataServer) worldSession.a(registryreadops, console.datapackconfiguration);
			if (worlddata == null) {
				System.out.println("No world data found in datapacks!");
				System.out.println("folder: "+worldSession.folder);
				System.out.println("level name: "+worldSession.getLevelName());
				Bukkit.getWorlds()
				//World folder: Minecraft only supports the three default types for the main world
				//Therefore we can't load a separate world with a custom dimension
				//How about just pretending we did
				return null;
			}

			worlddata.checkName(name);
			worlddata.a(console.getServerModName(), console.getModded().isPresent());
			if (console.options.has("forceUpgrade")) {
				Main.convertWorld(worldSession, DataConverterRegistry.a(), console.options.has("eraseCache"),
					() -> true, worlddata.getGeneratorSettings().d().d().stream().map((entry) -> ResourceKey.a(IRegistry.K,
						entry.getKey().a())).collect(ImmutableSet.toImmutableSet()));
			}

			long j = BiomeManager.a(Bukkit.getWorlds().get(0).getSeed());
			List<MobSpawner> list = ImmutableList.of(new MobSpawnerPhantom(), new MobSpawnerPatrol(), new MobSpawnerCat(), new VillageSiege(), new MobSpawnerTrader(worlddata));
			RegistryMaterials<WorldDimension> registrymaterials = worlddata.getGeneratorSettings().d();
			WorldDimension worlddimension = registrymaterials.a(actualDimension);
			DimensionManager dimensionmanager;
			ChunkGenerator chunkgenerator;
			if (worlddimension == null) {
				//dimensionmanager = console.f.a().d(DimensionManager.OVERWORLD);
				//chunkgenerator = GeneratorSettings.a(console.f.b(IRegistry.ay), console.f.b(IRegistry.ar), (new Random()).nextLong());
				System.out.println("World dimension is null!");
				return null;
			} else {
				dimensionmanager = worlddimension.b();
				chunkgenerator = worlddimension.c();
			}

			var environment = World.Environment.NORMAL;

			ResourceKey<net.minecraft.server.v1_16_R2.World> worldKey = ResourceKey.a(IRegistry.L, new MinecraftKey(name.toLowerCase(Locale.ENGLISH)));
			WorldServer internal = new WorldServer(console, console.executorService, worldSession, worlddata, worldKey,
				dimensionmanager, console.worldLoadListenerFactory.create(11), chunkgenerator,
				worlddata.getGeneratorSettings().isDebugWorld(), j, environment == World.Environment.NORMAL ? list : ImmutableList.of(),
				true, environment, null);
			if (Bukkit.getWorld(name.toLowerCase(Locale.ENGLISH)) == null) {
				System.out.println("Newly created world not found!");
				return null;
			} else {
				console.initWorld(internal, worlddata, worlddata, worlddata.getGeneratorSettings());
				internal.setSpawnFlags(true, true);
				console.worldServer.put(internal.getDimensionKey(), internal);
				server.getPluginManager().callEvent(new WorldInitEvent(internal.getWorld()));
				console.loadSpawn(internal.getChunkProvider().playerChunkMap.worldLoadListener, internal);
				server.getPluginManager().callEvent(new WorldLoadEvent(internal.getWorld()));
				return internal.getWorld();
			}
		}
	}*/
}
