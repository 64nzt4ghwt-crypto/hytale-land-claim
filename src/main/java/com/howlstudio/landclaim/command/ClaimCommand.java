package com.howlstudio.landclaim.command;

import com.howlstudio.landclaim.ClaimManager;
import com.howlstudio.landclaim.model.Claim;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * /claim command — Land claiming for players.
 *
 * Usage:
 *   /claim                     — Claim land at current position (default radius)
 *   /claim <radius>            — Custom radius (capped to config max)
 *   /claim info                — Show claim info at current position
 *   /claim list                — List your claims
 *   /claim remove              — Remove claim at your position
 *   /claim trust <player>      — Trust a player in your claim
 *   /claim untrust <player>    — Revoke trust
 *   /claim help                — Show help
 */
public class ClaimCommand extends AbstractPlayerCommand {

    private final ClaimManager manager;

    public ClaimCommand(ClaimManager manager) {
        super("claim", "Manage land claims.");
        this.manager = manager;
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> ref,
                           PlayerRef playerRef,
                           World world) {

        // Parse args from the raw input string (strip command name)
        String input = ctx.getInputString().trim();
        String[] parts = input.split("\\s+");
        // parts[0] = "claim", remaining are args
        String[] args = parts.length > 1
            ? java.util.Arrays.copyOfRange(parts, 1, parts.length)
            : new String[0];

        if (args.length == 0) {
            doClaim(playerRef, manager.getConfig().getDefaultClaimRadius());
            return;
        }

        switch (args[0].toLowerCase()) {
            case "info"    -> doInfo(playerRef);
            case "list"    -> doList(playerRef);
            case "remove"  -> doRemove(playerRef, false);
            case "trust"   -> doTrust(playerRef, args);
            case "untrust" -> doUntrust(playerRef, args);
            case "help"    -> sendHelp(playerRef);
            default        -> {
                try {
                    int r = Integer.parseInt(args[0]);
                    doClaim(playerRef, r);
                } catch (NumberFormatException e) {
                    send(playerRef, "§cUnknown subcommand. Use §e/claim help§c.");
                }
            }
        }
    }

    // ── Subcommand handlers ───────────────────────────────────────────────────

    private void doClaim(PlayerRef playerRef, int radius) {
        Vector3d pos = getPosition(playerRef);
        if (pos == null) { send(playerRef, "§cCould not determine your position."); return; }

        UUID uuid   = playerRef.getUuid();
        String name = playerRef.getUsername();
        UUID worldUuid = playerRef.getWorldUuid();

        String error = manager.createClaim(uuid, name, worldUuid,
            (int) pos.x, (int) pos.z, radius);

        if (error != null) {
            send(playerRef, "§c[LandClaim] " + error);
        } else {
            int r = Math.min(radius, manager.getConfig().getMaxClaimRadius());
            int size = r * 2 + 1;
            send(playerRef, "§a[LandClaim] §fClaimed §e" + size + "×" + size +
                " §fblocks around your position!");

            if (manager.getConfig().isAnnounceOnClaim()) {
                String msg = "§6[LandClaim] §e" + name + " §fclaimed land at §7("
                    + (int)pos.x + ", " + (int)pos.z + ")";
                for (PlayerRef p : Universe.get().getPlayers()) {
                    p.sendMessage(Message.raw(msg));
                }
            }
        }
    }

    private void doInfo(PlayerRef playerRef) {
        Vector3d pos = getPosition(playerRef);
        if (pos == null) { send(playerRef, "§cCould not determine your position."); return; }

        Optional<Claim> opt = manager.getClaimAt(
            playerRef.getWorldUuid(), (int) pos.x, (int) pos.z);

        if (opt.isEmpty()) {
            send(playerRef, "§7[LandClaim] §fThis land is §aunclaimed§f.");
            return;
        }

        Claim c = opt.get();
        send(playerRef, "§6[LandClaim] §fClaim by §e" + c.getOwnerName());
        send(playerRef, "§7  Bounds: §f" + c.boundsString());
        send(playerRef, "§7  Area:   §f" + c.getArea() + " blocks");
        send(playerRef, "§7  Trusted: §f" + c.getTrustedPlayers().size() + " player(s)");
        send(playerRef, "§7  ID: §8" + c.getId());
    }

