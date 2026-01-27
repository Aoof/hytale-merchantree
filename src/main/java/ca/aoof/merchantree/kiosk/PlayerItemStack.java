package ca.aoof.merchantree.kiosk;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import javax.annotation.Nonnull;

public class PlayerItemStack {
   public static final BuilderCodec<PlayerItemStack> CODEC = BuilderCodec.builder(PlayerItemStack.class, PlayerItemStack::new)
      .append(new KeyedCodec<>("ItemId", Codec.STRING), (stack, s) -> stack.itemId = s, stack -> stack.itemId)
      .addValidator(Validators.nonNull())
      .add()
      .<Integer>append(new KeyedCodec<>("Quantity", Codec.INTEGER), (stack, i) -> stack.quantity = i, stack -> stack.quantity)
      .addValidator(Validators.greaterThanOrEqual(1))
      .add()
      .build();
   protected String itemId;
   protected int quantity = 1;

   public PlayerItemStack(String itemId, int quantity) {
      this.itemId = itemId;
      this.quantity = quantity;
   }

   protected PlayerItemStack() {
   }

   public String getItemId() {
      return this.itemId;
   }

   public int getQuantity() {
      return this.quantity;
   }

   @Nonnull
   @Override
   public String toString() {
      return "PlayerItemStack{itemId='" + this.itemId + "', quantity=" + this.quantity + "}";
   }
}
