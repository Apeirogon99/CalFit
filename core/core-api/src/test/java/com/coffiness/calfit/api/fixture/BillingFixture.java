package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.CreateCostRequest;
import com.coffiness.calfit.api.v1.request.UpdateCostRequest;
import com.coffiness.calfit.api.v1.response.CostResponse;
import com.coffiness.calfit.api.v1.response.CostSummaryResponse;
import com.coffiness.calfit.api.v1.response.CostTrendResponse;
import com.coffiness.calfit.api.v1.response.DashboardSummaryResponse;
import com.coffiness.calfit.api.v1.response.InvoiceResponse;
import com.coffiness.calfit.api.v1.response.InvoiceSummaryResponse;
import com.coffiness.calfit.api.v1.response.PlanDistributionResponse;
import com.coffiness.calfit.api.v1.response.PnlReportResponse;
import com.coffiness.calfit.api.v1.response.PnlSummaryResponse;
import com.coffiness.calfit.api.v1.response.RevenueCostTrendResponse;
import com.coffiness.calfit.api.v1.response.SubscriptionResponse;
import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;

/**
 * Admin Billing API 테스트용 픽스처.
 *
 * <p>DataInitializer가 생성한 admin 계정(admin@coffiiness.com)을 기반으로 동작한다.
 */
