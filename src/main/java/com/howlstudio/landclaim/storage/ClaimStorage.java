package com.howlstudio.landclaim.storage;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.howlstudio.landclaim.model.Claim;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

/**
 * Persists claim data as JSON in the plugin data directory.
 *
 * File: plugins/LandClaim/claims.json
 *
 * Thread-safety: all public methods are synchronized on `this`.
 */
public class ClaimStorage {

    private final Path dataFile;
    private final Gson gson;

    public ClaimStorage(Path dataDir) {
        this.dataFile = dataDir.resolve("claims.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            System.err.println("[LandClaim] Failed to create data directory: " + e.getMessage());
        }
    }

    /** Load all claims from disk. Returns empty list if file doesn't exist or is invalid. */
    public synchronized List<SerializedClaim> load() {
        if (!Files.exists(dataFile)) return new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(dataFile)) {
            Type listType = new TypeToken<List<SerializedClaim>>() {}.getType();
            List<SerializedClaim> claims = gson.fromJson(reader, listType);
            return claims != null ? claims : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[LandClaim] Failed to load claims: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /** Save all claims to disk. */
    public synchronized void save(Collection<Claim> claims) {
        List<SerializedClaim> serialized = new ArrayList<>();
        for (Claim c : claims) {
            serialized.add(SerializedClaim.from(c));
        }

        try (Writer writer = Files.newBufferedWriter(dataFile)) {
            gson.toJson(serialized, writer);
        } catch (IOException e) {
            System.err.println("[LandClaim] Failed to save claims: " + e.getMessage());
        }
    }

    // ── Inner DTO ─────────────────────────────────────────────────────────────

    public static class SerializedClaim {
        public String id;
        public String ownerUuid;
        public String ownerName;
        public String worldUuid;
        public int minX, maxX, minZ, maxZ;
        public List<String> trustedPlayers;
        public long createdAt;

        public static SerializedClaim from(Claim c) {
            SerializedClaim s = new SerializedClaim();
            s.id           = c.getId();
            s.ownerUuid    = c.getOwnerUuid().toString();
            s.ownerName    = c.getOwnerName();
            s.worldUuid    = c.getWorldUuid().toString();
            s.minX         = c.getMinX();
            s.maxX         = c.getMaxX();
            s.minZ         = c.getMinZ();
            s.maxZ         = c.getMaxZ();
            s.trustedPlayers = new ArrayList<>();
            for (UUID u : c.getTrustedPlayers()) s.trustedPlayers.add(u.toString());
            s.createdAt    = c.getCreatedAt();
            return s;
        }

        public Claim toClaim() {
            Claim c = new Claim(
                UUID.fromString(ownerUuid), ownerName,
                UUID.fromString(worldUuid),
                minX, maxX, minZ, maxZ
            );
            c.setCreatedAt(createdAt);
            if (trustedPlayers != null) {
                for (String u : trustedPlayers) c.trust(UUID.fromString(u));
            }
            return c;
        }
    }
}
