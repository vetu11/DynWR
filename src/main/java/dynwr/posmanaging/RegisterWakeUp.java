package dynwr.posmanaging;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents.StopSleeping;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class RegisterWakeUp implements StopSleeping {
	private ServerWorld world;

	public RegisterWakeUp(ServerWorld world) {
		this.world = world;
	}

	public void onStopSleeping(LivingEntity entity, BlockPos sleepingPos) {
		this.world.setSpawnPos(sleepingPos, 0);
	}
}

