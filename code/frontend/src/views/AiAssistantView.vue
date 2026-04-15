<template>
  <div class="page-grid ai-page">
    <section class="module-hero ai-hero">
      <div>
        <h2>AI 经营助手</h2>
        <p>围绕经营解读与业务问答提供统一的智能工作台，让结论展示和追问交互都更清晰。</p>
      </div>
    </section>

    <div class="ai-workspace">
      <PagePanel title="报表自动解读" description="选择时间范围并生成结构化经营总结。">
        <div class="assistant-toolbar">
          <el-date-picker
            v-model="insightFilters.dateRange"
            type="daterange"
            unlink-panels
            clearable
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
          <el-input
            v-model="insightFilters.question"
            placeholder="补充分析角度，例如：重点关注库存和延期交付风险"
            clearable
          />
          <div class="toolbar-actions">
            <el-button @click="applyInsightExample('请重点解读库存短缺和延期交付风险')">库存与交付</el-button>
            <el-button @click="applyInsightExample('请从销售兑现、采购到货和资金占用三个角度分析')">经营概览</el-button>
            <el-button type="primary" :loading="insightLoading" @click="loadInsight">生成解读</el-button>
          </div>
        </div>

        <template v-if="insightData">
          <article class="insight-summary-card">
            <div class="summary-card-head">
              <div>
                <span>{{ insightData.periodLabel }}</span>
                <h4>AI 总评</h4>
              </div>
              <div class="summary-card-tags">
                <span class="summary-tag">亮点 {{ (insightData.highlights || []).length }}</span>
                <span class="summary-tag risk">风险 {{ (insightData.risks || []).length }}</span>
                <span class="summary-tag suggestion">建议 {{ (insightData.suggestions || []).length }}</span>
              </div>
            </div>

            <ul class="summary-points">
              <li v-for="item in summaryPoints" :key="item">{{ item }}</li>
            </ul>
          </article>

          <div class="insight-view-switcher">
            <button
              v-for="item in insightViewOptions"
              :key="item.key"
              type="button"
              class="insight-view-button"
              :class="{ active: activeInsightView === item.key }"
              @click="activeInsightView = item.key"
            >
              {{ item.label }}
            </button>
          </div>

          <template v-if="activeInsightView === 'overview'">
            <div class="insight-metrics-grid">
              <article class="insight-metric-card">
                <span>销售额</span>
                <strong>{{ formatMoney(insightData.snapshot?.salesAmount) }}</strong>
              </article>
              <article class="insight-metric-card">
                <span>采购额</span>
                <strong>{{ formatMoney(insightData.snapshot?.purchaseAmount) }}</strong>
              </article>
              <article class="insight-metric-card">
                <span>库存预警</span>
                <strong>{{ insightData.snapshot?.inventoryWarningCount ?? 0 }} 项</strong>
              </article>
              <article class="insight-metric-card">
                <span>延期风险工单</span>
                <strong>{{ insightData.snapshot?.delayRiskCount ?? 0 }} 张</strong>
              </article>
            </div>

            <div class="insight-section-grid compact">
              <article class="insight-section-card">
                <h4>经营亮点</h4>
                <ul>
                  <li v-for="item in previewHighlights" :key="`highlight-${item}`">{{ item }}</li>
                </ul>
              </article>
              <article class="insight-section-card risk">
                <h4>风险提示</h4>
                <ul>
                  <li v-for="item in previewRisks" :key="`risk-${item}`">{{ item }}</li>
                </ul>
              </article>
              <article class="insight-section-card suggestion">
                <h4>执行建议</h4>
                <ul>
                  <li v-for="item in previewSuggestions" :key="`suggestion-${item}`">{{ item }}</li>
                </ul>
              </article>
            </div>
          </template>

          <article v-else class="insight-detail-panel" :class="activeInsightView">
            <h4>{{ activeInsightPanelTitle }}</h4>
            <ul>
              <li v-for="item in activeInsightItems" :key="`${activeInsightView}-${item}`">{{ item }}</li>
            </ul>
          </article>
        </template>

        <el-empty v-else description="点击“生成解读”后展示经营摘要" />
      </PagePanel>
    </div>

    <button type="button" class="chat-fab" :class="{ open: chatOpen }" @click="chatOpen = !chatOpen">
      {{ chatOpen ? "收起问答" : "智能问答" }}
    </button>

    <transition name="chat-float">
      <section v-if="chatOpen" class="chat-float-panel">
        <div class="chat-float-head">
          <div>
            <strong>智能问答</strong>
            <span>围绕当前业务问题快速追问与说明</span>
          </div>
          <button type="button" class="chat-close" @click="chatOpen = false">关闭</button>
        </div>

        <div class="chat-shell">
          <div class="ai-chat chat-scroll">
            <div
              v-for="(message, index) in messages"
              :key="`${message.role}-${index}`"
              class="chat-bubble"
              :class="message.role"
            >
              <span class="chat-role">{{ message.role === "ai" ? "AI 助手" : "你的问题" }}</span>
              <p>{{ message.content }}</p>
            </div>
          </div>

          <div class="chat-composer">
            <el-input
              v-model="question"
              type="textarea"
              :rows="4"
              resize="none"
              placeholder="例如：本周哪些物料低于安全库存？"
              @keyup.ctrl.enter="submitQuestion"
            />
            <div class="toolbar-actions">
              <el-button @click="fillExample('本周哪些物料低于安全库存？')">库存风险</el-button>
              <el-button @click="fillExample('哪些生产计划最可能延期？')">生产风险</el-button>
              <el-button type="primary" :loading="sending" @click="submitQuestion">发送问题</el-button>
            </div>
          </div>
        </div>
      </section>
    </transition>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";

