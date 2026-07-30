package com.elfmcys.ysm.network.message.model;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.buffer.UniBufferIO;
import com.elfmcys.ysm.config.ServerConfig;
import com.elfmcys.ysm.network.NetworkHandler;
import com.elfmcys.ysm.network.NetworkPayload;
import com.elfmcys.ysm.network.protocol.FirstOccurrenceTracker;
import com.elfmcys.ysm.network.protocol.PeerLimits;
import com.elfmcys.ysm.network.protocol.ProtocolLimits;
import com.elfmcys.ysm.network.protocol.ProtocolUuid;
import com.elfmcys.ysm.network.protocol.ProtocolVersion;
import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Per-player credit window and bandwidth queue for model transfers. */
public final class ModelTransferSender implements AutoCloseable {
    private final ScheduledExecutorService executor;
    private final LongSupplier bytesPerSecond;
    private final ConcurrentHashMap<UUID, PlayerQueue> queues = new ConcurrentHashMap<>();
    private final FirstOccurrenceTracker<ReleaseAnomalyKey> releaseAnomalies =
            new FirstOccurrenceTracker<>();

    public ModelTransferSender(ScheduledExecutorService executor) {
        this(executor, () -> Math.max(1L, ServerConfig.BANDWIDTH_LIMIT.get()) * 125_000L);
    }

    ModelTransferSender(ScheduledExecutorService executor, LongSupplier bytesPerSecond) {
        this.executor = executor;
        this.bytesPerSecond = bytesPerSecond;
    }

    public TransferHandle send(ServerPlayer player, UUID sessionId, int resourceIndex,
                               TransferKind kind, PreparedTransfer payload) {
        var peerProfile = NetworkHandler.peerProfile(player.connection.connection)
                .orElseThrow(() -> new IllegalStateException("Player protocol handshake is incomplete"));
        if (!ProtocolVersion.supportsRequiredFeatures(peerProfile)) {
            throw new IllegalStateException("Player does not support asset transfer release credits");
        }
        return send(player.getUUID(), peerProfile.receiveLimits(), player.connection::isAcceptingMessages,
                fragment -> NetworkHandler.sendToClientPlayer(fragment, player),
                sessionId, resourceIndex, kind, payload);
    }

    TransferHandle send(UUID playerId, PeerLimits peerLimits, BooleanSupplier acceptingMessages,
                        Consumer<NetworkPayload<AssetTransferV0.AssetFragment>> fragmentSender,
                        UUID sessionId, int resourceIndex, TransferKind kind,
                        PreparedTransfer payload) {
        var job = new TransferJob(playerId, peerLimits, acceptingMessages, fragmentSender,
                sessionId, resourceIndex, kind, payload);
        queues.computeIfAbsent(playerId,
                ignored -> new PlayerQueue(job.maxActiveTransfers)).enqueue(job);
        return new TransferHandle(job);
    }

    public void release(UUID playerId, AssetTransferV0.AssetTransferRelease release) {
        final UUID transferId;
        final UUID requestId;
        try {
            transferId = ProtocolUuid.get(release.getTransferId());
            requestId = ProtocolUuid.get(release.getRequestId());
        } catch (RuntimeException error) {
            releaseAnomaly(playerId, ReleaseAnomaly.INVALID_IDENTITY,
                    "Ignoring asset transfer release with invalid identity from player={}: reason={}",
                    playerId, error.getMessage());
            return;
        }
        var queue = queues.get(playerId);
        var result = queue == null ? ReleaseResult.UNKNOWN
                : queue.release(transferId, requestId, release.getResourceIndex());
        switch (result) {
            case RELEASED -> {
            }
            case UNKNOWN -> releaseAnomaly(playerId, ReleaseAnomaly.UNKNOWN_OR_DUPLICATE,
                    "Ignoring unknown or duplicate asset transfer release from player={}: transfer={}",
                    playerId, transferId);
            case IDENTITY_MISMATCH -> releaseAnomaly(playerId, ReleaseAnomaly.IDENTITY_MISMATCH,
                    "Ignoring mismatched asset transfer release from player={}: transfer={} request={} resource={}",
                    playerId, transferId, requestId, release.getResourceIndex());
        }
    }

    public void cancel(UUID playerId, UUID sessionId) {
        var queue = queues.get(playerId);
        if (queue != null) {
            queue.cancel(sessionId);
        }
    }

