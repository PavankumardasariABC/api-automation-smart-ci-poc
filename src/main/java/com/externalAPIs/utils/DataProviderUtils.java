package com.externalAPIs.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.testng.annotations.DataProvider;
import com.externalAPIs.store.ResponseStore;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.GsonBuilder;


/**
 * Centralized TestNG DataProviders with type safety.
 */
public class DataProviderUtils {

    @DataProvider(name = "createClientData")
    public static Iterator<Object[]> getCreateClientData() throws Exception {
        String filePath = "src/test/resources/input/create_client_data.json";
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> data =
                new Gson().fromJson(new FileReader(filePath), listType);

        return data.stream()
                .map(entry -> new Object[]{entry})
                .iterator();
    }

    @DataProvider(name = "createOrganizationData")
    @SuppressWarnings("unchecked") // only for phone map casting
    public static Iterator<Object[]> getCreateOrganizationData() throws Exception {
        String filePath = "src/test/resources/input/create_organization_data.json";
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> data =
                new Gson().fromJson(new FileReader(filePath), listType);

        long timestamp = System.currentTimeMillis();

        List<Map<String, Object>> modifiedData = data.stream().map(entry -> {
            Map<String, Object> org = new HashMap<>(entry);

            org.put("name", "GLofox Organization " + timestamp);
            org.put("number", String.valueOf((int) (Math.random() * 900000) + 100000)); // ✅ 6-digit random


            String email = org.get("email").toString();
            String[] parts = email.split("@");
            org.put("email", parts[0] + "_" + timestamp + "@" + parts[1]);

            String site = org.get("websiteUrl").toString();
            org.put("websiteUrl", site.replace(".com", "-" + timestamp + ".com"));

            if (org.containsKey("phone")) {
                Map<String, Object> phone = (Map<String, Object>) org.get("phone");
                phone.put("number",
                        String.valueOf((long) (Math.random() * 9000000) + 1000000));
                org.put("phone", phone);
            }

            return org;
        }).collect(Collectors.toList());

        return modifiedData.stream()
                .map(d -> new Object[]{d})
                .iterator();
    }

    @DataProvider(name = "createLocationData")
    @SuppressWarnings("unchecked") // only for nested maps
    public static Iterator<Object[]> getCreateLocationData() throws Exception {
        String filePath = "src/test/resources/input/create_location_data.json";
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> data =
                new Gson().fromJson(new FileReader(filePath), listType);

        long timestamp = System.currentTimeMillis();

        List<Map<String, Object>> modifiedData = data.stream().map(entry -> {
            Map<String, Object> location = new HashMap<>(entry);

            // Dynamic unique name & email per run
            String name = location.get("name").toString();
            location.put("name", name + "-" + timestamp);
            location.put("email", name.toLowerCase() + "_" + timestamp + "@qa4life.com");

            // Randomize phone number slightly
            if (location.containsKey("phone")) {
                Map<String, Object> phone = (Map<String, Object>) location.get("phone");
                phone.put("number",
                        String.valueOf((long) (Math.random() * 9000000) + 1000000));
                location.put("phone", phone);
            }

            return location;
        }).collect(Collectors.toList());

        return modifiedData.stream()
                .map(d -> new Object[]{d})
                .iterator();
    }