import {
  chatWithAi,
  fetchAiReportInsight,
  fetchApSummary,
  fetchArSummary,
  fetchInventoryStocks,
  fetchInventorySummary,
  fetchProductionPlans,
  fetchProductionWorkOrders
} from "@/api/modules";
import PagePanel from "@/components/PagePanel.vue";
import { formatDate, formatMoney, formatNumber, formatPercent } from "@/utils/format";

const question = ref("");
const sending = ref(false);
const insightLoading = ref(false);
const connected = ref(false);
const insightData = ref(null);
const chatOpen = ref(false);
const activeInsightView = ref("overview");
const messages = ref([
  {
    role: "ai",
    content: "你好，请输入你的问题。"
  }
]);
const insightFilters = reactive({
  dateRange: [],
  question: ""
});

const fillExample = (value) => {
  question.value = value;
};

const applyInsightExample = (value) => {
  insightFilters.question = value;
};

const summaryPoints = computed(() =>
  String(insightData.value?.summary || "")
    .split(/[；。]/)
    .map((item) => item.trim())
    .filter(Boolean)
);

const previewHighlights = computed(() => (insightData.value?.highlights || []).slice(0, 2));
const previewRisks = computed(() => (insightData.value?.risks || []).slice(0, 2));
const previewSuggestions = computed(() => (insightData.value?.suggestions || []).slice(0, 2));
const insightViewOptions = computed(() => [
  { key: "overview", label: "总览" },
  { key: "highlights", label: "亮点" },
  { key: "risks", label: "风险" },
  { key: "suggestions", label: "建议" }
]);
const activeInsightItems = computed(() => {
  if (activeInsightView.value === "highlights") {
    return insightData.value?.highlights || [];
  }
  if (activeInsightView.value === "risks") {
    return insightData.value?.risks || [];
  }
  if (activeInsightView.value === "suggestions") {
    return insightData.value?.suggestions || [];
  }
  return [];
});
const activeInsightPanelTitle = computed(() => {
  if (activeInsightView.value === "highlights") {
    return "经营亮点";
  }
  if (activeInsightView.value === "risks") {
    return "风险提示";
  }
  if (activeInsightView.value === "suggestions") {
    return "执行建议";
  }
  return "总览";
});

