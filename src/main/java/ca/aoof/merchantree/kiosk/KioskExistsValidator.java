package ca.aoof.merchantree.kiosk;

import ca.aoof.merchantree.kiosk.KioskAsset;
import com.hypixel.hytale.server.npc.asset.builder.validators.AssetValidator;
import java.util.EnumSet;
import javax.annotation.Nonnull;

public class KioskExistsValidator extends AssetValidator {
    private static final KioskExistsValidator DEFAULT_INSTANCE = new KioskExistsValidator();

    private KioskExistsValidator() {
    }

    private KioskExistsValidator(EnumSet<AssetValidator.Config> config) {
        super(config);
    }

    @Nonnull
    @Override
    public String getDomain() {
        return "Kiosk";
    }

    @Override
    public boolean test(String marker) {
        return KioskAsset.getAssetMap().getAsset(marker) != null;
    }

    @Nonnull
    @Override
    public String errorMessage(String marker, String attributeName) {
        return "The barter shop asset with the name \"" + marker + "\" does not exist for attribute \"" + attributeName + "\"";
    }

    @Nonnull
    @Override
    public String getAssetName() {
        return KioskAsset.class.getSimpleName();
    }

    public static KioskExistsValidator required() {
        return DEFAULT_INSTANCE;
    }

    @Nonnull
    public static KioskExistsValidator withConfig(EnumSet<AssetValidator.Config> config) {
        return new KioskExistsValidator(config);
    }
}
