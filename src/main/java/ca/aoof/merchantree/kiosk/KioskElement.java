package ca.aoof.merchantree.kiosk;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceElement;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public class KioskElement extends ChoiceElement {
    public static final BuilderCodec<KioskElement> CODEC = BuilderCodec.builder(KioskElement.class, KioskElement::new, ChoiceElement.BASE_CODEC)
            .append(new KeyedCodec<>("Cost", Codec.INTEGER), (kioskElement, integer) -> kioskElement.cost = integer, kioskElement -> kioskElement.cost)
            .addValidator(Validators.greaterThanOrEqual(0))
            .add()
            .append(new KeyedCodec<>("Icon", Codec.STRING), (kioskElement, s) -> kioskElement.iconPath = s, kioskElement -> kioskElement.iconPath)
            .add()
            .build();
    protected int cost;
    protected String iconPath;

    @Override
    public void addButton(@Nonnull UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, String selector, PlayerRef playerRef) {
        commandBuilder.append("#ElementList", "Pages/KioskElementButton.ui");
        commandBuilder.set(selector + " #Icon.Background", this.iconPath);
        commandBuilder.setObject(selector + " #Name.Text", LocalizableString.fromMessageId(this.displayNameKey));
        commandBuilder.setObject(selector + " #Description.Text", LocalizableString.fromMessageId(this.descriptionKey));
        commandBuilder.set(selector + " #Cost.Text", this.cost + "");
    }

    @Override
    public boolean canFulfillRequirements(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef) {
        return super.canFulfillRequirements(store, ref, playerRef);
    }
}
