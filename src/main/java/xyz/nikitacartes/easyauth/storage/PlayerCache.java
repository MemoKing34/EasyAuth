package xyz.nikitacartes.easyauth.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

/**
 * Class used for storing the non-authenticated player's cache
 */
public class PlayerCache {

    /**
     * Whether player is authenticated.
     * Used for {@link AuthEventHandler#onPlayerJoin(ServerPlayerEntity) session validation}.
     */
    @Expose
    @SerializedName("is_authenticated")
    public boolean isAuthenticated = false;
    /**
     * Hashed password of player.
     */
    @Expose
    public String password = "";
    /**
     * Stores how many times the player has tried to log in.
     * Cleared on every successful login and every time the player is kicked for too many incorrect logins.
     */
    @Expose
    @SerializedName("login_tries")
    public AtomicInteger loginTries = new AtomicInteger();
    /**
     * Stores the last time a player was kicked for too many logins (unix ms).
     */
    @Expose
    @SerializedName("last_kicked")
    public long lastKicked = 0;
    /**
     * Last recorded IP of player.
     * Used for {@link AuthEventHandler#onPlayerJoin(ServerPlayerEntity) sessions}.
     */
    @Expose
    @SerializedName("last_ip")
    public String lastIp;
    /**
     * Time until session is valid.
     */
    @Expose
    @SerializedName("valid_until")
    public long validUntil;

    /**
     * Player stats before de-authentication.
     */
    @Expose
    @SerializedName("was_in_portal")
    public boolean wasInPortal = false;

    /**
     * Contains the UUID of the entity that the player was riding before leaving the server.
     */
    @Expose
    @SerializedName("riding_entity_uuid")
    public UUID ridingEntityUUID = null;

    /**
     * Whether player was dead
     */
    @Expose
    @SerializedName("was_dead")
    public boolean wasDead = false;

    /**
     * Last recorded position before de-authentication.
     */
    public static class LastLocation {
        public ServerWorld dimension;
        public Vec3d position;
        public float yaw;
        public float pitch;

        public String toString() {
            return String.format("LastLocation{dimension=%s, position=%s, yaw=%s, pitch=%s}", dimension, position, yaw, pitch);
        }
    }

    public final LastLocation lastLocation = new LastLocation();


    public static final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    /**
     * Creates an empty cache for player (when player doesn't exist in DB).
     *
     * @param player player to create cache for
     */


    public static PlayerCache fromJson(ServerPlayerEntity player, String fakeUuid) {
        LogDebug(String.format("Creating cache for %s %s", player != null ? player.getGameProfile().getName() : null, fakeUuid));

        String json = DB.getUserData(fakeUuid);
        PlayerCache playerCache;
        if (!json.isEmpty()) {
            // Parsing data from DB
            playerCache = gson.fromJson(json, PlayerCache.class);
        } else
            playerCache = new PlayerCache();
        if (player != null) {
            // Setting position cache
            playerCache.lastLocation.dimension = player.getServerWorld();
            playerCache.lastLocation.position = player.getPos();
            playerCache.lastLocation.yaw = player.getYaw();
            playerCache.lastLocation.pitch = player.getPitch();
            playerCache.ridingEntityUUID = player.getVehicle() != null ? player.getVehicle().getUuid() : null;
            playerCache.wasDead = player.isDead();

            playerCache.wasInPortal = false;
        }

        return playerCache;
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static boolean isAuthenticated(String uuid) {
        PlayerCache playerCache = playerCacheMap.get(uuid);
        return (playerCache != null && playerCache.isAuthenticated);
    }
}
