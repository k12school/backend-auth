package com.k12.tenant.infrastructure.persistence;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.models.Subdomain;
import com.k12.tenant.domain.models.TenantName;
import com.k12.tenant.domain.models.TenantStatus;
import com.k12.tenant.domain.models.events.TenantEvents;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.UUID;

/**
 * Thread-safe Kryo serializer for event sourcing.
 * Uses thread-local Kryo instances for optimal performance.
 */
public final class KryoEventSerializer {

    private KryoEventSerializer() {}

    /**
     * Serializes an event to a byte array using Kryo.
     *
     * @param event the event to serialize
     * @return serialized byte array
     */
    public static byte[] serialize(TenantEvents event) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Output output = new Output(baos)) {
            KryoHolder.kryo().writeClassAndObject(output, event);
            output.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize event: " + event, e);
        }
    }

    /**
     * Deserializes an event from a byte array using Kryo.
     *
     * @param data the byte array to deserialize
     * @return deserialized event
     */
    public static TenantEvents deserialize(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Cannot deserialize null or empty data");
        }
        try (Input input = new Input(data)) {
            Object obj = KryoHolder.kryo().readClassAndObject(input);
            if (!(obj instanceof TenantEvents)) {
                throw new SerializationException("Deserialized object is not a TenantEvents: " + obj.getClass());
            }
            return (TenantEvents) obj;
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize event", e);
        }
    }

    /**
     * Thread-local holder for configured Kryo instances.
     * Each thread gets its own Kryo instance for thread-safety.
     */
    private static class KryoHolder {
        private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL = ThreadLocal.withInitial(KryoHolder::createKryo);

        static Kryo kryo() {
            return KRYO_THREAD_LOCAL.get();
        }

        private static Kryo createKryo() {
            Kryo kryo = new Kryo();

            // Register common types for efficiency
            kryo.register(UUID.class, new KryoUuidSerializer());
            kryo.register(String.class);
            kryo.register(long.class);
            kryo.register(int.class);
            kryo.register(boolean.class);

            // Register Java time types (use Java serializer for compatibility)
            kryo.register(Instant.class, new JavaSerializer());

            // Register value objects
            kryo.register(TenantId.class);
            kryo.register(TenantName.class);
            kryo.register(Subdomain.class);
            kryo.register(TenantStatus.class);

            // Register event classes explicitly for version stability
            registerEventClasses(kryo);

            // Set references to false for immutable value objects
            kryo.setReferences(false);

            // Require class registration for safety
            kryo.setRegistrationRequired(true);

            return kryo;
        }

        private static void registerEventClasses(Kryo kryo) {
            // Register all TenantEvents subclasses
            // Using class names for version stability
            int classId = 100;

            kryo.register(com.k12.tenant.domain.models.events.TenantEvents.TenantCreated.class, classId++);
            kryo.register(com.k12.tenant.domain.models.events.TenantEvents.TenantActivated.class, classId++);
            kryo.register(com.k12.tenant.domain.models.events.TenantEvents.TenantSuspended.class, classId++);
            kryo.register(com.k12.tenant.domain.models.events.TenantEvents.TenantDeactivated.class, classId++);
            kryo.register(com.k12.tenant.domain.models.events.TenantEvents.TenantDeleted.class, classId++);
            kryo.register(com.k12.tenant.domain.models.events.TenantEvents.TenantNameUpdated.class, classId++);
            kryo.register(com.k12.tenant.domain.models.events.TenantEvents.TenantSubdomainUpdated.class, classId++);
        }
    }

    /**
     * Custom UUID serializer for Kryo.
     * Ensures UUIDs are serialized efficiently.
     */
    private static class KryoUuidSerializer extends com.esotericsoftware.kryo.Serializer<UUID> {
        @Override
        public void write(Kryo kryo, Output output, UUID uuid) {
            output.writeLong(uuid.getMostSignificantBits());
            output.writeLong(uuid.getLeastSignificantBits());
        }

        @Override
        public UUID read(Kryo kryo, Input input, Class<? extends UUID> type) {
            long mostSigBits = input.readLong();
            long leastSigBits = input.readLong();
            return new UUID(mostSigBits, leastSigBits);
        }
    }

    /**
     * Exception thrown when serialization or deserialization fails.
     */
    public static class SerializationException extends RuntimeException {
        public SerializationException(String message) {
            super(message);
        }

        public SerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
