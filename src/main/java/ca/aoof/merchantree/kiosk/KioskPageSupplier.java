package ca.aoof.merchantree.kiosk;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class KioskPageSupplier implements OpenCustomUIInteraction.CustomPageSupplier {
    public static final BuilderCodec<KioskPageSupplier> CODEC = BuilderCodec.builder(KioskPageSupplier.class, KioskPageSupplier::new)
            .appendInherited(
                    new KeyedCodec<>("KioskId", Codec.STRING), (data, o) -> data.kioskId = o, data -> data.kioskId, (data, parent) -> data.kioskId = parent.kioskId
            )
            .add()
            .build();
    protected String kioskId;

    @Nonnull
    @Override
    public CustomUIPage tryCreate(
            Ref<EntityStore> ref, ComponentAccessor<EntityStore> componentAccessor, @Nonnull PlayerRef playerRef, InteractionContext context
    ) {
        return new KioskPage(playerRef, this.kioskId);
    }
}