    @DataProvider(name = "createCompanyData")
    public static Iterator<Object[]> getCreateCompanyData() throws Exception {
        String filePath = "src/test/resources/input/create_company_data.json";
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> data =
                new Gson().fromJson(new FileReader(filePath), listType);

        long timestamp = System.currentTimeMillis();

        List<Map<String, Object>> modifiedData = data.stream().map(entry -> {
            Map<String, Object> company = new HashMap<>(entry);

            // Generate unique company name and short code
            String name = company.get("name").toString();
            company.put("name", name + " " + timestamp);

            String shortCode = company.get("shortCode").toString();
            company.put("shortCode", (shortCode + timestamp).substring(0, Math.min(12, shortCode.length() + 10)));

            return company;
        }).collect(Collectors.toList());

        return modifiedData.stream()
                .map(d -> new Object[]{d})
                .iterator();
    }
    @DataProvider(name = "createMerchantData")
    @SuppressWarnings("unchecked") // for nested address map
    public static Iterator<Object[]> getCreateMerchantData() throws Exception {
        String filePath = "src/test/resources/input/create_merchant_data.json";
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> data =
                new Gson().fromJson(new FileReader(filePath), listType);

        long timestamp = System.currentTimeMillis();
        Random random = new Random();

        List<Map<String, Object>> modifiedData = data.stream().map(entry -> {
            Map<String, Object> merchant = new HashMap<>(entry);

            // Unique name
            String name = merchant.get("name").toString();
            merchant.put("name", name + "-" + timestamp);

            // Random support phone
            String phone = (random.nextInt(900) + 100) + "-" +
                    (random.nextInt(900) + 100) + "-" +
                    (random.nextInt(9000) + 1000);
            merchant.put("supportPhone", phone);

            // Randomize address number slightly
            if (merchant.containsKey("address")) {
                Map<String, Object> address = (Map<String, Object>) merchant.get("address");
                int randomStreetNo = new Random().nextInt(900) + 100;
                address.put("address", randomStreetNo + " Main Street");
                merchant.put("address", address);
            }

            return merchant;
        }).collect(Collectors.toList());

        return modifiedData.stream()
                .map(d -> new Object[]{d})
                .iterator();
    }
    @DataProvider(name = "createClientProfileData")
    @SuppressWarnings("unchecked")
    public static Iterator<Object[]> getCreateClientProfileData() throws Exception {
        String filePath = "src/test/resources/input/create_client_profile_data.json";
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> data =
                new Gson().fromJson(new FileReader(filePath), listType);

        long timestamp = System.currentTimeMillis();

        List<Map<String, Object>> modifiedData = data.stream().map(entry -> {
            Map<String, Object> client = new HashMap<>(entry);

            // Dynamic name, email, and phone number
            client.put("name", "Client-" + timestamp);
            client.put("email", "qa_" + timestamp + "@testmail.com");
            client.put("phone", "901" + (long) (Math.random() * 10000000L));

            return client;
        }).collect(Collectors.toList());

        return modifiedData.stream()
                .map(d -> new Object[]{d})
                .iterator();
    }
    @DataProvider(name = "createProcessorData")
    @SuppressWarnings("unchecked")
    public static Iterator<Object[]> getCreateProcessorData() throws Exception {
        String filePath = "src/test/resources/input/create_processor_data.json";
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> data =
                new Gson().fromJson(new FileReader(filePath), listType);

        List<Map<String, Object>> modifiedData = data.stream().map(entry -> {
            Map<String, Object> processor = new HashMap<>(entry);

            // Generate a unique suffix for folder paths and name
            String uniqueSuffix = UUID.randomUUID().toString().substring(0, 6);
            processor.put("name", "Regions Bank QA-" + uniqueSuffix);

            // ✅ Normalize all number fields to Long or Integer
            normalizeNumber(processor, "tinLength", Integer.class);
            normalizeNumber(processor, "maxBatchPerFile", Long.class);
            normalizeNumber(processor, "maxTransactionPerBatch", Long.class);
            normalizeNumber(processor, "maxTransactionPerFile", Long.class);
            normalizeNumber(processor, "maxBatchingTxTimeout", Long.class);

            // ✅ Dynamic S3 folder paths
            Map<String, Object> storageDetails = (Map<String, Object>) processor.get("storageDetails");
            if (storageDetails != null) {
                Map<String, Object> send = (Map<String, Object>) storageDetails.get("send");
                Map<String, Object> receive = (Map<String, Object>) storageDetails.get("receive");

                if (send != null) send.put("folderPath", "transactions/requests_" + uniqueSuffix);
                if (receive != null) receive.put("folderPath", "transactions/responses_" + uniqueSuffix);

                processor.put("storageDetails", storageDetails);
            }

            // ✅ Dynamic SFTP paths for communication endpoints
            Map<String, Object> commDetails = (Map<String, Object>) processor.get("communicationDetails");
            if (commDetails != null) {
                Map<String, Object> sendEP = (Map<String, Object>) commDetails.get("sendEndpoint");
                Map<String, Object> receiveEP = (Map<String, Object>) commDetails.get("receiveEndpoint");

                if (sendEP != null) sendEP.put("folderPath", "/srv/qa/regions/requests/demo_" + uniqueSuffix);
                if (receiveEP != null) receiveEP.put("folderPath", "/srv/qa/regions/responses/demo_" + uniqueSuffix);

                processor.put("communicationDetails", commDetails);
            }

            return processor;
        }).collect(Collectors.toList());

        return modifiedData.stream().map(d -> new Object[]{d}).iterator();
    }

