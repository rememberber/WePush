package com.fangxuele.wepush.next.core.api;

import java.math.BigDecimal;

public sealed interface RecipientValue permits RecipientValue.TextValue,
        RecipientValue.NumberValue, RecipientValue.BooleanValue,
        RecipientValue.NullValue, RecipientValue.BinaryRefValue {

    record TextValue(String value) implements RecipientValue {
        public TextValue {
            if (value == null) {
                throw new IllegalArgumentException("value must not be null");
            }
        }
    }

    record NumberValue(BigDecimal value) implements RecipientValue {
        public NumberValue {
            if (value == null) {
                throw new IllegalArgumentException("value must not be null");
            }
        }
    }

    record BooleanValue(boolean value) implements RecipientValue {
    }

    enum NullValue implements RecipientValue {
        INSTANCE
    }

    record BinaryRefValue(ArtifactRef artifact) implements RecipientValue {
        public BinaryRefValue {
            if (artifact == null) {
                throw new IllegalArgumentException("artifact must not be null");
            }
        }
    }
}
