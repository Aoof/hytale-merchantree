package ca.aoof.merchantree.kiosk;

import ca.aoof.merchantree.kiosk.builders.BuilderActionOpenKiosk;
import com.hypixel.hytale.builtin.adventure.shop.barter.BarterPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import javax.annotation.Nonnull;
import java.util.logging.Level;

public class ActionOpenKiosk extends ActionBase {
    protected final String kioskId;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ActionOpenKiosk(@Nonnull BuilderActionOpenKiosk builder, @Nonnull BuilderSupport support) {
        super(builder);
        this.kioskId = builder.getShopId(support);
    }

    @Override
    public boolean canExecute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
        return super.canExecute(ref, role, sensorInfo, dt, store) && role.getStateSupport().getInteractionIterationTarget() != null;
    }

    @Override
    public boolean execute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
        super.execute(ref, role, sensorInfo, dt, store);
        LOGGER.at(Level.INFO).log("Executing ActionOpenKiosk for kioskId: " + this.kioskId);
        Ref<EntityStore> playerReference = role.getStateSupport().getInteractionIterationTarget();
        if (playerReference == null) {
            return false;
        } else {
            PlayerRef playerRefComponent = store.getComponent(playerReference, PlayerRef.getComponentType());
            if (playerRefComponent == null) {
                return false;
            } else {
                Player playerComponent = store.getComponent(playerReference, Player.getComponentType());
                if (playerComponent == null) {
                    return false;
                } else {
                    playerComponent.getPageManager().openCustomPage(ref, store, new BarterPage(playerRefComponent, this.kioskId));
                    return true;
                }
            }
        }
    }
}