    public void playerDisconnected(UUID playerId) {
        var queue = queues.remove(playerId);
        if (queue != null) {
            queue.close();
        }
        releaseAnomalies.removeIf(key -> key.playerId.equals(playerId));
    }

    @Override
    public void close() {
        var copy = queues.values().toArray(PlayerQueue[]::new);
        queues.clear();
        for (var queue : copy) {
            queue.close();
        }
        releaseAnomalies.clear();
    }

    private void releaseAnomaly(UUID playerId, ReleaseAnomaly anomaly,
                                String message, Object... arguments) {
        if (releaseAnomalies.first(new ReleaseAnomalyKey(playerId, anomaly))) {
            YesSteveModel.LOGGER.warn(message, arguments);
        } else {
            YesSteveModel.LOGGER.debug(message, arguments);
        }
    }

    public final class TransferHandle {
        private final TransferJob job;

        private TransferHandle(TransferJob job) {
            this.job = job;
        }

        public CompletableFuture<Void> completion() {
            return job.completion;
        }

        public void cancel() {
            var queue = queues.get(job.playerId);
            if (queue != null) {
                queue.cancel(job);
            }
        }
    }

    private final class PlayerQueue {
        private final int maxActiveTransfers;
        private final ArrayDeque<TransferJob> pending = new ArrayDeque<>();
        private final ArrayDeque<TransferJob> sendable = new ArrayDeque<>();
        private final HashMap<UUID, TransferJob> active = new HashMap<>();
        private ScheduledFuture<?> scheduled;
        private long nextEligibleNanos;
        private boolean draining;
        private boolean closed;

        private PlayerQueue(int maxActiveTransfers) {
            this.maxActiveTransfers = maxActiveTransfers;
        }

        private synchronized void enqueue(TransferJob job) {
            if (closed) {
                job.fail(new IllegalStateException("Player transfer queue is closed"));
                return;
            }
            if (job.maxActiveTransfers != maxActiveTransfers) {
                job.fail(new IllegalStateException(
                        "Peer transfer limit changed before the previous connection was closed"));
                return;
            }
            pending.addLast(job);
            activatePending();
            schedule();
        }

        private synchronized ReleaseResult release(UUID transferId, UUID requestId,
                                                   int resourceIndex) {
            var job = active.get(transferId);
            if (job == null) {
                return ReleaseResult.UNKNOWN;
            }
            if (!job.sessionId.equals(requestId) || job.resourceIndex != resourceIndex) {
                return ReleaseResult.IDENTITY_MISMATCH;
            }
            removeActive(job);
            job.succeed();
            activatePending();
            schedule();
            return ReleaseResult.RELEASED;
        }

        private synchronized void cancel(UUID sessionId) {
            for (var iterator = pending.iterator(); iterator.hasNext(); ) {
                var job = iterator.next();
                if (job.sessionId.equals(sessionId)) {
                    iterator.remove();
                    job.cancel();
                }
            }
            var copy = active.values().toArray(TransferJob[]::new);
            for (var job : copy) {
                if (job.sessionId.equals(sessionId)) {
                    removeActive(job);
                    job.cancel();
                }
            }
            activatePending();
            schedule();
        }

        private synchronized void cancel(TransferJob target) {
            if (pending.remove(target)) {
                target.cancel();
            } else if (active.get(target.transferId) == target) {
                removeActive(target);
                target.cancel();
            }
            activatePending();
            schedule();
        }

        private synchronized void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (scheduled != null) {
                scheduled.cancel(false);
                scheduled = null;
            }
            while (!pending.isEmpty()) {
                pending.removeFirst().cancel();
            }
            var copy = active.values().toArray(TransferJob[]::new);
            active.clear();
            sendable.clear();
            for (var job : copy) {
                job.cancelReleaseTimeout();
                job.cancel();
            }
        }