const buildScopedParams = () => ({
  startDate: insightFilters.dateRange?.[0] || undefined,
  endDate: insightFilters.dateRange?.[1] || undefined
});

const isRangeOverlappingScope = (startValue, endValue) => {
  const scopeStart = insightFilters.dateRange?.[0];
  const scopeEnd = insightFilters.dateRange?.[1];
  if (!scopeStart && !scopeEnd) {
    return true;
  }

  const normalizedStart = startValue || endValue;
  const normalizedEnd = endValue || startValue;
  if (!normalizedStart && !normalizedEnd) {
    return false;
  }
  if (scopeStart && normalizedEnd && normalizedEnd < scopeStart) {
    return false;
  }
  return !(scopeEnd && normalizedStart && normalizedStart > scopeEnd);
};

const loadInsight = async () => {
  insightLoading.value = true;

  try {
    insightData.value = await fetchAiReportInsight({
      startDate: insightFilters.dateRange?.[0] || undefined,
      endDate: insightFilters.dateRange?.[1] || undefined,
      question: insightFilters.question?.trim() || undefined
    });
    activeInsightView.value = "overview";
    connected.value = true;
  } catch (error) {
    ElMessage.error(error.message || "报表解读生成失败");
  } finally {
    insightLoading.value = false;
  }
};

const normalizeText = (value) => String(value || "").trim().toLowerCase();

const isInventoryRiskQuestion = (value) => {
  const text = normalizeText(value);
  return (
    (text.includes("库存") || text.includes("物料") || text.includes("安全库存") || text.includes("缺货")) &&
    (text.includes("低于") || text.includes("不足") || text.includes("短缺") || text.includes("预警") || text.includes("风险"))
  );
};

const isDelayRiskQuestion = (value) => {
  const text = normalizeText(value);
  return (
    (text.includes("延期") || text.includes("逾期") || text.includes("延误")) &&
    (text.includes("工单") || text.includes("计划") || text.includes("生产"))
  );
};

const isArRiskQuestion = (value) => {
  const text = normalizeText(value);
  return (
    (text.includes("应收") || text.includes("回款") || text.includes("欠款") || text.includes("资金占用")) &&
    (text.includes("客户") || text.includes("哪些") || text.includes("谁") || text.includes("风险"))
  );
};

const isApRiskQuestion = (value) => {
  const text = normalizeText(value);
  return (
    (text.includes("应付") || text.includes("付款")) &&
    (text.includes("供应商") || text.includes("哪些") || text.includes("谁") || text.includes("风险"))
  );
};

const formatDateRangeLabel = () => {
  if (!insightFilters.dateRange?.length) {
    return "当前全量数据范围";
  }
  return `${insightFilters.dateRange[0]} 至 ${insightFilters.dateRange[1]}`;
};

const buildInventoryRiskAnswer = async () => {
  const [inventorySummary, stockPage] = await Promise.all([fetchInventorySummary(buildScopedParams()), fetchInventoryStocks({ pageNo: 1, pageSize: 200 })]);
  const rows = (stockPage.records || [])
    .map((item) => {
      const safetyStock = Number(item.safetyStock || 0);
      const availableQty = Number(item.availableQty ?? item.qty ?? 0);
      const gapQty = Math.max(safetyStock - availableQty, 0);
      return {
        ...item,
        safetyStock,
        availableQty,
        gapQty
      };
    })
    .filter((item) => item.safetyStock > 0 && item.availableQty < item.safetyStock)
    .sort((left, right) => right.gapQty - left.gapQty)
    .slice(0, 5);

  if (!rows.length) {
    return `按${formatDateRangeLabel()}查看，库存统计口径下暂无明显预警；当前实时库存快照中也未发现低于安全库存的物料。`;
  }

  const detailText = rows
    .map(
      (item, index) =>
        `${index + 1}. ${item.materialName || item.materialCode || `物料#${item.materialId}`}（${item.warehouseName || "未指定仓库"}），可用 ${formatNumber(item.availableQty)}，安全库存 ${formatNumber(item.safetyStock)}，缺口 ${formatNumber(item.gapQty)}`
    )
    .join("\n");

  return `按${formatDateRangeLabel()}查看，库存统计摘要识别到 ${formatNumber(inventorySummary?.warningCount || 0, "0")} 项预警；其中当前实时库存快照中的重点低库存物料包括：\n${detailText}\n建议优先补齐缺口最大的物料，并结合采购或调拨动作处理。`;
};

