package com.socketdemo.northwind;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

/**
 * Northwind Order Service - a small demo backend used to show how Socket's
 * reachability analysis tells apart "a vulnerable library is on the
 * classpath" from "this application actually calls the vulnerable code".
 *
 * Every service below calls a real, publicly documented vulnerable API from
 * a known-CVE library version, using benign, hardcoded example data. Nothing
 * here sends network traffic, spawns a process, or touches the filesystem -
 * it is safe to build, run, and scan. See README.md for the CVE list and the
 * matching Socket reachability results.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        OrderCommentLogger commentLogger = new OrderCommentLogger();
        commentLogger.logComment("ORD-10231", "Please deliver after 5pm, thanks!");

        NotificationTemplateService notifications = new NotificationTemplateService();
        String rendered = notifications.renderTemplate("Hi ${sys:user.name}, your order shipped.");
        System.out.println(rendered);

        FeatureFlagConfigLoader flags = new FeatureFlagConfigLoader();
        Object flagValues = flags.loadConfig("expressCheckout: true\nbetaSearch: false\n");
        System.out.println(flagValues);

        PartnerWebhookDeserializer webhooks = new PartnerWebhookDeserializer();
        Object webhookPayload = webhooks.deserialize("[\"java.util.HashMap\",{\"carrier\":\"ups\",\"status\":\"delivered\"}]");
        System.out.println(webhookPayload);

        LegacyOrderXmlImporter xmlImporter = new LegacyOrderXmlImporter();
        Object order = xmlImporter.importOrders("<order><id>10231</id></order>");
        System.out.println(order);

        SessionCacheDeserializer sessions = new SessionCacheDeserializer();
        Object session = sessions.restoreSession(serializeSampleCart());
        System.out.println(session);
    }

    private static byte[] serializeSampleCart() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject("cart:10231");
        }
        return bos.toByteArray();
    }
}
