package buttondevteam.customdimensions;

import com.google.common.collect.ImmutableList;
import net.minecraft.server.v1_16_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R2.CraftServer;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R2.SpigotTimings;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.spigotmc.SpigotWorldConfig;
import org.spigotmc.TickLimiter;

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
		Bukkit.getPluginManager().registerEvents(this, this);
	}

	@EventHandler
	public void worldLoad(WorldLoadEvent event) {
		System.out.println("World loaded: " + event.getWorld().getName());
		if (!event.getWorld().getName().equals(Bukkit.getWorlds().get(0).getName()))
			return;
		System.out.println("Main world");
		try {
			load();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void load() throws Exception {
		System.out.println("Loading...");
		var console = ((CraftServer) Bukkit.getServer()).getServer();
		var field = console.getClass().getSuperclass().getDeclaredField("saveData");
		field.setAccessible(true);
		var saveData = (SaveData) field.get(console);
		IWorldDataServer iworlddataserver = saveData.H();
		GeneratorSettings generatorsettings = saveData.getGeneratorSettings();
		RegistryMaterials<WorldDimension> registrymaterials = generatorsettings.d();
		var worldloadlistener = console.worldLoadListenerFactory.create(11);

		var iterator = registrymaterials.d().iterator();

		var mainWorld = Bukkit.getWorlds().get(0);

		while (iterator.hasNext()) {
			Map.Entry<ResourceKey<WorldDimension>, WorldDimension> entry = iterator.next();
			ResourceKey<WorldDimension> resourcekey = entry.getKey();

			if (resourcekey != WorldDimension.OVERWORLD) {
				ResourceKey<World> resourcekey1 = ResourceKey.a(IRegistry.L, resourcekey.a());
				DimensionManager dimensionmanager1 = entry.getValue().b();
				ChunkGenerator chunkgenerator = entry.getValue().c();
				SecondaryWorldData secondaryworlddata = new SecondaryWorldData(saveData, iworlddataserver);
				WorldServer worldserver1 = new WorldServer(console, console.executorService, console.convertable,
						secondaryworlddata, resourcekey1, dimensionmanager1, worldloadlistener, chunkgenerator,
						false, //isDebugWorld
						BiomeManager.a(mainWorld.getSeed()), //Biome seed
						ImmutableList.of(), false, org.bukkit.World.Environment.NORMAL, null, secondaryworlddata.getName());

				((CraftWorld) mainWorld).getHandle().getWorldBorder().a(new IWorldBorderListener.a(worldserver1.getWorldBorder()));
				console.worldServer.put(resourcekey1, worldserver1);
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
