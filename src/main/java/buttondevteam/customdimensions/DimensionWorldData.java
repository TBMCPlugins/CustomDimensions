package buttondevteam.customdimensions;

import com.mojang.serialization.Lifecycle;
import net.minecraft.server.v1_16_R2.*;
import org.mockito.Mockito;

public class DimensionWorldData extends WorldDataServer {
	public DimensionWorldData(WorldSettings worldsettings, GeneratorSettings generatorsettings, Lifecycle lifecycle) {
		super(worldsettings, generatorsettings, lifecycle);
	}

	public static DimensionWorldData create(WorldDataServer data, String name, EnumGamemode gamemode) {
		var mock = Mockito.mock(DimensionWorldData.class, invocation -> {
			if (invocation.getMethod().getDeclaringClass() == DimensionWorldData.class)
				return invocation.callRealMethod();
			return invocation.getMethod().invoke(data, invocation.getArguments());
		});
		mock.b = new WorldSettings(name, gamemode, false, EnumDifficulty.EASY, true, data.q(), data.D());
		return mock;
	}
}
