<template>
  <div class="page-grid">
    <section class="module-hero">
      <div>
        <h2>AI 经营助手</h2>
      </div>
      <div class="hero-kpis">
        <div class="hero-kpi"><span>接口状态</span><strong>{{ connected ? "已接通" : "未验证" }}</strong></div>
        <div class="hero-kpi"><span>会话条数</span><strong>{{ messages.length }}</strong></div>
        <div class="hero-kpi"><span>推荐场景</span><strong>报表解读</strong></div>
        <div class="hero-kpi"><span>写操作权限</span><strong>关闭</strong></div>
      </div>
    </section>

    <div class="content-grid">
      <PagePanel title="报表自动解读" description="读取销售、采购、库存、生产与往来款真实汇总后，生成结构化经营结论">
        <div class="insight-toolbar">
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
            placeholder="可补充分析角度，例如：重点关注库存和延期交付风险"
            clearable
          />
          <div class="toolbar-actions">
            <el-button @click="applyInsightExample('请重点解读库存短缺和交付风险')">库存与交付</el-button>
            <el-button @click="applyInsightExample('请从销售兑现、采购到货和资金占用三个角度分析')">经营概览</el-button>
            <el-button type="primary" :loading="insightLoading" @click="loadInsight">生成解读</el-button>
          </div>
        </div>

        <template v-if="insightData">
          <div class="insight-summary-card">
            <span>{{ insightData.periodLabel }}</span>
            <p>{{ insightData.summary }}</p>
          </div>

          <div class="metric-grid insight-metrics">
            <div class="hero-kpi">
              <span>销售额</span>
              <strong>{{ formatMoney(insightData.snapshot?.salesAmount) }}</strong>
            </div>
            <div class="hero-kpi">
              <span>采购额</span>
              <strong>{{ formatMoney(insightData.snapshot?.purchaseAmount) }}</strong>
            </div>
            <div class="hero-kpi">
              <span>库存预警</span>
              <strong>{{ insightData.snapshot?.inventoryWarningCount ?? 0 }} 项</strong>
            </div>
            <div class="hero-kpi">
              <span>延期风险工单</span>
              <strong>{{ insightData.snapshot?.delayRiskCount ?? 0 }} 张</strong>
            </div>
          </div>

          <div class="insight-grid">
            <div class="list-stack">
              <div class="list-item" v-for="item in insightData.highlights || []" :key="`highlight-${item}`">
                <h4>经营亮点</h4>
                <p>{{ item }}</p>
              </div>
            </div>
            <div class="list-stack">
              <div class="list-item risk-item" v-for="item in insightData.risks || []" :key="`risk-${item}`">
                <h4>风险提示</h4>
                <p>{{ item }}</p>
              </div>
            </div>
            <div class="list-stack">
              <div class="list-item suggestion-item" v-for="item in insightData.suggestions || []" :key="`suggestion-${item}`">
                <h4>执行建议</h4>
                <p>{{ item }}</p>
              </div>
            </div>
          </div>
        </template>
      </PagePanel>

      <PagePanel title="智能对话" description="问题发送后由后端返回标准问答结果">
        <div class="ai-chat">
          <div
            v-for="(message, index) in messages"
            :key="`${message.role}-${index}`"
            class="chat-bubble"
            :class="message.role"
          >
            {{ message.content }}
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
      </PagePanel>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
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
import { formatDate, formatMoney, formatNumber, formatPercent, formatStatusLabel } from "@/utils/format";

const question = ref("");
const sending = ref(false);
const insightLoading = ref(false);
const connected = ref(false);
const insightData = ref(null);
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
  const [inventorySummary, stockPage] = await Promise.all([
    fetchInventorySummary(buildScopedParams()),
    fetchInventoryStocks({ pageNo: 1, pageSize: 200 })
  ]);
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
  const [workOrderPage, planPage] = await Promise.all([
    fetchProductionWorkOrders({ pageNo: 1, pageSize: 200 }),
    fetchProductionPlans({ pageNo: 1, pageSize: 200 })
  ]);
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
      const isDelayed =
        endDate &&
        endDate < startOfToday &&
        progressRate < 1 &&
        !["completed", "closed"].includes(normalizeText(item.status));
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
    const response = structuredAnswer
      ? { answer: structuredAnswer }
      : await chatWithAi({ question: currentQuestion, ...buildScopedParams() });
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
.insight-toolbar {
  display: grid;
  gap: 14px;
}

.insight-summary-card {
  margin-top: 18px;
  padding: 18px 20px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(188, 92, 50, 0.12), rgba(23, 76, 85, 0.08));
  border: 1px solid rgba(188, 92, 50, 0.15);
}

.insight-summary-card span {
  color: var(--text-soft);
  font-size: 13px;
}

.insight-summary-card p {
  margin: 10px 0 0;
  line-height: 1.8;
}

.insight-metrics {
  margin-top: 16px;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.risk-item {
  border-left: 3px solid #bc5c32;
}

.suggestion-item {
  border-left: 3px solid #7a8e35;
}

.chat-composer {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

@media (max-width: 1280px) {
  .insight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
