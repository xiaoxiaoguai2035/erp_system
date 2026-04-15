package com.dongjian.erp.manufacturingerpsystem.modules.ai.service.impl;

import com.dongjian.erp.manufacturingerpsystem.config.AiProperties;
import com.dongjian.erp.manufacturingerpsystem.modules.ai.service.AiAssistantService;
import com.dongjian.erp.manufacturingerpsystem.modules.ai.vo.AiReportInsightResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.report.service.ReportService;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AiProperties aiProperties;
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ReportService reportService;

    public AiAssistantServiceImpl(AiProperties aiProperties,
                                  ObjectProvider<ChatModel> chatModelProvider,
                                  ReportService reportService) {
        this.aiProperties = aiProperties;
        this.chatModelProvider = chatModelProvider;
        this.reportService = reportService;
    }

    @Override
    public String chat(String question, LocalDate startDate, LocalDate endDate) {
        if (!canUseAiModel()) {
            return buildScopedFallbackChat(question, startDate, endDate);
        }
        return chatModelProvider.getIfAvailable().chat(buildChatPrompt(question, startDate, endDate));
    }

    @Override
    public AiReportInsightResponse generateReportInsight(LocalDate startDate, LocalDate endDate, String question) {
        Map<String, Object> salesSummary = reportService.salesSummary(startDate, endDate, null);
        Map<String, Object> purchaseSummary = reportService.purchaseSummary(startDate, endDate, null);
        Map<String, Object> inventorySummary = reportService.inventorySummary(startDate, endDate, null);
        Map<String, Object> productionSummary = reportService.productionSummary(startDate, endDate);
        List<Map<String, Object>> arSummary = reportService.arSummary(startDate, endDate, 3, null);
        List<Map<String, Object>> apSummary = reportService.apSummary(startDate, endDate, 3, null);

        String periodLabel = buildPeriodLabel(startDate, endDate);
        Map<String, Object> snapshot = buildSnapshot(salesSummary, purchaseSummary, inventorySummary, productionSummary, arSummary, apSummary);
        List<String> highlights = buildHighlights(snapshot);
        List<String> risks = buildRisks(snapshot);
        List<String> suggestions = buildSuggestions(snapshot);

        AiReportInsightResponse response = new AiReportInsightResponse();
        response.setPeriodLabel(periodLabel);
        response.setHighlights(highlights);
        response.setRisks(risks);
        response.setSuggestions(suggestions);
        response.setSnapshot(snapshot);
        response.setSummary(generateInsightSummary(periodLabel, snapshot, highlights, risks, suggestions, question));
        return response;
    }

    private boolean canUseAiModel() {
        return aiProperties.isEnabled()
                && StringUtils.hasText(aiProperties.getApiKey())
                && chatModelProvider.getIfAvailable() != null;
    }

    private String buildChatPrompt(String question, LocalDate startDate, LocalDate endDate) {
        AiReportInsightResponse insight = generateReportInsight(startDate, endDate, question);
        return """
                你是制造企业 ERP 系统中的经营助手。
                你的回答必须简洁、正式、中文输出。
                如果问题涉及业务建议，请优先给出可执行的管理建议。
                回答必须严格基于下面这组系统真实经营快照，不要编造数据。

                统计区间：%s
                用户问题：%s
                经营摘要：%s
                亮点：%s
                风险：%s
                建议：%s
                快照：%s
                """.formatted(
                insight.getPeriodLabel(),
                question,
                insight.getSummary(),
                insight.getHighlights(),
                insight.getRisks(),
                insight.getSuggestions(),
                insight.getSnapshot()
        );
    }

    private String buildScopedFallbackChat(String question, LocalDate startDate, LocalDate endDate) {
        AiReportInsightResponse insight = generateReportInsight(startDate, endDate, question);
        String risk = insight.getRisks().isEmpty() ? "当前未发现突出的高优先级风险" : insight.getRisks().get(0);
        String suggestion = insight.getSuggestions().isEmpty() ? "建议继续关注销售、采购、库存和生产协同节奏" : insight.getSuggestions().get(0);
        return """
                当前 AI 模型未启用，已切换为本地经营助手模式。
                %s
                结合你的问题“%s”，优先建议：%s；同时需关注：%s。
                """.formatted(insight.getSummary(), question, suggestion, risk);
    }

    private String generateInsightSummary(String periodLabel,
                                          Map<String, Object> snapshot,
                                          List<String> highlights,
                                          List<String> risks,
                                          List<String> suggestions,
                                          String question) {
        String fallbackSummary = buildFallbackSummary(periodLabel, highlights, risks, suggestions);
        if (!canUseAiModel()) {
            return fallbackSummary;
        }

        String prompt = """
                你是制造企业 ERP 的经营分析助手。
                请基于以下真实报表快照，输出一段简洁的中文经营解读。
                要求：
                1. 使用三段短句，并用全角分号“；”连接
                2. 固定结构为“经营表现：...；核心风险：...；行动建议：...”
                3. 总长度控制在 90 到 130 字之间
                4. 语言正式、精炼，适合系统页面直接展示
                5. 不要编造不存在的数据

                分析期间：%s
                用户补充问题：%s
                关键指标：%s
                已提炼亮点：%s
                已提炼风险：%s
                已提炼建议：%s
                """.formatted(
                periodLabel,
                StringUtils.hasText(question) ? question.trim() : "无",
                snapshot,
                highlights,
                risks,
                suggestions
        );

        try {
            return chatModelProvider.getIfAvailable().chat(prompt);
        } catch (Exception ex) {
            return fallbackSummary;
        }
    }

    private String buildFallbackSummary(String periodLabel,
                                        List<String> highlights,
                                        List<String> risks,
                                        List<String> suggestions) {
        String highlightText = highlights.isEmpty() ? "整体经营数据较为平稳" : highlights.get(0);
        String riskText = risks.isEmpty() ? "当前未发现突出的高优先级经营风险" : risks.get(0);
        String suggestionText = suggestions.isEmpty() ? "建议继续保持订单、库存与生产节奏联动" : suggestions.get(0);
        return "经营表现：%s内，%s；核心风险：%s；行动建议：%s。".formatted(periodLabel, highlightText, riskText, suggestionText);
    }

    private String buildPeriodLabel(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "最近经营区间";
        }
        if (startDate != null && endDate != null) {
            return DATE_FORMATTER.format(startDate) + " 至 " + DATE_FORMATTER.format(endDate);
        }
        if (startDate != null) {
            return DATE_FORMATTER.format(startDate) + " 起";
        }
        return "截至 " + DATE_FORMATTER.format(endDate);
    }

    private Map<String, Object> buildSnapshot(Map<String, Object> salesSummary,
                                              Map<String, Object> purchaseSummary,
                                              Map<String, Object> inventorySummary,
                                              Map<String, Object> productionSummary,
                                              List<Map<String, Object>> arSummary,
                                              List<Map<String, Object>> apSummary) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("salesAmount", decimalValue(salesSummary.get("summaryAmount")));
        snapshot.put("purchaseAmount", decimalValue(purchaseSummary.get("summaryAmount")));
        snapshot.put("inventoryAmount", decimalValue(inventorySummary.get("inventoryAmount")));
        snapshot.put("shipmentRate", percentValue(salesSummary.get("shipmentRate")));
        snapshot.put("deliveryRate", percentValue(purchaseSummary.get("deliveryRate")));
        snapshot.put("inventoryWarningCount", integerValue(inventorySummary.get("warningCount")));
        snapshot.put("turnoverDays", decimalValue(inventorySummary.get("turnoverDays")));
        snapshot.put("onTimeRate", percentValue(productionSummary.get("onTimeRate")));
        snapshot.put("delayRiskCount", integerValue(productionSummary.get("delayRiskCount")));
        snapshot.put("inProgressCount", integerValue(productionSummary.get("inProgressCount")));
        snapshot.put("topProduct", textValue(salesSummary.get("topProduct"), "暂无销售产品"));
        snapshot.put("topSupplier", textValue(purchaseSummary.get("topSupplier"), "暂无供应商"));

        Map<String, Object> topAr = arSummary.isEmpty() ? Map.of() : arSummary.get(0);
        Map<String, Object> topAp = apSummary.isEmpty() ? Map.of() : apSummary.get(0);
        snapshot.put("topArCustomer", textValue(topAr.get("customer"), "暂无应收客户"));
        snapshot.put("topArAmount", decimalValue(topAr.get("amount")));
        snapshot.put("topApSupplier", textValue(topAp.get("supplier"), "暂无应付供应商"));
        snapshot.put("topApAmount", decimalValue(topAp.get("amount")));
        return snapshot;
    }

    private List<String> buildHighlights(Map<String, Object> snapshot) {
        List<String> highlights = new ArrayList<>();
        BigDecimal salesAmount = decimalValue(snapshot.get("salesAmount"));
        BigDecimal purchaseAmount = decimalValue(snapshot.get("purchaseAmount"));
        BigDecimal inventoryAmount = decimalValue(snapshot.get("inventoryAmount"));
        BigDecimal shipmentRate = decimalValue(snapshot.get("shipmentRate"));
        BigDecimal onTimeRate = decimalValue(snapshot.get("onTimeRate"));

        if (salesAmount.compareTo(BigDecimal.ZERO) > 0) {
            highlights.add("销售额为 " + formatAmount(salesAmount) + " 元，主销产品为“" + snapshot.get("topProduct") + "”");
        }
        if (purchaseAmount.compareTo(BigDecimal.ZERO) > 0) {
            highlights.add("采购额为 " + formatAmount(purchaseAmount) + " 元，主要采购来源供应商为“" + snapshot.get("topSupplier") + "”");
        }
        if (shipmentRate.compareTo(BigDecimal.valueOf(80)) >= 0) {
            highlights.add("销售发货率达到 " + formatPercent(shipmentRate) + "，订单兑现情况较好");
        }
        if (onTimeRate.compareTo(BigDecimal.valueOf(80)) >= 0) {
            highlights.add("生产按时完工率为 " + formatPercent(onTimeRate) + "，履约执行较稳定");
        }
        if (inventoryAmount.compareTo(BigDecimal.ZERO) > 0) {
            highlights.add("当前库存货值约 " + formatAmount(inventoryAmount) + " 元，可支撑近期供应与生产调度");
        }
        return highlights;
    }

    private List<String> buildRisks(Map<String, Object> snapshot) {
        List<String> risks = new ArrayList<>();
        int warningCount = integerValue(snapshot.get("inventoryWarningCount"));
        int delayRiskCount = integerValue(snapshot.get("delayRiskCount"));
        BigDecimal turnoverDays = decimalValue(snapshot.get("turnoverDays"));
        BigDecimal topArAmount = decimalValue(snapshot.get("topArAmount"));
        BigDecimal topApAmount = decimalValue(snapshot.get("topApAmount"));
        BigDecimal shipmentRate = decimalValue(snapshot.get("shipmentRate"));
        BigDecimal deliveryRate = decimalValue(snapshot.get("deliveryRate"));

        if (warningCount > 0) {
            risks.add("存在 " + warningCount + " 项库存预警，需优先关注低于安全库存的关键物料");
        }
        if (delayRiskCount > 0) {
            risks.add("存在 " + delayRiskCount + " 张工单处于延期风险状态，可能影响订单交付");
        }
        if (turnoverDays.compareTo(BigDecimal.valueOf(60)) > 0) {
            risks.add("库存周转天数为 " + formatAmount(turnoverDays) + " 天，库存占压偏高");
        }
        if (shipmentRate.compareTo(BigDecimal.valueOf(60)) < 0) {
            risks.add("销售发货率仅为 " + formatPercent(shipmentRate) + "，待发货压力较大");
        }
        if (deliveryRate.compareTo(BigDecimal.valueOf(60)) < 0) {
            risks.add("采购到货率仅为 " + formatPercent(deliveryRate) + "，供应兑现度偏弱");
        }
        if (topArAmount.compareTo(BigDecimal.ZERO) > 0) {
            risks.add("最大应收客户“" + snapshot.get("topArCustomer") + "”占款 " + formatAmount(topArAmount) + " 元，需关注回款节奏");
        }
        if (topApAmount.compareTo(BigDecimal.ZERO) > 0) {
            risks.add("最大应付供应商“" + snapshot.get("topApSupplier") + "”待付款 " + formatAmount(topApAmount) + " 元，需安排付款计划");
        }
        return risks;
    }

    private List<String> buildSuggestions(Map<String, Object> snapshot) {
        List<String> suggestions = new ArrayList<>();
        int warningCount = integerValue(snapshot.get("inventoryWarningCount"));
        int delayRiskCount = integerValue(snapshot.get("delayRiskCount"));
        BigDecimal shipmentRate = decimalValue(snapshot.get("shipmentRate"));
        BigDecimal deliveryRate = decimalValue(snapshot.get("deliveryRate"));
        BigDecimal topArAmount = decimalValue(snapshot.get("topArAmount"));

        if (warningCount > 0) {
            suggestions.add("优先结合 MRP 补齐短缺物料，并对预警物料设置采购或调拨跟催");
        }
        if (delayRiskCount > 0) {
            suggestions.add("对延期风险工单逐张检查报工、领料和排产，必要时调整优先级");
        }
        if (shipmentRate.compareTo(BigDecimal.valueOf(80)) < 0) {
            suggestions.add("围绕待发货订单倒排生产和出库计划，缩短销售兑现周期");
        }
        if (deliveryRate.compareTo(BigDecimal.valueOf(80)) < 0) {
            suggestions.add("对关键供应商执行到货跟踪，避免采购未到货影响生产齐套");
        }
        if (topArAmount.compareTo(BigDecimal.ZERO) > 0) {
            suggestions.add("对高额应收客户制定分批催收计划，降低资金占用");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("继续保持销售、采购、库存和生产数据联动，按周期复盘经营指标");
        }
        return suggestions;
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return new BigDecimal(text.trim());
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal percentValue(Object value) {
        BigDecimal decimal = decimalValue(value);
        return decimal.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private int integerValue(Object value) {
        return decimalValue(value).intValue();
    }

    private String textValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatPercent(BigDecimal percent) {
        return percent.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }
}
