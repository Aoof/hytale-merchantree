package ca.aoof.merchantree.debug.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.World;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class SpawnPlayerCommand extends AbstractPlayerCommand {
    public SpawnPlayerCommand() {
        super("spawnPlayer", "Spawns a test player in the world");
        this.setPermissionGroup(GameMode.Creative); // Only allow OPs to use this command
    }

    @Override
    protected void execute(@NonNull CommandContext ctx, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
    }
}