const buildDelayRiskAnswer = async () => {
  const [workOrderPage, planPage] = await Promise.all([fetchProductionWorkOrders({ pageNo: 1, pageSize: 200 }), fetchProductionPlans({ pageNo: 1, pageSize: 200 })]);
  const planMap = new Map((planPage.records || []).map((item) => [Number(item.id), item]));
  const today = new Date();
  const startOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate());

  const rows = (workOrderPage.records || [])
    .map((item) => {
      const planQty = Number(item.planQty || 0);
      const finishedQty = Number(item.finishedQty || 0);
      const progressRate = planQty > 0 ? Math.min(finishedQty / planQty, 1) : 0;
      const endDate = item.endDate ? new Date(item.endDate) : null;
      const delayDays =
        endDate && endDate < startOfToday ? Math.floor((startOfToday.getTime() - endDate.getTime()) / (24 * 60 * 60 * 1000)) : 0;
      const isDelayed = endDate && endDate < startOfToday && progressRate < 1 && !["completed", "closed"].includes(normalizeText(item.status));
      return {
        ...item,
        progressRate,
        delayDays,
        isDelayed,
        planCode: planMap.get(Number(item.planId))?.code
      };
    })
    .filter((item) => item.isDelayed && isRangeOverlappingScope(item.startDate, item.endDate))
    .sort((left, right) => right.delayDays - left.delayDays || left.progressRate - right.progressRate)
    .slice(0, 5);

  if (!rows.length) {
    return "当前未识别到明显的延期风险工单，生产执行节奏整体正常。";
  }

  const detailText = rows
    .map(
      (item, index) =>
        `${index + 1}. 工单 ${item.code} / ${item.materialName || "未命名产品"}，计划完工 ${formatDate(item.endDate)}，当前进度 ${formatPercent(item.progressRate, 1, "0.0%")}，已延期 ${item.delayDays} 天${item.planCode ? `，关联计划 ${item.planCode}` : ""}`
    )
    .join("\n");

  return `当前识别到 ${rows.length} 张高优先级延期风险工单：\n${detailText}\n建议优先核查这些工单的报工、领料和排产资源占用情况。`;
};

const buildArRiskAnswer = async () => {
  const rows = (await fetchArSummary({ ...buildScopedParams(), limit: 5 })) || [];
  if (!rows.length) {
    return `按${formatDateRangeLabel()}查看，当前没有明显的应收风险客户。`;
  }

  const detailText = rows
    .map(
      (item, index) =>
        `${index + 1}. ${item.customer}，应收 ${formatMoney(item.amount)} 元，涉及未完结订单 ${formatNumber(item.orderCount, "0")} 笔${item.sourceOrderCode ? `，代表订单 ${item.sourceOrderCode}` : ""}`
    )
    .join("\n");

  return `按${formatDateRangeLabel()}查看，应收风险客户主要集中在以下对象：\n${detailText}\n建议优先跟进排行靠前客户的回款计划。`;
};