        private void drain() {
            final NetworkPayload<AssetTransferV0.AssetFragment> fragment;
            final TransferJob job;
            final long wireBytes;
            synchronized (this) {
                scheduled = null;
                draining = true;
                discardCancelledSendable();
                activatePending();
                if (closed || sendable.isEmpty()) {
                    draining = false;
                    return;
                }
                var now = System.nanoTime();
                if (now < nextEligibleNanos) {
                    draining = false;
                    schedule(nextEligibleNanos - now);
                    return;
                }
                job = sendable.peekFirst();
                try {
                    fragment = job.nextFragment();
                    wireBytes = Math.max(1L,
                            fragment.protobuf().getSerializedSize() + fragment.rawSize());
                } catch (Throwable error) {
                    removeActive(job);
                    job.fail(error);
                    draining = false;
                    activatePending();
                    schedule();
                    return;
                }
            }

            Throwable sendError = null;
            var sent = false;
            try {
                if (job.acceptingMessages.getAsBoolean() && !job.cancelled) {
                    job.fragmentSender.accept(fragment);
                    sent = true;
                } else {
                    fragment.close();
                }
            } catch (Throwable error) {
                fragment.close();
                sendError = error;
            }

            synchronized (this) {
                if (active.get(job.transferId) != job || job.cancelled) {
                    draining = false;
                    activatePending();
                    schedule();
                    return;
                }
                if (sent) {
                    job.advance();
                    try {
                        var spacing = wireBytes * 1_000_000_000L
                                / Math.max(1L, bytesPerSecond.getAsLong());
                        nextEligibleNanos = System.nanoTime() + Math.max(1L, spacing);
                    } catch (Throwable error) {
                        removeActive(job);
                        job.fail(error);
                        draining = false;
                        activatePending();
                        schedule();
                        return;
                    }
                    sendable.removeFirstOccurrence(job);
                    if (job.complete()) {
                        awaitRelease(job);
                    } else {
                        sendable.addLast(job);
                    }
                } else {
                    removeActive(job);
                    job.fail(sendError != null ? sendError
                            : new IllegalStateException("Player connection is not accepting messages"));
                }
                draining = false;
                activatePending();
                schedule();
            }
        }

