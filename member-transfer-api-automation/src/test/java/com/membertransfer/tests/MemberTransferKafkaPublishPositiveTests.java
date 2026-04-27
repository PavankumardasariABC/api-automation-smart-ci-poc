package com.membertransfer.tests;

import com.membertransfer.config.ConfigManager;
import com.membertransfer.support.MemberTransferSupport;
import com.membertransfer.tests.support.MemberTransferKafkaTestSupport;
import io.qameta.allure.*;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Properties;

@Epic("Member transfer")
@Feature("Kafka publish verification")
@Severity(SeverityLevel.CRITICAL)
public class MemberTransferKafkaPublishPositiveTests {

    @Test(priority = 45, groups = {"Regression", "MemberTransfer", "Kafka", "KafkaPositive"})
    @Story("Bulk inquiry event is published to dedicated topic")
    @Description("After inquiry POST returns bulkId, the event must be published to bulk-account-transfer topic with required payload hints.")
    public void inquiry_shouldPublishKafkaMessage_toBulkTopic() {
        MemberTransferKafkaTestSupport.ensureInquiryInputsConfigured();
        Properties props = MemberTransferKafkaTestSupport.kafkaConsumerPropsOrSkip();
        String topic = MemberTransferKafkaTestSupport.kafkaTopic();
        String expectedFrom = MemberTransferSupport.fromBillingAccountId();
        String expectedTo = MemberTransferSupport.toLocationId();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            MemberTransferKafkaTestSupport.subscribeAndWarmup(consumer, topic);

            String bulkId = MemberTransferKafkaTestSupport.triggerInquiryAndGetBulkId();
            Allure.step("Inquiry accepted. bulkId=" + bulkId);

            int waitSeconds = MemberTransferKafkaTestSupport.intOrDefault(
                    ConfigManager.getOptional("member.transfer.kafka.max.wait.seconds"), 45);
            String kafkaMessage = MemberTransferKafkaTestSupport.waitForBulkMessage(consumer, bulkId, waitSeconds);

            Assert.assertNotNull(kafkaMessage, "Kafka message containing bulkId was not found in topic: " + topic);
            Allure.addAttachment("Kafka message", "application/json", kafkaMessage);

            Assert.assertTrue(kafkaMessage.contains(bulkId), "Payload should contain bulkId/inquiryGroupId");
            Assert.assertTrue(kafkaMessage.contains(expectedFrom), "Payload should contain source/from account id");
            Assert.assertTrue(kafkaMessage.contains(expectedTo), "Payload should contain destination/to location id");
            Assert.assertTrue(
                    kafkaMessage.contains("memberAgreement")
                            || kafkaMessage.contains("member_agreement")
                            || kafkaMessage.contains("items"),
                    "Payload should contain member agreement list or inquiry items"
            );
        }
    }
}
