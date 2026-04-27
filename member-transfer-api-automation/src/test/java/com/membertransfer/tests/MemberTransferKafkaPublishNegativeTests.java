package com.membertransfer.tests;

import com.membertransfer.tests.support.MemberTransferKafkaTestSupport;
import io.qameta.allure.*;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Properties;

@Epic("Member transfer")
@Feature("Kafka publish verification")
@Severity(SeverityLevel.NORMAL)
public class MemberTransferKafkaPublishNegativeTests {

    @Test(priority = 46, groups = {"Regression", "MemberTransfer", "Kafka", "KafkaNegative"})
    @Story("Inquiry event is not published to wrong topic")
    @Description("After inquiry is accepted, same bulkId should not appear in an unrelated/wrong topic.")
    public void inquiry_shouldNotPublishToWrongTopic() {
        MemberTransferKafkaTestSupport.ensureInquiryInputsConfigured();
        Properties props = MemberTransferKafkaTestSupport.kafkaConsumerPropsOrSkip();

        String wrongTopic = MemberTransferKafkaTestSupport.wrongTopic();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            MemberTransferKafkaTestSupport.subscribeAndWarmup(consumer, wrongTopic);

            String bulkId = MemberTransferKafkaTestSupport.triggerInquiryAndGetBulkId();
            String wrongTopicMessage = MemberTransferKafkaTestSupport.waitForBulkMessage(consumer, bulkId, 8);

            Assert.assertNull(wrongTopicMessage,
                    "Bulk event should not be published to wrong topic: " + wrongTopic);
        }
    }

    @Test(priority = 47, groups = {"Regression", "MemberTransfer", "Kafka", "KafkaNegative"})
    @Story("Inquiry event is not published to legacy topic")
    @Description("Bulk inquiry should use dedicated bulk topic and not the legacy transfer topic.")
    public void inquiry_shouldNotPublishToLegacyTopic() {
        MemberTransferKafkaTestSupport.ensureInquiryInputsConfigured();
        Properties props = MemberTransferKafkaTestSupport.kafkaConsumerPropsOrSkip();

        String legacyTopic = MemberTransferKafkaTestSupport.legacyTopic();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            MemberTransferKafkaTestSupport.subscribeAndWarmup(consumer, legacyTopic);

            String bulkId = MemberTransferKafkaTestSupport.triggerInquiryAndGetBulkId();
            String legacyMessage = MemberTransferKafkaTestSupport.waitForBulkMessage(consumer, bulkId, 8);

            Assert.assertNull(legacyMessage,
                    "Bulk event should not be published to legacy topic: " + legacyTopic);
        }
    }
}
