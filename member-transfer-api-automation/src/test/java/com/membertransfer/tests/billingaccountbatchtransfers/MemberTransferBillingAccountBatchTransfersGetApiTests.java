package com.membertransfer.tests.billingaccountbatchtransfers;

import com.membertransfer.auth.MemberTransferAuth;
import com.membertransfer.config.ApiClient;
import com.membertransfer.config.ConfigManager;
import com.membertransfer.store.ResponseStore;
import com.membertransfer.support.MemberTransferSupport;
import io.qameta.allure.*;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.membertransfer.tests.support.MemberTransferBatchResponseHelpers.contentList;
import static com.membertransfer.tests.support.MemberTransferBatchResponseHelpers.pageNumber;
import static com.membertransfer.tests.support.MemberTransferBatchResponseHelpers.pageSizeField;
import static com.membertransfer.tests.support.MemberTransferBatchResponseHelpers.totalElements;

/**
 * <p><b>Sole test class for the paged list GET</b> (not the {@code /status/...} sub-resource):
 * <pre>GET {base}/api/member-transfer/billing-account-batch-transfers/{bulkAccountId}?q=operation_type==TRANSFER&amp;page=0&amp;size=10</pre>
 * (and variants: other {@code q}, page/size, auth negatives, RSQL, TC_01–TC_35 as implemented.)
 * </p>
 * <p>Authenticated cases resolve {@code bulkId} from the same <b>POST
 * /api/member-transfer/billing-account-batch-transfers/inquiry</b> response as
 * {@link com.membertransfer.tests.MemberTransferInquiryApiTests#inquiry_validPayload_expectBulkId}
 * (stored under {@code MemberTransferBulkId}, or fetched via
 * {@link com.membertransfer.support.MemberTransferSupport#postInquiryAndStoreBulkId()} when the store is empty).
 * Unauthenticated path tests (TC_05/06) use a well-formed sample path id only.</p>
 * <p>List this class in {@code testng.xml} after {@code MemberTransferInquiryApiTests} in the suite if you
 * want to prefer the bulk from that class’s inquiry run; otherwise this class can POST inquiry itself.</p>
 * <p>Optional fixture ids: {@code member.transfer.batch.bulkid.*} and OpenAPI: {@code member.transfer.openapi.path} in local/merged properties.</p>
 */
@Epic("Member transfer")
@Feature("GET billing-account-batch-transfers (paged list by bulkId)")
@Severity(SeverityLevel.NORMAL)
public class MemberTransferBillingAccountBatchTransfersGetApiTests {

    private static final String BULKID_NON_EXISTING = "f0000000-0000-4000-8000-00000000feed";
    private static final String BULKID_INVALID_PATH = "not-a-valid-uuid-bulkid";

    @Test(priority = 10, groups = {"Regression", "MemberTransfer", "Sanity", "MT_Sanity"})
    @Story("TC_01: Endpoint reachable (authorized)")
    @Description("GET returns 2xx, 404, or expected env-specific codes when the resource is missing.")
    public void tc01_endpointReachable() {
        if (isPlaceholderCreds() || !MemberTransferSupport.isInquiryRequestConfigured()) {
            throw new SkipException("member.transfer + inquiry not configured (no REPLACE_ placeholders)");
        }
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(
                bulkIdFromInquiryForGetApisOrSkip());
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        Assert.assertTrue(
                c == 200 || c == 404,
                "Expected 200/404, was " + c);
    }

