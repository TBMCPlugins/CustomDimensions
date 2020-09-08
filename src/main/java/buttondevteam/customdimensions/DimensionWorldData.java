package buttondevteam.customdimensions;

import com.mojang.serialization.Lifecycle;
import net.minecraft.server.v1_16_R2.*;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DimensionWorldData extends WorldDataServer {
	public DimensionWorldData(WorldSettings worldsettings, GeneratorSettings generatorsettings, Lifecycle lifecycle) {
		super(worldsettings, generatorsettings, lifecycle);
	}

	private String name;
	private EnumGamemode gamemode;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public EnumGamemode getGameType() {
		return gamemode;
	}

	public static DimensionWorldData create(WorldDataServer data, String name, EnumGamemode gamemode) {
		var mock = Mockito.mock(DimensionWorldData.class, invocation -> {
			if (invocation.getMethod().getDeclaringClass() == DimensionWorldData.class)
				return invocation.callRealMethod();
			return invocation.getMethod().invoke(data, invocation.getArguments());
		});
		mock.name = name;
		mock.gamemode = gamemode; //There are a couple things that come from SaveData, but that's the same
		mock.b = data.b;
		return mock;
	}
}
