package ca.aoof.merchantree.kiosk;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import java.util.Arrays;
import javax.annotation.Nonnull;

public class PlayerTrade {
    public static final BuilderCodec<PlayerTrade> CODEC = BuilderCodec.builder(PlayerTrade.class, PlayerTrade::new)
            .append(new KeyedCodec<>("Output", PlayerItemStack.CODEC), (trade, stack) -> trade.output = stack, trade -> trade.output)
            .addValidator(Validators.nonNull())
            .add()
            .<PlayerItemStack[]>append(
                    new KeyedCodec<>("Input", new ArrayCodec<>(PlayerItemStack.CODEC, PlayerItemStack[]::new)),
                    (trade, stacks) -> trade.input = stacks,
                    trade -> trade.input
            )
            .addValidator(Validators.nonNull())
            .add()
            .<Integer>append(new KeyedCodec<>("Stock", Codec.INTEGER), (trade, i) -> trade.maxStock = i, trade -> trade.maxStock)
            .addValidator(Validators.greaterThanOrEqual(1))
            .add()
            .build();
    protected PlayerItemStack output;
    protected PlayerItemStack[] input;
    protected int maxStock = 10;

    public PlayerTrade(PlayerItemStack output, PlayerItemStack[] input, int maxStock) {
        this.output = output;
        this.input = input;
        this.maxStock = maxStock;
    }

    protected PlayerTrade() {
    }

    public PlayerItemStack getOutput() {
        return this.output;
    }

    public PlayerItemStack[] getInput() {
        return this.input;
    }

    public int getMaxStock() {
        return this.maxStock;
    }

    @Nonnull
    @Override
    public String toString() {
        return "PlayerTrade{output=" + this.output + ", input=" + Arrays.toString((Object[])this.input) + ", maxStock=" + this.maxStock + "}";
    }
}
