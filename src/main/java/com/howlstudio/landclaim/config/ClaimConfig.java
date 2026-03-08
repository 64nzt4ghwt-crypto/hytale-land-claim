package com.howlstudio.landclaim.config;

import com.google.gson.*;

import java.io.*;
import java.nio.file.*;

/**
 * Plugin configuration loaded from plugins/LandClaim/claim-config.json.
 *
 * Sensible defaults mean it works out of the box — server admins can tune it.
 */
public class ClaimConfig {

    private static final String CONFIG_FILE = "claim-config.json";

    // ── Defaults ─────────────────────────────────────────────────────────────
    private boolean enabled          = true;
    private int defaultClaimRadius   = 15;   // blocks from claim center each direction
    private int maxClaimRadius       = 50;   // max radius a player can request
    private int maxClaimsPerPlayer   = 3;    // claims per UUID (0 = unlimited)
    private int maxTrustedPerClaim   = 10;
    private boolean adminBypass      = true; // ops can bypass claim protection
    private String claimToolItem     = "shovel"; // item keyword to detect claim tool
    private boolean announceOnClaim  = true;

    public ClaimConfig(Path dataDir) {
        Path file = dataDir.resolve(CONFIG_FILE);

        if (!Files.exists(file)) {
            writeDefaults(file);
            return;
        }

        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            if (obj.has("enabled"))            enabled            = obj.get("enabled").getAsBoolean();
            if (obj.has("defaultClaimRadius")) defaultClaimRadius = obj.get("defaultClaimRadius").getAsInt();
            if (obj.has("maxClaimRadius"))     maxClaimRadius     = obj.get("maxClaimRadius").getAsInt();
            if (obj.has("maxClaimsPerPlayer")) maxClaimsPerPlayer = obj.get("maxClaimsPerPlayer").getAsInt();
            if (obj.has("maxTrustedPerClaim")) maxTrustedPerClaim = obj.get("maxTrustedPerClaim").getAsInt();
            if (obj.has("adminBypass"))        adminBypass        = obj.get("adminBypass").getAsBoolean();
            if (obj.has("claimToolItem"))      claimToolItem      = obj.get("claimToolItem").getAsString();
            if (obj.has("announceOnClaim"))    announceOnClaim    = obj.get("announceOnClaim").getAsBoolean();
        } catch (Exception e) {
            System.err.println("[LandClaim] Config parse error — using defaults: " + e.getMessage());
        }
    }

    private void writeDefaults(Path file) {
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled",            enabled);
        obj.addProperty("defaultClaimRadius", defaultClaimRadius);
        obj.addProperty("maxClaimRadius",     maxClaimRadius);
        obj.addProperty("maxClaimsPerPlayer", maxClaimsPerPlayer);
        obj.addProperty("maxTrustedPerClaim", maxTrustedPerClaim);
        obj.addProperty("adminBypass",        adminBypass);
        obj.addProperty("claimToolItem",      claimToolItem);
        obj.addProperty("announceOnClaim",    announceOnClaim);

        try (Writer w = Files.newBufferedWriter(file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(obj, w);
        } catch (IOException e) {
            System.err.println("[LandClaim] Failed to write default config: " + e.getMessage());
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public boolean isEnabled()           { return enabled; }
    public int getDefaultClaimRadius()   { return defaultClaimRadius; }
    public int getMaxClaimRadius()       { return maxClaimRadius; }
    public int getMaxClaimsPerPlayer()   { return maxClaimsPerPlayer; }
    public int getMaxTrustedPerClaim()   { return maxTrustedPerClaim; }
    public boolean isAdminBypass()       { return adminBypass; }
    public String getClaimToolItem()     { return claimToolItem; }
    public boolean isAnnounceOnClaim()   { return announceOnClaim; }
}
