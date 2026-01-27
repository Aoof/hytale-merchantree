package ca.aoof.merchantree.kiosk;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;

public class FixedTradeSlot extends TradeSlot {
   public static final BuilderCodec<FixedTradeSlot> CODEC = BuilderCodec.builder(FixedTradeSlot.class, FixedTradeSlot::new)
      .append(new KeyedCodec<>("Trade", PlayerTrade.CODEC), (slot, trade) -> slot.trade = trade, slot -> slot.trade)
      .addValidator(Validators.nonNull())
      .add()
      .build();
   protected PlayerTrade trade;

   public FixedTradeSlot(@Nonnull PlayerTrade trade) {
      this.trade = trade;
   }

   protected FixedTradeSlot() {
   }

   @Nonnull
   public PlayerTrade getTrade() {
      return this.trade;
   }

   @Nonnull
   @Override
   public List<PlayerTrade> resolve(@Nonnull Random random) {
      return Collections.singletonList(this.trade);
   }

   @Override
   public int getSlotCount() {
      return 1;
   }

   @Nonnull
   @Override
   public String toString() {
      return "FixedTradeSlot{trade=" + this.trade + "}";
   }
}