public record BillingFixture(
    BaseFixture base, UserFixture userFixture, WorkspaceFixture workspaceFixture) {

  static final String ADMIN_EMAIL = "admin@coffiiness.com";
  static final String ADMIN_PASSWORD = "admin1234!";

  public static BillingFixture create(Environment environment, ObjectMapper objectMapper) {
    BaseFixture base = BaseFixture.create(environment, objectMapper);
    UserFixture userFixture = UserFixture.create(environment, objectMapper);
    WorkspaceFixture workspaceFixture = WorkspaceFixture.create(environment, objectMapper);
    return new BillingFixture(base, userFixture, workspaceFixture);
  }

  /** DataInitializer가 생성한 admin 계정으로 로그인해 액세스 토큰을 반환한다. */
  public String adminToken() {
    return userFixture.login(ADMIN_EMAIL, ADMIN_PASSWORD).getData().accessToken();
  }

  /** 일반 멤버 토큰 (403 권한 테스트용) */
  public String memberToken() {
    return userFixture.createUserAndGetToken();
  }

  /**
   * 워크스페이스 생성 → WorkspaceCreatedEvent 발행 → BillingEventHandler가 Trial 구독 생성.
   *
   * @return 생성된 워크스페이스 ID
   */
  public String createWorkspaceAndGetId() {
    String token = memberToken();
    return workspaceFixture.createWorkspace(token).getData().workspaceId();
  }

  // ==================== Dashboard ====================

  public ApiResponse<DashboardSummaryResponse> getDashboardSummary(String token, String month) {
    String url = "/api/v1/admin/dashboard/summary" + (month != null ? "?month=" + month : "");
    return base.get(url, token, DashboardSummaryResponse.class);
  }

  public ApiResponse<RevenueCostTrendResponse> getRevenueCostTrend(String token) {
    return base.get(
        "/api/v1/admin/dashboard/revenue-cost-trend", token, RevenueCostTrendResponse.class);
  }

  public ApiResponse<PlanDistributionResponse> getPlanDistribution(String token) {
    return base.get(
        "/api/v1/admin/dashboard/plan-distribution", token, PlanDistributionResponse.class);
  }

  // ==================== Subscriptions ====================

  public ApiResponse<SubscriptionResponse[]> getSubscriptions(String token) {
    return base.get("/api/v1/admin/subscriptions", token, SubscriptionResponse[].class);
  }

  public ApiResponse<SubscriptionResponse[]> getSubscriptions(
      String token, String status, String search, int page, int size) {
    StringBuilder url =
        new StringBuilder("/api/v1/admin/subscriptions?page=")
            .append(page)
            .append("&size=")
            .append(size);
    if (status != null) url.append("&status=").append(status);
    if (search != null) url.append("&search=").append(search);
    return base.get(url.toString(), token, SubscriptionResponse[].class);
  }

  public ApiResponse<SubscriptionResponse> getSubscription(String token, Long id) {
    return base.get("/api/v1/admin/subscriptions/" + id, token, SubscriptionResponse.class);
  }

  // ==================== Invoices ====================

  public ApiResponse<InvoiceSummaryResponse> getInvoiceSummary(String token, String month) {
    String url = "/api/v1/admin/invoices/summary" + (month != null ? "?month=" + month : "");
    return base.get(url, token, InvoiceSummaryResponse.class);
  }

  public ApiResponse<InvoiceResponse[]> getInvoices(String token) {
    return base.get("/api/v1/admin/invoices", token, InvoiceResponse[].class);
  }

  public ApiResponse<InvoiceResponse> getInvoice(String token, Long id) {
    return base.get("/api/v1/admin/invoices/" + id, token, InvoiceResponse.class);
  }

  public ApiResponse<InvoiceResponse[]> getInvoices(String token, String status) {
    String url = "/api/v1/admin/invoices" + (status != null ? "?status=" + status : "");
    return base.get(url, token, InvoiceResponse[].class);
  }

  // ==================== Costs ====================

  public ApiResponse<CostSummaryResponse> getCostSummary(String token, String month) {
    String url = "/api/v1/admin/costs/summary" + (month != null ? "?month=" + month : "");
    return base.get(url, token, CostSummaryResponse.class);
  }

  public ApiResponse<CostTrendResponse> getCostTrend(String token) {
    return base.get("/api/v1/admin/costs/trend", token, CostTrendResponse.class);
  }

  public ApiResponse<CostResponse[]> getCosts(String token) {
    return base.get("/api/v1/admin/costs", token, CostResponse[].class);
  }

  public ApiResponse<CostResponse[]> getCosts(
      String token, Integer year, Integer month, String category) {
    StringBuilder url = new StringBuilder("/api/v1/admin/costs?");
    if (year != null) url.append("year=").append(year).append("&");
    if (month != null) url.append("month=").append(month).append("&");
    if (category != null) url.append("category=").append(category).append("&");
    return base.get(url.toString(), token, CostResponse[].class);
  }

  public ApiResponse<CostResponse[]> getCostsPaged(
      String token, Integer year, Integer month, String category, int page, int size) {
    StringBuilder url =
        new StringBuilder("/api/v1/admin/costs?page=").append(page).append("&size=").append(size);
    if (year != null) url.append("&year=").append(year);
    if (month != null) url.append("&month=").append(month);
    if (category != null) url.append("&category=").append(category);
    return base.get(url.toString(), token, CostResponse[].class);
  }

  public ApiResponse<CostResponse> createCost(
      String token,
      CostCategory category,
      String name,
      Long amount,
      int year,
      int month,
      String memo) {
    CreateCostRequest request = new CreateCostRequest(category, name, amount, year, month, memo);
    return base.post("/api/v1/admin/costs", request, token, CostResponse.class);
  }

  public ApiResponse<CostResponse> createCost(
      String token, CostCategory category, String name, Long amount, int year, int month) {
    return createCost(token, category, name, amount, year, month, null);
  }

  /** 유효성 검사 테스트용: 타입 안전성 없이 원시 요청 객체를 전송한다. */
  public ApiResponse<CostResponse> createCostRaw(String token, Object rawRequest) {
    return base.post("/api/v1/admin/costs", rawRequest, token, CostResponse.class);
  }

  public ApiResponse<CostResponse> updateCost(
      String token,
      Long id,
      CostCategory category,
      String name,
      Long amount,
      int year,
      int month,
      String memo) {
    UpdateCostRequest request = new UpdateCostRequest(category, name, amount, year, month, memo);
    return base.put("/api/v1/admin/costs/" + id, request, token, CostResponse.class);
  }

  /** 유효성 검사 테스트용 */
  public ApiResponse<CostResponse> updateCostRaw(String token, Long id, Object rawRequest) {
    return base.put("/api/v1/admin/costs/" + id, rawRequest, token, CostResponse.class);
  }

  public ApiResponse<Void> deleteCost(String token, Long id) {
    return base.delete("/api/v1/admin/costs/" + id, token, Void.class);
  }

  // ==================== P&L ====================

  public ApiResponse<PnlSummaryResponse> getPnlSummary(String token, String month) {
    String url = "/api/v1/admin/pnl/summary" + (month != null ? "?month=" + month : "");
    return base.get(url, token, PnlSummaryResponse.class);
  }

  public ApiResponse<PnlReportResponse> getPnlReport(String token, int months) {
    return base.get("/api/v1/admin/pnl/report?months=" + months, token, PnlReportResponse.class);
  }
}