    private void doList(PlayerRef playerRef) {
        List<Claim> claims = manager.getClaimsByOwner(playerRef.getUuid());
        if (claims.isEmpty()) {
            send(playerRef, "§7[LandClaim] §fNo claims. Use §e/claim§f to start one.");
            return;
        }
        send(playerRef, "§6[LandClaim] §fYour claims (" + claims.size() + "):");
        for (Claim c : claims) {
            send(playerRef, "  §7#" + c.getId() + " §f" + c.boundsString()
                + " §7(" + c.getArea() + " blocks)");
        }
    }

    private void doRemove(PlayerRef playerRef, boolean isAdmin) {
        Vector3d pos = getPosition(playerRef);
        if (pos == null) { send(playerRef, "§cCould not determine your position."); return; }

        String error = manager.removeClaim(
            playerRef.getUuid(), playerRef.getWorldUuid(),
            (int) pos.x, (int) pos.z, isAdmin
        );

        if (error != null) {
            send(playerRef, "§c[LandClaim] " + error);
        } else {
            send(playerRef, "§a[LandClaim] §fClaim removed.");
        }
    }

    private void doTrust(PlayerRef playerRef, String[] args) {
        if (args.length < 2) { send(playerRef, "§cUsage: /claim trust <player>"); return; }

        String targetName = args[1];
        Optional<UUID> targetUuid = manager.resolveUsername(targetName);
        if (targetUuid.isEmpty()) {
            send(playerRef, "§c" + targetName + " is not online.");
            return;
        }

        Vector3d pos = getPosition(playerRef);
        if (pos == null) { send(playerRef, "§cCould not determine your position."); return; }

        String error = manager.trustPlayer(
            playerRef.getUuid(), playerRef.getWorldUuid(),
            (int) pos.x, (int) pos.z,
            targetUuid.get(), targetName
        );

        if (error != null) {
            send(playerRef, "§c[LandClaim] " + error);
        } else {
            send(playerRef, "§a[LandClaim] §e" + targetName + " §fcan now build in your claim.");
        }
    }

    private void doUntrust(PlayerRef playerRef, String[] args) {
        if (args.length < 2) { send(playerRef, "§cUsage: /claim untrust <player>"); return; }

        String targetName = args[1];
        Optional<UUID> targetUuid = manager.resolveUsername(targetName);
        if (targetUuid.isEmpty()) {
            send(playerRef, "§c" + targetName + " is not online.");
            return;
        }

        Vector3d pos = getPosition(playerRef);
        if (pos == null) { send(playerRef, "§cCould not determine your position."); return; }

        String error = manager.untrustPlayer(
            playerRef.getUuid(), playerRef.getWorldUuid(),
            (int) pos.x, (int) pos.z, targetUuid.get()
        );

        if (error != null) {
            send(playerRef, "§c[LandClaim] " + error);
        } else {
            send(playerRef,
                "§a[LandClaim] §e" + targetName + " §fno longer has access to your claim.");
        }
    }

    private void sendHelp(PlayerRef playerRef) {
        send(playerRef, "§8══ §6LandClaim Help §8══");
        send(playerRef, "§e/claim §7— Claim land at your position");
        send(playerRef, "§e/claim <radius> §7— Claim with custom radius");
        send(playerRef, "§e/claim info §7— Info about claim at your position");
        send(playerRef, "§e/claim list §7— List your claims");
        send(playerRef, "§e/claim remove §7— Remove your claim here");
        send(playerRef, "§e/claim trust <player> §7— Trust a player");
        send(playerRef, "§e/claim untrust <player> §7— Revoke trust");
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void send(PlayerRef ref, String text) {
        ref.sendMessage(Message.raw(text));
    }

    /**
     * Get player position via Transform.getPosition().
     * Returns null if unavailable.
     */
    private Vector3d getPosition(PlayerRef ref) {
        try {
            Transform t = ref.getTransform();
            return t != null ? t.getPosition() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
