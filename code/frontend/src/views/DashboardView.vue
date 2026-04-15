<template>
  <div class="page-grid dashboard-grid" v-loading="loading">
    <PagePanel title="经营筛选" description="首页看板统一联动销售、采购、库存和生产执行摘要">
      <template #actions>
        <div class="toolbar-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="loadData">刷新看板</el-button>
          <el-button type="primary" plain @click="goToReports">查看完整报表</el-button>
        </div>
      </template>

      <div class="filter-grid dashboard-filter-grid">
        <el-date-picker
          v-model="filters.dateRange"
          type="daterange"
          unlink-panels
          clearable
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
        <el-select v-model="filters.workOrderFocus" placeholder="工单焦点">
          <el-option label="全部工单" value="all" />
          <el-option label="延期风险" value="delay" />
          <el-option label="执行中" value="in_progress" />
          <el-option label="已完工" value="completed" />
        </el-select>
        <div class="range-hint">
          <strong>{{ currentRangeLabel }}</strong>
        </div>
      </div>
    </PagePanel>

    <section class="metric-grid dashboard-metric-grid">
      <MetricCard v-for="card in summaryCards" :key="card.title" v-bind="card" />
    </section>

    <PagePanel title="异常指标焦点" description="补充采购兑现、销售交付和生产履约等答辩更容易讲清楚的异常指标">
      <div class="summary-grid dashboard-insight-grid">
        <div class="summary-card" v-for="item in operationalAlerts" :key="item.label">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
          <el-button text type="primary" @click="applyAlertInsight(item.key)">{{ item.actionText }}</el-button>
        </div>
      </div>
    </PagePanel>

    <div class="grid-2">
      <PagePanel title="销售与采购趋势" description="汇总接口按月份返回趋势序列">
        <template #actions>
          <el-button text @click="goToReports">进入统计分析</el-button>
        </template>
        <EChartPanel :option="trendOption" height="260px" @chart-click="handleTrendChartClick" />
      </PagePanel>

      <PagePanel title="执行与风险总览" description="库存预警、在制工单、延期风险和待执行量集中展示">
        <EChartPanel :option="riskOption" height="260px" @chart-click="handleRiskChartClick" />
      </PagePanel>
    </div>

    <PagePanel title="看板联动详情" description="点击趋势图、风险图后展示对应指标说明和来源明细">
      <template #actions>
        <el-button v-if="dashboardInsight.actionRoute" text type="primary" @click="navigateTo(dashboardInsight.actionRoute)">
          {{ dashboardInsight.actionText || "查看详情" }}
        </el-button>
      </template>
        <div class="stack-panel">
          <div class="insight-heading">
            <strong>{{ dashboardInsight.title }}</strong>
          </div>

        <div class="summary-grid dashboard-insight-grid">
          <div class="summary-card" v-for="item in dashboardInsight.metrics" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </div>
        </div>

        <el-table v-if="dashboardInsight.rows.length" :data="dashboardInsight.rows" stripe size="small" table-layout="fixed">
          <el-table-column
            v-for="column in dashboardInsight.columns"
            :key="column.prop"
            :prop="column.prop"
            :label="column.label"
            :min-width="column.minWidth || 120"
          >
            <template #default="{ row }">
              {{ formatDashboardInsightCell(row, column) }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else :description="dashboardInsight.emptyText" />
      </div>
    </PagePanel>

    <div class="grid-2">
      <PagePanel title="库存预警" description="按库存余额和安全库存比对，支持直接跳到库存页">
        <template #actions>
          <el-button text @click="goToInventory">查看库存余额</el-button>
        </template>
        <el-table :data="warningRows" stripe size="small" table-layout="fixed">
          <el-table-column prop="materialName" label="物料" min-width="160" />
          <el-table-column prop="warehouseName" label="仓库" min-width="120" />
          <el-table-column label="现存量" min-width="110">
            <template #default="{ row }">{{ formatNumber(row.qty) }}</template>
          </el-table-column>
          <el-table-column label="安全库存" min-width="110">
            <template #default="{ row }">{{ formatNumber(row.safetyStock) }}</template>
          </el-table-column>
          <el-table-column label="状态" min-width="100">
            <template #default="{ row }">
              <span class="table-tag" :class="row.levelClass">{{ row.level }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!warningRows.length" description="当前库存未发现明显短缺" />
      </PagePanel>

      <PagePanel title="生产执行焦点" description="按筛选聚合延期风险、执行中和已完工工单">
        <template #actions>
          <el-button text @click="goToProduction">查看全部工单</el-button>
        </template>
        <el-table :data="focusedWorkOrders" stripe size="small" table-layout="fixed">
          <el-table-column prop="code" label="工单号" min-width="150" />
          <el-table-column prop="materialName" label="产品" min-width="150" />
          <el-table-column label="进度" min-width="120">
            <template #default="{ row }">{{ formatPercent(row.progressRate, 1, "0.0%") }}</template>
          </el-table-column>
          <el-table-column label="完工日期" min-width="120">
            <template #default="{ row }">{{ formatDate(row.endDate) }}</template>
          </el-table-column>
          <el-table-column label="状态" min-width="110">
            <template #default="{ row }">
              <span class="table-tag" :class="row.statusClass">{{ row.statusText }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="120" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openWorkOrder(row.id)">查看工单</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!focusedWorkOrders.length" description="当前焦点筛选下暂无工单" />
      </PagePanel>
    </div>

    <div class="grid-2">
      <PagePanel title="生产计划链路" description="集中展示销售驱动计划和待下推工单计划">
        <template #actions>
          <el-button text @click="goToProduction">进入生产管理</el-button>
        </template>
        <div class="stack-panel">
          <div class="mini-section">
            <div class="mini-head">
              <strong>销售驱动计划</strong>
              <span>{{ salesDrivenPlanRows.length }} 条</span>
            </div>
            <el-table :data="salesDrivenPlanRows" stripe size="small" table-layout="fixed">
              <el-table-column prop="code" label="计划单号" min-width="150" />
              <el-table-column prop="materialName" label="产品" min-width="140" />
              <el-table-column label="计划完工" min-width="110">
                <template #default="{ row }">{{ formatDate(row.endDate) }}</template>
              </el-table-column>
              <el-table-column label="来源销售单" min-width="110">
                <template #default="{ row }">
                  <el-button text type="primary" @click="openSalesOrder(row.sourceSalesId)">#{{ row.sourceSalesId }}</el-button>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="110" fixed="right">
                <template #default="{ row }">
                  <el-button text type="primary" @click="openPlanDetail(row.id)">查看计划</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!salesDrivenPlanRows.length" description="当前暂无销售驱动计划" />
          </div>

          <div class="mini-section">
            <div class="mini-head">
              <strong>待下推工单计划</strong>
              <span>{{ pendingPlanRows.length }} 条</span>
            </div>
            <el-table :data="pendingPlanRows" stripe size="small" table-layout="fixed">
              <el-table-column prop="code" label="计划单号" min-width="150" />
              <el-table-column prop="materialName" label="产品" min-width="140" />
              <el-table-column label="计划数量" min-width="100">
                <template #default="{ row }">{{ formatNumber(row.planQty) }}</template>
              </el-table-column>
              <el-table-column label="状态" min-width="100">
                <template #default="{ row }">
                  <span class="table-tag" :class="getTagClass(row.status)">{{ formatStatusLabel(row.status) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="110" fixed="right">
                <template #default="{ row }">
                  <el-button text type="primary" @click="openPlanDetail(row.id)">查看计划</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!pendingPlanRows.length" description="当前无待下推工单计划" />
          </div>

          <div class="mini-section">
            <div class="mini-head">
              <strong>MRP 建议摘要</strong>
              <span v-if="mrpResult">采购 {{ mrpPurchaseRows.length }} / 工单 {{ mrpWorkOrderRows.length }}</span>
              <span v-else>待计算</span>
            </div>
            <div v-loading="mrpLoading" class="mrp-dashboard-block">
              <template v-if="mrpResult">
                <div class="capital-grid mrp-metric-grid">
                  <div class="capital-card">
                    <span>MRP 任务号</span>
                    <strong>{{ mrpResult.taskKey }}</strong>
                  </div>
                  <div class="capital-card">
                    <span>建议动作</span>
                    <strong>{{ mrpPurchaseRows.length + mrpWorkOrderRows.length }} 条</strong>
                  </div>
                </div>

                <div class="mini-head">
                  <strong>采购建议</strong>
                  <el-button text type="primary" @click="goToProduction">去生产模块执行</el-button>
                </div>
                <el-table :data="mrpPurchaseRows" stripe size="small" table-layout="fixed">
                  <el-table-column prop="materialCode" label="物料编码" min-width="110" />
                  <el-table-column prop="materialName" label="物料" min-width="130" />
                  <el-table-column label="缺口数量" min-width="100">
                    <template #default="{ row }">{{ formatNumber(row.shortageQty) }}</template>
                  </el-table-column>
                  <el-table-column label="需求日期" min-width="100">
                    <template #default="{ row }">{{ formatDate(row.needDate) }}</template>
                  </el-table-column>
                </el-table>
                <el-empty v-if="!mrpPurchaseRows.length" description="当前暂无采购建议" />

                <div class="mini-head">
                  <strong>工单建议</strong>
                  <el-button text type="primary" @click="goToProduction">查看完整建议</el-button>
                </div>
                <el-table :data="mrpWorkOrderRows" stripe size="small" table-layout="fixed">
                  <el-table-column prop="materialCode" label="产品编码" min-width="110" />
                  <el-table-column prop="materialName" label="产品" min-width="130" />
                  <el-table-column label="计划数量" min-width="100">
                    <template #default="{ row }">{{ formatNumber(row.planQty) }}</template>
                  </el-table-column>
                  <el-table-column label="完工日期" min-width="100">
                    <template #default="{ row }">{{ formatDate(row.endDate) }}</template>
                  </el-table-column>
                </el-table>
                <el-empty v-if="!mrpWorkOrderRows.length" description="当前暂无工单建议" />
              </template>
              <el-empty v-else description="当前没有可参与 MRP 的待下推计划" />
            </div>
          </div>
        </div>
      </PagePanel>

      <PagePanel title="近期报工与资金关注" description="最近报工记录和往来款排行集中展示">
        <template #actions>
          <el-button text @click="goToReports">查看应收应付</el-button>
        </template>

        <div class="stack-panel">
          <div class="mini-section">
            <div class="mini-head">
              <strong>近期报工</strong>
              <span>{{ reportRows.length }} 条</span>
            </div>
            <el-table :data="reportRows" stripe size="small" table-layout="fixed">
              <el-table-column prop="workOrderCode" label="工单号" min-width="140" />
              <el-table-column prop="processName" label="工序" min-width="120" />
              <el-table-column label="报工数量" min-width="100">
                <template #default="{ row }">{{ formatNumber(row.reportQty) }}</template>
              </el-table-column>
              <el-table-column label="时间" min-width="160">
                <template #default="{ row }">{{ formatDateTime(row.reportDate) }}</template>
              </el-table-column>
              <el-table-column label="操作" min-width="110" fixed="right">
                <template #default="{ row }">
                  <el-button text type="primary" @click="openWorkOrder(row.workOrderId)">查看工单</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="mini-section">
            <div class="mini-head">
              <strong>资金关注</strong>
              <span>应收 / 应付 Top 3</span>
            </div>
            <div class="capital-grid">
              <div class="capital-card">
                <span>应收最高</span>
                <strong>{{ topArItem?.customer || "--" }}</strong>
                <small>{{ topArItem ? `${formatMoney(topArItem.amount)} 元` : "暂无数据" }}</small>
                <el-button v-if="topArItem?.sourceOrderId" text type="primary" @click="openSalesOrder(topArItem.sourceOrderId)">
                  查看销售订单
                </el-button>
              </div>
              <div class="capital-card">
                <span>应付最高</span>
                <strong>{{ topApItem?.supplier || "--" }}</strong>
                <small>{{ topApItem ? `${formatMoney(topApItem.amount)} 元` : "暂无数据" }}</small>
                <el-button v-if="topApItem?.sourceOrderId" text type="primary" @click="openPurchaseOrder(topApItem.sourceOrderId)">
                  查看采购订单
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </PagePanel>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";

import {
  fetchApSummary,
  fetchArSummary,
  calculateProductionMrp,
  fetchInventoryStocks,
  fetchInventorySummary,
  fetchMaterialList,
  fetchProductionPlans,
  fetchProductionReports,
  fetchProductionSummary,
  fetchProductionWorkOrders,
  fetchPurchaseSummary,
  fetchSalesSummary
} from "@/api/modules";
import EChartPanel from "@/components/EChartPanel.vue";
import MetricCard from "@/components/MetricCard.vue";
import PagePanel from "@/components/PagePanel.vue";
import {
  formatDate,
  formatDateTime,
  formatMoney,
  formatNumber,
  formatPercent,
  formatStatusLabel,
  getTagClass
} from "@/utils/format";

const router = useRouter();
const loading = ref(false);
const salesSummary = ref({});
const purchaseSummary = ref({});
const inventorySummary = ref({});
const productionSummary = ref({});
const arSummary = ref([]);
const apSummary = ref([]);
const materials = ref([]);
const stocks = ref([]);
const plans = ref([]);
const workOrders = ref([]);
const reports = ref([]);
const mrpResult = ref(null);
const mrpLoading = ref(false);
const dashboardInsight = reactive(createEmptyDashboardInsight());

const filters = reactive({
  dateRange: [],
  workOrderFocus: "delay"
});

const currentRangeLabel = computed(() => {
  if (!filters.dateRange?.length) {
    return "最近经营概览";
  }
  return `${filters.dateRange[0]} 至 ${filters.dateRange[1]}`;
});

const summaryCards = computed(() => [
  {
    title: salesSummary.value.summaryLabel || "本月销售额",
    value: formatMoney(salesSummary.value.summaryAmount ?? salesSummary.value.currentMonthSales, "0.00"),
    unit: "元",
    trend: `待发货 ${formatNumber(salesSummary.value.pendingShipmentQty, "0")} / 发货率 ${formatPercent(salesSummary.value.shipmentRate)}`,
    tone: "sunrise"
  },
  {
    title: purchaseSummary.value.summaryLabel || "本月采购额",
    value: formatMoney(purchaseSummary.value.summaryAmount ?? purchaseSummary.value.currentMonthPurchase, "0.00"),
    unit: "元",
    trend: `待到货 ${formatNumber(purchaseSummary.value.pendingReceiveQty, "0")} / 到货率 ${formatPercent(purchaseSummary.value.deliveryRate)}`,
    tone: "steel"
  },
  {
    title: inventorySummary.value.summaryLabel || "库存快照",
    value: formatMoney(inventorySummary.value.inventoryAmount, "0.00"),
    unit: "元",
    trend: `预警 ${formatNumber(inventorySummary.value.warningCount, "0")} 项 / 周转 ${formatNumber(inventorySummary.value.turnoverDays, "0")} 天`,
    tone: "forest"
  },
  {
    title: productionSummary.value.summaryLabel || "生产执行概览",
    value: formatPercent(productionSummary.value.onTimeRate, 1, "0.0%"),
    unit: "",
    trend: `在制 ${formatNumber(productionSummary.value.inProgressCount, "0")} / 延期风险 ${formatNumber(productionSummary.value.delayRiskCount, "0")} 项`,
    tone: "ember"
  },
  {
    title: "待下推工单",
    value: formatNumber(pendingPlanRows.value.length, "0"),
    unit: "项",
    trend: "已审核但尚未生成工单的生产计划",
    tone: "steel"
  },
  {
    title: "销售拉动计划",
    value: formatNumber(salesDrivenPlanRows.value.length, "0"),
    unit: "项",
    trend: "可回溯到来源销售订单的生产计划",
    tone: "sunrise"
  }
]);

const trendLabels = computed(() => {
  const salesLabels = salesSummary.value.monthLabels || [];
  const purchaseLabels = purchaseSummary.value.monthLabels || [];
  return salesLabels.length >= purchaseLabels.length ? salesLabels : purchaseLabels;
});

function alignSeries(targetLabels, sourceLabels, sourceSeries) {
  const valueMap = new Map((sourceLabels || []).map((label, index) => [label, Number(sourceSeries?.[index] || 0)]));
  return (targetLabels || []).map((label) => valueMap.get(label) || 0);
}

const trendOption = computed(() => ({
  tooltip: { trigger: "axis" },
  legend: { bottom: 0, textStyle: { color: "#5b6678" } },
  grid: { left: 10, right: 10, bottom: 36, top: 20, containLabel: true },
  xAxis: {
    type: "category",
    data: trendLabels.value,
    axisLine: { lineStyle: { color: "rgba(89, 102, 120, 0.25)" } }
  },
  yAxis: {
    type: "value",
    splitLine: { lineStyle: { color: "rgba(89, 102, 120, 0.12)" } }
  },
  series: [
    {
      name: "销售额",
      type: "line",
      smooth: true,
      symbolSize: 8,
      data: alignSeries(trendLabels.value, salesSummary.value.monthLabels, salesSummary.value.series),
      lineStyle: { width: 4, color: "#bc5c32" },
      itemStyle: { color: "#bc5c32" },
      areaStyle: { color: "rgba(188, 92, 50, 0.12)" }
    },
    {
      name: "采购额",
      type: "line",
      smooth: true,
      symbolSize: 8,
      data: alignSeries(trendLabels.value, purchaseSummary.value.monthLabels, purchaseSummary.value.series),
      lineStyle: { width: 3, color: "#174c55" },
      itemStyle: { color: "#174c55" }
    }
  ]
}));

const riskOption = computed(() => ({
  tooltip: { trigger: "axis" },
  grid: { left: 10, right: 10, bottom: 10, top: 20, containLabel: true },
  xAxis: {
    type: "category",
    data: ["库存预警", "在制工单", "延期风险", "待发货", "待到货"]
  },
  yAxis: {
    type: "value",
    splitLine: { lineStyle: { color: "rgba(89, 102, 120, 0.12)" } }
  },
  series: [
    {
      type: "bar",
      barWidth: 28,
      data: [
        Number(inventorySummary.value.warningCount || 0),
        Number(productionSummary.value.inProgressCount || 0),
        Number(productionSummary.value.delayRiskCount || 0),
        Number(salesSummary.value.pendingShipmentQty || 0),
        Number(purchaseSummary.value.pendingReceiveQty || 0)
      ],
      itemStyle: {
        color: (params) => ["#d39c44", "#174c55", "#bc5c32", "#7a8e35", "#4f6980"][params.dataIndex],
        borderRadius: [10, 10, 0, 0]
      }
    }
  ]
}));

const warningRows = computed(() => {
  const materialMap = new Map(materials.value.map((item) => [item.id, item]));

  return stocks.value
    .map((stock) => {
      const material = materialMap.get(stock.materialId);
      if (!material || material.safetyStock === null || material.safetyStock === undefined) {
        return null;
      }

      const currentQty = Number(stock.qty || 0);
      const safetyStock = Number(material.safetyStock || 0);

      if (currentQty >= safetyStock || safetyStock <= 0) {
        return null;
      }

      const ratio = currentQty / safetyStock;
      return {
        ...stock,
        safetyStock,
        level: ratio < 0.5 ? "紧急" : "关注",
        levelClass: ratio < 0.5 ? "tag-danger" : "tag-warning"
      };
    })
    .filter(Boolean)
    .sort((left, right) => left.qty - right.qty)
    .slice(0, 6);
});

function normalizeStatus(status) {
  return String(status || "").toLowerCase();
}

function resolveProgressRate(row) {
  const planQty = Number(row.planQty || 0);
  const finishedQty = Number(row.finishedQty || 0);
  if (planQty <= 0) {
    return 0;
  }
  return Math.min(finishedQty / planQty, 1);
}

function mapWorkOrderStatus(row) {
  const status = normalizeStatus(row.status);
  const today = new Date();
  const endDate = row.endDate ? new Date(row.endDate) : null;
  const progressRate = resolveProgressRate(row);

  if (endDate && endDate < new Date(today.getFullYear(), today.getMonth(), today.getDate()) && progressRate < 1) {
    return { statusText: "延期风险", statusClass: "tag-danger" };
  }
  if (status === "completed" || progressRate >= 1) {
    return { statusText: "已完工", statusClass: "tag-success" };
  }
  if (["approved", "in_progress"].includes(status)) {
    return { statusText: "执行中", statusClass: "tag-warning" };
  }
  return { statusText: formatStatusLabel(row.status), statusClass: getTagClass(row.status) };
}

const focusedWorkOrders = computed(() => {
  const today = new Date();
  return workOrders.value
    .map((item) => {
      const progressRate = resolveProgressRate(item);
      const { statusText, statusClass } = mapWorkOrderStatus(item);
      const endDate = item.endDate ? new Date(item.endDate) : null;
      const isDelay = !!endDate && endDate < new Date(today.getFullYear(), today.getMonth(), today.getDate()) && progressRate < 1;
      return {
        ...item,
        progressRate,
        isDelay,
        statusText,
        statusClass
      };
    })
    .filter((item) => {
      if (filters.workOrderFocus === "delay") {
        return item.isDelay;
      }
      if (filters.workOrderFocus === "in_progress") {
        return item.statusText === "执行中";
      }
      if (filters.workOrderFocus === "completed") {
        return item.statusText === "已完工";
      }
      return true;
    })
    .sort((left, right) => {
      if (left.isDelay !== right.isDelay) {
        return left.isDelay ? -1 : 1;
      }
      return Number(new Date(left.endDate || 0)) - Number(new Date(right.endDate || 0));
    })
    .slice(0, 6);
});

const workOrderPlanIds = computed(() => new Set(workOrders.value.map((item) => Number(item.planId)).filter(Boolean)));
const salesDrivenPlanRows = computed(() =>
  plans.value.filter((item) => Number(item.sourceSalesId)).slice(0, 6)
);
const pendingPlanRows = computed(() =>
  plans.value
    .filter((item) => ["approved", "in_progress"].includes(normalizeStatus(item.status)))
    .filter((item) => !workOrderPlanIds.value.has(Number(item.id)))
    .slice(0, 6)
);
const reportRows = computed(() => (reports.value || []).slice(0, 6));
const topArItem = computed(() => arSummary.value[0] || null);
const topApItem = computed(() => apSummary.value[0] || null);
const mrpPurchaseRows = computed(() => (mrpResult.value?.purchaseSuggestions || []).slice(0, 4));
const mrpWorkOrderRows = computed(() => (mrpResult.value?.workOrderSuggestions || []).slice(0, 4));
const shipmentRatePercent = computed(() => Number(salesSummary.value.shipmentRate || 0) * 100);
const deliveryRatePercent = computed(() => Number(purchaseSummary.value.deliveryRate || 0) * 100);
const onTimeRatePercent = computed(() => Number(productionSummary.value.onTimeRate || 0) * 100);
const operationalAlerts = computed(() => [
  {
    key: "delivery",
    label: "采购兑现异常",
    value: deliveryRatePercent.value < 80 ? formatPercent(deliveryRatePercent.value, 1, "0.0%") : "正常",
    hint:
      deliveryRatePercent.value < 80
        ? `到货率偏低，待到货 ${formatNumber(purchaseSummary.value.pendingReceiveQty, "0")} 项`
        : `到货率 ${formatPercent(deliveryRatePercent.value, 1, "0.0%")} ，采购兑现稳定`,
    actionText: "查看采购说明"
  },
  {
    key: "shipment",
    label: "销售交付异常",
    value: shipmentRatePercent.value < 80 ? formatPercent(shipmentRatePercent.value, 1, "0.0%") : "正常",
    hint:
      shipmentRatePercent.value < 80
        ? `发货率偏低，待发货 ${formatNumber(salesSummary.value.pendingShipmentQty, "0")} 项`
        : `发货率 ${formatPercent(shipmentRatePercent.value, 1, "0.0%")} ，交付较稳定`,
    actionText: "查看交付说明"
  },
  {
    key: "production",
    label: "生产履约异常",
    value: Number(productionSummary.value.delayRiskCount || 0) > 0 ? `${formatNumber(productionSummary.value.delayRiskCount, "0")} 项` : "正常",
    hint:
      Number(productionSummary.value.delayRiskCount || 0) > 0
        ? `准时完工率 ${formatPercent(onTimeRatePercent.value, 1, "0.0%")} ，需优先关注延期工单`
        : `准时完工率 ${formatPercent(onTimeRatePercent.value, 1, "0.0%")} ，生产履约正常`,
    actionText: "查看生产说明"
  }
]);

function createEmptyDashboardInsight() {
  return {
    title: "经营联动概览",
    description: "默认聚焦当前最值得关注的异常或执行明细。",
    metrics: [],
    rows: [],
    columns: [],
    actionText: "",
    actionRoute: null,
    emptyText: "当前暂无可展示的联动明细"
  };
}

function setDashboardInsight(payload) {
  Object.assign(dashboardInsight, createEmptyDashboardInsight(), payload);
}

function resolvePendingMrpPlanIds(planRows, workOrderRows) {
  const linkedPlanIds = new Set((workOrderRows || []).map((item) => Number(item.planId)).filter(Boolean));
  return (planRows || [])
    .filter((item) => ["approved", "in_progress"].includes(normalizeStatus(item.status)))
    .filter((item) => !linkedPlanIds.has(Number(item.id)))
    .map((item) => Number(item.id))
    .filter(Boolean)
    .slice(0, 6);
}

function buildReportParams() {
  return {
    startDate: filters.dateRange?.[0] || undefined,
    endDate: filters.dateRange?.[1] || undefined
  };
}

async function loadData() {
  loading.value = true;

  try {
    const reportParams = buildReportParams();
    const [
      salesData,
      purchaseData,
      inventoryData,
      productionData,
      arData,
      apData,
      materialPage,
      stockPage,
      planPage,
      workOrderPage,
      reportPage
    ] = await Promise.all([
      fetchSalesSummary(reportParams),
      fetchPurchaseSummary(reportParams),
      fetchInventorySummary(reportParams),
      fetchProductionSummary(reportParams),
      fetchArSummary({ ...reportParams, limit: 3 }),
      fetchApSummary({ ...reportParams, limit: 3 }),
      fetchMaterialList({ pageNo: 1, pageSize: 200 }),
      fetchInventoryStocks({ pageNo: 1, pageSize: 200 }),
      fetchProductionPlans({ pageNo: 1, pageSize: 8 }),
      fetchProductionWorkOrders({ pageNo: 1, pageSize: 20 }),
      fetchProductionReports({ pageNo: 1, pageSize: 8 })
    ]);

    salesSummary.value = salesData || {};
    purchaseSummary.value = purchaseData || {};
    inventorySummary.value = inventoryData || {};
    productionSummary.value = productionData || {};
    arSummary.value = arData || [];
    apSummary.value = apData || [];
    materials.value = materialPage.records || [];
    stocks.value = stockPage.records || [];
    plans.value = planPage.records || [];
    workOrders.value = workOrderPage.records || [];
    reports.value = reportPage.records || [];
    syncDefaultDashboardInsight();
    await loadMrpSuggestions(planPage.records || [], workOrderPage.records || []);
  } catch (error) {
    ElMessage.error(error.message || "经营看板加载失败");
  } finally {
    loading.value = false;
  }
}

async function loadMrpSuggestions(planRows, workOrderRows) {
  const planIds = resolvePendingMrpPlanIds(planRows, workOrderRows);
  if (!planIds.length) {
    mrpResult.value = null;
    return;
  }

  mrpLoading.value = true;
  try {
    mrpResult.value = await calculateProductionMrp({ planIds });
  } catch (error) {
    mrpResult.value = null;
    ElMessage.warning(error.message || "首页 MRP 建议计算失败");
  } finally {
    mrpLoading.value = false;
  }
}

function resetFilters() {
  filters.dateRange = [];
  filters.workOrderFocus = "delay";
  loadData();
}

function syncDefaultDashboardInsight() {
  if (focusedWorkOrders.value.length) {
    setDashboardInsight({
      title: "生产执行焦点",
      description: "优先展示延期风险和执行中的关键工单，方便首页直接说明生产履约情况。",
      metrics: [
        {
          label: "延期风险",
          value: `${formatNumber(productionSummary.value.delayRiskCount, "0")} 项`,
          hint: "已逾计划完工日期且尚未完工"
        },
        {
          label: "在制工单",
          value: `${formatNumber(productionSummary.value.inProgressCount, "0")} 项`,
          hint: "当前仍在生产执行中的工单"
        },
        {
          label: "准时完工率",
          value: formatPercent(productionSummary.value.onTimeRate, 1, "0.0%"),
          hint: currentRangeLabel.value
        }
      ],
      rows: focusedWorkOrders.value.slice(0, 5),
      columns: [
        { prop: "code", label: "工单号", minWidth: 150 },
        { prop: "materialName", label: "产品", minWidth: 140 },
        { prop: "progressRate", label: "进度", minWidth: 100, type: "percent" },
        { prop: "endDate", label: "计划完工", minWidth: 110, type: "date" },
        { prop: "statusText", label: "执行判断", minWidth: 110 }
      ],
      actionText: "查看全部工单",
      actionRoute: { name: "production" }
    });
    return;
  }

  if (warningRows.value.length) {
    setDashboardInsight({
      title: "库存预警焦点",
      description: "当前没有明显延期工单时，默认展示最需要补料关注的低库存明细。",
      metrics: [
        {
          label: "预警物料",
          value: `${formatNumber(inventorySummary.value.warningCount, "0")} 项`,
          hint: "低于安全库存的物料项"
        },
        {
          label: "库存金额",
          value: `${formatMoney(inventorySummary.value.inventoryAmount, "0.00")} 元`,
          hint: "当前库存资产快照"
        }
      ],
      rows: warningRows.value.slice(0, 5),
      columns: [
        { prop: "materialName", label: "物料", minWidth: 150 },
        { prop: "warehouseName", label: "仓库", minWidth: 120 },
        { prop: "qty", label: "现存量", minWidth: 100, type: "number" },
        { prop: "safetyStock", label: "安全库存", minWidth: 100, type: "number" },
        { prop: "level", label: "状态", minWidth: 90 }
      ],
      actionText: "查看库存余额",
      actionRoute: { name: "inventory", query: { tab: "stocks" } }
    });
    return;
  }

  setDashboardInsight({
    title: "近期报工概览",
    description: "当前无明显风险时，展示最新报工记录作为看板说明素材。",
    metrics: [
      {
        label: "近期报工",
        value: `${formatNumber(reportRows.value.length, "0")} 条`,
        hint: "默认取最近报工记录"
      },
      {
        label: "销售拉动计划",
        value: `${formatNumber(salesDrivenPlanRows.value.length, "0")} 项`,
        hint: "可追溯到来源销售订单"
      }
    ],
    rows: reportRows.value.slice(0, 5),
    columns: [
      { prop: "workOrderCode", label: "工单号", minWidth: 140 },
      { prop: "processName", label: "工序", minWidth: 120 },
      { prop: "reportQty", label: "报工数量", minWidth: 100, type: "number" },
      { prop: "reportDate", label: "报工时间", minWidth: 160, type: "datetime" }
    ],
    actionText: "查看生产模块",
    actionRoute: { name: "production" }
  });
}

function handleTrendChartClick(params) {
  const label = String(params?.name || "");
  const labels = trendLabels.value || [];
  const salesSeries = alignSeries(labels, salesSummary.value.monthLabels, salesSummary.value.series);
  const purchaseSeries = alignSeries(labels, purchaseSummary.value.monthLabels, purchaseSummary.value.series);
  const monthIndex = labels.findIndex((item) => item === label);

  setDashboardInsight({
    title: `${label || "当前区间"} 销售与采购趋势`,
    description: "图表点击后，首页直接展示该时间点的金额对比和履约提示，便于答辩时快速口述经营变化。",
    metrics: [
      {
        label: "销售额",
        value: `${formatMoney(monthIndex >= 0 ? salesSeries[monthIndex] : 0, "0.00")} 元`,
        hint: "来自销售汇总趋势序列"
      },
      {
        label: "采购额",
        value: `${formatMoney(monthIndex >= 0 ? purchaseSeries[monthIndex] : 0, "0.00")} 元`,
        hint: "来自采购汇总趋势序列"
      },
      {
        label: "待发货",
        value: `${formatNumber(salesSummary.value.pendingShipmentQty, "0")} 项`,
        hint: "可结合销售订单继续查看"
      },
      {
        label: "待到货",
        value: `${formatNumber(purchaseSummary.value.pendingReceiveQty, "0")} 项`,
        hint: "可结合采购订单继续查看"
      }
    ],
    rows: [
      {
        metric: "销售额",
        amount: monthIndex >= 0 ? salesSeries[monthIndex] : 0,
        note: "销售趋势数据"
      },
      {
        metric: "采购额",
        amount: monthIndex >= 0 ? purchaseSeries[monthIndex] : 0,
        note: "采购趋势数据"
      }
    ],
    columns: [
      { prop: "metric", label: "指标", minWidth: 120 },
      { prop: "amount", label: "金额", minWidth: 120, type: "money" },
      { prop: "note", label: "说明", minWidth: 180 }
    ],
    actionText: "进入统计分析",
    actionRoute: { name: "reports" }
  });
}

function handleRiskChartClick(params) {
  const label = String(params?.name || "");

  if (label === "库存预警") {
    setDashboardInsight({
      title: "库存预警联动详情",
      description: "按低于安全库存的缺口优先级展示物料预警，适合直接说明补料压力。",
      metrics: [
        {
          label: "预警项",
          value: `${formatNumber(inventorySummary.value.warningCount, "0")} 项`,
          hint: "当前库存摘要接口返回"
        },
        {
          label: "周转天数",
          value: `${formatNumber(inventorySummary.value.turnoverDays, "0")} 天`,
          hint: "库存周转速度参考"
        }
      ],
      rows: warningRows.value.slice(0, 6),
      columns: [
        { prop: "materialName", label: "物料", minWidth: 150 },
        { prop: "warehouseName", label: "仓库", minWidth: 120 },
        { prop: "qty", label: "现存量", minWidth: 100, type: "number" },
        { prop: "safetyStock", label: "安全库存", minWidth: 100, type: "number" },
        { prop: "level", label: "级别", minWidth: 90 }
      ],
      actionText: "查看库存余额",
      actionRoute: { name: "inventory", query: { tab: "stocks" } },
      emptyText: "当前没有明显库存预警"
    });
    return;
  }

  if (label === "在制工单" || label === "延期风险") {
    const rows = focusedWorkOrders.value.filter((item) => (label === "延期风险" ? item.isDelay : item.statusText === "执行中"));
    setDashboardInsight({
      title: `${label}联动详情`,
      description: "展示首页聚合后的关键工单，可继续跳入生产模块查看进度、领料和完工入库。",
      metrics: [
        {
          label,
          value: `${formatNumber(label === "延期风险" ? productionSummary.value.delayRiskCount : productionSummary.value.inProgressCount, "0")} 项`,
          hint: currentRangeLabel.value
        },
        {
          label: "准时完工率",
          value: formatPercent(productionSummary.value.onTimeRate, 1, "0.0%"),
          hint: "生产汇总接口"
        }
      ],
      rows: rows.slice(0, 6),
      columns: [
        { prop: "code", label: "工单号", minWidth: 150 },
        { prop: "materialName", label: "产品", minWidth: 130 },
        { prop: "progressRate", label: "进度", minWidth: 100, type: "percent" },
        { prop: "endDate", label: "计划完工", minWidth: 110, type: "date" },
        { prop: "statusText", label: "状态", minWidth: 100 }
      ],
      actionText: "进入生产管理",
      actionRoute: { name: "production" },
      emptyText: `当前暂无${label}明细`
    });
    return;
  }

  const isSales = label === "待发货";
  const summary = isSales ? salesSummary.value : purchaseSummary.value;
  const focusRow = isSales ? topArItem.value : topApItem.value;
  setDashboardInsight({
    title: `${label}联动详情`,
    description: "结合资金关注卡片给出最值得继续追踪的来源订单，避免首页只停留在数字展示。",
    metrics: [
      {
        label,
        value: `${formatNumber(isSales ? salesSummary.value.pendingShipmentQty : purchaseSummary.value.pendingReceiveQty, "0")} 项`,
        hint: isSales ? "销售待发货数量" : "采购待到货数量"
      },
      {
        label: isSales ? "发货率" : "到货率",
        value: formatPercent(isSales ? summary.shipmentRate : summary.deliveryRate, 1, "0.0%"),
        hint: currentRangeLabel.value
      }
    ],
    rows: focusRow
      ? [
          {
            partner: isSales ? focusRow.customer : focusRow.supplier,
            orderCode: focusRow.sourceOrderCode || "--",
            amount: focusRow.amount,
            docDate: focusRow.sourceDocDate
          }
        ]
      : [],
    columns: [
      { prop: "partner", label: isSales ? "客户" : "供应商", minWidth: 140 },
      { prop: "orderCode", label: "来源订单", minWidth: 140 },
      { prop: "amount", label: "金额", minWidth: 120, type: "money" },
      { prop: "docDate", label: "单据日期", minWidth: 110, type: "date" }
    ],
    actionText: isSales ? "查看销售订单" : "查看采购订单",
    actionRoute: isSales ? { name: "sales", query: { tab: "orders" } } : { name: "purchase", query: { tab: "orders" } },
    emptyText: `当前暂无${label}来源单据`
  });
}

function applyAlertInsight(key) {
  if (key === "delivery") {
    handleRiskChartClick({ name: "待到货" });
    return;
  }
  if (key === "shipment") {
    handleRiskChartClick({ name: "待发货" });
    return;
  }
  handleRiskChartClick({ name: "延期风险" });
}

function formatDashboardInsightCell(row, column) {
  const value = row?.[column.prop];
  if (column.type === "number") {
    return formatNumber(value);
  }
  if (column.type === "money") {
    return formatMoney(value);
  }
  if (column.type === "percent") {
    return formatPercent(value, 1, "0.0%");
  }
  if (column.type === "date") {
    return formatDate(value);
  }
  if (column.type === "datetime") {
    return formatDateTime(value);
  }
  return value ?? "--";
}

function goToReports() {
  router.push({ name: "reports" });
}

function navigateTo(route) {
  if (!route) {
    return;
  }
  router.push(route);
}

function goToProduction() {
  router.push({ name: "production" });
}

function goToInventory() {
  router.push({ name: "inventory", query: { tab: "stocks" } });
}

function openWorkOrder(workOrderId) {
  if (!workOrderId) {
    return;
  }
  router.push({
    name: "production",
    query: {
      workOrderId: String(workOrderId)
    }
  });
}

function openSalesOrder(salesOrderId) {
  if (!salesOrderId) {
    return;
  }
  router.push({
    name: "sales",
    query: {
      tab: "orders",
      detailId: String(salesOrderId)
    }
  });
}

function openPurchaseOrder(purchaseOrderId) {
  if (!purchaseOrderId) {
    return;
  }
  router.push({
    name: "purchase",
    query: {
      tab: "orders",
      detailId: String(purchaseOrderId)
    }
  });
}

function openPlanDetail(planDetailId) {
  if (!planDetailId) {
    return;
  }
  router.push({
    name: "production",
    query: {
      planDetailId: String(planDetailId)
    }
  });
}

onMounted(loadData);
</script>

<style scoped>
.dashboard-grid {
  gap: 16px;
}

.dashboard-metric-grid {
  gap: 16px;
}

.dashboard-filter-grid {
  grid-template-columns: minmax(0, 2fr) 180px minmax(260px, 1fr);
  align-items: center;
}

.range-hint {
  display: grid;
  gap: 4px;
}

.range-hint strong {
  font-size: 15px;
}

.range-hint span {
  color: var(--text-soft);
  font-size: 13px;
}

.stack-panel {
  display: grid;
  gap: 16px;
}

.mini-section {
  display: grid;
  gap: 10px;
}

.mini-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.mini-head strong {
  font-size: 15px;
}

.mini-head span {
  color: var(--text-soft);
  font-size: 12px;
}

.insight-heading {
  display: grid;
  gap: 4px;
}

.insight-heading strong {
  font-size: 16px;
}

.insight-heading span,
.summary-card small {
  color: var(--text-soft);
  font-size: 13px;
}

.capital-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.mrp-dashboard-block {
  display: grid;
  gap: 12px;
}

.mrp-metric-grid {
  margin-bottom: 4px;
}

.capital-card {
  padding: 14px 16px;
  border-radius: 16px;
  background: var(--bg-soft);
  border: 1px solid var(--line);
}

.capital-card span {
  color: var(--text-soft);
  font-size: 13px;
}

.capital-card strong {
  display: block;
  margin-top: 8px;
  font-size: 20px;
}

.capital-card small {
  display: block;
  margin-top: 6px;
  color: var(--text-soft);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.dashboard-insight-grid {
  margin-top: 4px;
}

.summary-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: var(--bg-soft);
  border: 1px solid var(--line);
}

.summary-card span {
  color: var(--text-soft);
  font-size: 13px;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 22px;
}

.summary-card :deep(.el-button) {
  justify-self: flex-start;
  margin-top: 6px;
  padding-left: 0;
}

.dashboard-grid :deep(.panel-body) {
  padding: 18px;
}

.dashboard-grid :deep(.section-header) {
  margin-bottom: 12px;
}

.dashboard-grid :deep(.section-header h3) {
  font-size: 18px;
}

.dashboard-grid :deep(.section-header p) {
  margin-top: 4px;
  font-size: 12px;
}

.dashboard-grid :deep(.el-table) {
  width: 100%;
  font-size: 13px;
}

.dashboard-grid :deep(.el-table .cell) {
  white-space: nowrap;
}

.dashboard-grid :deep(.el-table td),
.dashboard-grid :deep(.el-table th) {
  padding: 8px 0;
}

@media (max-width: 1280px) {
  .dashboard-filter-grid,
  .summary-grid,
  .capital-grid,
  .mrp-metric-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-grid :deep(.el-table .cell) {
    white-space: normal;
  }
}
</style>
