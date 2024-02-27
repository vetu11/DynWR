package dynwr.posmanager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;


public class PosManager extends PersistentState {
	private static final int ENTRIES_PER_PLAYER = 10;
	private static final int MAX_PLAYER_ENTRIES = 50;
	private static final Duration PERIOD_DURATION = Duration.ofHours(22);
    public static final Logger LOGGER = LoggerFactory.getLogger("dynwr");

	// TODO: implemet period.
	private Date currentPeriodStart;
	private BlockPos lastPos;
	private double lastRadius;
	private HashMap<UUID, Integer> uuidCount;
	private ArrayList<BlockPos> blockEntries;
	

	public class SpawnPointConfig {
		public final BlockPos spawnPoint;
		public final float spawnRadious;

		public SpawnPointConfig(BlockPos spawnPoint, float spawnRadious) {
			this.spawnPoint = spawnPoint;
			this.spawnRadious = spawnRadious;
		}
	}

	public PosManager() {
		this.uuidCount = new HashMap<>();
		this.blockEntries = new ArrayList<>();
	}

	public void addPos(BlockPos newpos, UUID source) {
		LOGGER.info(String.format("Adding newpos: %s", newpos));
		if (this.uuidCount.containsKey(source)) {
			this.uuidCount.put(source, this.uuidCount.get(source) + 1);
		} else {
			this.uuidCount.put(source, 1);
		}
		this.limitUuidCount();
			
		this.blockEntries.add(newpos);
		LOGGER.info(String.format("Currently have %d entries", this.blockEntries.size()));

		while (this.blockEntries.size()
				> this.uuidCount.size() * PosManager.ENTRIES_PER_PLAYER)
		{
			this.blockEntries.remove(0);
		}
		this.markDirty();
	}

	public SpawnPointConfig getSpawnPointConfig() {
		float x, y, z;
		double radious;
		BlockPos spawnPoint;
		BlockPos p0 = this.blockEntries.get(0);
		x = 0;
		y = 0;
		z = 0;

		for (int i = 0; i < this.blockEntries.size(); i++) {
			BlockPos p = this.blockEntries.get(i);
			x = x + p.getX();
			y = y + p.getY();
			z = z + p.getZ();
			LOGGER.info(String.format("On iter %d coords sum is %f, %f, %f", i, x, y, z));
		}
		LOGGER.info(String.format("After iter coords sum is %f, %f, %f", x, y, z));
		x = x / this.blockEntries.size();
		y = y / this.blockEntries.size();
		z = z / this.blockEntries.size();
		LOGGER.info(String.format("After avg coords are is %f, %f, %f", x, y, z));
		spawnPoint = new BlockPos((int) x, (int) y, (int) z);

		radious = 0;
		for (int i = 0; i < this.blockEntries.size(); i++) {
			radious += Math.sqrt(this.blockEntries.get(i).getSquaredDistance(spawnPoint));
		}
		
		radious = radious / this.blockEntries.size();
		if (radious < 10) {
			radious = 10;
		}


		this.lastRadius = radious;
		this.lastPos = spawnPoint;
		LOGGER.info(String.format("New spawn point: %s, radious: %d", spawnPoint, (int) radious));
		return new SpawnPointConfig(spawnPoint, (float) radious);
	}

	private void limitUuidCount() {
		LOGGER.info("Limiting uuidCount");
		int sum = 0;
		for (int c: uuidCount.values()) {
			sum += c;
		}
		LOGGER.info(String.format("Current uuidCount sum is: %d", sum));

		// TODO: Update the limit to be player dependant.
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

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.putInt("blockEntriesSize", this.blockEntries.size());
		for (int i = 0; i < this.blockEntries.size(); i++) {
			nbt.put("entry_" + i, NbtHelper.fromBlockPos(this.blockEntries.get(i)));
		}

		nbt.putInt("uuidCountSize", uuidCount.size());
		int i = 0;
		for (UUID k: this.uuidCount.keySet()) {
			nbt.putUuid("uuid_" + i, k);
			nbt.putInt(k.toString() + "_count", this.uuidCount.get(k));
			i += 1;
		}
		return nbt;
	}

	public static PosManager createFromNbt(NbtCompound tag) {
		PosManager posmanager = new PosManager();
		int blockEntriesSize = tag.getInt("blockEntriesSize");
		int uuidCountSize = tag.getInt("uuidCountSize");
		ArrayList<BlockPos> blockEntries = new ArrayList<>(blockEntriesSize);
		HashMap<UUID, Integer> uuidCount = new HashMap<>(uuidCountSize);

		for (int i = 0; i < blockEntriesSize; i++) {
			blockEntries.add(NbtHelper.toBlockPos((NbtCompound) tag.get("entry_" + i)));
		}
		posmanager.blockEntries = blockEntries;

		for (int i = 0; i < uuidCountSize; i++) {
			UUID uuid = tag.getUuid("uuid_" + i);
			uuidCount.put(uuid, tag.getInt(uuid.toString() + "_count"));
		}
		posmanager.uuidCount = uuidCount;
		return posmanager;
	}
}

