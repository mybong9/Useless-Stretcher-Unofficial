package com.sorrowmist.useless.stretcher.network;

import com.sorrowmist.useless.stretcher.UselessStretcherMod;


import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.sorrowmist.useless.stretcher.content.blockentity.OmniversalMyriadBlockEntity;
import com.sorrowmist.useless.stretcher.menu.OmniversalMyriadMenu;
import com.sorrowmist.useless.stretcher.content.item.StaffTutorialData;
import com.sorrowmist.useless.stretcher.content.entity.WondrousStaffSummoning;
import com.sorrowmist.useless.stretcher.content.entity.WondrousStaffAccelerationEntity;
import com.sorrowmist.useless.stretcher.content.range.RangeAccelerationSavedData;
import com.sorrowmist.useless.stretcher.config.StretcherConfig;
import com.sorrowmist.useless.stretcher.config.ServerConfigSync;
import com.sorrowmist.useless.stretcher.content.acceleration.PermanentAccelerationHistory;
import com.sorrowmist.useless.stretcher.content.mold.MoldCatalogService;
import com.sorrowmist.useless.stretcher.content.apotheosis.ApotheosisStaffSettings;
import com.sorrowmist.useless.stretcher.content.apotheosis.ApotheosisTableData;
import com.sorrowmist.useless.stretcher.content.entity.TimeStopManager;
import com.sorrowmist.useless.stretcher.content.mold.MyriadPatternStore;
import com.sorrowmist.useless.stretcher.content.mold.PatternSearchIndex;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.sorrowmist.uselesscore.api.component.UselessStretcherComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreByteBufCodecs;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Network {
    private static final int MAX_RECLAIM_ENTRIES = 512;
    private Network() {
    }

    public static final int ACTION_REQUEST_STATE = 0;
    public static final int ACTION_SET_ENABLED = 1;
    public static final int ACTION_TOGGLE_PATTERNS = 2;
    public static final int ACTION_CLEAR_PATTERNS = 3;
    public static final int ACTION_TOGGLE_MOD_PATTERNS = 4;
    public static final int ACTION_FETCH_PATTERNS = 5;
    public static final int ACTION_REMOVE_PATTERNS = 6;
    public static final int ACTION_REQUEST_CATALOG = 7;

    public record MoldCatalogPayload(BlockPos pos, int part, int parts, String status, String progress,
                                     List<String> sourceIds, List<String> itemIds)
            implements CoreCustomPacketPayload {
        private static final int MAX_PART_ENTRIES = 256;
        public static final Type<MoldCatalogPayload> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "mold_catalog"));
        public static final CoreStreamCodec<FriendlyByteBuf, MoldCatalogPayload> STREAM_CODEC = CoreStreamCodec.of(
                (buffer, value) -> {
                    buffer.writeBlockPos(value.pos());
                    buffer.writeVarInt(value.part());
                    buffer.writeVarInt(value.parts());
                    buffer.writeUtf(value.status(), 16);
                    buffer.writeUtf(value.progress(), 32);
                    int size = Math.min(MAX_PART_ENTRIES, Math.min(value.sourceIds().size(), value.itemIds().size()));
                    buffer.writeVarInt(size);
                    for (int i = 0; i < size; i++) {
                        buffer.writeUtf(value.sourceIds().get(i), 128);
                        buffer.writeUtf(value.itemIds().get(i), 256);
                    }
                }, buffer -> {
                    BlockPos pos = buffer.readBlockPos();
                    int part = buffer.readVarInt();
                    int parts = buffer.readVarInt();
                    String status = buffer.readUtf(16);
                    String progress = buffer.readUtf(32);
                    int size = buffer.readVarInt();
                    if (size < 0 || size > MAX_PART_ENTRIES || parts < 0 || parts > 4096
                            || (parts > 0 && (part < 0 || part >= parts))) {
                        throw new IllegalArgumentException("Invalid mold catalog batch");
                    }
                    List<String> sources = new ArrayList<>(size);
                    List<String> ids = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        sources.add(buffer.readUtf(128));
                        ids.add(buffer.readUtf(256));
                    }
                    return new MoldCatalogPayload(pos, part, parts, status, progress, sources, ids);
                });
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public record MyriadStatePayload(BlockPos pos, List<String> enabledMolds, List<String> patternMolds,
                                     int patternCount, boolean aeBound, String progress,
                                     int part, int parts) implements CoreCustomPacketPayload {
        public static final Type<MyriadStatePayload> TYPE =
                new Type<>(new ResourceLocation(UselessStretcherMod.MODID, "myriad_state"));

        public static final CoreStreamCodec<FriendlyByteBuf, MyriadStatePayload> STREAM_CODEC = CoreStreamCodec.of(
                (buffer, value) -> {
                    buffer.writeBlockPos(value.pos());
                    buffer.writeCollection(value.enabledMolds(), (buf, id) -> buf.writeUtf(id));
                    buffer.writeCollection(value.patternMolds(), (buf, id) -> buf.writeUtf(id));
                    buffer.writeVarInt(value.patternCount());
                    buffer.writeBoolean(value.aeBound());
                    buffer.writeUtf(value.progress());
                    buffer.writeVarInt(value.part());
                    buffer.writeVarInt(value.parts());
                }, buffer -> new MyriadStatePayload(buffer.readBlockPos(),
                        buffer.readList(buf -> buf.readUtf()), buffer.readList(buf -> buf.readUtf()),
                        buffer.readVarInt(), buffer.readBoolean(), buffer.readUtf(),
                        buffer.readVarInt(), buffer.readVarInt()));

        @Override
        public Type<? extends CoreCustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MyriadActionPayload(BlockPos pos, int action, String moldId, List<String> enabledMolds)
            implements CoreCustomPacketPayload {
        public static final Type<MyriadActionPayload> TYPE =
                new Type<>(new ResourceLocation(UselessStretcherMod.MODID, "myriad_action"));

        public static final CoreStreamCodec<FriendlyByteBuf, MyriadActionPayload> STREAM_CODEC = CoreStreamCodec.composite(
                CoreByteBufCodecs.BLOCK_POS, MyriadActionPayload::pos,
                CoreByteBufCodecs.VAR_INT, MyriadActionPayload::action,
                CoreByteBufCodecs.STRING_UTF8, MyriadActionPayload::moldId,
                CoreByteBufCodecs.collection(ArrayList::new, CoreByteBufCodecs.STRING_UTF8), MyriadActionPayload::enabledMolds,
                MyriadActionPayload::new);

        @Override
        public Type<? extends CoreCustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** A bounded page of stored AE patterns. Keys keep their full component data. */
    public record PatternPagePayload(BlockPos pos, int page, int pages, boolean aeBound,
                                     boolean item, boolean offhand, List<AEItemKey> patterns,
                                     int requestId, String progress, byte[] duplicates,
                                     PatternSearchIndex.DuplicatePage selection) implements CoreCustomPacketPayload {
        public PatternPagePayload(BlockPos pos, int page, int pages, boolean aeBound, boolean item, boolean offhand,
                                  List<AEItemKey> patterns, int requestId, String progress, byte[] duplicates) {
            this(pos, page, pages, aeBound, item, offhand, patterns, requestId, progress, duplicates,
                    PatternSearchIndex.DuplicatePage.EMPTY);
        }
        public PatternPagePayload(BlockPos pos, int page, int pages, boolean aeBound,
                                  boolean item, boolean offhand, List<AEItemKey> patterns) {
            this(pos, page, pages, aeBound, item, offhand, patterns, 0, "", new byte[0]);
        }
        public PatternPagePayload(BlockPos pos, int page, int pages, boolean aeBound,
                                  boolean item, boolean offhand, List<AEItemKey> patterns, int requestId, String progress) {
            this(pos, page, pages, aeBound, item, offhand, patterns, requestId, progress, new byte[0]);
        }
        public static final Type<PatternPagePayload> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "myriad_pattern_page"));
        // Hard wire cap; the actual page size is requested per view (see PatternPageRequestPayload).
        public static final int MAX_PAGE = 256;
        public static final CoreStreamCodec<FriendlyByteBuf, PatternPagePayload> STREAM_CODEC = CoreStreamCodec.of(
                (buf, value) -> {
                    buf.writeBlockPos(value.pos());
                    buf.writeVarInt(value.page());
                    buf.writeVarInt(value.pages());
                    buf.writeBoolean(value.aeBound());
                    buf.writeBoolean(value.item());
                    buf.writeBoolean(value.offhand());
                    buf.writeVarInt(value.requestId());
                    buf.writeUtf(value.progress(), 64);
                    int size = Math.min(MAX_PAGE, value.patterns().size());
                    buf.writeVarInt(size);
                    for (int i = 0; i < size; i++) AEKey.writeKey(buf, value.patterns().get(i));
                    buf.writeByteArray(value.duplicates());
                    buf.writeLong(value.selection().revision());
                    buf.writeVarInt(value.selection().count());
                    buf.writeByteArray(value.selection().keepMost());
                    buf.writeByteArray(value.selection().keepLeast());
                }, buf -> {
                    BlockPos pos = buf.readBlockPos();
                    int page = buf.readVarInt();
                    int pages = buf.readVarInt();
                    boolean bound = buf.readBoolean();
                    boolean item = buf.readBoolean();
                    boolean offhand = buf.readBoolean();
                    int requestId = buf.readVarInt();
                    String progress = buf.readUtf(64);
                    int size = buf.readVarInt();
                    if (page < 0 || pages < 0 || (pages > 0 && page >= pages) || size < 0 || size > MAX_PAGE) {
                        throw new IllegalArgumentException("Invalid pattern page");
                    }
                    List<AEItemKey> values = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        var key = AEKey.readKey(buf);
                        if (key instanceof AEItemKey itemKey) values.add(itemKey);
                    }
                    return new PatternPagePayload(pos, page, pages, bound, item, offhand, values, requestId, progress,
                            buf.readByteArray((MAX_PAGE + 7) / 8),
                            new PatternSearchIndex.DuplicatePage(buf.readLong(), buf.readVarInt(),
                                    buf.readByteArray((MAX_PAGE + 7) / 8), buf.readByteArray((MAX_PAGE + 7) / 8)));
                });
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public record PatternPageRequestPayload(BlockPos pos, int page, boolean item, boolean offhand,
                                            int requestId, String query, byte[] matchingItems,
                                            byte[] matchingFluids, int pageSize, PatternSearchIndex.Mode mode) implements CoreCustomPacketPayload {
        public PatternPageRequestPayload(BlockPos pos, int page, boolean item, boolean offhand) {
            this(pos, page, item, offhand, 0, "", new byte[0], new byte[0], 64, PatternSearchIndex.Mode.ALL);
        }
        public PatternPageRequestPayload(BlockPos pos, int page, boolean item, boolean offhand, int pageSize) {
            this(pos, page, item, offhand, 0, "", new byte[0], new byte[0], pageSize, PatternSearchIndex.Mode.ALL);
        }
        public PatternPageRequestPayload(BlockPos pos, int page, boolean item, boolean offhand,
                int requestId, String query, byte[] items, byte[] fluids) {
            this(pos, page, item, offhand, requestId, query, items, fluids, 64, PatternSearchIndex.Mode.ALL);
        }
        public PatternPageRequestPayload(BlockPos pos, int page, boolean item, boolean offhand,
                int requestId, String query, byte[] items, byte[] fluids, int pageSize) {
            this(pos, page, item, offhand, requestId, query, items, fluids, pageSize, PatternSearchIndex.Mode.ALL);
        }
        public static final Type<PatternPageRequestPayload> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "myriad_pattern_page_request"));
        public static final CoreStreamCodec<FriendlyByteBuf, PatternPageRequestPayload> STREAM_CODEC =
                CoreStreamCodec.of((buf, value) -> {
                    buf.writeBlockPos(value.pos()); buf.writeVarInt(value.page());
                    buf.writeBoolean(value.item()); buf.writeBoolean(value.offhand());
                    buf.writeVarInt(value.requestId()); buf.writeUtf(value.query(), 128);
                    buf.writeByteArray(value.matchingItems()); buf.writeByteArray(value.matchingFluids());
                    buf.writeVarInt(value.pageSize());
                    buf.writeEnum(value.mode());
                }, buf -> new PatternPageRequestPayload(buf.readBlockPos(), buf.readVarInt(),
                        buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readUtf(128),
                        buf.readByteArray(131072), buf.readByteArray(131072),
                        Math.max(1, Math.min(PatternPagePayload.MAX_PAGE, buf.readVarInt())),
                        buf.readEnum(PatternSearchIndex.Mode.class)));
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public record PatternTrimPayload(PatternPageRequestPayload request) implements CoreCustomPacketPayload {
        public static final Type<PatternTrimPayload> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "myriad_pattern_trim"));
        public static final CoreStreamCodec<FriendlyByteBuf, PatternTrimPayload> STREAM_CODEC = CoreStreamCodec.composite(
                PatternPageRequestPayload.STREAM_CODEC, PatternTrimPayload::request, PatternTrimPayload::new);
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public record PatternDeduplicatePayload(PatternPageRequestPayload request, PatternSearchIndex.Keep keep,
                                            long revision) implements CoreCustomPacketPayload {
        public static final Type<PatternDeduplicatePayload> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "myriad_pattern_deduplicate"));
        public static final CoreStreamCodec<FriendlyByteBuf, PatternDeduplicatePayload> STREAM_CODEC = CoreStreamCodec.of(
                (buf, value) -> {
                    PatternPageRequestPayload.STREAM_CODEC.encode(buf, value.request());
                    buf.writeEnum(value.keep()); buf.writeLong(value.revision());
                }, buf -> new PatternDeduplicatePayload(PatternPageRequestPayload.STREAM_CODEC.decode(buf),
                        buf.readEnum(PatternSearchIndex.Keep.class), buf.readLong()));
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public record PatternDeletePayload(BlockPos pos, boolean item, boolean offhand, List<AEItemKey> patterns) implements CoreCustomPacketPayload {
        public static final Type<PatternDeletePayload> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "myriad_pattern_delete"));
        private static final int MAX_DELETE = PatternPagePayload.MAX_PAGE;
        public static final CoreStreamCodec<FriendlyByteBuf, PatternDeletePayload> STREAM_CODEC = CoreStreamCodec.of(
                (buf, value) -> {
                    buf.writeBlockPos(value.pos());
                    buf.writeBoolean(value.item());
                    buf.writeBoolean(value.offhand());
                    int size = Math.min(MAX_DELETE, value.patterns().size());
                    buf.writeVarInt(size);
                    for (int i = 0; i < size; i++) AEKey.writeKey(buf, value.patterns().get(i));
                }, buf -> {
                    BlockPos pos = buf.readBlockPos();
                    boolean item = buf.readBoolean();
                    boolean offhand = buf.readBoolean();
                    int size = buf.readVarInt();
                    if (size < 0 || size > MAX_DELETE) throw new IllegalArgumentException("Invalid pattern delete");
                    List<AEItemKey> values = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        var key = AEKey.readKey(buf);
                        if (key instanceof AEItemKey itemKey) values.add(itemKey);
                    }
                    return new PatternDeletePayload(pos, item, offhand, values);
                });
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public record FullSlotsPayload(boolean full) implements CoreCustomPacketPayload {
        public static final Type<FullSlotsPayload> TYPE =
                new Type<>(new ResourceLocation(UselessStretcherMod.MODID, "full_slots"));

        public static final CoreStreamCodec<FriendlyByteBuf, FullSlotsPayload> STREAM_CODEC = CoreStreamCodec.composite(
                CoreByteBufCodecs.BOOL, FullSlotsPayload::full,
                FullSlotsPayload::new);

        @Override
        public Type<? extends CoreCustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record StaffTutorialPayload() implements CoreCustomPacketPayload {
        public static final Type<StaffTutorialPayload> TYPE =
                new Type<>(new ResourceLocation(UselessStretcherMod.MODID, "staff_tutorial"));
        public static final CoreStreamCodec<FriendlyByteBuf, StaffTutorialPayload> STREAM_CODEC =
                CoreStreamCodec.of((buf, value) -> { }, buf -> new StaffTutorialPayload());

        @Override
        public Type<? extends CoreCustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Sent once when the player opens the X staff configuration screen. */
    public record StaffTutorialOpenedPayload() implements CoreCustomPacketPayload {
        public static final Type<StaffTutorialOpenedPayload> TYPE =
                new Type<>(new ResourceLocation(UselessStretcherMod.MODID, "staff_tutorial_opened"));
        public static final CoreStreamCodec<FriendlyByteBuf, StaffTutorialOpenedPayload> STREAM_CODEC =
                CoreStreamCodec.of((buf, value) -> { }, buf -> new StaffTutorialOpenedPayload());

        @Override
        public Type<? extends CoreCustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record WondrousStaffSpeedPayload(int speed, int mode, boolean accelerationEnabled, boolean offhand)
            implements CoreCustomPacketPayload {
        public static final Type<WondrousStaffSpeedPayload> TYPE =
                new Type<>(new ResourceLocation(UselessStretcherMod.MODID, "wondrous_staff_speed"));

        public static final CoreStreamCodec<FriendlyByteBuf, WondrousStaffSpeedPayload> STREAM_CODEC =
                CoreStreamCodec.composite(
                        CoreByteBufCodecs.VAR_INT, WondrousStaffSpeedPayload::speed,
                        CoreByteBufCodecs.VAR_INT, WondrousStaffSpeedPayload::mode,
                        CoreByteBufCodecs.BOOL, WondrousStaffSpeedPayload::accelerationEnabled,
                        CoreByteBufCodecs.BOOL, WondrousStaffSpeedPayload::offhand,
                        WondrousStaffSpeedPayload::new);

        @Override
        public Type<? extends CoreCustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** One-shot key action; the server owns the 12-second lifetime and ignores repeats while active. */
    public record StaffTimeStopPayload(boolean offhand) implements CoreCustomPacketPayload {
        public static final Type<StaffTimeStopPayload> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "staff_time_stop"));
        public static final CoreStreamCodec<FriendlyByteBuf, StaffTimeStopPayload> STREAM_CODEC =
                CoreStreamCodec.of((buf, value) -> buf.writeBoolean(value.offhand()),
                        buf -> new StaffTimeStopPayload(buf.readBoolean()));
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public record TimeStopStatePayload(int remaining) implements CoreCustomPacketPayload {
        public static final Type<TimeStopStatePayload> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "time_stop_state"));
        public static final CoreStreamCodec<FriendlyByteBuf, TimeStopStatePayload> STREAM_CODEC =
                CoreStreamCodec.composite(CoreByteBufCodecs.VAR_INT, TimeStopStatePayload::remaining, TimeStopStatePayload::new);
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public record EntityTimerSettingsPayload(boolean timers, int speed, boolean offhand) implements CoreCustomPacketPayload {
        public static final Type<EntityTimerSettingsPayload> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "entity_timer_settings"));
        public static final CoreStreamCodec<FriendlyByteBuf, EntityTimerSettingsPayload> STREAM_CODEC =
                CoreStreamCodec.composite(CoreByteBufCodecs.BOOL, EntityTimerSettingsPayload::timers,
                        CoreByteBufCodecs.VAR_INT, EntityTimerSettingsPayload::speed,
                        CoreByteBufCodecs.BOOL, EntityTimerSettingsPayload::offhand, EntityTimerSettingsPayload::new);
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public static void sendEntityTimerSettings(boolean timers, int speed, InteractionHand hand) {
        UselessStretcherMod.NETWORK.sendToServer(new EntityTimerSettingsPayload(timers, speed, hand == InteractionHand.OFF_HAND));
    }

    /** Staff-only feature toggles edited by the X screen. */
    public record WondrousStaffFeaturesPayload(boolean summonEnabled,
                                                boolean lootRefresh, boolean offhand)
            implements CoreCustomPacketPayload {
        public static final Type<WondrousStaffFeaturesPayload> TYPE =
                new Type<>(new ResourceLocation(UselessStretcherMod.MODID,
                        "wondrous_staff_features"));
        public static final CoreStreamCodec<FriendlyByteBuf, WondrousStaffFeaturesPayload> STREAM_CODEC =
                CoreStreamCodec.composite(
                        CoreByteBufCodecs.BOOL, WondrousStaffFeaturesPayload::summonEnabled,
                        CoreByteBufCodecs.BOOL, WondrousStaffFeaturesPayload::lootRefresh,
                        CoreByteBufCodecs.BOOL, WondrousStaffFeaturesPayload::offhand,
                        WondrousStaffFeaturesPayload::new);

        @Override
        public Type<? extends CoreCustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ApotheosisOptionsPayload(boolean selectionMode, int eterna, int quanta, int arcana,
                                           int clues, boolean offhand) implements CoreCustomPacketPayload {
        public static final Type<ApotheosisOptionsPayload> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "apotheosis_options"));
        public static final CoreStreamCodec<FriendlyByteBuf, ApotheosisOptionsPayload> STREAM_CODEC =
                CoreStreamCodec.composite(CoreByteBufCodecs.BOOL, ApotheosisOptionsPayload::selectionMode,
                        CoreByteBufCodecs.VAR_INT, ApotheosisOptionsPayload::eterna,
                        CoreByteBufCodecs.VAR_INT, ApotheosisOptionsPayload::quanta,
                        CoreByteBufCodecs.VAR_INT, ApotheosisOptionsPayload::arcana,
                        CoreByteBufCodecs.VAR_INT, ApotheosisOptionsPayload::clues,
                        CoreByteBufCodecs.BOOL, ApotheosisOptionsPayload::offhand,
                        ApotheosisOptionsPayload::new);
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public record ApotheosisTablePayload(BlockPos pos, boolean offhand) implements CoreCustomPacketPayload {
        public static final Type<ApotheosisTablePayload> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "apotheosis_table"));
        public static final CoreStreamCodec<FriendlyByteBuf, ApotheosisTablePayload> STREAM_CODEC =
                CoreStreamCodec.composite(CoreByteBufCodecs.BLOCK_POS, ApotheosisTablePayload::pos,
                        CoreByteBufCodecs.BOOL, ApotheosisTablePayload::offhand, ApotheosisTablePayload::new);
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    /** Selected entity registry IDs to summon. The server applies the hard count limit. */
    public record WondrousStaffSummonPayload(List<String> entityIds, boolean offhand)
            implements CoreCustomPacketPayload {
        public static final Type<WondrousStaffSummonPayload> TYPE =
                new Type<>(new ResourceLocation(UselessStretcherMod.MODID,
                        "wondrous_staff_summon"));
        public static final CoreStreamCodec<FriendlyByteBuf, WondrousStaffSummonPayload> STREAM_CODEC =
                CoreStreamCodec.of((buffer, value) -> {
                    int size = Math.min(value.entityIds().size(), WondrousStaffSummoning.MAX_SELECTION);
                    buffer.writeVarInt(size);
                    for (int i = 0; i < size; i++) buffer.writeUtf(value.entityIds().get(i), 256);
                    buffer.writeBoolean(value.offhand());
                }, buffer -> {
                    int size = buffer.readVarInt();
                    if (size < 0 || size > WondrousStaffSummoning.MAX_SELECTION) {
                        throw new IllegalArgumentException("Invalid summon selection size: " + size);
                    }
                    List<String> ids = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) ids.add(buffer.readUtf(256));
                    return new WondrousStaffSummonPayload(ids, buffer.readBoolean());
                });

        @Override
        public Type<? extends CoreCustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record WondrousStaffRecallPayload(boolean offhand) implements CoreCustomPacketPayload {
        public static final Type<WondrousStaffRecallPayload> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "wondrous_staff_recall"));
        public static final CoreStreamCodec<FriendlyByteBuf, WondrousStaffRecallPayload> STREAM_CODEC =
                CoreStreamCodec.of((buf, value) -> buf.writeBoolean(value.offhand()),
                        buf -> new WondrousStaffRecallPayload(buf.readBoolean()));
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    /** One range or machine marker shown by the operator reclaimer. */
    public record ReclaimerEntry(boolean range, UUID id, UUID owner, String ownerName,
                                 ResourceLocation dimension, BlockPos pos, String label)
            implements CoreCustomPacketPayload {
        public static final Type<ReclaimerEntry> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "reclaimer_entry"));
        public static final CoreStreamCodec<FriendlyByteBuf, ReclaimerEntry> STREAM_CODEC = CoreStreamCodec.of(
                (buf, value) -> {
                    buf.writeBoolean(value.range()); buf.writeUUID(value.id()); buf.writeUUID(value.owner());
                    buf.writeUtf(value.ownerName(), 64); CoreByteBufCodecs.RESOURCE_LOCATION.encode(buf, value.dimension());
                    buf.writeBlockPos(value.pos()); buf.writeUtf(value.label(), 128);
                }, buf -> new ReclaimerEntry(buf.readBoolean(), buf.readUUID(), buf.readUUID(), buf.readUtf(64),
                        CoreByteBufCodecs.RESOURCE_LOCATION.decode(buf), buf.readBlockPos(), buf.readUtf(128)));
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public record ReclaimerStatePayload(List<ReclaimerEntry> entries, boolean personal) implements CoreCustomPacketPayload {
        public static final Type<ReclaimerStatePayload> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "reclaimer_state"));
        public static final CoreStreamCodec<FriendlyByteBuf, ReclaimerStatePayload> STREAM_CODEC = CoreStreamCodec.of(
                (buf, value) -> {
                    buf.writeBoolean(value.personal());
                    int count = Math.min(MAX_RECLAIM_ENTRIES, value.entries().size()); buf.writeVarInt(count);
                    for (int i = 0; i < count; i++) ReclaimerEntry.STREAM_CODEC.encode(buf, value.entries().get(i));
                }, buf -> {
                    boolean personal = buf.readBoolean();
                    int count = buf.readVarInt();
                    if (count < 0 || count > MAX_RECLAIM_ENTRIES) throw new IllegalArgumentException("Invalid reclaim count");
                    List<ReclaimerEntry> entries = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) entries.add(ReclaimerEntry.STREAM_CODEC.decode(buf));
                    return new ReclaimerStatePayload(entries, personal);
                });
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public record ReclaimerRequestPayload(boolean personal) implements CoreCustomPacketPayload {
        public static final Type<ReclaimerRequestPayload> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "reclaimer_request"));
        public static final CoreStreamCodec<FriendlyByteBuf, ReclaimerRequestPayload> STREAM_CODEC =
                CoreStreamCodec.of((buf, value) -> buf.writeBoolean(value.personal()),
                        buf -> new ReclaimerRequestPayload(buf.readBoolean()));
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public record ReclaimerActionPayload(boolean range, UUID id, boolean personal) implements CoreCustomPacketPayload {
        public static final Type<ReclaimerActionPayload> TYPE = new Type<>(
                new ResourceLocation(UselessStretcherMod.MODID, "reclaimer_action"));
        public static final CoreStreamCodec<FriendlyByteBuf, ReclaimerActionPayload> STREAM_CODEC =
                CoreStreamCodec.of((buf, value) -> {
                    buf.writeBoolean(value.range()); buf.writeUUID(value.id()); buf.writeBoolean(value.personal());
                }, buf -> new ReclaimerActionPayload(buf.readBoolean(), buf.readUUID(), buf.readBoolean()));
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    /** Dimension-wide visual clock used only for client cloud movement. */
    public record TimeAccelerationStatePayload(String dimension, int speed) implements CoreCustomPacketPayload {
        public static final Type<TimeAccelerationStatePayload> TYPE =
                new Type<>(new ResourceLocation(UselessStretcherMod.MODID,
                        "time_acceleration_state"));

        public static final CoreStreamCodec<FriendlyByteBuf, TimeAccelerationStatePayload> STREAM_CODEC =
                CoreStreamCodec.composite(
                        CoreByteBufCodecs.STRING_UTF8, TimeAccelerationStatePayload::dimension,
                        CoreByteBufCodecs.VAR_INT, TimeAccelerationStatePayload::speed,
                        TimeAccelerationStatePayload::new);

        @Override
        public Type<? extends CoreCustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register() {
        UselessStretcherMod.NETWORK.registerToClient(ServerConfigSync.Payload.class, ServerConfigSync.Payload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.sorrowmist.useless.stretcher.client.StretcherConfigScreen.accept(payload)));

        UselessStretcherMod.NETWORK.registerToServer(MyriadActionPayload.class, MyriadActionPayload.STREAM_CODEC, Network::handleAction);
        UselessStretcherMod.NETWORK.registerToClient(MyriadStatePayload.class, MyriadStatePayload.STREAM_CODEC, (payload, context) -> ClientStateReceiver.accept(payload));
        UselessStretcherMod.NETWORK.registerToClient(PatternPagePayload.class, PatternPagePayload.STREAM_CODEC, (payload, context) -> ClientStateReceiver.acceptPatternPage(payload));
        UselessStretcherMod.NETWORK.registerToServer(PatternPageRequestPayload.class, PatternPageRequestPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> handlePatternPageRequest(payload, context)));
        UselessStretcherMod.NETWORK.registerToServer(PatternDeletePayload.class, PatternDeletePayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> handlePatternDelete(payload, context)));
        UselessStretcherMod.NETWORK.registerToServer(PatternTrimPayload.class, PatternTrimPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
                    var request = payload.request();
                    if (context.player() instanceof ServerPlayer player && request.item())
                        sendPatternPage(player, BlockPos.ZERO, request.page(), true, request.offhand(), request, true);
                }));
        UselessStretcherMod.NETWORK.registerToServer(PatternDeduplicatePayload.class, PatternDeduplicatePayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
                    var request = payload.request();
                    if (!(context.player() instanceof ServerPlayer player) || !request.item()) return;
                    var held = player.getItemInHand(request.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                    if (!held.is(com.sorrowmist.useless.stretcher.init.ModItems.USELESS_STRETCHER.get())) return;
                    UUID ref = com.sorrowmist.useless.stretcher.content.mold.MyriadMoldData.readPatternRef(held);
                    if (ref == null) return;
                    com.sorrowmist.useless.stretcher.content.mold.PatternRepositorySearch.deduplicate(player, ref,
                            MyriadPatternStore.get(player.serverLevel()), payload);
                }));
        UselessStretcherMod.NETWORK.registerToClient(MoldCatalogPayload.class, MoldCatalogPayload.STREAM_CODEC, (payload, context) -> ClientStateReceiver.acceptMoldCatalog(payload));
        UselessStretcherMod.NETWORK.registerToClient(FullSlotsPayload.class, FullSlotsPayload.STREAM_CODEC, (payload, context) -> ClientStateReceiver.handleFullSlots(payload));
        UselessStretcherMod.NETWORK.registerToClient(StaffTutorialPayload.class, StaffTutorialPayload.STREAM_CODEC, (payload, context) -> ClientStateReceiver.handleStaffTutorial());
        UselessStretcherMod.NETWORK.registerToServer(StaffTutorialOpenedPayload.class, StaffTutorialOpenedPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        StaffTutorialData.get(player.getServer()).markUiOpened(player.getUUID());
                    }
                }));
        UselessStretcherMod.NETWORK.registerToClient(TimeAccelerationStatePayload.class, TimeAccelerationStatePayload.STREAM_CODEC, (payload, context) -> ClientStateReceiver.handleTimeAcceleration(payload));
        UselessStretcherMod.NETWORK.registerToServer(WondrousStaffSpeedPayload.class, WondrousStaffSpeedPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> handleWondrousStaffSpeed(payload, context)));
        UselessStretcherMod.NETWORK.registerToServer(StaffTimeStopPayload.class, StaffTimeStopPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> handleStaffTimeStop(payload, context)));
        UselessStretcherMod.NETWORK.registerToClient(TimeStopStatePayload.class, TimeStopStatePayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientStateReceiver.handleTimeStop(payload)));
        UselessStretcherMod.NETWORK.registerToServer(EntityTimerSettingsPayload.class, EntityTimerSettingsPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player) || !StretcherConfig.serverStaffAcceleration()) return;
                    ItemStack held = player.getItemInHand(payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                    if (!held.is(com.sorrowmist.useless.stretcher.init.ModItems.WONDROUS_STAFF.get())) return;
                    CoreItemComponents.set(held, UselessStretcherComponents.ENTITY_TIMER_MODE.get(), payload.timers());
                    CoreItemComponents.set(held, UselessStretcherComponents.ENTITY_TIMER_SPEED.get(), com.sorrowmist.useless.stretcher.content.entity.EntityTimerAcceleration.normalizeSpeed(payload.speed()));
                }));
        UselessStretcherMod.NETWORK.registerToServer(WondrousStaffFeaturesPayload.class, WondrousStaffFeaturesPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> handleWondrousStaffFeatures(payload, context)));
        UselessStretcherMod.NETWORK.registerToServer(ApotheosisOptionsPayload.class, ApotheosisOptionsPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> handleApotheosisOptions(payload, context)));
        UselessStretcherMod.NETWORK.registerToServer(ApotheosisTablePayload.class, ApotheosisTablePayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> handleApotheosisTable(payload, context)));
        ApotheosisNetwork.register();
        UselessStretcherMod.NETWORK.registerToServer(WondrousStaffSummonPayload.class, WondrousStaffSummonPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> handleWondrousStaffSummon(payload, context)));
        UselessStretcherMod.NETWORK.registerToServer(WondrousStaffRecallPayload.class, WondrousStaffRecallPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> handleWondrousStaffRecall(payload, context)));
        UselessStretcherMod.NETWORK.registerToServer(ReclaimerRequestPayload.class, ReclaimerRequestPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() ->
                        sendReclaimerState((ServerPlayer) context.player(), payload.personal())));
        UselessStretcherMod.NETWORK.registerToServer(ReclaimerActionPayload.class, ReclaimerActionPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> handleReclaimerAction(payload, context)));
        UselessStretcherMod.NETWORK.registerToClient(ReclaimerStatePayload.class, ReclaimerStatePayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientStateReceiver.handleReclaimer(payload)));
        RangeNetwork.register();
    }

    private static void handleWondrousStaffSpeed(WondrousStaffSpeedPayload payload,
                                                 com.sorrowmist.uselesscore.api.network.CorePacketContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!StretcherConfig.serverStaffAcceleration()) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() != com.sorrowmist.useless.stretcher.init.ModItems.WONDROUS_STAFF.get()) return;
        int speed = switch (payload.speed()) {
            case 0, 2, 4, 16, 32, 64, 128, 256, 512, 1024 -> payload.speed();
            default -> com.sorrowmist.useless.stretcher.content.entity.WondrousStaffAcceleration.DEFAULT_GEAR;
        };
        int mode = payload.mode() >= 0
                && payload.mode() < com.sorrowmist.useless.stretcher.content.entity.WondrousStaffAcceleration.STAFF_MODE_COUNT
                ? payload.mode()
                : com.sorrowmist.useless.stretcher.content.entity.WondrousStaffAcceleration.STAFF_MODE_NORMAL;
        CoreItemComponents.set(held, com.sorrowmist.uselesscore.api.component.UselessStretcherComponents.WONDROUS_STAFF_SPEED.get(), speed);
        com.sorrowmist.useless.stretcher.content.entity.WondrousStaffAcceleration.setMode(held, mode);
        CoreItemComponents.set(held, com.sorrowmist.uselesscore.api.component.UselessComponents.BeefTimeAccelerationEnabledComponent.get(), payload.accelerationEnabled());
        if (StaffTutorialData.get(player.getServer()).markHintShown(player.getUUID(),
                StaffTutorialData.HINT_GEAR_CHANGE)) {
            sendStaffTutorial(player);
        }
    }

    private static void handleStaffTimeStop(StaffTimeStopPayload payload,
                                            com.sorrowmist.uselesscore.api.network.CorePacketContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !StretcherConfig.serverTimeStop()) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(com.sorrowmist.useless.stretcher.init.ModItems.WONDROUS_STAFF.get())) return;
        TimeStopManager.start(player);
    }

    public static void sendStaffTimeStop(InteractionHand hand) {
        UselessStretcherMod.NETWORK.sendToServer(new StaffTimeStopPayload(hand == InteractionHand.OFF_HAND));
    }

    public static void sendTimeStopState(ServerPlayer player, int remaining) {
        UselessStretcherMod.NETWORK.sendToPlayer(player, new TimeStopStatePayload(Math.max(0, remaining)));
    }

    public static void sendWondrousStaffSpeed(int speed, int mode, boolean accelerationEnabled,
                                              InteractionHand hand) {
        UselessStretcherMod.NETWORK.sendToServer(new WondrousStaffSpeedPayload(
                speed, mode, accelerationEnabled, hand == InteractionHand.OFF_HAND));
    }

    private static void handleWondrousStaffFeatures(WondrousStaffFeaturesPayload payload,
                                                    com.sorrowmist.uselesscore.api.network.CorePacketContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(com.sorrowmist.useless.stretcher.init.ModItems.WONDROUS_STAFF.get())) return;
        CoreItemComponents.set(held, UselessStretcherComponents.WONDROUS_STAFF_SUMMON_ENABLED.get(), payload.summonEnabled());
        CoreItemComponents.set(held, UselessStretcherComponents.WONDROUS_STAFF_LOOT_REFRESH.get(), payload.lootRefresh()
                        && com.sorrowmist.useless.stretcher.config.StretcherConfig.enableStaffLootRefresh());
    }

    private static boolean apotheosisAllowed() {
        // Boost/selection options only need Apothic Enchanting, which is standalone.
        return StretcherConfig.enableApotheosisCompat() && ApotheosisStaffSettings.isEnchantingAvailable();
    }

    private static void handleApotheosisOptions(ApotheosisOptionsPayload payload,
            com.sorrowmist.uselesscore.api.network.CorePacketContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !apotheosisAllowed()) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(com.sorrowmist.useless.stretcher.init.ModItems.WONDROUS_STAFF.get())) return;
        var boost = new ApotheosisTableData.Boost(
                payload.eterna(), payload.quanta(), payload.arcana(), payload.clues()).normalized();
        ApotheosisStaffSettings.setBoost(held, boost);
        ApotheosisStaffSettings.setSelectionMode(held, payload.selectionMode());
        if (payload.selectionMode()) {
            CoreItemComponents.set(held, UselessStretcherComponents.RANGE_FILTER_MARKING_MODE.get(), false);
            CoreItemComponents.set(held, UselessStretcherComponents.RANGE_PLACEMENT_MODE.get(), false);
        }
        ApotheosisTableData data = ApotheosisTableData.get(player.getServer());
        if (data.updateOwned(player.getUUID(), boost) > 0) {
            ApotheosisTableData.refreshOpenMenus(player.getServer());
        }
        player.inventoryMenu.broadcastChanges();
        ApotheosisNetwork.sendSettings(player, hand);
    }

    private static void handleApotheosisTable(ApotheosisTablePayload payload,
            com.sorrowmist.uselesscore.api.network.CorePacketContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ApotheosisStaffSettings.markTable(player, hand, payload.pos());
    }

    private static void handleWondrousStaffSummon(WondrousStaffSummonPayload payload,
                                                  com.sorrowmist.uselesscore.api.network.CorePacketContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(com.sorrowmist.useless.stretcher.init.ModItems.WONDROUS_STAFF.get())) return;
        WondrousStaffSummoning.summon(player, held, payload.entityIds());
    }

    private static void handleWondrousStaffRecall(WondrousStaffRecallPayload payload,
                                                   com.sorrowmist.uselesscore.api.network.CorePacketContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ItemStack held = player.getItemInHand(payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        WondrousStaffSummoning.recall(player, held);
    }

    private static boolean canManage(ServerPlayer player, boolean personal) {
        if (player == null || (!personal && !StretcherConfig.serverRemoteReclaimer())) return false;
        var item = personal ? com.sorrowmist.useless.stretcher.init.ModItems.WONDROUS_STAFF.get()
                : com.sorrowmist.useless.stretcher.init.ModItems.RANGE_RECLAIMER.get();
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    private static void sendReclaimerState(ServerPlayer player, boolean personal) {
        if (!canManage(player, personal)) return;
        List<ReclaimerEntry> result = new ArrayList<>();
        RangeAccelerationSavedData data = RangeAccelerationSavedData.get(player.getServer());
        for (RangeAccelerationSavedData.Field field : data.operatorFields()) {
            if (personal && !field.owner().equals(player.getUUID())) continue;
            if (result.size() >= MAX_RECLAIM_ENTRIES) break;
            result.add(new ReclaimerEntry(true, field.id(), field.owner(), ownerName(player, field.owner()),
                    field.dimension(), field.center(), (field.name().isBlank() ? "时间流逝" : field.name())
                            + " · x" + field.speed()));
        }
        for (var entry : PermanentAccelerationHistory.get(player.getServer())
                .activeEntries(personal ? player.getUUID() : null)) {
            if (result.size() >= MAX_RECLAIM_ENTRIES) break;
            result.add(new ReclaimerEntry(false, entry.id(), entry.owner(), ownerName(player, entry.owner()),
                    entry.dimension(), entry.pos(), entry.label()));
        }
        result.sort(java.util.Comparator.comparing(ReclaimerEntry::ownerName)
                .thenComparing(entry -> entry.owner().toString()).thenComparing(ReclaimerEntry::label));
        UselessStretcherMod.NETWORK.sendToPlayer(player, new ReclaimerStatePayload(result, personal));
    }

    private static String ownerName(ServerPlayer viewer, UUID owner) {
        ServerPlayer online = viewer.getServer().getPlayerList().getPlayer(owner);
        if (online != null) return online.getGameProfile().getName();
        var cache = viewer.getServer().getProfileCache();
        return cache == null ? owner.toString()
                : cache.get(owner).map(com.mojang.authlib.GameProfile::getName).orElse(owner.toString());
    }

    private static void handleReclaimerAction(ReclaimerActionPayload payload,
                                               com.sorrowmist.uselesscore.api.network.CorePacketContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!canManage(player, payload.personal())) return;
        boolean removed = false;
        if (payload.range()) {
            var ranges = RangeAccelerationSavedData.get(player.getServer());
            removed = payload.personal()
                    ? ranges.reclaim(player.getServer(), player.getUUID(), payload.id()) != null
                    : ranges.reclaimByOperator(player.getServer(), payload.id()) != null;
        } else {
            removed = PermanentAccelerationHistory.get(player.getServer())
                    .reclaim(player.getServer(), payload.id(), payload.personal() ? player.getUUID() : null);
        }
        player.displayClientMessage(Component.translatable(removed
                ? "msg.useless_stretcher.reclaimer.removed"
                : "msg.useless_stretcher.reclaimer.missing"), true);
        sendReclaimerState(player, payload.personal());
    }

    public static void sendWondrousStaffFeatures(boolean summonEnabled, boolean lootRefresh,
                                                 InteractionHand hand) {
        UselessStretcherMod.NETWORK.sendToServer(new WondrousStaffFeaturesPayload(
                summonEnabled, lootRefresh, hand == InteractionHand.OFF_HAND));
    }

    public static void sendApotheosisOptions(boolean selectionMode, ApotheosisTableData.Boost boost,
                                             InteractionHand hand) {
        UselessStretcherMod.NETWORK.sendToServer(new ApotheosisOptionsPayload(selectionMode, boost.eterna(),
                boost.quanta(), boost.arcana(), boost.clues(), hand == InteractionHand.OFF_HAND));
    }

    public static void markApotheosisTable(BlockPos pos, InteractionHand hand) {
        UselessStretcherMod.NETWORK.sendToServer(new ApotheosisTablePayload(pos, hand == InteractionHand.OFF_HAND));
    }

    public static void sendWondrousStaffSummon(List<String> entityIds, InteractionHand hand) {
        UselessStretcherMod.NETWORK.sendToServer(new WondrousStaffSummonPayload(
                List.copyOf(entityIds), hand == InteractionHand.OFF_HAND));
    }

    public static void recallWondrousStaff(InteractionHand hand) {
        UselessStretcherMod.NETWORK.sendToServer(new WondrousStaffRecallPayload(hand == InteractionHand.OFF_HAND));
    }

    public static void requestReclaimer() {
        UselessStretcherMod.NETWORK.sendToServer(new ReclaimerRequestPayload(false));
    }

    public static void requestPersonalReclaimer() {
        UselessStretcherMod.NETWORK.sendToServer(new ReclaimerRequestPayload(true));
    }

    public static void requestReclaimerState(ServerPlayer player) {
        sendReclaimerState(player, false);
    }

    public static void reclaimTarget(boolean range, UUID id, boolean personal) {
        UselessStretcherMod.NETWORK.sendToServer(new ReclaimerActionPayload(range, id, personal));
    }

    public static void sendStaffTutorial(ServerPlayer player) {
        UselessStretcherMod.NETWORK.sendToPlayer(player, new StaffTutorialPayload());
    }

    public static void sendStaffTutorialOpened() {
        UselessStretcherMod.NETWORK.sendToServer(new StaffTutorialOpenedPayload());
    }

    public static void sendTimeAccelerationState(net.minecraft.server.level.ServerLevel level, int speed) {
        UselessStretcherMod.NETWORK.sendToDimension(level, new TimeAccelerationStatePayload(
                level.dimension().location().toString(), Math.max(0, speed)));
    }

    private static void handleAction(MyriadActionPayload payload, com.sorrowmist.uselesscore.api.network.CorePacketContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!(player.containerMenu instanceof OmniversalMyriadMenu menu)
                    || !menu.getPos().equals(payload.pos())
                    || player.distanceToSqr(payload.pos().getCenter()) > 64.0
                    || !player.level().hasChunkAt(payload.pos())) return;
            if (player.level().getBlockEntity(payload.pos()) instanceof OmniversalMyriadBlockEntity be) {
                switch (payload.action()) {
                    case ACTION_SET_ENABLED -> {
                        Set<ResourceLocation> enabled = menu.receiveSelection(payload.moldId(), payload.enabledMolds());
                        if (enabled == null) return;
                        be.setEnabledMolds(enabled);
                    }
                    case ACTION_TOGGLE_PATTERNS -> {
                        ResourceLocation moldId = ResourceLocation.tryParse(payload.moldId());
                        if (moldId != null) {
                            if (be.getRequestedPatternMolds().contains(moldId)) {
                                be.removePatterns(List.of(moldId));
                            } else {
                                be.requestPatterns(List.of(moldId));
                            }
                        }
                    }
                    case ACTION_CLEAR_PATTERNS -> be.clearPatterns();
                    case ACTION_TOGGLE_MOD_PATTERNS -> {
                        List<ResourceLocation> ids = payload.enabledMolds().stream()
                                .map(ResourceLocation::tryParse)
                                .filter(java.util.Objects::nonNull)
                                .toList();
                        Set<ResourceLocation> current = be.getRequestedPatternMolds();
                        boolean all = !ids.isEmpty() && ids.stream().allMatch(current::contains);
                        if (all) be.removePatterns(ids);
                        else be.requestPatterns(ids);
                    }
                    case ACTION_FETCH_PATTERNS, ACTION_REMOVE_PATTERNS -> {
                        Set<ResourceLocation> ids = menu.receiveSelection(
                                payload.action(), payload.moldId(), payload.enabledMolds());
                        if (ids == null) return;
                        boolean fetch = payload.action() == ACTION_FETCH_PATTERNS;
                        if (fetch) be.requestPatterns(ids);
                        else be.removePatterns(ids);
                    }
                    case ACTION_REQUEST_STATE -> {
                        // fall through to reply below
                    }
                    case ACTION_REQUEST_CATALOG -> MoldCatalogService.request(player, payload.pos());
                }
                replyState(player, be);
            }
        });
    }

    private static boolean canViewPatterns(ServerPlayer player, BlockPos pos) {
        return player != null && player.level().hasChunkAt(pos)
                && player.distanceToSqr(pos.getCenter()) <= 64.0D
                && player.level().getBlockEntity(pos) instanceof OmniversalMyriadBlockEntity;
    }

    private static void handlePatternPageRequest(PatternPageRequestPayload payload,
                                                   com.sorrowmist.uselesscore.api.network.CorePacketContext context) {
        if (!(context.player() instanceof ServerPlayer player) || payload.page() < 0) return;
        if (payload.item()) {
            ItemStack held = player.getItemInHand(payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!held.is(com.sorrowmist.useless.stretcher.init.ModItems.USELESS_STRETCHER.get())) return;
            sendPatternPage(player, BlockPos.ZERO, payload.page(), true, payload.offhand(), payload);
        } else if (canViewPatterns(player, payload.pos())) {
            sendPatternPage(player, payload.pos(), payload.page(), false, payload.offhand(), payload);
        }
    }

    private static void handlePatternDelete(PatternDeletePayload payload,
                                              com.sorrowmist.uselesscore.api.network.CorePacketContext context) {
        if (!(context.player() instanceof ServerPlayer player) || payload.patterns().isEmpty()) return;
        UUID ref = null;
        OmniversalMyriadBlockEntity be = null;
        if (payload.item()) {
            ItemStack held = player.getItemInHand(payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!held.is(com.sorrowmist.useless.stretcher.init.ModItems.USELESS_STRETCHER.get())) return;
            ref = com.sorrowmist.useless.stretcher.content.mold.MyriadMoldData.readPatternRef(held);
        } else if (canViewPatterns(player, payload.pos())
                && player.level().getBlockEntity(payload.pos()) instanceof OmniversalMyriadBlockEntity myriad) {
            be = myriad;
            ref = be.getPatternRef();
        }
        if (ref == null) return;
        com.sorrowmist.useless.stretcher.content.mold.PatternRepositorySearch.cancel(player);
        MyriadPatternStore.get((ServerLevel) player.level()).removeKeys(ref, payload.patterns());
        if (be != null) { be.markPatternsChanged(); replyState(player, be); }
        // The item screen follows deletion with its current tagged search request. An untagged
        // response here could replace that filter or reopen an already closed screen.
        if (!payload.item()) sendPatternPage(player, payload.pos(), 0, false, payload.offhand());
    }

    private static void sendPatternPage(ServerPlayer player, BlockPos pos, int requestedPage,
                                        boolean item, boolean offhand) {
        sendPatternPage(player, pos, requestedPage, item, offhand,
                new PatternPageRequestPayload(pos, requestedPage, item, offhand));
    }

    private static void sendPatternPage(ServerPlayer player, BlockPos pos, int requestedPage,
                                        boolean item, boolean offhand, PatternPageRequestPayload request) {
        sendPatternPage(player, pos, requestedPage, item, offhand, request, false);
    }

    private static void sendPatternPage(ServerPlayer player, BlockPos pos, int requestedPage,
                                        boolean item, boolean offhand, PatternPageRequestPayload request, boolean trim) {
        UUID ref;
        boolean aeBound;
        if (item) {
            ItemStack held = player.getItemInHand(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!held.is(com.sorrowmist.useless.stretcher.init.ModItems.USELESS_STRETCHER.get())) return;
            ref = com.sorrowmist.useless.stretcher.content.mold.MyriadMoldData.readPatternRef(held);
            UUID aeRef = com.sorrowmist.useless.stretcher.content.mold.MyriadMoldData.readAeRef(held);
            var serverLevel = (ServerLevel) player.level();
            BlockPos nodePos = com.sorrowmist.useless.stretcher.content.ae.AeBindingStore.get(serverLevel).get(aeRef);
            aeBound = isConnectedAeNode(serverLevel, nodePos);
        } else {
            if (!canViewPatterns(player, pos)) return;
            OmniversalMyriadBlockEntity be = (OmniversalMyriadBlockEntity) player.level().getBlockEntity(pos);
            if (be == null) return;
            ref = be.getPatternRef();
            aeBound = isConnectedAeNode((ServerLevel) player.level(), be.getAeNodePos());
        }
        var store = MyriadPatternStore.get((ServerLevel) player.level());
        com.sorrowmist.useless.stretcher.content.mold.PatternRepositorySearch.cancel(player);
        if ((item || !request.query().isBlank()) && ref != null) {
            com.sorrowmist.useless.stretcher.content.mold.PatternRepositorySearch.request(
                    player, ref, store, request, aeBound, trim);
            return;
        }
        var all = store.keys(ref);
        int pageSize = Math.max(1, Math.min(PatternPagePayload.MAX_PAGE, request.pageSize()));
        int pages = Math.max(1, (all.size() + pageSize - 1) / pageSize);
        int page = Math.min(requestedPage, pages - 1);
        int from = page * pageSize;
        List<AEItemKey> pageKeys = all.stream().skip(from).limit(pageSize).toList();
        UselessStretcherMod.NETWORK.sendToPlayer(player, new PatternPagePayload(pos, page, pages, aeBound, item, offhand,
                pageKeys, request.requestId(), ""));
    }

    private static boolean isConnectedAeNode(ServerLevel level, BlockPos pos) {
        if (pos == null || !level.hasChunkAt(pos)
                || !(level.getBlockEntity(pos) instanceof appeng.api.networking.IInWorldGridNodeHost host)) return false;
        for (var side : net.minecraft.core.Direction.values()) {
            var node = host.getGridNode(side);
            if (node != null && node.isActive()) return true;
        }
        return false;
    }

    public static void replyToViewers(OmniversalMyriadBlockEntity be) {
        if (!(be.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        for (ServerPlayer player : level.players()) {
            if (player.containerMenu instanceof OmniversalMyriadMenu menu
                    && menu.getPos().equals(be.getBlockPos())) replyState(player, be);
        }
    }

    private static void replyState(ServerPlayer player, OmniversalMyriadBlockEntity be) {
        if (player.containerMenu instanceof OmniversalMyriadMenu menu
                && !menu.needsSelections(be.selectionVersion())) {
            UselessStretcherMod.NETWORK.sendToPlayer(player, new MyriadStatePayload(be.getBlockPos(), List.of(), List.of(),
                    be.getPatternCount(), be.getAeRef() != null, be.getFetchProgress(), -1, 0));
            return;
        }
        var enabled = MyriadSelectionBatches.split(be.getEnabledMolds().stream().map(ResourceLocation::toString).toList());
        var patternMolds = MyriadSelectionBatches.split(be.getRequestedPatternMolds().stream().map(ResourceLocation::toString).toList());
        int patternCount = be.getPatternCount();
        int parts = Math.max(enabled.size(), patternMolds.size());
        for (int part = 0; part < parts; part++) {
            UselessStretcherMod.NETWORK.sendToPlayer(player,
                    new MyriadStatePayload(be.getBlockPos(), part < enabled.size() ? enabled.get(part) : List.of(),
                            part < patternMolds.size() ? patternMolds.get(part) : List.of(), patternCount,
                            be.getAeRef() != null, be.getFetchProgress(), part, parts));
        }
    }

    public static void requestState(BlockPos pos) {
        UselessStretcherMod.NETWORK.sendToServer(new MyriadActionPayload(pos, ACTION_REQUEST_STATE, "", List.of()));
    }

    public static void setEnabled(BlockPos pos, List<String> enabled) {
        var batches = MyriadSelectionBatches.split(enabled);
        for (int part = 0; part < batches.size(); part++) {
            String phase = batches.size() == 1 ? "single" : part == 0 ? "begin"
                    : part == batches.size() - 1 ? "end" : "append";
            UselessStretcherMod.NETWORK.sendToServer(new MyriadActionPayload(pos, ACTION_SET_ENABLED, phase, batches.get(part)));
        }
    }

    public static void togglePatterns(BlockPos pos, String moldId) {
        UselessStretcherMod.NETWORK.sendToServer(new MyriadActionPayload(pos, ACTION_TOGGLE_PATTERNS, moldId, List.of()));
    }

    public static void toggleModPatterns(BlockPos pos, List<String> moldIds) {
        UselessStretcherMod.NETWORK.sendToServer(new MyriadActionPayload(pos, ACTION_TOGGLE_MOD_PATTERNS, "", moldIds));
    }

    public static void setPatterns(BlockPos pos, List<String> moldIds, boolean fetch) {
        var batches = MyriadSelectionBatches.split(moldIds);
        for (int part = 0; part < batches.size(); part++) {
            String phase = batches.size() == 1 ? "single" : part == 0 ? "begin"
                    : part == batches.size() - 1 ? "end" : "append";
            UselessStretcherMod.NETWORK.sendToServer(new MyriadActionPayload(pos,
                    fetch ? ACTION_FETCH_PATTERNS : ACTION_REMOVE_PATTERNS, phase, batches.get(part)));
        }
    }

    public static void clearPatterns(BlockPos pos) {
        UselessStretcherMod.NETWORK.sendToServer(new MyriadActionPayload(pos, ACTION_CLEAR_PATTERNS, "", List.of()));
    }

    public static void requestMoldCatalog(BlockPos pos) {
        UselessStretcherMod.NETWORK.sendToServer(new MyriadActionPayload(pos, ACTION_REQUEST_CATALOG, "", List.of()));
    }

    public static void requestPatternPage(BlockPos pos, int page) {
        UselessStretcherMod.NETWORK.sendToServer(new PatternPageRequestPayload(pos, Math.max(0, page), false, false));
    }

    public static void requestStretcherPatternPage(InteractionHand hand, int page) {
        UselessStretcherMod.NETWORK.sendToServer(new PatternPageRequestPayload(BlockPos.ZERO, Math.max(0, page), true,
                hand == InteractionHand.OFF_HAND));
    }

    public static void requestStretcherPatternPage(InteractionHand hand, int page, int requestId,
                                                  String query, byte[] items, byte[] fluids) {
        requestStretcherPatternPage(hand, page, requestId, query, items, fluids, 64);
    }

    public static void requestStretcherPatternPage(InteractionHand hand, int page, int requestId,
                                                  String query, byte[] items, byte[] fluids, int pageSize) {
        requestStretcherPatternPage(hand, page, requestId, query, items, fluids, pageSize, PatternSearchIndex.Mode.ALL);
    }

    public static void requestStretcherPatternPage(InteractionHand hand, int page, int requestId,
            String query, byte[] items, byte[] fluids, int pageSize, PatternSearchIndex.Mode mode) {
        UselessStretcherMod.NETWORK.sendToServer(new PatternPageRequestPayload(BlockPos.ZERO, Math.max(0, page), true,
                hand == InteractionHand.OFF_HAND, requestId, query, items, fluids, Math.max(1, pageSize), mode));
    }

    public static void trimStretcherPatterns(InteractionHand hand, int page, int requestId, int pageSize) {
        UselessStretcherMod.NETWORK.sendToServer(new PatternTrimPayload(new PatternPageRequestPayload(BlockPos.ZERO,
                Math.max(0, page), true, hand == InteractionHand.OFF_HAND, requestId, "", new byte[0], new byte[0],
                Math.max(1, pageSize), PatternSearchIndex.Mode.OUTPUT)));
    }

    public static void deletePatterns(BlockPos pos, List<AEItemKey> patterns) {
        if (patterns == null || patterns.isEmpty()) return;
        UselessStretcherMod.NETWORK.sendToServer(new PatternDeletePayload(pos, false, false, List.copyOf(patterns)));
    }

    public static void deduplicateStretcherPatterns(InteractionHand hand, int page, int requestId, int pageSize,
                                                    PatternSearchIndex.Keep keep, long revision) {
        UselessStretcherMod.NETWORK.sendToServer(new PatternDeduplicatePayload(new PatternPageRequestPayload(BlockPos.ZERO,
                Math.max(0, page), true, hand == InteractionHand.OFF_HAND, requestId, "", new byte[0], new byte[0],
                Math.max(1, pageSize), PatternSearchIndex.Mode.OUTPUT), keep, revision));
    }

    public static void deleteStretcherPatterns(InteractionHand hand, List<AEItemKey> patterns) {
        if (patterns == null || patterns.isEmpty()) return;
        UselessStretcherMod.NETWORK.sendToServer(new PatternDeletePayload(BlockPos.ZERO, true,
                hand == InteractionHand.OFF_HAND, List.copyOf(patterns)));
    }

    public static void sendMoldCatalogStatus(ServerPlayer player, BlockPos pos, String status, String progress) {
        UselessStretcherMod.NETWORK.sendToPlayer(player,
                new MoldCatalogPayload(pos, -1, 0, status, progress, List.of(), List.of()));
    }

    public static void sendMoldCatalog(ServerPlayer player, BlockPos pos,
                                       Map<String, List<ResourceLocation>> bySource) {
        List<String> sources = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        bySource.forEach((source, sourceIds) -> sourceIds.forEach(id -> {
            sources.add(source);
            ids.add(id.toString());
        }));
        int parts = Math.max(1, (ids.size() + 255) / 256);
        for (int part = 0; part < parts; part++) {
            int from = part * 256;
            int to = Math.min(ids.size(), from + 256);
            UselessStretcherMod.NETWORK.sendToPlayer(player, new MoldCatalogPayload(pos, part, parts, "done", "",
                    List.copyOf(sources.subList(from, to)), List.copyOf(ids.subList(from, to))));
        }
    }

    public static void sendFullSlots(ServerPlayer player) {
        UselessStretcherMod.NETWORK.sendToPlayer(player, new FullSlotsPayload(true));
    }
}