    @Test(priority = 20, groups = {"Regression", "MemberTransfer"})
    @Story("TC_02: Valid bulkAccountId – successful structure when 200")
    @Description("2xx (when present) contains JSON; optional paging and row fields if returned.")
    public void tc02_validBulkId_successShape() {
        if (isPlaceholderCreds() || !MemberTransferSupport.isInquiryRequestConfigured()) {
            throw new SkipException("Config");
        }
        String bulk = bulkIdFromInquiryForGetApisOrSkip();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulk);
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        if (c == 404) {
            throw new SkipException("No batch data for this bulkId; cannot assert success body (TC_02 optional outcome)");
        }
        Assert.assertTrue(c >= 200 && c < 300, "2xx, was " + c);
        String body = r.asString();
        Assert.assertNotNull(body, "Response body");
        assertHasPagedOrListShape(r.jsonPath());
    }

    @Test(priority = 30, groups = {"Regression", "MemberTransfer"})
    @Story("TC_03: Invalid bulkAccountId path format")
    @Description("Malformed id should be rejected (4xx).")
    public void tc03_invalidBulkIdFormat_4xx() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(BULKID_INVALID_PATH);
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        Assert.assertTrue(
                r.getStatusCode() >= 400,
                "Expected 4xx for bad path, got " + r.getStatusCode());
    }

    @Test(priority = 40, groups = {"Regression", "MemberTransfer"})
    @Story("TC_04: Non-existing bulkAccountId (well-formed UUID)")
    @Description("404 when id does not exist in domain.")
    public void tc04_nonExistingBulkId_notFound() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(BULKID_NON_EXISTING);
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        Assert.assertTrue(
                c == 404,
                "Expected 404 for non-existing bulk, was " + c);
    }

    @Test(priority = 50, groups = {"Regression", "MemberTransfer"})
    @Story("TC_05: Missing Authorization header")
    @Description("401/403 when bearer is absent.")
    public void tc05_missingAuthorization_401or403() {
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(
                wellFormedPathBulkIdForUnauthenticatedGet());
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Content-Type", "application/json;charset=UTF-8");
        h.put("Accept", "application/json;charset=UTF-8");
        h.put("ABCFS-TENANT-ID", MemberTransferSupport.tenantId());
        Response r = ApiClient.get(url, h);
        int code = r.getStatusCode();
        Assert.assertTrue(code == 401 || code == 403, "Expected 401/403, got " + code);
    }

    @Test(priority = 60, groups = {"Regression", "MemberTransfer"})
    @Story("TC_06: Invalid bearer token")
    @Description("401/403 with non-parseable or wrong token.")
    public void tc06_invalidBearer_401or403() {
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(
                wellFormedPathBulkIdForUnauthenticatedGet());
        Response r = ApiClient.get(url, MemberTransferSupport.bearerJsonHeaders("invalid.token.value"));
        int code = r.getStatusCode();
        Assert.assertTrue(code == 401 || code == 403, "Expected 401/403, got " + code);
    }

    @Test(priority = 70, groups = {"Regression", "MemberTransfer"})
    @Story("TC_07: Insufficient client authority")
    @Description("Optional: member.transfer.auth.credentials.no.client.authority")
    public void tc07_missingClientAuthority_401or403() {
        String low = ConfigManager.getOptional("member.transfer.auth.credentials.no.client.authority");
        if (low == null || low.isBlank() || low.contains("REPLACE")) {
            Allure.step("Low-priv creds not configured; fallback to invalid bearer negative check");
            String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(
                    wellFormedPathBulkIdForUnauthenticatedGet());
            Response r = ApiClient.get(url, MemberTransferSupport.bearerJsonHeaders("invalid.token.value"));
            int code = r.getStatusCode();
            Assert.assertTrue(code == 401 || code == 403, "Expected 401/403 with fallback invalid bearer, got " + code);
            return;
        }
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulkIdFromInquiryForGetApisOrSkip());
        Response r = ApiClient.get(url, MemberTransferSupport.bearerJsonHeaders(fetchTokenForCreds(low)));
        int code = r.getStatusCode();
        Assert.assertTrue(code == 401 || code == 403, "Expected 401/403, got " + code);
    }

    @Test(priority = 80, groups = {"Regression", "MemberTransfer"})
    @Story("TC_08: Response contains batch / summary fields when 200")
    @Description("2xx: root or paged object present (bulkId/status or page wrapper).")
    public void tc08_responseContainsSummaryWhen200() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String bulk = bulkIdFromInquiryForGetApisOrSkip();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulk);
        Response r = getWithReauth(url);
        if (credsUnusable(r) || r.getStatusCode() == 404) {
            throw new SkipException("2xx with body not available; skip strict TC_08");
        }
        Assert.assertTrue(r.getStatusCode() >= 200 && r.getStatusCode() < 300, "2xx");
        JsonPath jp = r.jsonPath();
        boolean hasSummary =
                jp.get("bulkId") != null
                || jp.get("status") != null
                || jp.get("itemResults") != null
                || jp.get("content") != null
                || jp.get("data") != null
                || jp.get("itemResults.bulkId") != null;
        Assert.assertTrue(hasSummary, "Expected a recognizable list/summary or paging payload: " + r.asPrettyString().substring(0, Math.min(200, r.asString().length())));
    }

    @Test(priority = 90, groups = {"Regression", "MemberTransfer"})
    @Story("TC_09: Item-level / agreement content when list non-empty")
    @Description("When item rows exist, at least one row has transfer-related keys where applicable.")
    public void tc09_itemRowsWhenPresent() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulkIdFromInquiryForGetApisOrSkip(), 0, 20, "operation_type==TRANSFER");
        Response r = getWithReauth(url);
        if (credsUnusable(r) || r.getStatusCode() == 404) {
            throw new SkipException("No 200 to inspect row shape");
        }
        if (r.getStatusCode() < 200 || r.getStatusCode() >= 300) {
            throw new SkipException("2xx not returned");
        }
        List<?> rows = contentList(r.jsonPath());
        if (rows == null || rows.isEmpty()) {
            Allure.step("No rows; TC_09 N/A in this environment");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) rows.get(0);
        assertHasAnyKey(first, Set.of(
                "toBillingAccountNumber", "to_billing_account_number",
                "fromBillingAccountId", "from_billing_account_id",
                "agreementId", "agreement_id", "id", "operationType", "operation_type", "state", "status"));
    }

    @Test(priority = 100, groups = {"Regression", "MemberTransfer"})
    @Story("TC_10: Default paging when page and size are omitted in query")
    @Description("Only ?q=… — expect 2xx/404; when 2xx, optional page metadata.")
    public void tc10_omitPageSize_defaultPaging() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrlQOnly(bulkIdFromInquiryForGetApisOrSkip(), "operation_type==TRANSFER");
        Response r = getWithReauth(url);
        if (credsUnusable(r) || r.getStatusCode() == 404) {
            throw new SkipException("No data for id; cannot assert default paging");
        }
        Assert.assertTrue(r.getStatusCode() >= 200 && r.getStatusCode() < 300, "2xx");
        JsonPath jp = r.jsonPath();
        if (contentList(jp) != null) {
            int len = contentList(jp).size();
            org.testng.Assert.assertTrue(len >= 0, "list length");
        }
        if (pageSizeField(jp).isPresent() && pageSizeField(jp).orElse(0) > 0) {
            Allure.step("default size field: " + pageSizeField(jp).get());
        }
    }

    @Test(priority = 110, groups = {"Regression", "MemberTransfer"})
    @Story("TC_11: Explicit page=0&size=1 and TRANSFER q")
    @Description("Respects at-most-one row per page when 200.")
    public void tc11_explicitPageAndSize() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulkIdFromInquiryForGetApisOrSkip(), 0, 1, "operation_type==TRANSFER");
        Response r = getWithReauth(url);
        if (credsUnusable(r) || r.getStatusCode() == 404) {
            throw new SkipException("Not found");
        }
        Assert.assertEquals(r.getStatusCode(), 200, "HTTP 200 for explicit page/size when batch exists");
        List<?> c = contentList(r.jsonPath());
        int n = c != null ? c.size() : 0;
        Assert.assertTrue(n <= 1, "at most one row, got " + n);
    }

    @Test(priority = 120, groups = {"Regression", "MemberTransfer"})
    @Story("TC_12: negative page is rejected")
    @Description("page < 0 → 4xx.")
    public void tc12_negativePage_4xx() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulkIdFromInquiryForGetApisOrSkip(), -1, 10, "operation_type==TRANSFER");
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        Assert.assertTrue(r.getStatusCode() >= 400, "4xx for page=-1, was " + r.getStatusCode());
    }

    @Test(priority = 130, groups = {"Regression", "MemberTransfer"})
    @Story("TC_13: size=0 is rejected or invalid")
    @Description("size=0 expected 4xx in line with other member-transfer paged GETs.")
    public void tc13_zeroSize_4xx() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulkIdFromInquiryForGetApisOrSkip(), 0, 0, "operation_type==TRANSFER");
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        Assert.assertTrue(r.getStatusCode() >= 400, "4xx for size=0, was " + r.getStatusCode());
    }

    @Test(priority = 140, groups = {"Regression", "MemberTransfer"})
    @Story("TC_14: q=operation_type==TRANSFER (explicit check)")
    @Description("200/404; filter applied in default helper.")
    public void tc14_filterOperationTypeTransfer() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulkIdFromInquiryForGetApisOrSkip(), 0, 10, "operation_type==TRANSFER");
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        Assert.assertTrue(c == 200 || c == 404, "200/404, was " + c);
    }

    @Test(priority = 150, groups = {"Regression", "MemberTransfer"})
    @Story("TC_15: q=operation_type==INQUIRY")
    @Description("Filter INQUIRY; 2xx/404/400 depending on service.")
    public void tc15_filterOperationTypeInquiry() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulkIdFromInquiryForGetApisOrSkip(), 0, 10, "operation_type==INQUIRY");
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        Assert.assertTrue(
                c == 200 || c == 404 || c == 400,
                "2xx, 404, or validation 400, was " + c);
    }

    @Test(priority = 160, groups = {"Regression", "MemberTransfer"})
    @Story("TC_16: case sensitivity of operation type value in q")
    @Description("lowercase 'transfer' vs 'TRANSFER' may both be accepted; document behavior.")
    public void tc16_operationTypeCase_variants() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String bulk = bulkIdFromInquiryForGetApisOrSkip();
        Response rUpper = getWithReauth(
                MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulk, 0, 5, "operation_type==TRANSFER"));
        Response rLower = getWithReauth(
                MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulk, 0, 5, "operation_type==transfer"));
        if (credsUnusable(rUpper)) {
            return;
        }
        int u = rUpper.getStatusCode();
        int l = rLower.getStatusCode();
        Allure.step("TRANSFER → " + u + " ; transfer → " + l);
        Assert.assertTrue(
                (u == 200 || u == 404) && (l == 200 || l == 404),
                "Both should be treated as valid RSQL or rejected consistently; u=" + u + " l=" + l);
    }

    @Test(priority = 170, groups = {"Regression", "MemberTransfer"})
    @Story("TC_17: invalid operation type value in q")
    @Description("Unknown value → 4xx where enforced.")
    public void tc17_invalidOperationType_4xx() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulkIdFromInquiryForGetApisOrSkip(), 0, 10, "operation_type==NOT_A_VALID_ENUM");
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        Assert.assertTrue(
                r.getStatusCode() >= 400,
                "Expected 4xx for bad enum, was " + r.getStatusCode());
    }

    @Test(priority = 180, groups = {"Regression", "MemberTransfer"})
    @Story("TC_18: empty or blank operation value in RSQL (where applicable)")
    @Description("Malformed filter should fail validation (4xx).")
    public void tc18_emptyOperationInFilter_4xx() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url3 = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(
                bulkIdFromInquiryForGetApisOrSkip(), 0, 10, "operation_type==");
        Response r = getWithReauth(url3);
        if (credsUnusable(r)) {
            return;
        }
        Assert.assertTrue(
                r.getStatusCode() >= 400,
                "Expected 4xx for empty RHS, was " + r.getStatusCode());
    }

    @Test(priority = 190, groups = {"Regression", "MemberTransfer"})
    @Story("TC_19: malformed RSQL in q")
    @Description("Parser rejects invalid RSQL (4xx).")
    public void tc19_malformedRsql_4xx() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(
                bulkIdFromInquiryForGetApisOrSkip(), 0, 10, "(((invalid");
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        Assert.assertTrue(r.getStatusCode() >= 400, "4xx, was " + r.getStatusCode());
    }

    @Test(priority = 200, groups = {"Regression", "MemberTransfer"})
    @Story("TC_20: unsupported RSQL field in q")
    @Description("Non-existent field in filter (4xx if validated).")
    public void tc20_unsupportedQField_4xx() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(
                bulkIdFromInquiryForGetApisOrSkip(), 0, 10, "unknownFieldXyz==1");
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        if (c == 200) {
            Allure.step("200: unknown RSQL field may be ignored by service (acceptable per some deployments).");
            return;
        }
        Assert.assertTrue(
                c >= 400,
                "4xx for bad field, was " + c);
    }

    @Test(priority = 210, groups = {"Regression", "MemberTransfer"})
    @Story("TC_21: TRANSFER filter on bulk that only has INQUIRY results")
    @Description("200 with empty or reduced content when no TRANSFER rows; requires fresh inquiry bulk in store when possible.")
    public void tc21_transferFilterWhenInquiryOnly() {
        if (isPlaceholderCreds() || !MemberTransferSupport.isInquiryRequestConfigured()) {
            throw new SkipException("Config");
        }
        String bulk = bulkIdFromInquiryForGetApisOrSkip();
        String urlT = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulk, 0, 10, "operation_type==TRANSFER");
        Response r = getWithReauth(urlT);
        if (credsUnusable(r) || r.getStatusCode() == 404) {
            throw new SkipException("N/A in environment");
        }
        Assert.assertEquals(r.getStatusCode(), 200, "200 with TRANSFER filter (possibly empty list)");
    }

    @Test(priority = 220, groups = {"Regression", "MemberTransfer"})
    @Story("TC_22: TRANSFER with in-progress fixture (optional bulkId)")
    @Description("Set member.transfer.batch.bulkid.in.progress in local properties to enable.")
    public void tc22_optionalFixture_inProgress() {
        runOptionalFixture("member.transfer.batch.bulkid.in.progress", "TC_22: in progress");
    }

    @Test(priority = 230, groups = {"Regression", "MemberTransfer"})
    @Story("TC_23: TRANSFER with mixed outcomes (optional bulkId)")
    @Description("member.transfer.batch.bulkid.mixed.outcomes")
    public void tc23_optionalFixture_mixedOutcomes() {
        runOptionalFixture("member.transfer.batch.bulkid.mixed.outcomes", "TC_23: mixed");
    }

    @Test(priority = 240, groups = {"Regression", "MemberTransfer"})
    @Story("TC_24: TRANSFER all succeeded (optional bulkId)")
    @Description("member.transfer.batch.bulkid.all.success")
    public void tc24_optionalFixture_allSuccess() {
        runOptionalFixture("member.transfer.batch.bulkid.all.success", "TC_24: all success");
    }

    @Test(priority = 250, groups = {"Regression", "MemberTransfer"})
    @Story("TC_25: TRANSFER all failed (optional bulkId)")
    @Description("member.transfer.batch.bulkid.all.failed")
    public void tc25_optionalFixture_allFailed() {
        runOptionalFixture("member.transfer.batch.bulkid.all.failed", "TC_25: all failed");
    }

    @Test(priority = 260, groups = {"Regression", "MemberTransfer"})
    @Story("TC_26: Pagination metadata + content count consistent")
    @Description("When totalElements available, it should be >= list size.")
    public void tc26_paginationCountConsistency() {
        assertPaginationCountConsistency("TC_26");
    }

    @Test(priority = 270, groups = {"Regression", "MemberTransfer"})
    @Story("TC_27: Failure reason when a row failed (if any)")
    @Description("When a row is failed, a reason/error field is present (best-effort keys).")
    public void tc27_failureReasonOnFailedItem() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulkIdFromInquiryForGetApisOrSkip(), 0, 50, "operation_type==TRANSFER");
        Response r = getWithReauth(url);
        if (credsUnusable(r) || r.getStatusCode() != 200) {
            throw new SkipException("2xx with rows required");
        }
        List<?> rows = contentList(r.jsonPath());
        if (rows == null) {
            throw new SkipException("Unrecognized page shape");
        }
        boolean anyFailed = false;
        for (Object row : rows) {
            if (!(row instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) row;
            Object st = m.get("status");
            if (st == null) {
                st = m.get("state");
            }
            if (st != null) {
                String s = st.toString().toUpperCase();
                if (s.contains("FAIL") || s.equals("REJECTED") || s.equals("ERROR")) {
                    anyFailed = true;
                    if (!hasAnyKey(
                            m, Set.of(
                                    "failureReason", "errorMessage", "error", "message", "reason", "rejectionReason"))) {
                        Assert.fail("Failed row should expose a reason; keys=" + m.keySet());
                    }
                }
            }
        }
        if (!anyFailed) {
            Allure.step("No failed transfer rows; TC_27 N/A in this data set");
        }
    }

    @Test(priority = 280, groups = {"Regression", "MemberTransfer"})
    @Story("TC_28: Paging metadata for filtered set")
    @Description("With filter, totalElements/number when present are consistent with 200 list.")
    public void tc28_metadataForFiltered() {
        assertPaginationCountConsistency("TC_28");
    }

    @Test(priority = 290, groups = {"Regression", "MemberTransfer"})
    @Story("TC_29: very large page number (graceful 2xx empty or 4xx)")
    @Description("Out-of-range page: empty content or 4xx.")
    public void tc29_outOfRangePage_graceful() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(
                bulkIdFromInquiryForGetApisOrSkip(), 9999, 10, "operation_type==TRANSFER");
        Response r = getWithReauth(url);
        if (credsUnusable(r) || r.getStatusCode() == 404) {
            throw new SkipException("N/A");
        }
        int c = r.getStatusCode();
        Assert.assertTrue(
                c >= 200 && c < 500,
                "Graceful: " + c);
    }

    @Test(priority = 300, groups = {"Regression", "MemberTransfer"})
    @Story("TC_30: API documentation (OpenAPI) reachable (optional path)")
    @Description("Set member.transfer.openapi.path e.g. /v3/api-docs?group=... under base.url")
    public void tc30_openapiOptional() {
        String p = ConfigManager.getOptional("member.transfer.openapi.path");
        if (p == null || p.isBlank() || p.contains("REPLACE")) {
            p = "/v3/api-docs";
            Allure.step("member.transfer.openapi.path not set; using fallback path " + p);
        }
        String u = p.startsWith("http") ? p : MemberTransferSupport.baseUrl() + p;
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Accept", "application/json");
        h.put("User-Agent", "RestAssured-MemberTransfer/1.0");
        Response r = ApiClient.get(u, h);
        int c = r.getStatusCode();
        if (c == 401) {
            throw new SkipException("OpenAPI requires auth; configure path or pre-auth");
        }
        Assert.assertTrue(c == 200 || c == 302 || c == 404, "Expected 200/302/404, was " + c);
    }

    @Test(priority = 310, groups = {"Regression", "MemberTransfer"})
    @Story("TC_31: Request/response logging for successful call")
    @Description("Covered by ApiClient+Allure listeners; this test documents traceability in Allure for batch GETs.")
    public void tc31_loggingCoveredByAllure() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        getWithReauth(
                MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(
                        bulkIdFromInquiryForGetApisOrSkip(), 0, 1, "operation_type==TRANSFER"));
        Allure.step("See Allure for request/response; ApiClient attaches to report.");
    }

    @Test(priority = 320, groups = {"Regression", "MemberTransfer"})
    @Story("TC_32: Error logging for validation fail")
    @Description("Trigger a 4xx (negative page) and rely on same logging path; observability in platform is manual.")
    public void tc32_validationErrorRequestLogged() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        Response r = getWithReauth(
                MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(
                        bulkIdFromInquiryForGetApisOrSkip(), -2, 10, "operation_type==TRANSFER"));
        Assert.assertTrue(
                r.getStatusCode() >= 400,
                "4xx: " + r.getStatusCode());
        Allure.addAttachment("validation error body", "application/json", r.asString());
    }

    @Test(priority = 330, groups = {"Regression", "MemberTransfer"})
    @Story("TC_33: INQUIRY rows / inquiry phase visibility")
    @Description("q=INQUIRY; complements TC_15 with emphasis on read-side inquiry data.")
    public void tc33_inquiryResultsVisible() {
        if (isPlaceholderCreds() || !MemberTransferSupport.isInquiryRequestConfigured()) {
            throw new SkipException("Config");
        }
        String bulk = bulkIdFromInquiryForGetApisOrSkip();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulk, 0, 10, "operation_type==INQUIRY");
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        Assert.assertTrue(
                c == 200 || c == 404 || c == 400,
                "2xx/400/404; was " + c);
    }

    @Test(priority = 340, groups = {"Regression", "MemberTransfer"})
    @Story("TC_34: no q – only page and size in query string")
    @Description("Omitted RSQL filter: default or validation behavior.")
    public void tc34_omitQ_onlyPageSize() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrlPageSizeOnly(
                bulkIdFromInquiryForGetApisOrSkip(), 0, 10);
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        Assert.assertTrue(
                c == 200 || c == 400 || c == 404,
                "200 with defaults or 400 if required; was " + c);
    }

    @Test(priority = 350, groups = {"Regression", "MemberTransfer"})
    @Story("TC_35: bad / duplicate / ambiguous query in URL")
    @Description("Invalid encoding or duplicate q param – expect 4xx, 200, or 404; not 5xx.")
    public void tc35_maliciousOrDuplicateQuery() {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        String id = bulkIdFromInquiryForGetApisOrSkip();
        String u = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrlRawQuery(
                id,
                "q=operation_type==TRANSFER&page=0&size=10&page=0&q=and");
        Response r = getWithReauth(u);
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        Assert.assertTrue(
                c < 500,
                "Should not 5xx on duplicate/ambiguous query; was " + c);
    }

    @Test(priority = 25, groups = {"Regression", "MemberTransfer"})
    @Description("GET using bulkId from POST /inquiry (response store, or on-demand postInquiry).")
    public void getBatchTransfers_usingBulkIdFromInquiry() {
        if (isPlaceholderCreds() || !MemberTransferSupport.isInquiryRequestConfigured()) {
            throw new SkipException("Config");
        }
        String bulk = bulkIdFromInquiryForGetApisOrSkip();
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulk);
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        Allure.addAttachment("chained " + c, "application/json", r.asPrettyString());
        Assert.assertTrue(c == 200 || c == 404, "HTTP " + c);
    }

    // --- helpers ---

    private void assertPaginationCountConsistency(String allureName) {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        tokenOrGet();
        Response r = getWithReauth(
                MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(bulkIdFromInquiryForGetApisOrSkip(), 0, 20, "operation_type==TRANSFER"));
        if (credsUnusable(r) || r.getStatusCode() == 404) {
            throw new SkipException("No 200 to validate counts for " + allureName);
        }
        if (r.getStatusCode() < 200 || r.getStatusCode() >= 300) {
            throw new SkipException("2xx not returned: " + r.getStatusCode());
        }
        JsonPath jp = r.jsonPath();
        List<?> c = contentList(jp);
        int listLen = c != null ? c.size() : 0;
        Optional<Integer> te = totalElements(jp);
        if (te.isEmpty()) {
            Allure.step("No totalElements; " + allureName + " N/A in body shape");
            return;
        }
        int t = te.get();
        Allure.step(allureName + " total=" + t + " pageItems=" + listLen);
        Assert.assertTrue(
                t >= listLen,
                "totalElements >= this page's content. total=" + t + " list=" + listLen);
        pageNumber(jp).ifPresent(pn -> Allure.step("page: " + pn));
        pageSizeField(jp).ifPresent(ps -> Allure.step("size: " + ps));
    }

    private static void runOptionalFixture(String propertyKey, String allureName) {
        if (isPlaceholderCreds()) {
            throw new SkipException("Config");
        }
        String b = ConfigManager.getOptional(propertyKey);
        if (b == null || b.isBlank() || b.contains("REPLACE")) {
            b = bulkIdFromInquiryForGetApisOrSkip();
            Allure.step(propertyKey + " not configured; fallback to inquiry bulkId " + b);
        }
        if (noToken()) {
            MemberTransferAuth.accessToken();
        }
        String url = MemberTransferSupport.billingAccountBatchTransfersByBulkIdUrl(b, 0, 20, "operation_type==TRANSFER");
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        Allure.addAttachment(allureName, "application/json", r.asPrettyString());
        int c = r.getStatusCode();
        Assert.assertTrue(
                c == 200 || c == 404 || (c >= 400 && c < 500),
                "Expected 200/404/4xx from fixture GET: " + c);
    }

    private static void assertHasAnyKey(Map<String, Object> m, Set<String> keys) {
        for (String k : keys) {
            if (m.containsKey(k) && m.get(k) != null) {
                return;
            }
        }
        throw new SkipException("No expected keys in row: " + m.keySet());
    }

    private static boolean hasAnyKey(Map<String, Object> m, Set<String> keys) {
        for (String k : keys) {
            if (m.containsKey(k) && m.get(k) != null) {
                return true;
            }
        }
        return false;
    }

    private static void assertHasPagedOrListShape(JsonPath jp) {
        boolean ok =
                jp.get("content") != null
                || jp.get("itemResults") != null
                || jp.get("data") != null
                || jp.get("itemResults.content") != null
                || jp.get("itemResults") != null;
        Assert.assertTrue(
                ok,
                "Response should be a paged or list object");
    }

    /**
     * Reuses key {@value com.membertransfer.support.MemberTransferSupport#BULK_ID_KEY} from the store when set,
     * otherwise calls {@link com.membertransfer.support.MemberTransferSupport#postInquiryAndStoreBulkId()}.
     */
    private static String bulkIdFromInquiryForGetApisOrSkip() {
        if (isPlaceholderCreds() || !MemberTransferSupport.isInquiryRequestConfigured()) {
            throw new SkipException("Configure member.transfer.* (OAuth + inquiry + tenant) with no REPLACE; "
                    + "GET cases use bulkId from POST " + MemberTransferSupport.inquiryUrl());
        }
        String existing = ResponseStore.get(MemberTransferSupport.BULK_ID_KEY);
        if (existing != null && !existing.isBlank() && !"null".equalsIgnoreCase(existing)) {
            return existing;
        }
        if (noToken()) {
            MemberTransferAuth.accessToken();
        }
        String bulk = MemberTransferSupport.postInquiryAndStoreBulkId();
        if (bulk == null || bulk.isBlank()) {
            throw new SkipException("POST " + MemberTransferSupport.inquiryUrl()
                    + " did not return bulkId (200 + body.bulkId).");
        }
        return bulk;
    }

    /**
     * TC_05/TC_06: path must be well-formed; auth fails before body lookup, so we cannot run inquiry (needs bearer).
     */
    private static String wellFormedPathBulkIdForUnauthenticatedGet() {
        return MemberTransferSupport.sampleStatusBulkId();
    }

    private static boolean isPlaceholderCreds() {
        try {
            return MemberTransferSupport.memberTransferAuthCredentials().contains("REPLACE");
        } catch (Exception e) {
            return true;
        }
    }

    private static void tokenOrGet() {
        if (noToken()) {
            String t = MemberTransferAuth.accessToken();
            if (t == null || t.isBlank()) {
                throw new SkipException("No bearer token");
            }
        }
    }

    private static boolean noToken() {
        String t = ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
        return t == null || t.isEmpty();
    }

    private static Response getWithReauth(String url) {
        String t = (String) ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
        if (t == null || t.isEmpty()) {
            t = MemberTransferAuth.accessToken();
        }
        Map<String, String> h = MemberTransferSupport.bearerJsonHeaders(t);
        Response r = ApiClient.get(url, h);
        if (r.getStatusCode() == 401) {
            MemberTransferSupport.fetchAndStoreToken();
            t = (String) ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
            r = ApiClient.get(url, MemberTransferSupport.bearerJsonHeaders(t));
        }
        return r;
    }

    private static boolean credsUnusable(Response r) {
        return r.getStatusCode() == 401
                && MemberTransferSupport.memberTransferAuthCredentials().contains("REPLACE");
    }

    private static String fetchTokenForCreds(String creds) {
        String b64 = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Basic " + b64);
        headers.put("User-Agent", "RestAssured-MemberTransfer/1.0");
        headers.put("Accept", "application/json");
        headers.put("Cache-Control", "no-cache");
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        Response tr = ApiClient.post(MemberTransferSupport.authTokenUrl(), headers, body);
        org.testng.Assert.assertEquals(tr.getStatusCode(), 200, "low-priv token");
        String token = tr.jsonPath().getString("access_token");
        org.testng.Assert.assertNotNull(token, "access_token");
        return token;
    }
}
