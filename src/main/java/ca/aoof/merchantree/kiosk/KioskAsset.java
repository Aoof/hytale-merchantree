package ca.aoof.merchantree.kiosk;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class KioskAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, KioskAsset>> {
   public static final AssetBuilderCodec<String, KioskAsset> CODEC = AssetBuilderCodec.builder(
         KioskAsset.class,
         KioskAsset::new,
         Codec.STRING,
         (asset, s) -> asset.id = s,
         asset -> asset.id,
         (asset, data) -> asset.extraData = data,
         asset -> asset.extraData
      )
      .append(new KeyedCodec<>("DisplayNameKey", Codec.STRING), (asset, s) -> asset.displayNameKey = s, asset -> asset.displayNameKey)
         .add()
      .append(
         new KeyedCodec<>("RefreshInterval", RefreshInterval.CODEC), (asset, interval) -> asset.refreshInterval = interval, asset -> asset.refreshInterval
      )
        .add()
      .append(
         new KeyedCodec<>("Trades", new ArrayCodec<>(PlayerTrade.CODEC, PlayerTrade[]::new)), (asset, trades) -> asset.trades = trades, asset -> asset.trades
      )
        .add()
      .append(
         new KeyedCodec<>("TradeSlots", new ArrayCodec<>(TradeSlot.CODEC, TradeSlot[]::new)),
         (asset, slots) -> asset.tradeSlots = slots,
         asset -> asset.tradeSlots
      )
        .add()
      .append(new KeyedCodec<>("RestockHour", Codec.INTEGER, true), (asset, hour) -> asset.restockHour = hour, asset -> asset.restockHour)
        .add()
      .build();
   public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(KioskAsset::getAssetStore));
   private static AssetStore<String, KioskAsset, DefaultAssetMap<String, KioskAsset>> ASSET_STORE;
   protected AssetExtraInfo.Data extraData;
   public static final int DEFAULT_RESTOCK_HOUR = 7;
   protected String id;
   protected String displayNameKey;
   protected RefreshInterval refreshInterval;
   protected PlayerTrade[] trades;
   protected TradeSlot[] tradeSlots;
   protected Integer restockHour;

   public static AssetStore<String, KioskAsset, DefaultAssetMap<String, KioskAsset>> getAssetStore() {
      if (ASSET_STORE == null) {
         ASSET_STORE = AssetRegistry.getAssetStore(KioskAsset.class);
      }

      return ASSET_STORE;
   }

   public static DefaultAssetMap<String, KioskAsset> getAssetMap() {
      return (DefaultAssetMap<String, KioskAsset>)getAssetStore().getAssetMap();
   }

   public KioskAsset(
      String id, String displayNameKey, RefreshInterval refreshInterval, PlayerTrade[] trades, TradeSlot[] tradeSlots, @Nullable Integer restockHour
   ) {
      this.id = id;
      this.displayNameKey = displayNameKey;
      this.refreshInterval = refreshInterval;
      this.trades = trades;
      this.tradeSlots = tradeSlots;
      this.restockHour = restockHour;
   }

   protected KioskAsset() {
   }

   public String getId() {
      return this.id;
   }

   public String getDisplayNameKey() {
      return this.displayNameKey;
   }

   public RefreshInterval getRefreshInterval() {
      return this.refreshInterval;
   }

   public PlayerTrade[] getTrades() {
      return this.trades;
   }

   @Nullable
   public TradeSlot[] getTradeSlots() {
      return this.tradeSlots;
   }

   public boolean hasTradeSlots() {
      return this.tradeSlots != null && this.tradeSlots.length > 0;
   }

   public int getRestockHour() {
      return this.restockHour != null ? this.restockHour : 7;
   }

   @Nonnull
   @Override
   public String toString() {
      return "KioskAsset{id='"
         + this.id
         + "', displayNameKey='"
         + this.displayNameKey
         + "', refreshInterval="
         + this.refreshInterval
         + ", restockHour="
         + this.getRestockHour()
         + ", trades="
         + Arrays.toString((Object[])this.trades)
         + ", tradeSlots="
         + Arrays.toString((Object[])this.tradeSlots)
         + "}";
   }
}