    /**
     * Helper method to normalize number fields safely.
     */
    private static void normalizeNumber(Map<String, Object> map, String key, Class<?> targetType) {
        if (!map.containsKey(key)) return;
        Object val = map.get(key);

        if (val instanceof Double) {
            if (targetType.equals(Long.class)) {
                map.put(key, ((Double) val).longValue());
            } else if (targetType.equals(Integer.class)) {
                map.put(key, ((Double) val).intValue());
            }
        } else if (val instanceof String) {
            try {
                if (targetType.equals(Long.class)) {
                    map.put(key, Long.parseLong((String) val));
                } else if (targetType.equals(Integer.class)) {
                    map.put(key, Integer.parseInt((String) val));
                }
            } catch (NumberFormatException e) {
                // fallback default values if parsing fails
                map.put(key, targetType.equals(Long.class) ? 1L : 1);
            }
        }
    }
    @DataProvider(name = "updateMerchantData")
    @SuppressWarnings("unchecked")
    public static Iterator<Object[]> getUpdateMerchantData() {

        String fullMerchantResponse = ResponseStore.get("MerchantFullResponse");
        if (fullMerchantResponse == null || fullMerchantResponse.isBlank()) {
            System.err.println("❌ No MerchantFullResponse found — please run CreateMerchantTests first!");
            throw new org.testng.SkipException("Skipping UpdateMerchantTests: Merchant data missing.");
        }

        Map<String, Object> merchant = new Gson().fromJson(fullMerchantResponse, Map.class);
        final String processorId = ResponseStore.get("ProcessorId");
        final String processorName = ResponseStore.get("ProcessorName");
        final String paymentType = ResponseStore.get("ProcessorPaymentType");

        // Randomize support phone
        String newPhone = "39" + (new Random().nextInt(9000000) + 1000000);
        merchant.put("supportPhone", newPhone);

        if (merchant.containsKey("merchantSupportPhone")) {
            Map<String, Object> phone = (Map<String, Object>) merchant.get("merchantSupportPhone");
            phone.put("number", newPhone);
            phone.put("countryCode", "US");
        }

        if (processorId != null) {
            Map<String, Object> processorObj = new HashMap<>();
            processorObj.put("processorId", processorId);
            processorObj.put("providerSpecificMId", "364801462");
            processorObj.put("isPrimary", true);
            processorObj.put("name", processorName);
            processorObj.put("status", "READY");
            processorObj.put("paymentType", paymentType);
            merchant.put("processors", Collections.singletonList(processorObj));
        }

        System.out.println("🧩 Update Merchant Data Prepared:\n" +
                new GsonBuilder().setPrettyPrinting().create().toJson(merchant));

        return Collections.singletonList(new Object[]{merchant}).iterator();
    }



    @DataProvider(name = "createTerminalData")
    public static Iterator<Object[]> getCreateTerminalData() throws Exception {
        String filePath = "src/test/resources/input/create_terminal_data.json";
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> data = new Gson().fromJson(new FileReader(filePath), listType);

        long timestamp = System.currentTimeMillis();

        List<Map<String, Object>> modifiedData = data.stream().map(entry -> {
            Map<String, Object> terminal = new HashMap<>(entry);

            // Add unique terminal number and name
            terminal.put("terminalNumber", "qa_dynamic_terminal_" + timestamp);
            terminal.put("name", terminal.get("name") + "_" + timestamp);

            // The merchantProcessorId will be dynamically injected during test execution
            return terminal;
        }).collect(Collectors.toList());

        return modifiedData.stream().map(d -> new Object[]{d}).iterator();
    }