        private void awaitRelease(TransferJob job) {
            job.awaitingRelease = true;
            try {
                job.releaseTimeout = executor.schedule(() -> releaseTimedOut(job),
                        ProtocolLimits.ASSET_TRANSFER_RELEASE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (RuntimeException error) {
                removeActive(job);
                job.fail(error);
            }
        }

        private void releaseTimedOut(TransferJob job) {
            synchronized (this) {
                if (active.get(job.transferId) != job || !job.awaitingRelease) {
                    return;
                }
                removeActive(job);
                job.fail(new TimeoutException("Timed out waiting for asset transfer release"));
                activatePending();
                schedule();
            }
        }

        private void activatePending() {
            while (!closed && active.size() < maxActiveTransfers && !pending.isEmpty()) {
                var job = pending.removeFirst();
                if (job.cancelled) {
                    continue;
                }
                active.put(job.transferId, job);
                sendable.addLast(job);
            }
        }

        private void discardCancelledSendable() {
            sendable.removeIf(job -> job.cancelled || active.get(job.transferId) != job);
        }

        private void removeActive(TransferJob job) {
            active.remove(job.transferId, job);
            sendable.removeFirstOccurrence(job);
            job.awaitingRelease = false;
            job.cancelReleaseTimeout();
        }

        private void schedule() {
            schedule(Math.max(0, nextEligibleNanos - System.nanoTime()));
        }

        private void schedule(long delayNanos) {
            if (!closed && !draining && scheduled == null && !sendable.isEmpty()) {
                try {
                    scheduled = executor.schedule(this::drain, delayNanos, TimeUnit.NANOSECONDS);
                } catch (RuntimeException error) {
                    failQueue(error);
                }
            }
        }

        private void failQueue(Throwable error) {
            closed = true;
            scheduled = null;
            while (!pending.isEmpty()) {
                pending.removeFirst().fail(error);
            }
            var copy = active.values().toArray(TransferJob[]::new);
            active.clear();
            sendable.clear();
            for (var job : copy) {
                job.cancelReleaseTimeout();
                job.fail(error);
            }
        }
    }

    private static final class TransferJob {
        private final UUID playerId;
        private final UUID sessionId;
        private final int resourceIndex;
        private final TransferKind kind;
        private final PreparedTransfer payload;
        private final UUID transferId = UUID.randomUUID();
        private final int fragmentBytes;
        private final int count;
        private final int maxActiveTransfers;
        private final BooleanSupplier acceptingMessages;
        private final Consumer<NetworkPayload<AssetTransferV0.AssetFragment>> fragmentSender;
        private final CompletableFuture<Void> completion = new CompletableFuture<>();
        private volatile boolean cancelled;
        private int index;
        private boolean awaitingRelease;
        private ScheduledFuture<?> releaseTimeout;

        private TransferJob(UUID playerId, PeerLimits peerLimits,
                            BooleanSupplier acceptingMessages,
                            Consumer<NetworkPayload<AssetTransferV0.AssetFragment>> fragmentSender,
                            UUID sessionId, int resourceIndex,
                            TransferKind kind, PreparedTransfer payload) {
            this.playerId = playerId;
            this.sessionId = sessionId;
            this.resourceIndex = resourceIndex;
            this.kind = kind;
            this.acceptingMessages = acceptingMessages;
            this.fragmentSender = fragmentSender;
            maxActiveTransfers = Math.min(ProtocolLimits.MAX_IN_FLIGHT_TRANSFERS,
                    peerLimits.maxInFlightTransfers());
            var encodedLimit = Math.min(ProtocolLimits.MAX_ENCODED_ASSET_BYTES,
                    peerLimits.maxEncodedAssetBytes());
            var decodedLimit = Math.min(ProtocolLimits.MAX_DECODED_ASSET_BYTES,
                    peerLimits.maxDecodedAssetBytes());
            if (payload.contentSize() > encodedLimit || payload.decodedSize() > decodedLimit) {
                throw new IllegalArgumentException("Model transfer exceeds unstable protocol limits");
            }
            fragmentBytes = Math.min(ProtocolLimits.FRAGMENT_DATA_BYTES,
                    Math.min(peerLimits.maxFragmentBytes(), peerLimits.maxMessageBytes() - 4 * 1024));
            if (fragmentBytes <= 0) {
                throw new IllegalArgumentException("Peer message limit cannot carry an asset fragment");
            }
            this.payload = payload.acquire();
            count = Math.max(1, (payload.contentSize() + fragmentBytes - 1) / fragmentBytes);
        }

        private NetworkPayload<AssetTransferV0.AssetFragment> nextFragment() {
            try (var content = payload.acquireContent()) {
                var offset = index * fragmentBytes;
                var length = Math.min(fragmentBytes, content.size() - offset);
                var fragment = AssetTransferV0.AssetFragment.newInstance()
                        .setResourceIndex(resourceIndex)
                        .setKind(kind.toProto())
                        .setDecodedSize(payload.decodedSize())
                        .setTransferSize(payload.contentSize())
                        .setFragmentOffset(offset)
                        .setFragmentIndex(index)
                        .setFragmentCount(count)
                        .setPayloadEncoding(payload.encoding());
                ProtocolUuid.set(fragment.getMutableTransferId(), transferId);
                ProtocolUuid.set(fragment.getMutableRequestId(), sessionId);
                if (index == 0 && payload.manifest() != null) {
                    fragment.setModelAssetManifest(payload.manifest());
                }
                if (payload.encoding()
                        == AssetTransferV0.TransferPayloadEncoding.TRANSFER_PAYLOAD_ENCODING_PROTOBUF) {
                    var bytes = new byte[Math.max(0, length)];
                    if (length > 0) {
                        try (var target = ArrayBuffer.borrow(bytes)) {
                            UniBufferIO.copy(content, offset, target, 0, length);
                        }
                    }
                    fragment.getMutableProtobufFragment().setInternalArray(bytes);
                    return NetworkPayload.protobuf(fragment);
                }
                if (length == 0) {
                    return NetworkPayload.protobuf(fragment);
                }
                return NetworkPayload.withRaw(fragment, content.slice(offset, length).acquire());
            }
        }

        private void advance() {
            index++;
        }

        private boolean complete() {
            return index >= count;
        }

        private void cancel() {
            cancelled = true;
            fail(new java.util.concurrent.CancellationException("Model transfer was cancelled"));
        }

        private void succeed() {
            if (completion.complete(null)) {
                payload.close();
            }
        }

        private void fail(Throwable error) {
            if (completion.completeExceptionally(error)) {
                payload.close();
            }
        }

        private void cancelReleaseTimeout() {
            if (releaseTimeout != null) {
                releaseTimeout.cancel(false);
                releaseTimeout = null;
            }
        }
    }

    private enum ReleaseResult {
        RELEASED,
        UNKNOWN,
        IDENTITY_MISMATCH
    }

    private enum ReleaseAnomaly {
        INVALID_IDENTITY,
        UNKNOWN_OR_DUPLICATE,
        IDENTITY_MISMATCH
    }

    private record ReleaseAnomalyKey(UUID playerId, ReleaseAnomaly anomaly) {
    }
}
