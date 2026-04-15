<template>
  <div class="page-grid" v-loading="loading">
    <section class="module-hero report-hero">
      <div>
        <h2>统计分析与经营洞察</h2>
        <p>把趋势、排行、穿透分析和高亮结果拆成菜单入口，只展示当前想看的图形或结果。</p>
      </div>
      <div class="hero-kpis">
        <div class="hero-kpi actionable-kpi" v-for="item in heroCards" :key="item.title">
          <span>{{ item.title }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
          <el-button text type="primary" @click="navigateTo(item.route)">{{ item.actionText }}</el-button>
        </div>
      </div>
    </section>

    <PagePanel title="分析菜单" description="按菜单切换图形、排行和穿透结果，减少页面噪音。">
      <div class="panel-menu">
        <button
          v-for="item in reportSections"
          :key="item.key"
          type="button"
          class="panel-menu-item"
          :class="{ active: reportSection === item.key }"
          @click="reportSection = item.key"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </button>
      </div>
    </PagePanel>

    <PagePanel title="筛选与导出" description="报表接口支持按日期筛选，应收应付排行支持限制条数">
      <template #actions>
        <div class="toolbar-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="loadReports">查询</el-button>
          <el-button type="primary" plain @click="exportSnapshot">导出快照</el-button>
        </div>
      </template>

      <div class="filter-grid report-filter-grid">
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
        <el-select v-model="filters.rankingLimit" placeholder="排行条数">
          <el-option v-for="item in rankingOptions" :key="item" :label="`Top ${item}`" :value="item" />
        </el-select>
        <el-select v-model="filters.customerId" clearable filterable placeholder="客户维度">
          <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="filters.supplierId" clearable filterable placeholder="供应商维度">
          <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="filters.warehouseId" clearable filterable placeholder="仓库维度">
          <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <div class="range-hint">
          <strong>{{ currentRangeLabel }}</strong>
          <span>按客户、供应商、仓库和排行条数联动下方分析模块</span>
        </div>
      </div>

      <div v-show="reportSection === 'overview'" class="summary-grid">
        <div class="summary-card" v-for="item in insightCards" :key="item.label">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </div>
      </div>

      <div v-show="reportSection === 'overview'" class="summary-grid">
        <div class="summary-card focus-card" v-for="item in anomalyCards" :key="item.label">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
          <el-button text type="primary" @click="applyAnomalyHighlight(item.key)">{{ item.actionText }}</el-button>
        </div>
      </div>
    </PagePanel>

    <div v-show="reportSection === 'charts'" class="grid-2">
      <PagePanel title="经营趋势" description="销售与采购金额按月份趋势汇总">
        <EChartPanel :option="trendOption" @chart-click="handleTrendChartClick" />
      </PagePanel>

      <PagePanel title="执行与风险" description="库存、生产与待执行量集中展示">
        <EChartPanel :option="riskOption" @chart-click="handleRiskChartClick" />
      </PagePanel>
    </div>

    <PagePanel
      v-show="reportSection === 'highlight'"
      title="联动高亮"
      description="图表点击、排行点击和穿透分析点击后，在这里汇总展示说明与来源明细。"
    >
      <template #actions>
        <el-button v-if="reportHighlight.actionRoute" text type="primary" @click="navigateTo(reportHighlight.actionRoute)">
          {{ reportHighlight.actionText || "查看来源页面" }}
        </el-button>
      </template>

      <div class="stack-panel">
        <div class="highlight-heading">
          <strong>{{ reportHighlight.title }}</strong>
        </div>

        <div class="summary-grid">
          <div class="summary-card" v-for="item in reportHighlight.metrics" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </div>
        </div>

        <el-table v-if="reportHighlight.rows.length" :data="reportHighlight.rows" stripe table-layout="fixed">
          <el-table-column
            v-for="column in reportHighlight.columns"
            :key="column.prop"
            :prop="column.prop"
            :label="column.label"
            :min-width="column.minWidth || 120"
          >
            <template #default="{ row }">
              {{ formatReportHighlightCell(row, column) }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else :description="reportHighlight.emptyText" />
      </div>
    </PagePanel>

    <div v-show="reportSection === 'accounts'" class="grid-2">
      <PagePanel title="应收账款排行" description="可直接跳到来源销售订单详情">
        <el-table :data="filteredArSummary" stripe>
          <el-table-column prop="customer" label="客户" min-width="150" />
          <el-table-column label="金额(元)" min-width="120">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="未完结订单" min-width="100">
            <template #default="{ row }">{{ formatNumber(row.orderCount, "0") }}</template>
          </el-table-column>
          <el-table-column prop="sourceOrderCode" label="来源订单" min-width="150" />
          <el-table-column label="单据日期" min-width="110">
            <template #default="{ row }">{{ formatDate(row.sourceDocDate) }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="120" fixed="right">
            <template #default="{ row }">
              <el-button text @click="highlightArRow(row)">高亮分析</el-button>
              <el-button text type="primary" :disabled="!row.sourceOrderId" @click="openSourceOrder('sales', row.sourceOrderId)">
                查看订单
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </PagePanel>

      <PagePanel title="应付账款排行" description="可直接跳到来源采购订单详情">
        <el-table :data="filteredApSummary" stripe>
          <el-table-column prop="supplier" label="供应商" min-width="150" />
          <el-table-column label="金额(元)" min-width="120">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="未完结订单" min-width="100">
            <template #default="{ row }">{{ formatNumber(row.orderCount, "0") }}</template>
          </el-table-column>
          <el-table-column prop="sourceOrderCode" label="来源订单" min-width="150" />
          <el-table-column label="单据日期" min-width="110">
            <template #default="{ row }">{{ formatDate(row.sourceDocDate) }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="120" fixed="right">
            <template #default="{ row }">
              <el-button text @click="highlightApRow(row)">高亮分析</el-button>
              <el-button text type="primary" :disabled="!row.sourceOrderId" @click="openSourceOrder('purchase', row.sourceOrderId)">
                查看订单
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </PagePanel>
    </div>

    <div v-show="reportSection === 'drilldown'" class="grid-2">
      <PagePanel title="库存穿透分析" description="支持按仓库下钻低库存物料并跳到库存页">
        <template #actions>
          <el-button text type="primary" @click="navigateTo({ name: 'inventory', query: { tab: 'stocks' } })">查看库存余额</el-button>
        </template>
        <el-table :data="inventoryDrillRows" stripe>
          <el-table-column prop="materialName" label="物料" min-width="160" />
          <el-table-column prop="warehouseName" label="仓库" min-width="120" />
          <el-table-column label="可用库存" min-width="100">
            <template #default="{ row }">{{ formatNumber(row.availableQty) }}</template>
          </el-table-column>
          <el-table-column label="安全库存" min-width="100">
            <template #default="{ row }">{{ formatNumber(row.safetyStock) }}</template>
          </el-table-column>
          <el-table-column label="缺口" min-width="90">
            <template #default="{ row }">{{ formatNumber(row.shortageQty) }}</template>
          </el-table-column>
          <el-table-column label="状态" min-width="90">
            <template #default="{ row }">
              <span class="table-tag" :class="row.shortageQty > row.safetyStock * 0.5 ? 'tag-danger' : 'tag-warning'">
                {{ row.shortageQty > row.safetyStock * 0.5 ? "紧急" : "关注" }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="110" fixed="right">
            <template #default="{ row }">
              <el-button text @click="highlightInventoryRow(row)">高亮分析</el-button>
              <el-button text type="primary" @click="openInventoryDrill(row.warehouseId, row.materialId)">查看库存</el-button>
            </template>
          </el-table-column>
        </el-table>
      </PagePanel>

      <PagePanel title="生产穿透分析" description="识别延期风险工单并跳转到生产模块详情">
        <template #actions>
          <el-button text type="primary" @click="navigateTo({ name: 'production' })">查看全部工单</el-button>
        </template>
        <el-table :data="productionDrillRows" stripe>
          <el-table-column prop="code" label="工单号" min-width="150" />
          <el-table-column prop="materialName" label="产品" min-width="140" />
          <el-table-column label="计划完工" min-width="110">
            <template #default="{ row }">{{ formatDate(row.endDate) }}</template>
          </el-table-column>
          <el-table-column label="进度" min-width="90">
            <template #default="{ row }">{{ formatPercent(row.progressRate, 1, "0.0%") }}</template>
          </el-table-column>
          <el-table-column label="延期天数" min-width="90">
            <template #default="{ row }">{{ formatNumber(row.delayDays, "0") }}</template>
          </el-table-column>
          <el-table-column label="状态" min-width="100">
            <template #default="{ row }">
              <span class="table-tag" :class="row.delayDays > 3 ? 'tag-danger' : 'tag-warning'">
                {{ formatStatusLabel(row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="110" fixed="right">
            <template #default="{ row }">
              <el-button text @click="highlightProductionRow(row)">高亮分析</el-button>
              <el-button text type="primary" @click="openWorkOrder(row.id)">查看工单</el-button>
            </template>
          </el-table-column>
        </el-table>
      </PagePanel>
    </div>

    <PagePanel
      v-show="reportSection === 'focus'"
      title="多维经营聚焦"
      description="按客户、供应商、仓库聚焦当前最值得关注的对象。"
    >
      <div class="summary-grid focus-grid">
        <div class="summary-card focus-card">
          <span>客户焦点</span>
          <strong>{{ focusedCustomerName }}</strong>
          <small>{{ focusedCustomerHint }}</small>
          <el-button text type="primary" :disabled="!focusedCustomerOrderId" @click="openSourceOrder('sales', focusedCustomerOrderId)">查看来源订单</el-button>
        </div>
        <div class="summary-card focus-card">
          <span>供应商焦点</span>
          <strong>{{ focusedSupplierName }}</strong>
          <small>{{ focusedSupplierHint }}</small>
          <el-button text type="primary" :disabled="!focusedSupplierOrderId" @click="openSourceOrder('purchase', focusedSupplierOrderId)">查看来源订单</el-button>
        </div>
        <div class="summary-card focus-card">
          <span>仓库焦点</span>
          <strong>{{ focusedWarehouseName }}</strong>
          <small>{{ focusedWarehouseHint }}</small>
          <el-button text type="primary" :disabled="!focusedWarehouseId" @click="openInventoryByWarehouse">查看库存穿透</el-button>
        </div>
      </div>
    </PagePanel>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";

import {
  fetchCustomerList,
  fetchApSummary,
  fetchArSummary,
  fetchInventoryStocks,
  fetchInventorySummary,
  fetchProductionSummary,
  fetchProductionWorkOrders,
  fetchPurchaseSummary,
  fetchSalesSummary,
  fetchSupplierList,
  fetchWarehouseList
} from "@/api/modules";
import EChartPanel from "@/components/EChartPanel.vue";
import PagePanel from "@/components/PagePanel.vue";
import { formatDate, formatMoney, formatNumber, formatPercent, formatStatusLabel } from "@/utils/format";

const router = useRouter();
const loading = ref(false);
const salesSummary = ref({});
const purchaseSummary = ref({});
const inventorySummary = ref({});
const productionSummary = ref({});
const arSummary = ref([]);
const apSummary = ref([]);
const customerOptions = ref([]);
const supplierOptions = ref([]);
const warehouseOptions = ref([]);
const inventoryStocks = ref([]);
const productionWorkOrders = ref([]);
const reportHighlight = reactive(createEmptyHighlight());
const reportSection = ref("overview");

const rankingOptions = [5, 10, 15, 20];
const filters = reactive({
  dateRange: [],
  rankingLimit: 10,
  customerId: undefined,
  supplierId: undefined,
  warehouseId: undefined
});

const currentRangeLabel = computed(() => {
  if (!filters.dateRange?.length) {
    return "最近经营概览";
  }
  return `${filters.dateRange[0]} 至 ${filters.dateRange[1]}`;
});

const heroCards = computed(() => [
  {
    title: salesSummary.value.summaryLabel || "本月销售额",
    value: `${formatMoney(salesSummary.value.summaryAmount ?? salesSummary.value.currentMonthSales, "0.00")} 元`,
    hint: `待发货 ${formatNumber(salesSummary.value.pendingShipmentQty, "0")} / 发货率 ${formatPercent(salesSummary.value.shipmentRate)}`,
    actionText: "查看销售订单",
    route: { name: "sales", query: { tab: "orders" } }
  },
  {
    title: purchaseSummary.value.summaryLabel || "本月采购额",
    value: `${formatMoney(purchaseSummary.value.summaryAmount ?? purchaseSummary.value.currentMonthPurchase, "0.00")} 元`,
    hint: `待到货 ${formatNumber(purchaseSummary.value.pendingReceiveQty, "0")} / 到货率 ${formatPercent(purchaseSummary.value.deliveryRate)}`,
    actionText: "查看采购订单",
    route: { name: "purchase", query: { tab: "orders" } }
  },
  {
    title: inventorySummary.value.summaryLabel || "库存快照",
    value: `${formatMoney(inventorySummary.value.inventoryAmount, "0.00")} 元`,
    hint: `可用库存 ${formatNumber(inventorySummary.value.availableQty, "0")} / 预警 ${formatNumber(inventorySummary.value.warningCount, "0")} 项`,
    actionText: "查看库存余额",
    route: { name: "inventory", query: { tab: "stocks" } }
  },
  {
    title: productionSummary.value.summaryLabel || "生产执行概览",
    value: formatPercent(productionSummary.value.onTimeRate, 1, "0.0%"),
    hint: `在制 ${formatNumber(productionSummary.value.inProgressCount, "0")} / 延期风险 ${formatNumber(productionSummary.value.delayRiskCount, "0")} 项`,
    actionText: "查看生产工单",
    route: { name: "production" }
  }
]);

const insightCards = computed(() => [
  {
    label: "销冠产品",
    value: salesSummary.value.topProduct || "--",
    hint: `本期订单 ${formatNumber(salesSummary.value.orderCount, "0")} 张`
  },
  {
    label: "核心供应商",
    value: purchaseSummary.value.topSupplier || "--",
    hint: `本期采购 ${formatNumber(purchaseSummary.value.orderCount, "0")} 张`
  },
  {
    label: "库存周转",
    value: `${formatNumber(inventorySummary.value.turnoverDays, "0")} 天`,
    hint: `库存总量 ${formatNumber(inventorySummary.value.inventoryQty, "0")}`
  },
  {
    label: "生产完工",
    value: `${formatNumber(productionSummary.value.completedCount, "0")} 张`,
    hint: `工单总数 ${formatNumber(productionSummary.value.workOrderCount, "0")}`
  }
]);

const filteredArSummary = computed(() => {
  const rows = arSummary.value || [];
  if (!filters.customerId) {
    return rows;
  }
  return rows.filter((item) => Number(item.customerId) === Number(filters.customerId));
});

const filteredApSummary = computed(() => {
  const rows = apSummary.value || [];
  if (!filters.supplierId) {
    return rows;
  }
  return rows.filter((item) => Number(item.supplierId) === Number(filters.supplierId));
});

const inventoryDrillRows = computed(() =>
  (inventoryStocks.value || [])
    .filter((item) => !filters.warehouseId || Number(item.warehouseId) === Number(filters.warehouseId))
    .map((item) => ({
      ...item,
      safetyStock: Number(item.safetyStock || 0),
      availableQty: Number(item.availableQty || 0),
      shortageQty: Math.max(Number(item.safetyStock || 0) - Number(item.availableQty || 0), 0)
    }))
    .filter((item) => item.safetyStock > 0 && item.shortageQty > 0)
    .sort((left, right) => right.shortageQty - left.shortageQty)
    .slice(0, filters.rankingLimit)
);

const productionDrillRows = computed(() => {
  const today = new Date();
  const startOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  return (productionWorkOrders.value || [])
    .map((item) => {
      const planQty = Number(item.planQty || 0);
      const finishedQty = Number(item.finishedQty || 0);
      const progressRate = planQty > 0 ? Math.min(finishedQty / planQty, 1) : 0;
      const endDate = item.endDate ? new Date(item.endDate) : null;
      const delayDays =
        endDate && endDate < startOfToday ? Math.floor((startOfToday.getTime() - endDate.getTime()) / (24 * 60 * 60 * 1000)) : 0;
      return {
        ...item,
        progressRate,
        delayDays
      };
    })
    .filter((item) => item.delayDays > 0 && !["completed", "closed"].includes(String(item.status || "").toLowerCase()))
    .sort((left, right) => right.delayDays - left.delayDays || left.progressRate - right.progressRate)
    .slice(0, filters.rankingLimit);
});

const focusedCustomerRow = computed(() => filteredArSummary.value[0] || null);
const focusedSupplierRow = computed(() => filteredApSummary.value[0] || null);
const focusedWarehouseRow = computed(() => inventoryDrillRows.value[0] || null);
const focusedCustomerName = computed(() => focusedCustomerRow.value?.customer || "暂无客户焦点");
const focusedSupplierName = computed(() => focusedSupplierRow.value?.supplier || "暂无供应商焦点");
const focusedWarehouseName = computed(() => focusedWarehouseRow.value?.warehouseName || "暂无仓库焦点");
const focusedCustomerOrderId = computed(() => focusedCustomerRow.value?.sourceOrderId || null);
const focusedSupplierOrderId = computed(() => focusedSupplierRow.value?.sourceOrderId || null);
const focusedWarehouseId = computed(() => focusedWarehouseRow.value?.warehouseId || null);
const focusedCustomerHint = computed(() =>
  focusedCustomerRow.value
    ? `应收 ${formatMoney(focusedCustomerRow.value.amount)} 元，涉及 ${formatNumber(focusedCustomerRow.value.orderCount, "0")} 笔未完结订单`
    : "当前筛选条件下暂无应收风险客户"
);
const focusedSupplierHint = computed(() =>
  focusedSupplierRow.value
    ? `应付 ${formatMoney(focusedSupplierRow.value.amount)} 元，涉及 ${formatNumber(focusedSupplierRow.value.orderCount, "0")} 笔未完结订单`
    : "当前筛选条件下暂无应付风险供应商"
);
const focusedWarehouseHint = computed(() =>
  focusedWarehouseRow.value
    ? `低库存物料 ${focusedWarehouseRow.value.materialName}，缺口 ${formatNumber(focusedWarehouseRow.value.shortageQty)}`
    : "当前筛选条件下暂无仓库预警焦点"
);
const shipmentRatePercent = computed(() => Number(salesSummary.value.shipmentRate || 0) * 100);
const deliveryRatePercent = computed(() => Number(purchaseSummary.value.deliveryRate || 0) * 100);
const onTimeRatePercent = computed(() => Number(productionSummary.value.onTimeRate || 0) * 100);

function createEmptyHighlight() {
  return {
    title: "默认经营高亮",
    description: "优先展示当前筛选区间内最值得说明的风险或来源单据。",
    metrics: [],
    rows: [],
    columns: [],
    actionText: "",
    actionRoute: null,
    emptyText: "当前筛选下暂无可展示的高亮明细"
  };
}

function setReportHighlight(payload) {
  Object.assign(reportHighlight, createEmptyHighlight(), payload);
}

const anomalyCards = computed(() => [
  {
    key: "shipment",
    label: "销售交付异常",
    value: shipmentRatePercent.value < 80 ? formatPercent(shipmentRatePercent.value, 1, "0.0%") : "正常",
    hint:
      shipmentRatePercent.value < 80
        ? `待发货 ${formatNumber(salesSummary.value.pendingShipmentQty, "0")} 项，建议继续查看应收与来源订单`
        : `发货率 ${formatPercent(shipmentRatePercent.value, 1, "0.0%")} ，当前交付较稳定`,
    actionText: "高亮交付"
  },
  {
    key: "delivery",
    label: "采购兑现异常",
    value: deliveryRatePercent.value < 80 ? formatPercent(deliveryRatePercent.value, 1, "0.0%") : "正常",
    hint:
      deliveryRatePercent.value < 80
        ? `待到货 ${formatNumber(purchaseSummary.value.pendingReceiveQty, "0")} 项，建议继续查看应付与来源订单`
        : `到货率 ${formatPercent(deliveryRatePercent.value, 1, "0.0%")} ，当前采购兑现较稳定`,
    actionText: "高亮采购"
  },
  {
    key: "production",
    label: "生产履约异常",
    value: Number(productionSummary.value.delayRiskCount || 0) > 0 ? `${formatNumber(productionSummary.value.delayRiskCount, "0")} 项` : "正常",
    hint:
      Number(productionSummary.value.delayRiskCount || 0) > 0
        ? `准时完工率 ${formatPercent(onTimeRatePercent.value, 1, "0.0%")} ，建议优先处理延期工单`
        : `准时完工率 ${formatPercent(onTimeRatePercent.value, 1, "0.0%")} ，生产履约较稳定`,
    actionText: "高亮生产"
  },
  {
    key: "inventory",
    label: "缺料预警异常",
    value: Number(inventorySummary.value.warningCount || 0) > 0 ? `${formatNumber(inventorySummary.value.warningCount, "0")} 项` : "正常",
    hint:
      Number(inventorySummary.value.warningCount || 0) > 0
        ? `优先关注 ${focusedWarehouseName.value} 的低库存物料`
        : `库存周转 ${formatNumber(inventorySummary.value.turnoverDays, "0")} 天，当前无明显缺料`,
    actionText: "高亮库存"
  }
]);

const reportSections = computed(() => [
  {
    key: "overview",
    label: "经营总览",
    value: `${insightCards.value.length + anomalyCards.value.length} 张摘要`,
    hint: "先看核心摘要和异常卡片"
  },
  {
    key: "charts",
    label: "趋势图形",
    value: `${trendLabels.value.length || 0} 个月`,
    hint: "切换趋势与风险图形查看变化"
  },
  {
    key: "highlight",
    label: "联动高亮",
    value: `${reportHighlight.rows.length || reportHighlight.metrics.length || 0} 条结果`,
    hint: "集中查看图表、排行和穿透后的说明"
  },
  {
    key: "accounts",
    label: "账款排行",
    value: `${filteredArSummary.value.length + filteredApSummary.value.length} 条排行`,
    hint: "查看应收和应付的主要对象"
  },
  {
    key: "drilldown",
    label: "穿透分析",
    value: `${inventoryDrillRows.value.length + productionDrillRows.value.length} 条明细`,
    hint: "查看库存与生产异常穿透结果"
  },
  {
    key: "focus",
    label: "多维聚焦",
    value: "3 个对象",
    hint: "按客户、供应商和仓库收束重点"
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
      lineStyle: { width: 3, color: "#4f46e5" },
      itemStyle: { color: "#4f46e5" },
      areaStyle: { color: "rgba(79, 70, 229, 0.14)" }
    },
    {
      name: "采购额",
      type: "line",
      smooth: true,
      symbolSize: 8,
      data: alignSeries(trendLabels.value, purchaseSummary.value.monthLabels, purchaseSummary.value.series),
      lineStyle: { width: 3, color: "#0891b2" },
      itemStyle: { color: "#0891b2" }
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
        color: (params) => ["#f59e0b", "#0f766e", "#4f46e5", "#2563eb", "#06b6d4"][params.dataIndex],
        borderRadius: [10, 10, 0, 0]
      }
    }
  ]
}));

function buildQueryParams() {
  return {
    startDate: filters.dateRange?.[0] || undefined,
    endDate: filters.dateRange?.[1] || undefined
  };
}

function resolveMonthRange(label) {
  const matched = String(label || "").match(/^(\d{4})-(\d{2})$/);
  if (!matched) {
    return null;
  }
  const year = Number(matched[1]);
  const month = Number(matched[2]);
  const start = new Date(year, month - 1, 1);
  const end = new Date(year, month, 0);
  const toText = (value) => {
    const y = value.getFullYear();
    const m = `${value.getMonth() + 1}`.padStart(2, "0");
    const d = `${value.getDate()}`.padStart(2, "0");
    return `${y}-${m}-${d}`;
  };
  return [toText(start), toText(end)];
}

async function loadReports() {
  loading.value = true;

  try {
    const baseParams = buildQueryParams();
    const [salesData, purchaseData, inventoryData, productionData, arData, apData, stockPage, workOrderPage] = await Promise.all([
      fetchSalesSummary(baseParams),
      fetchPurchaseSummary(baseParams),
      fetchInventorySummary(baseParams),
      fetchProductionSummary(baseParams),
      fetchArSummary({ ...baseParams, limit: filters.rankingLimit }),
      fetchApSummary({ ...baseParams, limit: filters.rankingLimit }),
      fetchInventoryStocks({ pageNo: 1, pageSize: 200, warehouseId: filters.warehouseId || undefined }),
      fetchProductionWorkOrders({ pageNo: 1, pageSize: 200 })
    ]);

    salesSummary.value = salesData || {};
    purchaseSummary.value = purchaseData || {};
    inventorySummary.value = inventoryData || {};
    productionSummary.value = productionData || {};
    arSummary.value = arData || [];
    apSummary.value = apData || [];
    inventoryStocks.value = stockPage.records || [];
    productionWorkOrders.value = workOrderPage.records || [];
    syncDefaultHighlight();
  } catch (error) {
    ElMessage.error(error.message || "报表加载失败");
  } finally {
    loading.value = false;
  }
}

async function loadFilterOptions() {
  try {
    const [customerPage, supplierPage, warehousePage] = await Promise.all([
      fetchCustomerList({ pageNo: 1, pageSize: 200 }),
      fetchSupplierList({ pageNo: 1, pageSize: 200 }),
      fetchWarehouseList({ pageNo: 1, pageSize: 200 })
    ]);
    customerOptions.value = customerPage.records || [];
    supplierOptions.value = supplierPage.records || [];
    warehouseOptions.value = warehousePage.records || [];
  } catch (error) {
    ElMessage.error(error.message || "筛选维度加载失败");
  }
}

function resetFilters() {
  filters.dateRange = [];
  filters.rankingLimit = 10;
  filters.customerId = undefined;
  filters.supplierId = undefined;
  filters.warehouseId = undefined;
  reportSection.value = "overview";
  loadReports();
}

function navigateTo(route) {
  router.push(route);
}

async function handleTrendChartClick(params) {
  reportSection.value = "highlight";
  const range = resolveMonthRange(params?.name);
  if (!range) {
    return;
  }
  filters.dateRange = range;
  await loadReports();
  const labels = trendLabels.value || [];
  const salesSeries = alignSeries(labels, salesSummary.value.monthLabels, salesSummary.value.series);
  const purchaseSeries = alignSeries(labels, purchaseSummary.value.monthLabels, purchaseSummary.value.series);
  const monthIndex = labels.findIndex((item) => item === params?.name);
  setReportHighlight({
    title: `${params.name} 趋势高亮`,
    description: "已自动切换到所点月份，并补充该月份的销售、采购和履约提示，便于演示图表联动。",
    metrics: [
      {
        label: "销售额",
        value: `${formatMoney(monthIndex >= 0 ? salesSeries[monthIndex] : 0, "0.00")} 元`,
        hint: "来自销售趋势序列"
      },
      {
        label: "采购额",
        value: `${formatMoney(monthIndex >= 0 ? purchaseSeries[monthIndex] : 0, "0.00")} 元`,
        hint: "来自采购趋势序列"
      },
      {
        label: "待发货",
        value: `${formatNumber(salesSummary.value.pendingShipmentQty, "0")} 项`,
        hint: "当前月份过滤后的销售履约压力"
      },
      {
        label: "待到货",
        value: `${formatNumber(purchaseSummary.value.pendingReceiveQty, "0")} 项`,
        hint: "当前月份过滤后的采购履约压力"
      }
    ],
    rows: [
      { metric: "销售额", amount: monthIndex >= 0 ? salesSeries[monthIndex] : 0, note: "趋势明细" },
      { metric: "采购额", amount: monthIndex >= 0 ? purchaseSeries[monthIndex] : 0, note: "趋势明细" }
    ],
    columns: [
      { prop: "metric", label: "指标", minWidth: 120 },
      { prop: "amount", label: "金额", minWidth: 120, type: "money" },
      { prop: "note", label: "说明", minWidth: 150 }
    ]
  });
  ElMessage.success(`已按 ${params.name} 重新筛选统计结果`);
}

function handleRiskChartClick(params) {
  reportSection.value = "highlight";
  const label = String(params?.name || "");
  if (label === "库存预警") {
    setReportHighlight({
      title: "库存预警高亮",
      description: "展示低于安全库存的关键物料，可继续跳转到库存余额页查看完整清单。",
      metrics: [
        {
          label: "预警项",
          value: `${formatNumber(inventorySummary.value.warningCount, "0")} 项`,
          hint: currentRangeLabel.value
        },
        {
          label: "库存周转",
          value: `${formatNumber(inventorySummary.value.turnoverDays, "0")} 天`,
          hint: "库存效率参考"
        }
      ],
      rows: inventoryDrillRows.value.slice(0, Math.min(6, filters.rankingLimit)),
      columns: [
        { prop: "materialName", label: "物料", minWidth: 150 },
        { prop: "warehouseName", label: "仓库", minWidth: 120 },
        { prop: "availableQty", label: "可用库存", minWidth: 100, type: "number" },
        { prop: "safetyStock", label: "安全库存", minWidth: 100, type: "number" },
        { prop: "shortageQty", label: "缺口", minWidth: 100, type: "number" }
      ],
      actionText: "查看库存页",
      actionRoute: { name: "inventory", query: { tab: "stocks" } },
      emptyText: "当前筛选下没有库存预警"
    });
    return;
  }
  if (label === "在制工单" || label === "延期风险") {
    setReportHighlight({
      title: `${label}高亮`,
      description: "聚焦生产执行异常或在制状态，支持继续跳转到工单详情。",
      metrics: [
        {
          label,
          value: `${formatNumber(label === "延期风险" ? productionSummary.value.delayRiskCount : productionSummary.value.inProgressCount, "0")} 项`,
          hint: "生产执行摘要"
        },
        {
          label: "准时完工率",
          value: formatPercent(productionSummary.value.onTimeRate, 1, "0.0%"),
          hint: currentRangeLabel.value
        }
      ],
      rows: productionDrillRows.value.slice(0, Math.min(6, filters.rankingLimit)),
      columns: [
        { prop: "code", label: "工单号", minWidth: 150 },
        { prop: "materialName", label: "产品", minWidth: 130 },
        { prop: "endDate", label: "计划完工", minWidth: 110, type: "date" },
        { prop: "progressRate", label: "进度", minWidth: 90, type: "percent" },
        { prop: "delayDays", label: "延期天数", minWidth: 100, type: "number" }
      ],
      actionText: "查看生产工单",
      actionRoute: { name: "production" },
      emptyText: `当前筛选下没有${label}明细`
    });
    return;
  }
  if (label === "待发货") {
    setReportHighlight({
      title: "待发货高亮",
      description: "从应收排行中优先展示待持续跟进的客户和来源订单。",
      metrics: [
        {
          label: "待发货",
          value: `${formatNumber(salesSummary.value.pendingShipmentQty, "0")} 项`,
          hint: "销售履约关注"
        },
        {
          label: "发货率",
          value: formatPercent(salesSummary.value.shipmentRate, 1, "0.0%"),
          hint: currentRangeLabel.value
        }
      ],
      rows: filteredArSummary.value.slice(0, Math.min(5, filters.rankingLimit)),
      columns: [
        { prop: "customer", label: "客户", minWidth: 140 },
        { prop: "sourceOrderCode", label: "来源订单", minWidth: 140 },
        { prop: "amount", label: "金额", minWidth: 120, type: "money" },
        { prop: "sourceDocDate", label: "单据日期", minWidth: 110, type: "date" }
      ],
      actionText: "查看销售订单",
      actionRoute: { name: "sales", query: { tab: "orders" } },
      emptyText: "当前筛选下没有待发货关注订单"
    });
    return;
  }
  if (label === "待到货") {
    setReportHighlight({
      title: "待到货高亮",
      description: "从应付排行中展示采购交付和资金占用的主要来源单据。",
      metrics: [
        {
          label: "待到货",
          value: `${formatNumber(purchaseSummary.value.pendingReceiveQty, "0")} 项`,
          hint: "采购履约关注"
        },
        {
          label: "到货率",
          value: formatPercent(purchaseSummary.value.deliveryRate, 1, "0.0%"),
          hint: currentRangeLabel.value
        }
      ],
      rows: filteredApSummary.value.slice(0, Math.min(5, filters.rankingLimit)),
      columns: [
        { prop: "supplier", label: "供应商", minWidth: 140 },
        { prop: "sourceOrderCode", label: "来源订单", minWidth: 140 },
        { prop: "amount", label: "金额", minWidth: 120, type: "money" },
        { prop: "sourceDocDate", label: "单据日期", minWidth: 110, type: "date" }
      ],
      actionText: "查看采购订单",
      actionRoute: { name: "purchase", query: { tab: "orders" } },
      emptyText: "当前筛选下没有待到货关注订单"
    });
  }
}

function applyAnomalyHighlight(key) {
  reportSection.value = "highlight";
  if (key === "shipment") {
    handleRiskChartClick({ name: "待发货" });
    return;
  }
  if (key === "delivery") {
    handleRiskChartClick({ name: "待到货" });
    return;
  }
  if (key === "production") {
    handleRiskChartClick({ name: "延期风险" });
    return;
  }
  handleRiskChartClick({ name: "库存预警" });
}

function syncDefaultHighlight() {
  if (productionDrillRows.value.length) {
    setReportHighlight({
      title: "默认高亮：生产延期风险",
      description: "报表加载完成后，优先展示最适合答辩说明的延期风险工单。",
      metrics: [
        {
          label: "延期风险",
          value: `${formatNumber(productionSummary.value.delayRiskCount, "0")} 项`,
          hint: currentRangeLabel.value
        },
        {
          label: "准时完工率",
          value: formatPercent(productionSummary.value.onTimeRate, 1, "0.0%"),
          hint: "生产执行总体表现"
        }
      ],
      rows: productionDrillRows.value.slice(0, Math.min(5, filters.rankingLimit)),
      columns: [
        { prop: "code", label: "工单号", minWidth: 150 },
        { prop: "materialName", label: "产品", minWidth: 130 },
        { prop: "endDate", label: "计划完工", minWidth: 110, type: "date" },
        { prop: "progressRate", label: "进度", minWidth: 90, type: "percent" },
        { prop: "delayDays", label: "延期天数", minWidth: 100, type: "number" }
      ],
      actionText: "查看生产工单",
      actionRoute: { name: "production" }
    });
    return;
  }

  if (inventoryDrillRows.value.length) {
    setReportHighlight({
      title: "默认高亮：库存预警",
      description: "若生产延期不明显，则展示当前缺料最突出的库存预警。",
      metrics: [
        {
          label: "预警项",
          value: `${formatNumber(inventorySummary.value.warningCount, "0")} 项`,
          hint: currentRangeLabel.value
        },
        {
          label: "库存金额",
          value: `${formatMoney(inventorySummary.value.inventoryAmount, "0.00")} 元`,
          hint: "库存资产快照"
        }
      ],
      rows: inventoryDrillRows.value.slice(0, Math.min(5, filters.rankingLimit)),
      columns: [
        { prop: "materialName", label: "物料", minWidth: 150 },
        { prop: "warehouseName", label: "仓库", minWidth: 120 },
        { prop: "availableQty", label: "可用库存", minWidth: 100, type: "number" },
        { prop: "shortageQty", label: "缺口", minWidth: 100, type: "number" }
      ],
      actionText: "查看库存页",
      actionRoute: { name: "inventory", query: { tab: "stocks" } }
    });
    return;
  }

  setReportHighlight({
    title: "默认高亮：客户应收焦点",
    description: "当前无明显生产或库存异常时，展示资金占用较高的客户来源订单。",
    metrics: [
      {
        label: "应收焦点",
        value: focusedCustomerName.value,
        hint: focusedCustomerHint.value
      },
      {
        label: "应付焦点",
        value: focusedSupplierName.value,
        hint: focusedSupplierHint.value
      }
    ],
    rows: filteredArSummary.value.slice(0, Math.min(5, filters.rankingLimit)),
    columns: [
      { prop: "customer", label: "客户", minWidth: 140 },
      { prop: "sourceOrderCode", label: "来源订单", minWidth: 140 },
      { prop: "amount", label: "金额", minWidth: 120, type: "money" },
      { prop: "sourceDocDate", label: "单据日期", minWidth: 110, type: "date" }
    ],
    actionText: "查看销售订单",
    actionRoute: { name: "sales", query: { tab: "orders" } },
    emptyText: "当前筛选下暂无应收排行明细"
  });
}

function highlightArRow(row) {
  reportSection.value = "highlight";
  setReportHighlight({
    title: `客户 ${row.customer} 应收焦点`,
    description: "按单个客户高亮展示应收金额、订单数量和来源订单，适合答辩时点名说明。",
    metrics: [
      {
        label: "应收金额",
        value: `${formatMoney(row.amount)} 元`,
        hint: "当前客户未完结订单形成"
      },
      {
        label: "未完结订单",
        value: `${formatNumber(row.orderCount, "0")} 笔`,
        hint: "需继续跟进发货与回款"
      }
    ],
    rows: [row],
    columns: [
      { prop: "customer", label: "客户", minWidth: 140 },
      { prop: "sourceOrderCode", label: "来源订单", minWidth: 140 },
      { prop: "amount", label: "金额", minWidth: 120, type: "money" },
      { prop: "sourceDocDate", label: "单据日期", minWidth: 110, type: "date" }
    ],
    actionText: "查看销售订单",
    actionRoute: row.sourceOrderId ? { name: "sales", query: { tab: "orders", detailId: String(row.sourceOrderId) } } : null
  });
}

function highlightApRow(row) {
  reportSection.value = "highlight";
  setReportHighlight({
    title: `供应商 ${row.supplier} 应付焦点`,
    description: "按单个供应商高亮采购占款与来源订单，便于说明交付与资金压力。",
    metrics: [
      {
        label: "应付金额",
        value: `${formatMoney(row.amount)} 元`,
        hint: "当前供应商未完结订单形成"
      },
      {
        label: "未完结订单",
        value: `${formatNumber(row.orderCount, "0")} 笔`,
        hint: "需继续跟进到货与结算"
      }
    ],
    rows: [row],
    columns: [
      { prop: "supplier", label: "供应商", minWidth: 140 },
      { prop: "sourceOrderCode", label: "来源订单", minWidth: 140 },
      { prop: "amount", label: "金额", minWidth: 120, type: "money" },
      { prop: "sourceDocDate", label: "单据日期", minWidth: 110, type: "date" }
    ],
    actionText: "查看采购订单",
    actionRoute: row.sourceOrderId ? { name: "purchase", query: { tab: "orders", detailId: String(row.sourceOrderId) } } : null
  });
}

function highlightInventoryRow(row) {
  reportSection.value = "highlight";
  setReportHighlight({
    title: `${row.materialName} 库存预警`,
    description: "按单个低库存物料高亮展示缺口规模和仓库位置。",
    metrics: [
      {
        label: "可用库存",
        value: formatNumber(row.availableQty),
        hint: `仓库：${row.warehouseName}`
      },
      {
        label: "库存缺口",
        value: formatNumber(row.shortageQty),
        hint: `安全库存 ${formatNumber(row.safetyStock)}`
      }
    ],
    rows: [row],
    columns: [
      { prop: "materialName", label: "物料", minWidth: 150 },
      { prop: "warehouseName", label: "仓库", minWidth: 120 },
      { prop: "availableQty", label: "可用库存", minWidth: 100, type: "number" },
      { prop: "safetyStock", label: "安全库存", minWidth: 100, type: "number" },
      { prop: "shortageQty", label: "缺口", minWidth: 100, type: "number" }
    ],
    actionText: "查看库存",
    actionRoute: { name: "inventory", query: { tab: "stocks", warehouseId: String(row.warehouseId), materialId: String(row.materialId) } }
  });
}

function highlightProductionRow(row) {
  reportSection.value = "highlight";
  setReportHighlight({
    title: `${row.code} 生产风险高亮`,
    description: "按单张工单说明延期天数、进度和产品信息，便于直接展开生产闭环描述。",
    metrics: [
      {
        label: "当前进度",
        value: formatPercent(row.progressRate, 1, "0.0%"),
        hint: `产品：${row.materialName || "--"}`
      },
      {
        label: "延期天数",
        value: `${formatNumber(row.delayDays, "0")} 天`,
        hint: `计划完工 ${formatDate(row.endDate)}`
      }
    ],
    rows: [row],
    columns: [
      { prop: "code", label: "工单号", minWidth: 150 },
      { prop: "materialName", label: "产品", minWidth: 130 },
      { prop: "endDate", label: "计划完工", minWidth: 110, type: "date" },
      { prop: "progressRate", label: "进度", minWidth: 90, type: "percent" },
      { prop: "delayDays", label: "延期天数", minWidth: 100, type: "number" }
    ],
    actionText: "查看工单",
    actionRoute: { name: "production", query: { workOrderId: String(row.id) } }
  });
}

function openSourceOrder(moduleName, orderId) {
  if (!orderId) {
    return;
  }
  router.push({
    name: moduleName,
    query: {
      tab: "orders",
      detailId: String(orderId)
    }
  });
}

function openInventoryDrill(warehouseId, materialId) {
  router.push({
    name: "inventory",
    query: {
      tab: "stocks",
      warehouseId: warehouseId ? String(warehouseId) : undefined,
      materialId: materialId ? String(materialId) : undefined
    }
  });
}

function openInventoryByWarehouse() {
  if (!focusedWarehouseId.value) {
    return;
  }
  openInventoryDrill(focusedWarehouseId.value, focusedWarehouseRow.value?.materialId);
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

function formatReportHighlightCell(row, column) {
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
  return value ?? "--";
}

function csvEscape(value) {
  const text = value === null || value === undefined ? "" : String(value);
  return `"${text.replaceAll('"', '""')}"`;
}

function exportSnapshot() {
  const rows = [
    ["统计分析快照", ""],
    ["导出时间", new Date().toLocaleString("zh-CN")],
    ["统计区间", currentRangeLabel.value],
    [],
    ["指标", "数值", "说明"],
    [heroCards.value[0].title, heroCards.value[0].value, heroCards.value[0].hint],
    [heroCards.value[1].title, heroCards.value[1].value, heroCards.value[1].hint],
    [heroCards.value[2].title, heroCards.value[2].value, heroCards.value[2].hint],
    [heroCards.value[3].title, heroCards.value[3].value, heroCards.value[3].hint],
    [],
    ["应收排行"],
    ["客户", "金额", "未完结订单", "来源订单", "单据日期"],
    ...filteredArSummary.value.map((item) => [
      item.customer,
      formatMoney(item.amount),
      formatNumber(item.orderCount, "0"),
      item.sourceOrderCode || "--",
      formatDate(item.sourceDocDate)
    ]),
    [],
    ["应付排行"],
    ["供应商", "金额", "未完结订单", "来源订单", "单据日期"],
    ...filteredApSummary.value.map((item) => [
      item.supplier,
      formatMoney(item.amount),
      formatNumber(item.orderCount, "0"),
      item.sourceOrderCode || "--",
      formatDate(item.sourceDocDate)
    ])
  ];

  const csvContent = `\uFEFF${rows.map((row) => row.map(csvEscape).join(",")).join("\n")}`;
  const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "经营分析快照.csv";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
  ElMessage.success("报表快照已导出");
}

onMounted(async () => {
  await loadFilterOptions();
  await loadReports();
});
</script>

<style scoped>
small {
  display: block;
  margin-top: 6px;
  color: var(--text-soft);
}

.panel-menu {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.panel-menu-item {
  display: grid;
  gap: 6px;
  padding: 16px 18px;
  border: 1px solid rgba(191, 219, 254, 0.4);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(238, 242, 255, 0.88));
  color: var(--text-main);
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.panel-menu-item:hover,
.panel-menu-item.active {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.42);
  box-shadow: 0 14px 30px rgba(79, 70, 229, 0.12);
}

.panel-menu-item span,
.panel-menu-item small {
  color: var(--text-soft);
}

.panel-menu-item strong {
  font-size: 18px;
}

.report-filter-grid {
  grid-template-columns: minmax(0, 2fr) repeat(4, minmax(140px, 160px)) minmax(220px, 1fr);
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

.highlight-heading {
  display: grid;
  gap: 4px;
}

.highlight-heading strong {
  font-size: 16px;
}

.highlight-heading span {
  color: var(--text-soft);
  font-size: 13px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
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

.actionable-kpi {
  display: grid;
  gap: 4px;
}

.actionable-kpi :deep(.el-button) {
  justify-self: flex-start;
  padding-left: 0;
}

.focus-grid {
  margin-top: 0;
}

.focus-card {
  display: grid;
  gap: 4px;
}

.focus-card :deep(.el-button) {
  justify-self: flex-start;
  padding-left: 0;
}

@media (max-width: 1280px) {
  .panel-menu,
  .report-filter-grid,
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .panel-menu,
  .report-filter-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
