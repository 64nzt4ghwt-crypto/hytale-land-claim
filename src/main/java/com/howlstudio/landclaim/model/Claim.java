package com.howlstudio.landclaim.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a single land claim on a server.
 *
 * Stored as a rectangular area (min/max X and Z) within a specific world.
 * Y-axis is intentionally ignored — claims extend from bedrock to sky.
 */
public class Claim {

    private final String id;
    private final UUID ownerUuid;
    private String ownerName;
    private final UUID worldUuid;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    private final Set<UUID> trustedPlayers;
    private long createdAt;

    public Claim(UUID ownerUuid, String ownerName, UUID worldUuid,
                 int minX, int maxX, int minZ, int maxZ) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.worldUuid = worldUuid;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.trustedPlayers = new HashSet<>();
        this.createdAt = System.currentTimeMillis();
    }

    /** Check if a block position falls within this claim. Y is ignored. */
    public boolean contains(UUID world, int x, int z) {
        if (!this.worldUuid.equals(world)) return false;
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /** Check if a player is allowed to interact with this claim. */
    public boolean canInteract(UUID playerUuid) {
        return ownerUuid.equals(playerUuid) || trustedPlayers.contains(playerUuid);
    }

    public void trust(UUID playerUuid) {
        trustedPlayers.add(playerUuid);
    }

    public void untrust(UUID playerUuid) {
        trustedPlayers.remove(playerUuid);
    }

    /** Block area of this claim (width * length). */
    public int getArea() {
        return (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    /** Human-readable bounds string. */
    public String boundsString() {
        return String.format("(%d,%d) to (%d,%d)", minX, minZ, maxX, maxZ);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getId()              { return id; }
    public UUID getOwnerUuid()         { return ownerUuid; }
    public String getOwnerName()       { return ownerName; }
    public void setOwnerName(String n) { this.ownerName = n; }
    public UUID getWorldUuid()         { return worldUuid; }
    public int getMinX()               { return minX; }
    public int getMaxX()               { return maxX; }
    public int getMinZ()               { return minZ; }
    public int getMaxZ()               { return maxZ; }
    public Set<UUID> getTrustedPlayers() { return trustedPlayers; }
    public long getCreatedAt()         { return createdAt; }
    public void setCreatedAt(long t)   { this.createdAt = t; }
}
