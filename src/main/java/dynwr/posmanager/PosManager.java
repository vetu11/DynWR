package dynrw.posmanager;

import java.net.FileNameMap;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;

import javax.swing.text.StyledEditorKit.BoldAction;

import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

//public class PosManager extends PersistentState {
public class PosManager {
	private static final int ENTRIES_PER_PLAYER = 10;
	private static final int MAX_PLAYER_ENTRIES = 50;
	private static final Duration PERIOD_DURATION = Duration.ofHours(22);

	// TODO: persistence.
	// TODO: implemet period.
	private Date currentPeriodStart;
	private BlockPos lastPos;
	private float lastRadius;
	private HashMap<UUID, Integer> uuidCount;
	private List<BlockPos> blockEntries;
	
	//private static Type<PosManager> type = new Type<>(
	//		PosManager::new,
	//		PosManager::createFromNbt,
	//		null);
	public class SpawnPointConfig {
		public final BlockPos spawnPoint;
		public final float spawnRadious;

		public SpawnPointConfig(BlockPos spawnPoint, float spawnRadious) {
			this.spawnPoint = spawnPoint;
			this.spawnRadious = spawnRadious;
		}
	}

	public void addPos(BlockPos newpos, UUID source) {
		if (this.uuidCount.containsKey(source)) {
			this.uuidCount.put(source, this.uuidCount.get(source) + 1);
		} else {
			this.uuidCount.put(source, this.uuidCount.get(source));
		}
		this.limitUuidCount();
			
		this.blockEntries.add(newpos);

		while (this.blockEntries.size()
				> this.uuidCount.size() * PosManager.ENTRIES_PER_PLAYER)
		{
			this.blockEntries.remove(0);
		}
		//this.markDirty();
	}

	public SpawnPointConfig getSpawnPointConfig() {
		float x, y, z, factor;
		double radious;
		BlockPos spawnPoint;
		BlockPos p0 = this.blockEntries.get(0);
		x = p0.getX();
		y = p0.getY();
		z = p0.getZ();
		factor = 1 / this.blockEntries.size();

		for (int i = 1; i < this.blockEntries.size(); i++) {
			BlockPos p = this.blockEntries.get(i);
			x = x + p.getX() * factor;
			y = y + p.getY() * factor;
			z = z + p.getZ() * factor;
		}
		spawnPoint = new BlockPos((int) x, (int) y, (int) z);

		radious = p0.getSquaredDistance(spawnPoint);
		for (int i = 1; i < this.blockEntries.size(); i++) {
			radious += this.blockEntries.get(i).getSquaredDistance(spawnPoint) * factor;
		}

		return new SpawnPointConfig(spawnPoint, (float) radious);
	}

	private void limitUuidCount() {
		int sum = 0;
		for (int c: uuidCount.values()) {
			sum += c;
		}

		if (sum > PosManager.MAX_PLAYER_ENTRIES) {
			HashMap<UUID, Integer> limitedCount = new HashMap<>();
			for (Entry<UUID, Integer> entry: uuidCount.entrySet()) {
				if (entry.getValue() > 1) {
					limitedCount.put(entry.getKey(), entry.getValue() - 1);
				}
				this.uuidCount = limitedCount;
			}
		}
	}


	//@Override
	//public NbtCompound writeNbt(NbtCompound var1) {
	//	// TODO Auto-generated method stub
	//	return null;
	//}

	//public PosManager createFromNbt(NbtCompound tag) {
	//	// TODO
	//	PosManager posmanager = PosManager();

	//}
}

