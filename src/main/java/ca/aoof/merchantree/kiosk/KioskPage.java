package ca.aoof.merchantree.kiosk;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nonnull;

public class KioskPage extends InteractiveCustomUIPage<KioskPage.KioskEventData> {
   private final KioskAsset shopAsset;

   public KioskPage(@Nonnull PlayerRef playerRef, @Nonnull String shopId) {
      super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, KioskPage.KioskEventData.CODEC);
      this.shopAsset = KioskAsset.getAssetMap().getAsset(shopId);
   }

   private boolean isTradeValid(PlayerTrade trade) {
      if (!ItemModule.exists(trade.getOutput().getItemId())) {
         return false;
      } else {
         for (PlayerItemStack input : trade.getInput()) {
            if (!ItemModule.exists(input.getItemId())) {
               return false;
            }
         }

         return true;
      }
   }

   private String getSafeItemId(String itemId) {
      return ItemModule.exists(itemId) ? itemId : "Unknown";
   }

   @Override
   public void build(
      @Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store
   ) {
      if (this.shopAsset != null) {
         commandBuilder.append("Pages/KioskPage.ui");
         String titleKey = this.shopAsset.getDisplayNameKey() != null ? this.shopAsset.getDisplayNameKey() : this.shopAsset.getId();
         commandBuilder.set("#ShopTitle.Text", Message.translation(titleKey));
         WorldTimeResource timeResource = store.getResource(WorldTimeResource.getResourceType());
         Instant gameTime = timeResource != null ? timeResource.getGameTime() : Instant.now();
         KioskState kioskState = KioskState.get();
         int[] stockArray = kioskState.getStockArray(this.shopAsset, gameTime);
         Message refreshText = this.getRefreshTimerText(kioskState, gameTime);
         if (refreshText != null) {
            commandBuilder.set("#RefreshTimer.Text", refreshText);
         }

         commandBuilder.clear("#TradeGrid");
         Ref<EntityStore> playerEntityRef = this.playerRef.getReference();
         Player playerComponent = playerEntityRef != null ? store.getComponent(playerEntityRef, Player.getComponentType()) : null;
         ItemContainer playerInventory = null;
         if (playerComponent != null) {
            playerInventory = playerComponent.getInventory().getCombinedHotbarFirst();
         }

         PlayerTrade[] trades = kioskState.getResolvedTrades(this.shopAsset, gameTime);

         for (int i = 0; i < trades.length; i++) {
            PlayerTrade trade = trades[i];
            String selector = "#TradeGrid[" + i + "]";
            int stock = i < stockArray.length ? stockArray[i] : 0;
            boolean tradeValid = this.isTradeValid(trade);
            commandBuilder.append("#TradeGrid", "Pages/PlayerTradeRow.ui");
            commandBuilder.set(selector + " #OutputSlot.ItemId", this.getSafeItemId(trade.getOutput().getItemId()));
            int outputQty = trade.getOutput().getQuantity();
            commandBuilder.set(selector + " #OutputQuantity.Text", outputQty > 1 ? String.valueOf(outputQty) : "");
            boolean canAfford = true;
            int playerHas = 0;
            if (trade.getInput().length > 0) {
               PlayerItemStack firstInput = trade.getInput()[0];
               String inputItemId = firstInput.getItemId();
               int inputQty = firstInput.getQuantity();
               commandBuilder.set(selector + " #InputSlot.ItemId", this.getSafeItemId(inputItemId));
               commandBuilder.set(selector + " #InputQuantity.Text", inputQty > 1 ? String.valueOf(inputQty) : "");
               if (ItemModule.exists(inputItemId)) {
                  playerHas = playerInventory != null ? this.countItemsInContainer(playerInventory, inputItemId) : 0;
                  canAfford = playerHas >= inputQty;
               } else {
                  canAfford = false;
               }

               commandBuilder.set(selector + " #InputSlotBorder.Background", canAfford ? "#2a5a3a" : "#5a2a2a");
               commandBuilder.set(selector + " #HaveNeedLabel.Text", "Have: " + playerHas);
               commandBuilder.set(selector + " #HaveNeedLabel.Style.TextColor", canAfford ? "#3d913f" : "#962f2f");
            }

            if (!tradeValid) {
               commandBuilder.set(selector + " #Stock.Visible", false);
               commandBuilder.set(selector + " #OutOfStockOverlay.Visible", true);
               commandBuilder.set(selector + " #OutOfStockLabel.Text", "INVALID ITEM");
               commandBuilder.set(selector + " #OutOfStockLabel.Style.TextColor", "#cc8844");
               commandBuilder.set(selector + " #TradeButton.Disabled", true);
               commandBuilder.set(selector + " #TradeButton.Style.Disabled.Background", "#4a3020");
            } else if (stock <= 0) {
               commandBuilder.set(selector + " #Stock.Visible", false);
               commandBuilder.set(selector + " #OutOfStockOverlay.Visible", true);
               commandBuilder.set(selector + " #OutOfStockLabel.Text", "OUT OF STOCK");
               commandBuilder.set(selector + " #OutOfStockLabel.Style.TextColor", "#cc4444");
               commandBuilder.set(selector + " #TradeButton.Disabled", true);
               commandBuilder.set(selector + " #TradeButton.Style.Disabled.Background", "#4a2020");
            } else {
               commandBuilder.set(selector + " #Stock.TextSpans", Message.translation("server.kiosk.customUI.kioskPage.inStock").param("count", stock));
            }

            eventBuilder.addEventBinding(
               CustomUIEventBindingType.Activating, selector + " #TradeButton", EventData.of("TradeIndex", String.valueOf(i)).append("Quantity", "1"), false
            );
            eventBuilder.addEventBinding(
               CustomUIEventBindingType.RightClicking, selector + " #TradeButton", EventData.of("TradeIndex", String.valueOf(i)).append("Quantity", "1"), false
            );
         }

         int cardsPerRow = 3;
         int remainder = trades.length % cardsPerRow;
         if (remainder > 0) {
            int spacersNeeded = cardsPerRow - remainder;

            for (int s = 0; s < spacersNeeded; s++) {
               commandBuilder.append("#TradeGrid", "Pages/PlayerGridSpacer.ui");
            }
         }
      }
   }

   public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull KioskPage.KioskEventData data) {
      if (this.shopAsset != null) {
         int tradeIndex = data.getTradeIndex();
         int requestedQuantity = data.getQuantity();
         if (requestedQuantity > 0) {
            WorldTimeResource timeResource = store.getResource(WorldTimeResource.getResourceType());
            Instant gameTime = timeResource != null ? timeResource.getGameTime() : Instant.now();
            KioskState kioskState = KioskState.get();
            PlayerTrade[] trades = kioskState.getResolvedTrades(this.shopAsset, gameTime);
            if (tradeIndex >= 0 && tradeIndex < trades.length) {
               PlayerTrade trade = trades[tradeIndex];
               if (this.isTradeValid(trade)) {
                  KioskState.ShopInstanceState shopState = kioskState.getOrCreateShopState(this.shopAsset, gameTime);
                  int currentStock = shopState.getCurrentStock()[tradeIndex];
                  if (currentStock > 0) {
                     Ref<EntityStore> playerEntityRef = this.playerRef.getReference();
                     if (playerEntityRef != null) {
                        Player playerComponent = store.getComponent(playerEntityRef, Player.getComponentType());
                        if (playerComponent != null) {
                           Inventory inventory = playerComponent.getInventory();
                           CombinedItemContainer container = inventory.getCombinedHotbarFirst();
                           int maxQuantity = Math.min(requestedQuantity, currentStock);

                           for (PlayerItemStack inputStack : trade.getInput()) {
                              int has = this.countItemsInContainer(container, inputStack.getItemId());
                              int canAfford = has / inputStack.getQuantity();
                              maxQuantity = Math.min(maxQuantity, canAfford);
                           }

                           if (maxQuantity > 0) {
                              int quantity = maxQuantity;

                              for (PlayerItemStack inputStack : trade.getInput()) {
                                 int toRemove = inputStack.getQuantity() * quantity;
                                 this.removeItemsFromContainer(container, inputStack.getItemId(), toRemove);
                              }

                              PlayerItemStack output = trade.getOutput();
                              ItemStack outputStack = new ItemStack(output.getItemId(), output.getQuantity() * quantity);
                              ItemStackTransaction transaction = container.addItemStack(outputStack);
                              ItemStack remainder = transaction.getRemainder();
                              if (remainder != null && !remainder.isEmpty()) {
                                 int addedQty = outputStack.getQuantity() - remainder.getQuantity();
                                 if (addedQty > 0) {
                                    playerComponent.notifyPickupItem(playerEntityRef, outputStack.withQuantity(addedQty), null, store);
                                 }

                                 ItemUtils.dropItem(playerEntityRef, remainder, store);
                              } else {
                                 playerComponent.notifyPickupItem(playerEntityRef, outputStack, null, store);
                              }

                              kioskState.executeTrade(this.shopAsset, tradeIndex, quantity, gameTime);
                              this.updateAfterTrade(ref, store, tradeIndex);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void updateAfterTrade(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, int tradedIndex) {
      UICommandBuilder commandBuilder = new UICommandBuilder();
      WorldTimeResource timeResource = store.getResource(WorldTimeResource.getResourceType());
      Instant gameTime = timeResource != null ? timeResource.getGameTime() : Instant.now();
      KioskState kioskState = KioskState.get();
      int[] stockArray = kioskState.getStockArray(this.shopAsset, gameTime);
      PlayerTrade[] trades = kioskState.getResolvedTrades(this.shopAsset, gameTime);
      Ref<EntityStore> playerEntityRef = this.playerRef.getReference();
      Player playerComponent = playerEntityRef != null ? store.getComponent(playerEntityRef, Player.getComponentType()) : null;
      ItemContainer playerInventory = null;
      if (playerComponent != null) {
         playerInventory = playerComponent.getInventory().getCombinedHotbarFirst();
      }

      for (int i = 0; i < trades.length; i++) {
         PlayerTrade trade = trades[i];
         String selector = "#TradeGrid[" + i + "]";
         int stock = i < stockArray.length ? stockArray[i] : 0;
         boolean tradeValid = this.isTradeValid(trade);
         if (trade.getInput().length > 0) {
            PlayerItemStack firstInput = trade.getInput()[0];
            int playerHas = 0;
            boolean canAfford = false;
            if (ItemModule.exists(firstInput.getItemId())) {
               playerHas = playerInventory != null ? this.countItemsInContainer(playerInventory, firstInput.getItemId()) : 0;
               canAfford = playerHas >= firstInput.getQuantity();
            }

            commandBuilder.set(selector + " #InputSlotBorder.Background", canAfford ? "#2a5a3a" : "#5a2a2a");
            commandBuilder.set(selector + " #HaveNeedLabel.Text", "Have: " + playerHas);
            commandBuilder.set(selector + " #HaveNeedLabel.Style.TextColor", canAfford ? "#3d913f" : "#962f2f");
         }

         if (!tradeValid) {
            commandBuilder.set(selector + " #Stock.Visible", false);
            commandBuilder.set(selector + " #OutOfStockOverlay.Visible", true);
            commandBuilder.set(selector + " #TradeButton.Disabled", true);
         } else if (stock <= 0) {
            commandBuilder.set(selector + " #Stock.Visible", false);
            commandBuilder.set(selector + " #OutOfStockOverlay.Visible", true);
            commandBuilder.set(selector + " #OutOfStockLabel.Text", "OUT OF STOCK");
            commandBuilder.set(selector + " #OutOfStockLabel.Style.TextColor", "#cc4444");
            commandBuilder.set(selector + " #TradeButton.Disabled", true);
            commandBuilder.set(selector + " #TradeButton.Style.Disabled.Background", "#4a2020");
         } else {
            commandBuilder.set(selector + " #Stock.Visible", true);
            commandBuilder.set(selector + " #Stock.TextSpans", Message.translation("server.kiosk.customUI.kioskPage.inStock").param("count", stock));
            commandBuilder.set(selector + " #OutOfStockOverlay.Visible", false);
            commandBuilder.set(selector + " #TradeButton.Disabled", false);
            commandBuilder.set(selector + " #TradeButton.Style.Default.Background", "#1e2a3a");
         }
      }

      this.sendUpdate(commandBuilder, new UIEventBuilder(), false);
   }

   private int countItemsInContainer(ItemContainer container, String itemId) {
      return container.countItemStacks(stack -> itemId.equals(stack.getItemId()));
   }

   private void removeItemsFromContainer(ItemContainer container, String itemId, int amount) {
      container.removeItemStack(new ItemStack(itemId, amount));
   }

   private void refreshUI(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
      UICommandBuilder commandBuilder = new UICommandBuilder();
      UIEventBuilder eventBuilder = new UIEventBuilder();
      this.build(ref, commandBuilder, eventBuilder, store);
      this.sendUpdate(commandBuilder, eventBuilder, true);
   }

   private Message getRefreshTimerText(KioskState kioskState, Instant gameTime) {
      if (this.shopAsset == null) {
         return null;
      } else {
         RefreshInterval interval = this.shopAsset.getRefreshInterval();
         if (interval == null) {
            return null;
         } else {
            KioskState.ShopInstanceState shopState = kioskState.getOrCreateShopState(this.shopAsset, gameTime);
            Instant nextRefresh = shopState.getNextRefreshTime();
            if (nextRefresh == null) {
               return null;
            } else {
               Duration remaining = Duration.between(gameTime, nextRefresh);
               if (!remaining.isNegative() && !remaining.isZero()) {
                  long currentDayNumber = gameTime.getEpochSecond() / WorldTimeResource.SECONDS_PER_DAY;
                  long refreshDayNumber = nextRefresh.getEpochSecond() / WorldTimeResource.SECONDS_PER_DAY;
                  long daysUntilRefresh = refreshDayNumber - currentDayNumber;
                  int hour = this.shopAsset.getRestockHour();
                  String amPm = hour >= 12 ? "PM" : "AM";
                  int displayHour = hour % 12;
                  if (displayHour == 0) {
                     displayHour = 12;
                  }

                  String timeString = String.format("%d:00 %s", displayHour, amPm);
                  if (daysUntilRefresh <= 0L) {
                     return Message.translation("server.kiosk.customUI.kioskPage.restocksToday").param("restockTime", timeString);
                  } else {
                     return daysUntilRefresh == 1L
                        ? Message.translation("server.kiosk.customUI.kioskPage.restocksTomorrow").param("restockTime", timeString)
                        : Message.translation("server.kiosk.customUI.kioskPage.restocksInDays").param("days", (int)daysUntilRefresh);
                  }
               } else {
                  return null;
               }
            }
         }
      }
   }

   public static class KioskEventData {
      static final String TRADE_INDEX = "TradeIndex";
      static final String QUANTITY = "Quantity";
      static final String SHIFT_HELD = "ShiftHeld";
      public static final BuilderCodec<KioskPage.KioskEventData> CODEC = BuilderCodec.builder(
            KioskPage.KioskEventData.class, KioskPage.KioskEventData::new
         )
         .append(new KeyedCodec<>("TradeIndex", Codec.STRING), (data, s) -> data.tradeIndex = Integer.parseInt(s), data -> String.valueOf(data.tradeIndex))
         .add()
         .append(new KeyedCodec<>("Quantity", Codec.STRING), (data, s) -> data.quantity = Integer.parseInt(s), data -> String.valueOf(data.quantity))
         .add()
         .append(new KeyedCodec<>("ShiftHeld", Codec.BOOLEAN), (data, b) -> {
            if (b != null) {
               data.shiftHeld = b;
            }
         }, data -> data.shiftHeld)
         .add()
         .build();
      private int tradeIndex;
      private int quantity = 1;
      private boolean shiftHeld = false;

      public int getTradeIndex() {
         return this.tradeIndex;
      }

      public int getQuantity() {
         return this.shiftHeld ? 10 : this.quantity;
      }

      public boolean isShiftHeld() {
         return this.shiftHeld;
      }
   }
}
