package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.domain.ModelPackDescriptor;
import com.elfmcys.ysm.model.domain.ModelScanError;

import java.util.Objects;

public sealed interface ModelPackSourceState permits ModelPackSourceState.Ready,
        ModelPackSourceState.Rejected {
    PackObservation observation();

    record Ready(PackObservation observation,
                 ModelPackDescriptor descriptor) implements ModelPackSourceState {
        public Ready {
            Objects.requireNonNull(observation, "observation");
            Objects.requireNonNull(descriptor, "descriptor");
        }
    }

    record Rejected(PackObservation observation,
                    ModelScanError error) implements ModelPackSourceState {
        public Rejected {
            Objects.requireNonNull(observation, "observation");
            Objects.requireNonNull(error, "error");
        }
    }
}