const buildApRiskAnswer = async () => {
  const rows = (await fetchApSummary({ ...buildScopedParams(), limit: 5 })) || [];
  if (!rows.length) {
    return `按${formatDateRangeLabel()}查看，当前没有明显的应付风险供应商。`;
  }

  const detailText = rows
    .map(
      (item, index) =>
        `${index + 1}. ${item.supplier}，应付 ${formatMoney(item.amount)} 元，涉及未完结订单 ${formatNumber(item.orderCount, "0")} 笔${item.sourceOrderCode ? `，代表订单 ${item.sourceOrderCode}` : ""}`
    )
    .join("\n");

  return `按${formatDateRangeLabel()}查看，应付压力主要集中在以下供应商：\n${detailText}\n建议结合到货节奏和现金计划安排付款优先级。`;
};

const tryHandleStructuredQuestion = async (value) => {
  if (isInventoryRiskQuestion(value)) {
    return await buildInventoryRiskAnswer();
  }
  if (isDelayRiskQuestion(value)) {
    return await buildDelayRiskAnswer();
  }
  if (isArRiskQuestion(value)) {
    return await buildArRiskAnswer();
  }
  if (isApRiskQuestion(value)) {
    return await buildApRiskAnswer();
  }
  return null;
};

const submitQuestion = async () => {
  const currentQuestion = question.value.trim();
  if (!currentQuestion) {
    ElMessage.warning("请输入问题后再发送");
    return;
  }

  messages.value.push({
    role: "user",
    content: currentQuestion
  });

  sending.value = true;
  question.value = "";

  try {
    const structuredAnswer = await tryHandleStructuredQuestion(currentQuestion);
    const response = structuredAnswer ? { answer: structuredAnswer } : await chatWithAi({ question: currentQuestion, ...buildScopedParams() });
    messages.value.push({
      role: "ai",
      content: response.answer || "后端未返回有效答案。"
    });
    connected.value = true;
  } catch (error) {
    messages.value.push({
      role: "ai",
      content: `本次请求失败：${error.message || "AI 服务暂不可用"}`
    });
    ElMessage.error(error.message || "AI 问答失败");
  } finally {
    sending.value = false;
  }
};

onMounted(loadInsight);
</script>

<style scoped>
.ai-page {
  gap: 20px;
}

.ai-hero {
  gap: 10px;
  padding: 24px 30px;
}

.ai-hero p {
  max-width: 760px;
}

.ai-workspace {
  display: grid;
  gap: 20px;
}

.assistant-toolbar {
  display: grid;
  gap: 14px;
}

.insight-summary-card {
  margin-top: 18px;
  padding: 20px 22px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.96), rgba(239, 246, 255, 0.92));
  border: 1px solid rgba(191, 219, 254, 0.42);
}

.summary-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.insight-summary-card span {
  color: var(--text-soft);
  font-size: 13px;
}

.summary-card-head h4 {
  margin: 8px 0 0;
  font-size: 22px;
}

.summary-card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.summary-tag {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(79, 70, 229, 0.08);
  color: var(--primary);
  font-size: 12px;
  font-weight: 600;
}

.summary-tag.risk {
  background: rgba(248, 113, 113, 0.12);
  color: #dc2626;
}

.summary-tag.suggestion {
  background: rgba(134, 239, 172, 0.18);
  color: #15803d;
}

.summary-points {
  margin: 16px 0 0;
  padding-left: 18px;
  color: var(--text-main);
  line-height: 1.9;
}

.insight-view-switcher {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.insight-view-button {
  padding: 9px 14px;
  border: 1px solid rgba(191, 219, 254, 0.44);
  border-radius: 999px;
  background: #ffffff;
  color: var(--text-soft);
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease,
    color 0.18s ease;
}

.insight-view-button.active {
  border-color: rgba(79, 70, 229, 0.44);
  background: rgba(79, 70, 229, 0.08);
  color: var(--primary);
}

.insight-metrics-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.insight-metric-card {
  padding: 18px 20px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.04);
}

