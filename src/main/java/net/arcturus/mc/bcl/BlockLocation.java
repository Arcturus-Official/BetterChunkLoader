package net.arcturus.mc.bcl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class BlockLocation {
	@XmlAttribute
	private String world;
	@XmlAttribute
	private int x;
	@XmlAttribute
	private int y;
	@XmlAttribute
	private int z;

	BlockLocation() {
	}

	public BlockLocation(Block block) {
		this(block.getLocation());
	}

	public BlockLocation(Location location) {
		this(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	public BlockLocation(String world, int x, int y, int z) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public World getWorld() {
		return Bukkit.getServer().getWorld(this.world);
	}

	public String getWorldName() {
		return this.world;
	}

	public Chunk getChunk() {
		return this.getWorld().getChunkAt(this.getChunkX(), this.getChunkZ());
	}

	public Block getBlock() {
		World world = this.getWorld();
		return world == null ? null : world.getBlockAt(this.x, this.y, this.z);
	}

	public Location getLocation() {
		Block block = this.getBlock();
		return block == null ? null : block.getLocation();
	}

	public int getChunkX() {
		return (int)Math.floor((double)this.x / 16.0);
	}

	public int getChunkZ() {
		return (int)Math.floor((double)this.z / 16.0);
	}

	public boolean equals(Object obj) {
		if (obj instanceof BlockLocation) {
			BlockLocation loc = (BlockLocation)obj;
			return this.x == loc.x && this.y == loc.y && this.z == loc.z && this.world.equals(loc.world);
		} else if (obj instanceof Location) {
			Location loc = (Location)obj;
			return this.x == loc.getBlockX() && this.y == loc.getBlockY() && this.z == loc.getBlockZ() && this.world.equals(loc.getWorld().getName());
		} else {
			return false;
		}
	}

	public int hashCode() {
		return 37 * (37 * (37 * this.world.hashCode() + this.x) + this.y) + this.z;
	}

	public String toString() {
		return this.world + ":" + this.x + "," + this.y + "," + this.z;
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public int getZ() {
		return this.z;
	}
}