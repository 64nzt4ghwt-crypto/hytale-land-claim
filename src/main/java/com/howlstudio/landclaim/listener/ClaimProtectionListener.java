package com.howlstudio.landclaim.listener;

import com.howlstudio.landclaim.ClaimManager;
import com.howlstudio.landclaim.model.Claim;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers event listeners for land claim protection.
 *
 * Protection strategy:
 *  - UseBlockEvent$Pre: cancellable, provides InteractionContext with entity Ref.
 *    We map Ref → PlayerRef to identify the acting player.
 *  - BreakBlockEvent / PlaceBlockEvent: the Hytale API v1 does not expose which
 *    entity triggered these in the event object. Protection for those actions
 *    will be added in v1.1 when the API matures.
 *
 * Player tracking:
 *  - PlayerReadyEvent  → store Ref → PlayerRef mapping
 *  - PlayerDisconnectEvent → remove from mapping
 */
public class ClaimProtectionListener {

    private final ClaimManager manager;

    /**
     * Maps entity store Ref → PlayerRef for online players.
     * Used to identify the player from UseBlockEvent's InteractionContext.
     */
    private final Map<Ref<EntityStore>, PlayerRef> refToPlayer = new ConcurrentHashMap<>();

    public ClaimProtectionListener(ClaimManager manager) {
        this.manager = manager;
    }

    @SuppressWarnings("unchecked")
    public void register() {
        var bus = HytaleServer.get().getEventBus();

        bus.registerGlobal(PlayerReadyEvent.class,     this::onPlayerReady);
        bus.registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        // UseBlockEvent$Pre is the primary protection hook.
        // It fires before a block interaction completes and is cancellable.
        bus.registerGlobal(UseBlockEvent.Pre.class, this::onUseBlockPre);
    }

    // ── Player lifecycle ──────────────────────────────────────────────────────

    private void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        PlayerRef ref = player.getPlayerRef();
        if (ref == null) return;

        Ref<EntityStore> entityRef = ref.getReference();
        if (entityRef != null) refToPlayer.put(entityRef, ref);

        UUID uuid = ref.getUuid();
        String name = ref.getUsername() != null ? ref.getUsername() : uuid.toString();
        manager.trackPlayer(uuid, name);
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef ref = event.getPlayerRef();
        if (ref == null) return;

        Ref<EntityStore> entityRef = ref.getReference();
        if (entityRef != null) refToPlayer.remove(entityRef);

        manager.untrackPlayer(ref.getUuid());
    }

    // ── Block protection (UseBlockEvent$Pre) ──────────────────────────────────

    @SuppressWarnings("unchecked")
    private void onUseBlockPre(UseBlockEvent.Pre event) {
        Vector3i pos = event.getTargetBlock();
        if (pos == null) return;

        InteractionContext ctx = event.getContext();
        if (ctx == null) return;

        // Resolve acting player via entity ref → PlayerRef map
        Ref<EntityStore> entityRef = (Ref<EntityStore>) ctx.getEntity();
        if (entityRef == null) return;

        PlayerRef playerRef = refToPlayer.get(entityRef);
        if (playerRef == null) {
            // Fallback: scan online players by ref equality
            playerRef = resolveByRef(entityRef);
            if (playerRef == null) return; // non-player entity
        }

        UUID playerUuid = playerRef.getUuid();
        UUID worldUuid  = playerRef.getWorldUuid();

        Optional<Claim> claim = manager.getClaimAt(worldUuid, pos.getX(), pos.getZ());
        if (claim.isEmpty()) return; // unclaimed — allow

        if (!claim.get().canInteract(playerUuid)) {
            event.setCancelled(true);
            playerRef.sendMessage(Message.raw(
                "§c[LandClaim] §fThis land belongs to §e" + claim.get().getOwnerName() + "§f."
            ));
        }
    }

    /** Scan online players for a matching entity ref (fallback). */
    @SuppressWarnings("unchecked")
    private PlayerRef resolveByRef(Ref<EntityStore> entityRef) {
        try {
            for (PlayerRef p : Universe.get().getPlayers()) {
                if (entityRef.equals(p.getReference())) return p;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
