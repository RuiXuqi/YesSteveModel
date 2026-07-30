package com.elfmcys.ysm.model.source;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * Client configuration for a future external catalog/asset provider.
 * Credentials are referenced by name and are never embedded in protocol objects or URIs.
 */
public record ExternalModelSourceDefinition(SourceId id,
                                            URI endpoint,
                                            Optional<String> credentialReference,
                                            AccessPolicy accessPolicy,
                                            int priority,
                                            boolean enabled) {
    public ExternalModelSourceDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(endpoint, "endpoint");
        credentialReference = Objects.requireNonNull(credentialReference, "credentialReference");
        Objects.requireNonNull(accessPolicy, "accessPolicy");
        if (!endpoint.isAbsolute() || endpoint.getUserInfo() != null) {
            throw new IllegalArgumentException("External model source endpoint must be absolute and contain no user info");
        }
        credentialReference = credentialReference.map(String::trim).filter(value -> !value.isEmpty());
    }

    public ModelSourceDescriptor descriptor() {
        return new ModelSourceDescriptor(id, SourceKind.EXTERNAL, priority);
    }
}
