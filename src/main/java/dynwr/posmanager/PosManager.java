package dynwr.posmanager;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
	private static final Duration PERIOD_DURATION = Duration.ofHours(22);
    public static final Logger LOGGER = LoggerFactory.getLogger("dynwr");

	private Instant currentPeriodStart;
	private HashMap<UUID, BlockPos> currentSleepEntries;
	private HashMap<UUID, BlockPos> currentRespawnEntries;
	private HashMap<UUID, Integer> uuidCount;
	private ArrayList<BlockPos> blockEntries;
	
	public enum EventType {
		SLEEP,
		RESPAWN,
	}

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
		this.currentPeriodStart = Instant.now();
		this.currentSleepEntries = new HashMap<>();
		this.currentRespawnEntries = new HashMap<>();
	}

	private void engravePosEntries() {
		LOGGER.info("Engraving entries");
		for (Entry<UUID, BlockPos> entry: this.currentSleepEntries.entrySet()) {
			if (this.uuidCount.containsKey(entry.getKey())) {
				this.uuidCount.put(entry.getKey(), this.uuidCount.get(entry.getKey()) + 1);
			} else {
				this.uuidCount.put(entry.getKey(), 1);
			}
			this.blockEntries.add(entry.getValue());
		}
		for (Entry<UUID, BlockPos> entry: this.currentRespawnEntries.entrySet()) {
			if (this.uuidCount.containsKey(entry.getKey())) {
				this.uuidCount.put(entry.getKey(), this.uuidCount.get(entry.getKey()) + 1);
			} else {
				this.uuidCount.put(entry.getKey(), 1);
			}
			this.blockEntries.add(entry.getValue());
		}
		this.limitUuidCount();
		
		while (this.blockEntries.size()
				> this.uuidCount.size() * PosManager.ENTRIES_PER_PLAYER)
		{
			this.blockEntries.remove(0);
		}
		this.markDirty();
		
		this.currentSleepEntries = new HashMap<>(this.currentSleepEntries.size() + 5);
		this.currentRespawnEntries = new HashMap<>(this.currentRespawnEntries.size() + 5);
	}

	public void addPos(BlockPos newpos, UUID source, EventType type) {
		LOGGER.info(String.format("Adding newpos: %s", newpos));

		switch (type) {
			case SLEEP:
				this.currentSleepEntries.put(source, newpos);
				break;
			case RESPAWN:
				this.currentRespawnEntries.put(source, newpos);
				break;
		}
		if (this.currentPeriodStart.plus(PERIOD_DURATION).compareTo(Instant.now()) < 0) {
			this.engravePosEntries();
			this.currentPeriodStart = Instant.now();
		}
		this.markDirty();
	}

	private ArrayList<BlockPos> allPosEntries() {
		ArrayList<BlockPos> entries = new ArrayList<>(
				this.blockEntries.size()
				+ this.currentSleepEntries.size()
				+ this.currentRespawnEntries.size());
		for (int i = 0; i < this.blockEntries.size(); i++) {
			entries.add(this.blockEntries.get(i));
		}
		for (BlockPos p: this.currentSleepEntries.values()) {
			entries.add(p);
		}
		for (BlockPos p: this.currentRespawnEntries.values()) {
			entries.add(p);
		}
		LOGGER.info(String.format("blockEntries: %d, sleep: %d, respawn: %d, total: %d",
									this.blockEntries.size(),
									this.currentSleepEntries.size(),
									this.currentRespawnEntries.size(),
									entries.size()));
		return entries;
	}

	public SpawnPointConfig getSpawnPointConfig() {
		ArrayList<BlockPos> entries = this.allPosEntries();
		float x, y, z;
		double radious;
		BlockPos spawnPoint;
		x = 0;
		y = 0;
		z = 0;

		for (int i = 0; i < entries.size(); i++) {
			BlockPos p = entries.get(i);
			x = x + p.getX();
			y = y + p.getY();
			z = z + p.getZ();
		}
		x = x / entries.size();
		y = y / entries.size();
		z = z / entries.size();
		spawnPoint = new BlockPos((int) x, (int) y, (int) z);

		radious = 0;
		for (int i = 0; i < entries.size(); i++) {
			radious += Math.sqrt(entries.get(i).getSquaredDistance(spawnPoint));
		}
		
		radious = radious / entries.size();
		if (radious < 10) {
			radious = 10;
		}


		LOGGER.info(String.format("New spawn point: %s, radious: %d", spawnPoint, (int) radious));
		return new SpawnPointConfig(spawnPoint, (float) radious);
	}

	private void limitUuidCount() {
		LOGGER.info("Limiting uuidCount");
		int sum = 0;
		for (int c: uuidCount.values()) {
			sum += c;
		}
		LOGGER.info(String.format("Pre uuidCount sum is %d distributed in %d entries",
									sum,
									uuidCount.size()));

		while (sum > (this.uuidCount.size() * ENTRIES_PER_PLAYER + 1)) {
			HashMap<UUID, Integer> limitedCount = new HashMap<>();
			for (Entry<UUID, Integer> entry: uuidCount.entrySet()) {
				if (entry.getValue() > 1) {
					limitedCount.put(entry.getKey(), entry.getValue() - 1);
				}
				this.uuidCount = limitedCount;
			}

			sum = 0;
			for (int c: this.uuidCount.values()) {
				sum += c;
			}
			}
		LOGGER.info(String.format("After uuidCount sum is %d discributed in %d entries",
									sum,
									this.uuidCount.size()));
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		engravePosEntries();
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

