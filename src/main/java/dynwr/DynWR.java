package dynwr;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents.StopSleeping;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynWR implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("dynwr");
    public static final Item CUSTOM_ITEM = new Item(new FabricItemSettings());

	public class WorldHooker implements ServerWorldEvents.Load {
		public void onWorldLoad(MinecraftServer server, ServerWorld world) {
			EntitySleepEvents.STOP_SLEEPING.register(new RegisterWakeUp(world));
		}
	}

	public class RegisterWakeUp implements StopSleeping {
		private ServerWorld world;

		public RegisterWakeUp(ServerWorld world) {
			this.world = world;
		}

		public void onStopSleeping(LivingEntity entity, BlockPos sleepingPos) {
			this.world.setSpawnPos(sleepingPos, 0);
		}
	}

	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		
		// ServerPlayerEvents.AFTER_RESPAWN
		// EntitySleepEvents.STOP_SLEEPING


		ServerWorldEvents.LOAD.register(new WorldHooker());


		LOGGER.info("TU pute NEIDRE caodre");
        Registry.register(Registries.ITEM, new Identifier("lelitem", "lelitem"), CUSTOM_ITEM);
	}
}
