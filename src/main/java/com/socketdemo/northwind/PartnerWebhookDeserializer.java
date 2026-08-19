package com.socketdemo.northwind;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Deserializes inbound webhook callbacks from shipping partners.
 *
 * jackson-databind polymorphic-deserialization CVEs (multiple, including
 * CVE-2019-12384 and related gadget-chain issues in the 2.9.x line):
 * enableDefaultTyping() makes the mapper trust a type name carried inside
 * the payload itself when the target type is Object, letting the sender
 * pick the concrete class to instantiate.
 */
public class PartnerWebhookDeserializer {

    public Object deserialize(String payload) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping();
        return mapper.readValue(payload, Object.class);
    }
}
