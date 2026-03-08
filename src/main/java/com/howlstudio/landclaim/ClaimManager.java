package com.howlstudio.landclaim;

import com.howlstudio.landclaim.config.ClaimConfig;
import com.howlstudio.landclaim.model.Claim;
import com.howlstudio.landclaim.storage.ClaimStorage;
import com.howlstudio.landclaim.storage.ClaimStorage.SerializedClaim;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Core claim management — in-memory store with persistence.
 *
 * All mutations call save() immediately (small data size, no meaningful perf hit).
 */
public class ClaimManager {

    private final ClaimConfig config;
    private final ClaimStorage storage;

    /** All claims, keyed by claim ID for fast lookup. */
    private final Map<String, Claim> claimsById = new ConcurrentHashMap<>();

    /** Online players: UUID → username (for trust lookups by name). */
    private final Map<UUID, String> onlinePlayers = new ConcurrentHashMap<>();

    /** Online player names → UUID (reverse map for /trust <name> resolution). */
    private final Map<String, UUID> nameToUuid = new ConcurrentHashMap<>();

    public ClaimManager(Path dataDir) {
        this.config  = new ClaimConfig(dataDir);
        this.storage = new ClaimStorage(dataDir);
        loadFromDisk();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    private void loadFromDisk() {
        List<SerializedClaim> raw = storage.load();
        for (SerializedClaim s : raw) {
            try {
                Claim c = s.toClaim();
                claimsById.put(c.getId(), c);
            } catch (Exception e) {
                System.err.println("[LandClaim] Skipped corrupt claim entry: " + e.getMessage());
            }
        }
        System.out.println("[LandClaim] Loaded " + claimsById.size() + " claims from disk.");
    }

    private void saveToDisk() {
        storage.save(claimsById.values());
    }

    // ── Player tracking ───────────────────────────────────────────────────────

    public void trackPlayer(UUID uuid, String name) {
        onlinePlayers.put(uuid, name);
        nameToUuid.put(name.toLowerCase(), uuid);

        // Update owner name in their claims
        for (Claim c : claimsById.values()) {
            if (c.getOwnerUuid().equals(uuid)) c.setOwnerName(name);
        }
    }

    public void untrackPlayer(UUID uuid) {
        String name = onlinePlayers.remove(uuid);
        if (name != null) nameToUuid.remove(name.toLowerCase());
    }

    /** Resolve a player name to UUID (online players only). */
    public Optional<UUID> resolveUsername(String name) {
        return Optional.ofNullable(nameToUuid.get(name.toLowerCase()));
    }

    // ── Claim creation / removal ─────────────────────────────────────────────

    /**
     * Create a claim centered at (cx, cz) with the configured radius.
     *
     * @return null on success, or an error message string.
     */
    public String createClaim(UUID ownerUuid, String ownerName, UUID worldUuid,
                              int cx, int cz, int radius) {

        if (!config.isEnabled()) return "Land claiming is currently disabled.";

        int r = Math.min(radius, config.getMaxClaimRadius());
        int minX = cx - r, maxX = cx + r;
        int minZ = cz - r, maxZ = cz + r;

        // Check player claim limit
        int maxClaims = config.getMaxClaimsPerPlayer();
        if (maxClaims > 0) {
            long existing = claimsById.values().stream()
                .filter(c -> c.getOwnerUuid().equals(ownerUuid))
                .count();
            if (existing >= maxClaims) {
                return "You already have " + existing + " claim(s). Max is " + maxClaims + ".";
            }
        }

        // Check overlap with existing claims
        for (Claim c : claimsById.values()) {
            if (!c.getWorldUuid().equals(worldUuid)) continue;
            if (c.getMaxX() < minX || c.getMinX() > maxX) continue;
            if (c.getMaxZ() < minZ || c.getMinZ() > maxZ) continue;
            return "This area overlaps with " + c.getOwnerName() + "'s claim!";
        }

        Claim claim = new Claim(ownerUuid, ownerName, worldUuid, minX, maxX, minZ, maxZ);
        claimsById.put(claim.getId(), claim);
        saveToDisk();
        return null;
    }

    /**
     * Remove the claim at position (x, z) in the given world that belongs to ownerUuid.
     *
     * @return null on success, or an error message.
     */
    public String removeClaim(UUID ownerUuid, UUID worldUuid, int x, int z, boolean isAdmin) {
        Claim found = getClaimAt(worldUuid, x, z).orElse(null);
        if (found == null) return "No claim found at this position.";

        if (!isAdmin && !found.getOwnerUuid().equals(ownerUuid)) {
            return "You don't own this claim.";
        }

        claimsById.remove(found.getId());
        saveToDisk();
        return null;
    }

    // ── Trust management ──────────────────────────────────────────────────────

    /**
     * Trust a player in the claim at (x, z).
     */
    public String trustPlayer(UUID ownerUuid, UUID worldUuid, int x, int z, UUID targetUuid, String targetName) {
        Claim c = getClaimAt(worldUuid, x, z).orElse(null);
        if (c == null) return "No claim at your current position.";
        if (!c.getOwnerUuid().equals(ownerUuid)) return "You don't own this claim.";
        if (c.getTrustedPlayers().size() >= config.getMaxTrustedPerClaim()) {
            return "Claim already has " + config.getMaxTrustedPerClaim() + " trusted players (max).";
        }
        c.trust(targetUuid);
        saveToDisk();
        return null; // success
    }

    /**
     * Remove trust from a player in the claim at (x, z).
     */
    public String untrustPlayer(UUID ownerUuid, UUID worldUuid, int x, int z, UUID targetUuid) {
        Claim c = getClaimAt(worldUuid, x, z).orElse(null);
        if (c == null) return "No claim at your current position.";
        if (!c.getOwnerUuid().equals(ownerUuid)) return "You don't own this claim.";
        c.untrust(targetUuid);
        saveToDisk();
        return null;
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /** Find the claim containing position (x, z) in the given world, if any. */
    public Optional<Claim> getClaimAt(UUID worldUuid, int x, int z) {
        for (Claim c : claimsById.values()) {
            if (c.contains(worldUuid, x, z)) return Optional.of(c);
        }
        return Optional.empty();
    }

    /** All claims owned by a given UUID. */
    public List<Claim> getClaimsByOwner(UUID uuid) {
        return claimsById.values().stream()
            .filter(c -> c.getOwnerUuid().equals(uuid))
            .collect(Collectors.toList());
    }

    /** Whether an action at (x, z) by playerUuid is blocked. */
    public boolean isBlocked(UUID worldUuid, int x, int z, UUID playerUuid) {
        return getClaimAt(worldUuid, x, z)
            .map(c -> !c.canInteract(playerUuid))
            .orElse(false);
    }

    public ClaimConfig getConfig() { return config; }
    public int totalClaims()       { return claimsById.size(); }
}
