package com.howlstudio.landclaim;

import com.howlstudio.landclaim.command.ClaimCommand;
import com.howlstudio.landclaim.listener.ClaimProtectionListener;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

/**
 * LandClaimPlugin — Grief protection for Hytale servers.
 *
 * Players claim rectangular areas of land (bedrock to sky) and control
 * who can interact with blocks inside.
 *
 * Commands: /claim [subcommand] [args]
 * Config:   plugins/LandClaim/claim-config.json
 * Data:     plugins/LandClaim/claims.json
 *
 * First dedicated land claim plugin on CurseForge / hymods.io for Hytale.
 *
 * @version 1.0.0
 */
public final class LandClaimPlugin extends JavaPlugin {

    private ClaimManager claimManager;

    public LandClaimPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        log("[LandClaim] Loading...");

        claimManager = new ClaimManager(getDataDirectory());

        if (!claimManager.getConfig().isEnabled()) {
            log("[LandClaim] Disabled in config. Unloading.");
            return;
        }

        // Register block protection + player tracking events
        new ClaimProtectionListener(claimManager).register();

        // Register /claim command
        CommandManager.get().register(new ClaimCommand(claimManager));

        log("[LandClaim] Ready! " + claimManager.totalClaims() + " claims loaded.");
        log("[LandClaim] Default radius: " + claimManager.getConfig().getDefaultClaimRadius()
            + " | Max claims/player: " + claimManager.getConfig().getMaxClaimsPerPlayer());
    }

    @Override
    protected void shutdown() {
        log("[LandClaim] Shutting down. Storage is already flushed.");
    }

    private void log(String msg) {
        System.out.println(msg);
    }
}