    @DataProvider(name = "createPaymentSessionConsumerData")
    public static Iterator<Object[]> getCreatePaymentSessionConsumerData() throws Exception {
        String filePath = "src/test/resources/input/create_payment_session_consumer_data.json";
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> data = new Gson().fromJson(new FileReader(filePath), listType);

        long timestamp = System.currentTimeMillis();

        List<Map<String, Object>> modifiedData = data.stream().map(entry -> {
            Map<String, Object> consumer = new HashMap<>(entry);

            // Unique name for each test execution
            consumer.put("name", "consumer_glofox_" + timestamp);

            return consumer;
        }).collect(Collectors.toList());

        return modifiedData.stream().map(d -> new Object[]{d}).iterator();
    }
    @DataProvider(name = "createPaymentTokenSessionData")
    public static Iterator<Object[]> getCreatePaymentTokenSessionData() throws Exception {
        String filePath = "src/test/resources/input/create_payment_token_session_data.json";
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> data = new Gson().fromJson(new FileReader(filePath), listType);

        // Optionally enrich dynamic fields if needed
        List<Map<String, Object>> updated = data.stream().map(entry -> {
            Map<String, Object> session = new HashMap<>(entry);
            session.put("metadata", entry.get("metadata")); // preserve metadata
            return session;
        }).collect(Collectors.toList());

        return updated.stream().map(d -> new Object[]{d}).iterator();
    }
    @DataProvider(name = "createBankPaymentTokenData")
    public static Iterator<Object[]> getCreateBankPaymentTokenData() throws Exception {
        String filePath = "src/test/resources/input/create_bank_payment_token_data.json";
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> data = new Gson().fromJson(new FileReader(filePath), listType);

        List<Map<String, Object>> updatedData = data.stream().map(entry -> {
            Map<String, Object> bank = new HashMap<>(entry);

            // Random 12-digit US bank account number
            long accountNumber = (long) (100000000000L + Math.random() * 900000000000L);
            bank.put("bankAccountNumber", String.valueOf(accountNumber));

            // Random client name
            String clientName = "Client-" + UUID.randomUUID().toString().substring(0, 6);
            bank.put("accountHolderName", clientName);

            return bank;
        }).collect(Collectors.toList());

        return updatedData.stream().map(d -> new Object[]{d}).iterator();
    }
    @DataProvider(name = "createClientBankAccountData")
    public static Iterator<Object[]> getCreateClientBankAccountData() throws Exception {
        String filePath = "src/test/resources/input/create_client_bank_account_data.json";
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> data = new Gson().fromJson(new FileReader(filePath), listType);

        // Random dynamic values
        List<Map<String, Object>> updatedData = data.stream().map(entry -> {
            Map<String, Object> bankAccount = new HashMap<>(entry);

            // Random phone number and alias
            String phone = "901" + (long) (Math.random() * 10000000L);
            bankAccount.put("phone", phone);
            bankAccount.put("alias", "Alias-" + UUID.randomUUID().toString().substring(0, 6));

            return bankAccount;
        }).collect(Collectors.toList());

        return updatedData.stream().map(d -> new Object[]{d}).iterator();
    }
    @DataProvider(name = "createAchPaymentChargeData")
    @SuppressWarnings("unchecked")
    public static Iterator<Object[]> getCreateAchPaymentChargeData() throws Exception {
        String filePath = "src/test/resources/input/create_ach_payment_charge_data.json";
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> data = new Gson().fromJson(new FileReader(filePath), listType);

        long timestamp = System.currentTimeMillis();

        List<Map<String, Object>> modifiedData = data.stream().map(entry -> {
            Map<String, Object> record = new HashMap<>(entry);

            // ✅ 1️⃣ Safely handle tokenId (cast + fallback)
            String tokenFromStore = (String) ResponseStore.get("tokenSessionId");
            String tokenId = (tokenFromStore != null && !tokenFromStore.trim().isEmpty())
                    ? tokenFromStore
                    : String.valueOf(record.getOrDefault("tokenId", "11f092f8-1139-f15c-91b9-ef36cde23978"));
            record.put("tokenId", tokenId);

            // ✅ 2️⃣ Add unique referenceId and relatedReferenceId
            record.put("referenceId", UUID.randomUUID().toString());
            if (!record.containsKey("relatedReferenceId") || record.get("relatedReferenceId") == null) {
                record.put("relatedReferenceId", UUID.randomUUID().toString());
            }

            // ✅ 3️⃣ Add dynamic descriptor
            String descriptor = String.valueOf(record.getOrDefault("descriptor", "ACH Charge Test"));
            record.put("descriptor", descriptor + " - " + timestamp);

            // ✅ 4️⃣ Safely handle terminalId (cast + fallback)
            String terminalFromStore = (String) ResponseStore.get("TerminalId");
            String terminalId = (terminalFromStore != null && !terminalFromStore.trim().isEmpty())
                    ? terminalFromStore
                    : String.valueOf(record.getOrDefault("terminalId", "11f0743b-fcfb-85ed-9765-6749eb40ee5d"));
            record.put("terminalId", terminalId);

            // ✅ 5️⃣ Ensure required fields
            record.putIfAbsent("amount", "2300");
            record.putIfAbsent("effectiveDate", "2026-09-16");

            return record;
        }).collect(Collectors.toList());

        return modifiedData.stream().map(d -> new Object[]{d}).iterator();
    }
    @DataProvider(name = "createRefundPaymentData")
    @SuppressWarnings("unchecked")
    public static Iterator<Object[]> getCreateRefundPaymentData() throws Exception {
        String filePath = "src/test/resources/input/refundPaymentData.json";

        // Parse wrapper object { "refundData": [ ... ] }
        Map<String, List<Map<String, Object>>> wrapper = new Gson().fromJson(
                new FileReader(filePath),
                new TypeToken<Map<String, List<Map<String, Object>>>>() {}.getType()
        );
        List<Map<String, Object>> data = wrapper.get("refundData");
        if (data == null || data.isEmpty()) {
            throw new IllegalStateException("❌ 'refundData' array missing or empty in refundPaymentData.json");
        }

        long timestamp = System.currentTimeMillis();

        List<Map<String, Object>> modifiedData = data.stream().map(entry -> {
            Map<String, Object> record = new HashMap<>(entry);

            // 1️⃣ Token from ResponseStore or fallback
            String tokenFromStore = (String) ResponseStore.get("AchTransactionTokenId");
            String tokenId = (tokenFromStore != null && !tokenFromStore.trim().isEmpty())
                    ? tokenFromStore
                    : String.valueOf(record.getOrDefault("tokenId", "11f092f8-1139-f15c-91b9-ef36cde23978"));
            record.put("tokenId", tokenId);

            // 2️⃣ ReferenceId from Charge API or fallback
            String refIdFromStore = (String) ResponseStore.get("AchReferenceId");
            if (refIdFromStore != null && !refIdFromStore.trim().isEmpty()) {
                record.put("referenceId", refIdFromStore);
            } else {
                record.put("referenceId", UUID.randomUUID().toString());
            }

            // 3️⃣ TerminalId from store or fallback
            String terminalFromStore = (String) ResponseStore.get("TerminalId");
            String terminalId = (terminalFromStore != null && !terminalFromStore.trim().isEmpty())
                    ? terminalFromStore
                    : String.valueOf(record.getOrDefault("terminalId", "11f092f7-a168-74cf-a13a-2bf2273e4f27"));
            record.put("terminalId", terminalId);

            // 4️⃣ Descriptor + timestamp
            String descriptor = String.valueOf(record.getOrDefault("descriptor", "Refund Payment Test"));
            record.put("descriptor", descriptor + " - " + timestamp);

            // 5️⃣ Ensure required fields
            record.putIfAbsent("amount", "2300");
            record.putIfAbsent("effectiveDate", "2025-09-16");
            record.putIfAbsent("metadata", new HashMap<>());

            System.out.println("🧾 Prepared Refund Data: " + record);
            return record;
        }).collect(Collectors.toList());

        return modifiedData.stream().map(d -> new Object[]{d}).iterator();
    }


}
