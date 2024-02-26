package dynwr;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents.StopSleeping;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AfterRespawn;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dynwr.posmanager.PosManager;
import dynwr.posmanager.PosManager.SpawnPointConfig;


public class DynWR implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("dynwr");

	public class WorldHooker implements ServerWorldEvents.Load {
		private PosManager posManager;

		public WorldHooker(){
			//TODO: Persistence
			this.posManager = new PosManager();
		}

		public void onWorldLoad(MinecraftServer server, ServerWorld world) {
			PosManager pm = new PosManager();
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
			LOGGER.info("onStopSleeping");
			this.posManager.addPos(sleepingPos, entity.getUuid());

			SpawnPointConfig spawn = this.posManager.getSpawnPointConfig();
			this.world.setSpawnPos(spawn.spawnPoint, 0);
			// TODO: Change spawnRadious
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
		public void afterRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
			LOGGER.info("afterRespawn");
			Vec3d p = newPlayer.getPos();
			BlockPos pos = new BlockPos((int)p.x, (int)p.y, (int)p.z);
			this.posManager.addPos(pos, newPlayer.getUuid());

			SpawnPointConfig spawn = this.posManager.getSpawnPointConfig();
			this.world.setSpawnPos(spawn.spawnPoint, 0);
			// TODO: Change spawnRadious
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
