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
	/*@Command2.Subcommand
	public void def(CommandSender sender, String name) {
		*sender.sendMessage("Starting creation of " + name + "...");
		var world = load();
		if (world == null)
			sender.sendMessage("Failed to load world.");
		else
			sender.sendMessage("World loaded! " + world.getName());*
	}*/

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
		System.out.println("Loading...");
		var console = ((CraftServer) Bukkit.getServer()).getServer();
		var field = console.getClass().getSuperclass().getDeclaredField("saveData");
		field.setAccessible(true);
		var saveData = (SaveData) field.get(console);
		//IWorldDataServer iworlddataserver = saveData.H();
		GeneratorSettings generatorsettings = saveData.getGeneratorSettings();
		RegistryMaterials<WorldDimension> registrymaterials = generatorsettings.d();
		var worldloadlistener = console.worldLoadListenerFactory.create(11);

		var iterator = registrymaterials.d().iterator();

		var mainWorld = Bukkit.getWorlds().get(0);

		var convertable = Convertable.a(Bukkit.getWorldContainer().toPath());

		while (iterator.hasNext()) {
			Map.Entry<ResourceKey<WorldDimension>, WorldDimension> entry = iterator.next();
			ResourceKey<WorldDimension> resourcekey = entry.getKey();

			if (resourcekey != WorldDimension.OVERWORLD
					&& resourcekey != WorldDimension.THE_NETHER
					&& resourcekey != WorldDimension.THE_END) {
				ResourceKey<World> resourcekey1 = ResourceKey.a(IRegistry.L, resourcekey.a());
				DimensionManager dimensionmanager = entry.getValue().b();
				ChunkGenerator chunkgenerator = entry.getValue().c(); //TODO: Shade
				var name = resourcekey.a().getKey();
				var session = convertable.new ConversionSession(name, resourcekey) { //The original session isn't prepared for custom dimensions
					@Override
					public File a(ResourceKey<World> resourcekey) {
						return new File(this.folder.toFile(), "custom");
					}
				};
				MinecraftServer.convertWorld(session);

				//Load world settings and create a custom
				RegistryReadOps<NBTBase> registryreadops = RegistryReadOps.a(DynamicOpsNBT.a, console.dataPackResources.h(), console.f);
				WorldDataServer worlddata = (WorldDataServer) session.a(registryreadops, console.datapackconfiguration);
				if (worlddata == null) {
					Properties properties = new Properties();
					properties.put("level-seed", Objects.toString(mainWorld.getSeed()));
					properties.put("generate-structures", Objects.toString(true));
					properties.put("level-type", WorldType.NORMAL.getName());
					GeneratorSettings generatorsettings2 = GeneratorSettings.a(console.aX(), properties);
					WorldSettings worldSettings = new WorldSettings(name,
							EnumGamemode.getById(Bukkit.getDefaultGameMode().getValue()),
							false, //Hardcore
							EnumDifficulty.EASY, false, new GameRules(), console.datapackconfiguration);
					worlddata = new WorldDataServer(worldSettings, generatorsettings2, Lifecycle.stable());
				}

				var data = DimensionWorldData.create(worlddata, name, EnumGamemode.CREATIVE);

				worlddata.checkName(name);
				worlddata.a(console.getServerModName(), console.getModded().isPresent());
				if (console.options.has("forceUpgrade")) {
					net.minecraft.server.v1_16_R2.Main.convertWorld(session, DataConverterRegistry.a(),
							console.options.has("eraseCache"), () -> true,
							worlddata.getGeneratorSettings().d().d().stream()
									.map((entry2) -> ResourceKey.a(IRegistry.K, entry2.getKey().a()))
									.collect(ImmutableSet.toImmutableSet()));
				}

				List<MobSpawner> list = ImmutableList.of(new MobSpawnerPhantom(), new MobSpawnerPatrol(), new MobSpawnerCat(), new VillageSiege(), new MobSpawnerTrader(worlddata));

				//Register dimension manager in registry
				ResourceKey<DimensionManager> dimManResKey = ResourceKey.a(IRegistry.K, resourcekey.a());
				((RegistryMaterials<DimensionManager>) console.f.a()).a(dimManResKey, dimensionmanager, Lifecycle.stable());


				//RegistryMaterials<WorldDimension> registrymaterials = worlddata.getGeneratorSettings().d();
				//WorldDimension worlddimension = (WorldDimension) registrymaterials2.a(actualDimension);
				//Use the main world's dimension data

				WorldServer worldserver = new WorldServer(console, console.executorService, session,
						data, resourcekey1, dimensionmanager, worldloadlistener, chunkgenerator,
						false, //isDebugWorld
						BiomeManager.a(mainWorld.getSeed()), //Biome seed
						list, false, org.bukkit.World.Environment.NORMAL, null);

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
		System.out.println("Loading finished!");
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
