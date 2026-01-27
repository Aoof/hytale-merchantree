package ca.aoof.merchantree.kiosk;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.util.BsonUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bson.BsonDocument;

public class KioskState {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static KioskState instance;
    private static Path saveDirectory;
    public static final BuilderCodec<KioskState.ShopInstanceState> SHOP_INSTANCE_CODEC = BuilderCodec.builder(
                    KioskState.ShopInstanceState.class, KioskState.ShopInstanceState::new
            )
            .append(new KeyedCodec<>("Stock", Codec.INT_ARRAY), (state, stock) -> state.currentStock = stock, state -> state.currentStock)
            .add()
            .append(new KeyedCodec<>("NextRefresh", Codec.INSTANT, true), (state, instant) -> state.nextRefreshTime = instant, state -> state.nextRefreshTime)
            .add()
            .append(new KeyedCodec<>("ResolveSeed", Codec.LONG, true), (state, seed) -> state.resolveSeed = seed, state -> state.resolveSeed)
            .add()
            .build();
    public static final BuilderCodec<KioskState> CODEC = BuilderCodec.builder(KioskState.class, KioskState::new)
            .append(
                    new KeyedCodec<>("Shops", new MapCodec<>(SHOP_INSTANCE_CODEC, Object2ObjectOpenHashMap::new, false)),
                    (state, shops) -> state.shopStates.putAll(shops),
                    state -> new Object2ObjectOpenHashMap<>(state.shopStates)
            )
            .add()
            .build();
    private final Map<String, KioskState.ShopInstanceState> shopStates = new ConcurrentHashMap<>();

    public static void initialize(@Nonnull Path dataDirectory) {
        saveDirectory = dataDirectory;
        load();
    }

    @Nonnull
    public static KioskState get() {
        if (instance == null) {
            instance = new KioskState();
        }

        return instance;
    }

    public static void load() {
        if (saveDirectory == null) {
            LOGGER.at(Level.WARNING).log("Cannot load kiosk state: save directory not set");
            instance = new KioskState();
        } else {
            Path file = saveDirectory.resolve("kiosk_state.json");
            if (!Files.exists(file)) {
                LOGGER.at(Level.INFO).log("No saved kiosk state found, starting fresh");
                instance = new KioskState();
            } else {
                try {
                    BsonDocument document = BsonUtil.readDocumentNow(file);
                    if (document != null) {
                        ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
                        instance = CODEC.decode(document, extraInfo);
                        extraInfo.getValidationResults().logOrThrowValidatorExceptions(LOGGER);
                        LOGGER.at(Level.INFO).log("Loaded kiosk state with %d shops", instance.shopStates.size());
                    } else {
                        instance = new KioskState();
                    }
                } catch (Exception var3) {
                    LOGGER.at(Level.WARNING).withCause(var3).log("Failed to load kiosk state, starting fresh");
                    instance = new KioskState();
                }
            }
        }
    }

    public static void save() {
        if (saveDirectory != null && instance != null) {
            try {
                if (!Files.exists(saveDirectory)) {
                    Files.createDirectories(saveDirectory);
                }

                Path file = saveDirectory.resolve("kiosk_state.json");
                BsonUtil.writeSync(file, CODEC, instance, LOGGER);
                LOGGER.at(Level.FINE).log("Saved kiosk state");
            } catch (IOException var1) {
                LOGGER.at(Level.WARNING).withCause(var1).log("Failed to save kiosk state");
            }
        }
    }

    public static void shutdown() {
        save();
        instance = null;
    }

    private static Instant calculateNextScheduledRestock(@Nonnull Instant gameTime, int intervalDays, int restockHour) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(gameTime, ZoneOffset.UTC);
        long daysSinceEpoch = Duration.between(WorldTimeResource.ZERO_YEAR, gameTime).toDays();
        long currentCycle = daysSinceEpoch / intervalDays;
        long nextRestockDaySinceEpoch = (currentCycle + 1L) * intervalDays;
        boolean isTodayRestockDay = daysSinceEpoch % intervalDays == 0L;
        if (isTodayRestockDay && dateTime.getHour() < restockHour) {
            nextRestockDaySinceEpoch = daysSinceEpoch;
        }

