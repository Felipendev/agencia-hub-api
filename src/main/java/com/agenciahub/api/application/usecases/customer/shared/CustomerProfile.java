package com.agenciahub.api.application.usecases.customer.shared;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Merge partial profile fields. Explicit null clears a value; absent keys remain unchanged. */
public final class CustomerProfile {
    private CustomerProfile() {}

    public static ObjectNode merge(ObjectNode current, ObjectNode patch) {
        ObjectNode result = current == null ? JsonNodeFactory.instance.objectNode() : current.deepCopy();
        patch.fields().forEachRemaining(field -> {
            if (field.getValue() instanceof ObjectNode nested) {
                ObjectNode previous = result.get(field.getKey()) instanceof ObjectNode object ? object : null;
                result.set(field.getKey(), merge(previous, nested));
            } else {
                result.set(field.getKey(), field.getValue().deepCopy());
            }
        });
        return result;
    }
}
