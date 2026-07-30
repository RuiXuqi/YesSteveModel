package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.domain.ModelScanError;
import com.elfmcys.ysm.model.storage.ModelFileHandle;

import java.util.Objects;

public sealed interface ModelSourceState permits ModelSourceState.Ready,
        ModelSourceState.Rejected {
    SourceObservation observation();

    record Ready(SourceObservation observation, ModelHash modelHash,
                 ModelFileHandle handle) implements ModelSourceState {
        public Ready {
            Objects.requireNonNull(observation, "observation");
            Objects.requireNonNull(modelHash, "modelHash");
            Objects.requireNonNull(handle, "handle");
        }
    }

    record Rejected(SourceObservation observation,
                    ModelScanError error) implements ModelSourceState {
        public Rejected {
            Objects.requireNonNull(observation, "observation");
            Objects.requireNonNull(error, "error");
        }
    }
}
