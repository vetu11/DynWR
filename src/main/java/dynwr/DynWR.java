package dynwr;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents.StopSleeping;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AfterRespawn;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.PersistentStateManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dynwr.posmanager.PosManager;
import dynwr.posmanager.PosManager.EventType;
import dynwr.posmanager.PosManager.SpawnPointConfig;


public class DynWR implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("dynwr");

	public class WorldHooker implements ServerWorldEvents.Load {
		public void onWorldLoad(MinecraftServer server, ServerWorld world) {
			if (!world.getDimension().bedWorks()) return;
			PersistentStateManager per = world.getPersistentStateManager();
			PosManager pm = per.getOrCreate(PosManager::createFromNbt, PosManager::new, "dynwr:pos_manager");
			EntitySleepEvents.STOP_SLEEPING.register(new RegisterWakeUp(pm, world));
			ServerPlayerEvents.AFTER_RESPAWN.register(new RegisterRespawn(pm, world));
		}
	}

	public class RegisterWakeUp implements StopSleeping {
		private PosManager posManager;
		private ServerWorld world;

		public RegisterWakeUp(PosManager posManager, ServerWorld world) {
			this.posManager = posManager;
			this.world = world;
		}

		@Override
		public void onStopSleeping(LivingEntity entity, BlockPos sleepingPos) {
			//TODO: check the time to avoid abuse.
			if (!entity.isPlayer()) return;
			LOGGER.info("onStopSleeping");
			this.posManager.addPos(sleepingPos, entity.getUuid(), EventType.SLEEP);

			SpawnPointConfig spawn = this.posManager.getSpawnPointConfig();
			this.world.setSpawnPos(spawn.spawnPoint, 0);
			this.world.getServer().getGameRules().get(GameRules.SPAWN_RADIUS)
				.set((int)spawn.spawnRadious, world.getServer());
		}
	}

	public class RegisterRespawn implements AfterRespawn {
		private PosManager posManager;
		private ServerWorld world;

		public RegisterRespawn(PosManager posManager, ServerWorld world) {
			this.posManager = posManager;
			this.world = world;
		}

		@Override
		public void afterRespawn(ServerPlayerEntity _oldPlayer, ServerPlayerEntity newPlayer, boolean _alive) {
			//TODO: What happens when respawning on other dimensions?
			LOGGER.info("afterRespawn");
			Vec3d p = newPlayer.getPos();
			BlockPos pos = new BlockPos((int)p.x, (int)p.y, (int)p.z);
			this.posManager.addPos(pos, newPlayer.getUuid(), EventType.RESPAWN);

			SpawnPointConfig spawn = this.posManager.getSpawnPointConfig();
			this.world.setSpawnPos(spawn.spawnPoint, 0);
			this.world.getServer().getGameRules().get(GameRules.SPAWN_RADIUS)
				.set((int)spawn.spawnRadious, world.getServer());
		}
	}

	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		
		// ServerPlayerEvents.AFTER_RESPAWN
		// EntitySleepEvents.STOP_SLEEPING


		ServerWorldEvents.LOAD.register(new WorldHooker());
		LOGGER.info("WorldLoad hook registered.");
	}
}
