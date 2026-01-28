package ca.aoof.merchantree.kiosk.builders;

import com.google.gson.JsonElement;
import ca.aoof.merchantree.kiosk.ActionOpenKiosk;
import ca.aoof.merchantree.kiosk.KioskExistsValidator;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.InstructionType;
import com.hypixel.hytale.server.npc.asset.builder.holder.AssetHolder;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import java.util.EnumSet;
import javax.annotation.Nonnull;

public class BuilderActionOpenKiosk extends BuilderActionBase {
    protected final AssetHolder shopId = new AssetHolder();

    @Nonnull
    @Override
    public String getShortDescription() {
        return "Open the kiosk UI for the current player";
    }

    @Nonnull
    @Override
    public String getLongDescription() {
        return this.getShortDescription();
    }

    @Nonnull
    public Action build(@Nonnull BuilderSupport builderSupport) {
        return new ActionOpenKiosk(this, builderSupport);
    }

    @Nonnull
    @Override
    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.Stable;
    }

    @Nonnull
    public BuilderActionOpenKiosk readConfig(@Nonnull JsonElement data) {
        this.requireAsset(data, "Shop", this.shopId, KioskExistsValidator.required(), BuilderDescriptorState.Stable, "The kiosk to open", null);
        this.requireInstructionType(EnumSet.of(InstructionType.Interaction));
        return this;
    }

    public String getShopId(@Nonnull BuilderSupport support) {
        return this.shopId.get(support.getExecutionContext());
    }
}

