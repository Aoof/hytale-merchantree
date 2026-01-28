package ca.aoof.merchantree;

import ca.aoof.merchantree.kiosk.KioskAsset;
import ca.aoof.merchantree.kiosk.builders.BuilderActionOpenKiosk;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.builtin.adventure.shop.GiveItemInteraction;
import com.hypixel.hytale.builtin.adventure.shop.ShopElement;
import com.hypixel.hytale.builtin.adventure.shop.ShopPageSupplier;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceElement;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceInteraction;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.Config;
import com.hypixel.hytale.server.npc.NPCPlugin;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class MerchantreePlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static MerchantreePlugin instance;
    private Config<MerchantreeConfig> config;

    public MerchantreePlugin(@NonNull JavaPluginInit init) {
        super(init);
        instance = this;

        config = withConfig(MerchantreeConfig.CONFIG_FILE_NAME, MerchantreeConfig.CODEC);
    }

    @Override
    public CompletableFuture<Void> preLoad() {
        LOGGER.atInfo().log("Pre-loading Merchantree plugin configurations...");
        return CompletableFuture.runAsync(() -> {
            config.load();
            super.preLoad();
        });
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Loaded %s version %s", this.getName(), this.getManifest().getVersion().toString());

        NPCPlugin.get().registerCoreComponentType("OpenKiosk", BuilderActionOpenKiosk::new);

        this.getAssetRegistry()
            .register(
                    HytaleAssetStore.builder(KioskAsset.class, new DefaultAssetMap<>())
                    .setPath("Kiosks")
                    .setCodec(KioskAsset.CODEC)
                    .setKeyFunction(KioskAsset::getId)
                    .loadsAfter(Item.class)
                    .build()
            );

        this.getCodecRegistry(ChoiceElement.CODEC).register("KioskShopElement", ShopElement.class, ShopElement.CODEC);
        this.getCodecRegistry(ChoiceInteraction.CODEC).register("KioskGiveItem", GiveItemInteraction.class, GiveItemInteraction.CODEC);
        this.getCodecRegistry(OpenCustomUIInteraction.PAGE_CODEC).register("Kiosk", ShopPageSupplier.class, ShopPageSupplier.CODEC);
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Starting Merchantree plugin...");
        this.InitializeEvents();
    }

    protected void InitializeEvents() {
        this.getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);
    }

    protected void onPlayerConnect(@NonNull PlayerConnectEvent event) {
        String welcome = config.get().getWelcomeMessage();
        LOGGER.atInfo().log("Player connected! Sending welcome message: %s", welcome);

        World world = event.getWorld();
        if (world == null) {
            LOGGER.atWarning().log("World is null for player %s", event.getPlayerRef().getUsername());
            return;
        }

        event.getWorld().sendMessage(Message.raw("New player connected! " + welcome + " " + event.getPlayerRef().getUsername()));
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down Merchantree plugin...");

        config.save();
        savePlayersData();
    }

    protected void savePlayersData() {
        // Probably gonna be in a different class later
        LOGGER.atInfo().log("Saving all players' data...");
    }

    public static MerchantreePlugin getInstance() {
        return instance;
    }
}
