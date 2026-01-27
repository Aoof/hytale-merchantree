package ca.aoof.merchantree;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import java.util.ArrayList;
import java.util.List;

public class MerchantreeConfig {
    public static final BuilderCodec<MerchantreeConfig> CODEC =
            BuilderCodec.builder(MerchantreeConfig.class, MerchantreeConfig::new)
                    .append(new KeyedCodec<String>("WelcomeMessage", Codec.STRING),
                            (c, v) -> c.welcomeMessage = v,
                            c -> c.welcomeMessage
                    )
                    .add()
                    .append(new KeyedCodec<String[]>("Features", Codec.STRING_ARRAY),
                            (c, v) -> c.features = v,
                            c -> c.features
                    )
                    .add()
                    .append(new KeyedCodec<Boolean>("DebugMode", Codec.BOOLEAN),
                            (c, v) -> c.debugMode = v,
                            c -> c.debugMode
                    )
                    .add()
                    .build();

    private String welcomeMessage;
    private String[] features;
    private boolean debugMode;
    public static final String CONFIG_FILE_NAME = "merchantree_config";

    public MerchantreeConfig() {
        this.welcomeMessage = "Welcome to Merchantree";
        this.features = new String[] {"Trading", "Auctions", "Marketplaces"};
        this.debugMode = false;
    }

    public MerchantreeConfig(String welcomeMessage, String[] features, boolean debugMode) {
        this.welcomeMessage = welcomeMessage;
        this.features = features;
        this.debugMode = debugMode;
    }

    public String getWelcomeMessage() { return welcomeMessage; }
    public String[] getFeatures() { return features; }
    public boolean isDebugMode() { return debugMode; }
}