        return WorldTimeResource.ZERO_YEAR.plus(Duration.ofDays(nextRestockDaySinceEpoch)).plus(Duration.ofHours(restockHour));
    }

    @Nonnull
    public KioskState.ShopInstanceState getOrCreateShopState(KioskAsset asset, @Nonnull Instant gameTime) {
        return this.shopStates.computeIfAbsent(asset.getId(), id -> {
            KioskState.ShopInstanceState state = new KioskState.ShopInstanceState();
            state.resetStockAndResolve(asset);
            RefreshInterval interval = asset.getRefreshInterval();
            if (interval != null) {
                state.setNextRefreshTime(calculateNextScheduledRestock(gameTime, interval.getDays(), asset.getRestockHour()));
            }

            return state;
        });
    }

    public void checkRefresh(KioskAsset asset, @Nonnull Instant gameTime) {
        RefreshInterval interval = asset.getRefreshInterval();
        if (interval != null) {
            KioskState.ShopInstanceState state = this.getOrCreateShopState(asset, gameTime);
            Instant nextRefresh = state.getNextRefreshTime();
            if (nextRefresh == null) {
                state.setNextRefreshTime(calculateNextScheduledRestock(gameTime, interval.getDays(), asset.getRestockHour()));
                save();
            } else {
                if (!gameTime.isBefore(nextRefresh)) {
                    state.resetStockAndResolve(asset);
                    state.setNextRefreshTime(calculateNextScheduledRestock(gameTime, interval.getDays(), asset.getRestockHour()));
                    save();
                }
            }
        }
    }

    public int[] getStockArray(KioskAsset asset, @Nonnull Instant gameTime) {
        this.checkRefresh(asset, gameTime);
        KioskState.ShopInstanceState state = this.getOrCreateShopState(asset, gameTime);
        if (state.expandStockIfNeeded(asset)) {
            save();
        }

        return (int[])state.getCurrentStock().clone();
    }

    @Nonnull
    public PlayerTrade[] getResolvedTrades(KioskAsset asset, @Nonnull Instant gameTime) {
        this.checkRefresh(asset, gameTime);
        KioskState.ShopInstanceState state = this.getOrCreateShopState(asset, gameTime);
        return state.getResolvedTrades(asset);
    }

    public boolean executeTrade(KioskAsset asset, int tradeIndex, int quantity, @Nonnull Instant gameTime) {
        this.checkRefresh(asset, gameTime);
        KioskState.ShopInstanceState state = this.getOrCreateShopState(asset, gameTime);
        boolean success = state.decrementStock(tradeIndex, quantity);
        if (success) {
            save();
        }

        return success;
    }

    public static class ShopInstanceState {
        private int[] currentStock = new int[0];
        private Instant nextRefreshTime;
        private Long resolveSeed;
        private transient PlayerTrade[] resolvedTrades;

        public ShopInstanceState() {
        }

        public ShopInstanceState(int tradeCount) {
            this.currentStock = new int[tradeCount];
            this.nextRefreshTime = null;
        }

        public int[] getCurrentStock() {
            return this.currentStock;
        }

        @Nullable
        public Instant getNextRefreshTime() {
            return this.nextRefreshTime;
        }

        public void setNextRefreshTime(Instant time) {
            this.nextRefreshTime = time;
        }

        @Nullable
        public Long getResolveSeed() {
            return this.resolveSeed;
        }

        public void setResolveSeed(Long seed) {
            this.resolveSeed = seed;
        }

        @Nonnull
        public PlayerTrade[] getResolvedTrades(@Nonnull KioskAsset asset) {
            if (!asset.hasTradeSlots()) {
                return asset.getTrades() != null ? asset.getTrades() : new PlayerTrade[0];
            } else if (this.resolvedTrades != null) {
                return this.resolvedTrades;
            } else {
                if (this.resolveSeed == null) {
                    this.resolveSeed = ThreadLocalRandom.current().nextLong();
                }

                this.resolvedTrades = resolveTradeSlots(asset, this.resolveSeed);
                return this.resolvedTrades;
            }
        }

        @Nonnull
        private static PlayerTrade[] resolveTradeSlots(@Nonnull KioskAsset asset, long seed) {
            TradeSlot[] slots = asset.getTradeSlots();
            if (slots != null && slots.length != 0) {
                Random random = new Random(seed);
                List<PlayerTrade> result = new ObjectArrayList<>();

                for (TradeSlot slot : slots) {
                    result.addAll(slot.resolve(random));
                }

                return result.toArray(new PlayerTrade[0]);
            } else {
                return new PlayerTrade[0];
            }
        }

        public void resetStockAndResolve(@Nonnull KioskAsset asset) {
            if (asset.hasTradeSlots()) {
                this.resolveSeed = ThreadLocalRandom.current().nextLong();
                this.resolvedTrades = resolveTradeSlots(asset, this.resolveSeed);
            } else {
                this.resolvedTrades = null;
            }

            PlayerTrade[] trades = this.getResolvedTrades(asset);
            this.currentStock = new int[trades.length];

            for (int i = 0; i < trades.length; i++) {
                this.currentStock[i] = trades[i].getMaxStock();
            }
        }

        public boolean expandStockIfNeeded(KioskAsset asset) {
            PlayerTrade[] trades = this.getResolvedTrades(asset);
            if (this.currentStock.length >= trades.length) {
                return false;
            } else {
                int[] newStock = new int[trades.length];
                System.arraycopy(this.currentStock, 0, newStock, 0, this.currentStock.length);

                for (int i = this.currentStock.length; i < trades.length; i++) {
                    newStock[i] = trades[i].getMaxStock();
                }

                this.currentStock = newStock;
                return true;
            }
        }

        public boolean hasStock(int tradeIndex, int quantity) {
            return tradeIndex >= 0 && tradeIndex < this.currentStock.length && this.currentStock[tradeIndex] >= quantity;
        }

        public boolean decrementStock(int tradeIndex, int quantity) {
            if (!this.hasStock(tradeIndex, quantity)) {
                return false;
            } else {
                this.currentStock[tradeIndex] = this.currentStock[tradeIndex] - quantity;
                return true;
            }
        }

        public int getStock(int tradeIndex) {
            return tradeIndex >= 0 && tradeIndex < this.currentStock.length ? this.currentStock[tradeIndex] : 0;
        }
    }
}
