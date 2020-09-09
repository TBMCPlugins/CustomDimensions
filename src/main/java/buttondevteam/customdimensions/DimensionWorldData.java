package buttondevteam.customdimensions;

import com.mojang.serialization.Lifecycle;
import net.minecraft.server.v1_16_R2.GeneratorSettings;
import net.minecraft.server.v1_16_R2.WorldDataServer;
import net.minecraft.server.v1_16_R2.WorldSettings;
import org.mockito.Mockito;

public class DimensionWorldData extends WorldDataServer {
	public DimensionWorldData(WorldSettings worldsettings, GeneratorSettings generatorsettings, Lifecycle lifecycle) {
		super(worldsettings, generatorsettings, lifecycle);
	}

	public static DimensionWorldData create(WorldDataServer data, String name) {
		var mock = Mockito.mock(DimensionWorldData.class, Mockito.withSettings().defaultAnswer(invocation -> {
			if (invocation.getMethod().getDeclaringClass() == DimensionWorldData.class)
				return invocation.callRealMethod();
			return invocation.getMethod().invoke(data, invocation.getArguments());
		}).stubOnly());
		mock.b = new WorldSettings(name, data.getGameType(), false, data.getDifficulty(), true, data.q(), data.D());
		return mock;
	}
}
