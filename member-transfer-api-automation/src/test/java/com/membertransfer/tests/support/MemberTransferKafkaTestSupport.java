package com.membertransfer.tests.support;

import com.membertransfer.auth.MemberTransferAuth;
import com.membertransfer.config.ApiClient;
import com.membertransfer.config.ConfigManager;
import com.membertransfer.store.ResponseStore;
import com.membertransfer.support.MemberTransferSupport;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.testng.Assert;
import org.testng.SkipException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public final class MemberTransferKafkaTestSupport {

    private MemberTransferKafkaTestSupport() {
    }

    public static void ensureInquiryInputsConfigured() {
        if (containsReplace(MemberTransferSupport::fromBillingAccountId)
                || containsReplace(MemberTransferSupport::toLocationId)
                || containsReplace(MemberTransferSupport::tenantId)
                || containsReplace(ConfigManager::getMemberTransferAuthCredentials)) {
            throw new SkipException("Configure member.transfer inquiry credentials + ids before Kafka verification");
        }
    }

    public static String kafkaTopic() {
        String configured = ConfigManager.getOptional("member.transfer.kafka.topic");
        if (configured == null || configured.isBlank()) {
            return "bulk-account-transfer";
        }
        return configured;
    }

    public static String legacyTopic() {
        String configured = ConfigManager.getOptional("member.transfer.kafka.legacy.topic");
        if (configured == null || configured.isBlank()) {
            return "account-transfer";
        }
        return configured;
    }

    public static String wrongTopic() {
        String configured = ConfigManager.getOptional("member.transfer.kafka.negative.wrong.topic");
        if (configured == null || configured.isBlank()) {
            return "bulk-account-transfer-invalid";
        }
        return configured;
    }

    public static Properties kafkaConsumerPropsOrSkip() {
        String bootstrap = ConfigManager.getOptional("member.transfer.kafka.bootstrap.servers");
        if (bootstrap == null || bootstrap.isBlank() || bootstrap.contains("REPLACE")) {
            throw new SkipException("Set member.transfer.kafka.bootstrap.servers in qa.local.properties to enable Kafka verification tests");
        }

        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "member-transfer-kafka-verify-" + UUID.randomUUID());
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                ConfigManager.getOptional("member.transfer.kafka.auto.offset.reset") == null
                        ? "latest" : ConfigManager.getOptional("member.transfer.kafka.auto.offset.reset"));
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        String protocol = ConfigManager.getOptional("member.transfer.kafka.security.protocol");
        String mechanism = ConfigManager.getOptional("member.transfer.kafka.sasl.mechanism");
        String username = ConfigManager.getOptional("member.transfer.kafka.sasl.username");
        String password = ConfigManager.getOptional("member.transfer.kafka.sasl.password");
        String jaas = ConfigManager.getOptional("member.transfer.kafka.sasl.jaas.config");

        if (protocol != null && !protocol.isBlank()) {
            p.put("security.protocol", protocol);
        }
        if (mechanism != null && !mechanism.isBlank()) {
            p.put("sasl.mechanism", mechanism);
        }
        if (jaas != null && !jaas.isBlank()) {
            p.put("sasl.jaas.config", jaas);
        } else if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            p.put("sasl.jaas.config",
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                            + username + "\" password=\"" + password + "\";");
        }

        return p;
    }

    public static String triggerInquiryAndGetBulkId() {
        String token = ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
        if (token == null || token.isBlank()) {
            token = MemberTransferAuth.accessToken();
        }

        String responseBody = MemberTransferSupport.defaultInquiryBodyJson();
        Response response = ApiClient.post(MemberTransferSupport.inquiryUrl(), MemberTransferSupport.bearerJsonHeaders(token), responseBody);
        if (response.getStatusCode() == 401) {
            MemberTransferSupport.fetchAndStoreToken();
            token = ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
            response = ApiClient.post(MemberTransferSupport.inquiryUrl(), MemberTransferSupport.bearerJsonHeaders(token), responseBody);
        }

        Assert.assertEquals(response.getStatusCode(), 200,
                "Inquiry should be accepted before Kafka verification: " + response.getStatusCode() + " -> " + response.asString());

        String bulkId = response.jsonPath().getString("bulkId");
        Assert.assertNotNull(bulkId, "bulkId must be present in inquiry response");
        ResponseStore.put(MemberTransferSupport.BULK_ID_KEY, bulkId);
        return bulkId;
    }

    public static String waitForBulkMessage(KafkaConsumer<String, String> consumer, String bulkId, int waitSeconds) {
        int pollMs = intOrDefault(ConfigManager.getOptional("member.transfer.kafka.poll.interval.ms"), 1000);
        Instant deadline = Instant.now().plusSeconds(waitSeconds);
        int seen = 0;

        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(pollMs));
            for (ConsumerRecord<String, String> rec : records) {
                seen++;
                String key = rec.key() == null ? "" : rec.key();
                String value = rec.value() == null ? "" : rec.value();
                if (key.contains(bulkId) || value.contains(bulkId)) {
                    Allure.step("Found Kafka event in topic " + rec.topic() + " partition " + rec.partition() + " offset " + rec.offset());
                    return value;
                }
            }
        }
        Allure.step("No matching event found for bulkId=" + bulkId + " (records scanned=" + seen + ")");
        return null;
    }

    public static void subscribeAndWarmup(KafkaConsumer<String, String> consumer, String topic) {
        consumer.subscribe(List.of(topic));
        consumer.poll(Duration.ofSeconds(1));
    }

    public static int intOrDefault(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean containsReplace(java.util.function.Supplier<String> supplier) {
        try {
            String value = supplier.get();
            return value == null || value.isBlank() || value.contains("REPLACE");
        } catch (Exception e) {
            return true;
        }
    }
}