.insight-metric-card span {
  color: var(--text-soft);
  font-size: 13px;
}

.insight-metric-card strong {
  display: block;
  margin-top: 10px;
  font-size: 28px;
  line-height: 1.1;
}

.insight-section-grid {
  display: grid;
  gap: 14px;
  margin-top: 16px;
}

.insight-section-grid.compact {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.insight-section-card {
  padding: 20px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.04);
}

.insight-section-card h4 {
  margin: 0;
  font-size: 17px;
}

.insight-section-card ul {
  margin: 14px 0 0;
  padding-left: 18px;
  color: var(--text-main);
  line-height: 1.9;
}

.insight-section-card.risk {
  border-color: rgba(248, 113, 113, 0.32);
}

.insight-section-card.suggestion {
  border-color: rgba(134, 239, 172, 0.42);
}

.insight-detail-panel {
  margin-top: 16px;
  padding: 20px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.04);
}

.insight-detail-panel h4 {
  margin: 0;
  font-size: 18px;
}

.insight-detail-panel ul {
  margin: 14px 0 0;
  padding-left: 18px;
  line-height: 1.9;
}

.insight-detail-panel.risks {
  border-color: rgba(248, 113, 113, 0.32);
}

.insight-detail-panel.suggestions {
  border-color: rgba(134, 239, 172, 0.42);
}

.chat-shell {
  display: grid;
  gap: 16px;
}

.chat-scroll {
  min-height: 280px;
  max-height: 420px;
  padding-right: 6px;
  overflow: auto;
}

.chat-bubble {
  max-width: 92%;
  padding: 16px 18px;
  border-radius: 18px;
  line-height: 1.8;
}

.chat-bubble.ai {
  background: rgba(241, 245, 249, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.chat-bubble.user {
  background: rgba(224, 231, 255, 0.88);
  border: 1px solid rgba(191, 219, 254, 0.7);
  margin-left: auto;
}

.chat-role {
  display: inline-block;
  margin-bottom: 8px;
  color: var(--text-soft);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.chat-bubble p {
  margin: 0;
  white-space: pre-wrap;
}

.chat-composer {
  display: grid;
  gap: 14px;
  margin-top: 4px;
}

.chat-fab {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 30;
  padding: 14px 20px;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #4f46e5, #2563eb);
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 18px 36px rgba(79, 70, 229, 0.22);
  cursor: pointer;
}

.chat-fab.open {
  background: linear-gradient(135deg, #1e293b, #334155);
}

.chat-float-panel {
  position: fixed;
  right: 28px;
  bottom: 88px;
  z-index: 29;
  display: grid;
  gap: 16px;
  width: min(420px, calc(100vw - 32px));
  padding: 18px;
  border: 1px solid rgba(191, 219, 254, 0.5);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.97);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(18px);
}

.chat-float-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.chat-float-head strong,
.chat-float-head span {
  display: block;
}

.chat-float-head strong {
  font-size: 18px;
}

.chat-float-head span {
  margin-top: 4px;
  color: var(--text-soft);
  font-size: 12px;
}

.chat-close {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-soft);
  font-size: 13px;
  cursor: pointer;
}

.chat-float-enter-active,
.chat-float-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.chat-float-enter-from,
.chat-float-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

@media (max-width: 1280px) {
  .ai-workspace {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .ai-hero {
    padding: 22px 24px;
  }

  .insight-metrics-grid {
    grid-template-columns: 1fr;
  }

  .summary-card-head,
  .summary-card-tags {
    justify-content: flex-start;
  }

  .insight-section-grid.compact {
    grid-template-columns: 1fr;
  }

  .chat-scroll {
    min-height: 280px;
  }

  .chat-fab {
    right: 16px;
    bottom: 16px;
  }

  .chat-float-panel {
    right: 16px;
    bottom: 76px;
    width: calc(100vw - 32px);
  }
}
</style>
