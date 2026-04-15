<template>
  <div class="page-grid" v-loading="loading">
    <section class="module-hero">
      <div>
        <h2>{{ currentConfig.heroTitle }}</h2>
      </div>
      <div class="hero-kpis">
        <div class="hero-kpi"><span>库存金额</span><strong>{{ formatMoney(summary.inventoryAmount, "0.00") }}</strong><small>元</small></div>
        <div class="hero-kpi"><span>预警物料</span><strong>{{ formatNumber(summary.warningCount, "0") }}</strong><small>项</small></div>
        <div class="hero-kpi"><span>周转天数</span><strong>{{ formatNumber(summary.turnoverDays, "0") }}</strong><small>天</small></div>
        <div class="hero-kpi"><span>当前页条数</span><strong>{{ pagination.total }}</strong><small>条</small></div>
      </div>
    </section>

    <PagePanel :title="currentConfig.panelTitle" :description="currentConfig.panelDescription">
      <template #actions>
        <div class="toolbar-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="loadRows">查询</el-button>
          <el-button
            v-if="currentConfig.formEnabled"
            type="primary"
            plain
            @click="openCreateDialog"
          >
            新增{{ currentConfig.shortTitle }}
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="库存余额" name="stocks" />
        <el-tab-pane label="库存单据" name="docs" />
        <el-tab-pane label="调拨单" name="transfers" />
        <el-tab-pane label="盘点单" name="checks" />
      </el-tabs>

      <div class="filter-grid">
        <el-input v-model="filters.keyword" clearable :placeholder="currentConfig.keywordPlaceholder" />
        <el-select v-model="filters.warehouseId" clearable placeholder="全部仓库">
          <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-input
          v-if="activeTab === 'stocks'"
          v-model="filters.lotNo"
          clearable
          placeholder="按批次号筛选"
        />
        <el-select
          v-if="currentConfig.showStatusFilter"
          v-model="filters.status"
          clearable
          placeholder="全部状态"
        >
          <el-option label="草稿" value="draft" />
          <el-option label="已审核" value="approved" />
        </el-select>
        <el-select
          v-if="activeTab === 'docs'"
          v-model="filters.docType"
          clearable
          placeholder="全部单据类型"
        >
          <el-option label="采购入库" value="purchase_in" />
          <el-option label="销售出库" value="sales_out" />
          <el-option label="生产领料" value="work_order_pick" />
          <el-option label="完工入库" value="work_order_finish_in" />
          <el-option label="库存调拨" value="transfer" />
          <el-option label="库存盘点" value="check" />
        </el-select>
      </div>

      <el-table v-if="activeTab === 'stocks'" class="management-table" :data="groupedStockRows" stripe row-key="groupKey">
        <el-table-column type="expand" width="52">
          <template #default="{ row }">
            <div class="stock-expand-panel">
              <el-table class="stock-child-table" :data="row.batchRows" stripe>
                <el-table-column prop="lotNo" label="批次号" min-width="180">
                  <template #default="{ row: batchRow }">{{ batchRow.lotNo || "无批次" }}</template>
                </el-table-column>
                <el-table-column label="库存数量" min-width="100" align="center">
                  <template #default="{ row: batchRow }">{{ formatNumber(batchRow.qty) }}</template>
                </el-table-column>
                <el-table-column label="锁定数量" min-width="100" align="center">
                  <template #default="{ row: batchRow }">{{ formatNumber(batchRow.lockedQty) }}</template>
                </el-table-column>
                <el-table-column label="可用数量" min-width="100" align="center">
                  <template #default="{ row: batchRow }">{{ formatNumber(batchRow.availableQty) }}</template>
                </el-table-column>
                <el-table-column label="更新时间" min-width="150" align="center">
                  <template #default="{ row: batchRow }">{{ formatDateTime(batchRow.updatedAt) }}</template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="库存信息" min-width="240">
          <template #default="{ row }">
            <div class="record-cell">
              <strong class="record-code">{{ row.materialName }}</strong>
              <span class="record-subtitle">
                {{ row.materialCode }} / {{ row.batchCount > 1 ? `${row.batchCount} 个批次` : row.batchRows?.[0]?.lotNo || "无批次" }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="仓库" min-width="120" />
        <el-table-column prop="unitCode" label="单位" min-width="72" align="center" />
        <el-table-column label="数量" min-width="130" align="center">
          <template #default="{ row }">
            <div class="qty-stack compact">
              <span><em>库存</em>{{ formatNumber(row.qty) }}</span>
              <span><em>可用</em>{{ formatNumber(row.availableQty) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="90" align="center">
          <template #default="{ row }">
            <span class="table-tag" :class="getTagClass(row.stockStatus)">{{ formatStatusLabel(row.stockStatus) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="补充" min-width="180">
          <template #default="{ row }">
            <div class="record-cell compact">
              <span class="record-subtitle">安全库存 {{ formatNumber(row.safetyStock) }}</span>
              <span class="record-subtitle">锁定 {{ formatNumber(row.lockedQty) }}</span>
              <span class="record-subtitle">{{ formatDateTime(row.updatedAt) }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else class="management-table" :data="rows" stripe>
        <el-table-column label="单据信息" min-width="220">
          <template #default="{ row }">
            <div class="record-cell">
              <strong class="record-code">{{ row.code }}</strong>
              <span class="record-subtitle">{{ row.docTypeName || currentConfig.shortTitle }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="仓库流向" min-width="170">
          <template #default="{ row }">
            <div class="record-cell compact">
              <span class="record-subtitle">主仓库 {{ row.warehouseName || "--" }}</span>
              <span v-if="activeTab === 'transfers' || activeTab === 'docs'" class="record-subtitle">
                目标仓库 {{ row.targetWarehouseName || "--" }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="业务日期" min-width="100" align="center">
          <template #default="{ row }">{{ formatDate(row.bizDate) }}</template>
        </el-table-column>
        <el-table-column label="统计" min-width="150" align="center">
          <template #default="{ row }">
            <div class="qty-stack compact">
              <span><em>明细</em>{{ formatNumber(row.itemCount, "0") }}</span>
              <span><em>数量</em>{{ formatNumber(row.totalQty) }}</span>
              <span><em>金额</em>{{ formatMoney(row.totalAmount) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="96" align="center">
          <template #default="{ row }">
            <span class="table-tag" :class="getTagClass(row.status)">{{ formatStatusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" min-width="180" align="center" class-name="action-cell">
          <template #default="{ row }">
            <div class="table-actions row-actions">
              <el-button text @click="openDetailDrawer(row.id)">详情</el-button>
              <el-button
                v-if="currentConfig.approveEnabled && String(row.status || '').toLowerCase() === 'draft'"
                text
                type="success"
                @click="approveDoc(row.id)"
              >
                审核
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :current-page="pagination.pageNo"
          :page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </PagePanel>

    <el-dialog v-model="formVisible" :title="dialogTitle" width="960px">
      <template v-if="activeTab === 'transfers'">
        <div class="form-grid">
          <el-select v-model="transferForm.warehouseId" placeholder="源仓库">
            <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="transferForm.targetWarehouseId" placeholder="目标仓库">
            <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-date-picker v-model="transferForm.bizDate" type="date" value-format="YYYY-MM-DD" placeholder="业务日期" />
          <el-input v-model="transferForm.remark" placeholder="备注" class="full-span" />
        </div>

        <div class="subtable-head">
          <strong>调拨明细</strong>
          <el-button type="primary" plain @click="addTransferItem">新增明细</el-button>
        </div>

        <el-table :data="transferForm.items" stripe>
          <el-table-column label="物料" min-width="200">
            <template #default="{ row }">
              <el-select v-model="row.materialId" placeholder="选择物料">
                <el-option
                  v-for="item in materialOptions"
                  :key="item.id"
                  :label="`${item.code} / ${item.name}`"
                  :value="item.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="批次号" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.lotNo" placeholder="可选批次号" />
            </template>
          </el-table-column>
          <el-table-column label="数量" min-width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.qty" :min="0" :precision="2" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="单价" min-width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.unitPrice" :min="0" :precision="2" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="金额" min-width="120">
            <template #default="{ row }">{{ formatMoney(Number(row.qty || 0) * Number(row.unitPrice || 0), "0.00") }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="90" fixed="right">
            <template #default="{ $index }">
              <el-button text type="danger" @click="removeTransferItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-else-if="activeTab === 'checks'">
        <div class="form-grid">
          <el-select v-model="checkForm.warehouseId" placeholder="盘点仓库">
            <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-date-picker v-model="checkForm.bizDate" type="date" value-format="YYYY-MM-DD" placeholder="业务日期" />
          <el-input v-model="checkForm.remark" placeholder="备注" class="full-span" />
        </div>

        <div class="subtable-head">
          <strong>盘点明细</strong>
          <el-button type="primary" plain @click="addCheckItem">新增明细</el-button>
        </div>

        <el-table :data="checkForm.items" stripe>
          <el-table-column label="物料" min-width="200">
            <template #default="{ row }">
              <el-select v-model="row.materialId" placeholder="选择物料">
                <el-option
                  v-for="item in materialOptions"
                  :key="item.id"
                  :label="`${item.code} / ${item.name}`"
                  :value="item.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="批次号" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.lotNo" placeholder="可选批次号" />
            </template>
          </el-table-column>
          <el-table-column label="实盘数量" min-width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.actualQty" :min="0" :precision="2" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="90" fixed="right">
            <template #default="{ $index }">
              <el-button text type="danger" @click="removeCheckItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" :title="detailTitle" size="56%">
      <template v-if="detailRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="单号">{{ detailRecord.code }}</el-descriptions-item>
          <el-descriptions-item label="单据类型">{{ detailRecord.docTypeName || "--" }}</el-descriptions-item>
          <el-descriptions-item label="主仓库">{{ detailRecord.warehouseName || "--" }}</el-descriptions-item>
          <el-descriptions-item label="目标仓库">{{ detailRecord.targetWarehouseName || "--" }}</el-descriptions-item>
          <el-descriptions-item label="业务日期">{{ formatDate(detailRecord.bizDate) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ formatStatusLabel(detailRecord.status) }}</el-descriptions-item>
          <el-descriptions-item label="总数量">{{ formatNumber(detailRecord.totalQty) }}</el-descriptions-item>
          <el-descriptions-item label="总金额">{{ formatMoney(detailRecord.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="来源类型">{{ detailRecord.sourceType || "--" }}</el-descriptions-item>
          <el-descriptions-item label="来源ID">{{ detailRecord.sourceId || "--" }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailRecord.remark || "--" }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="sourceLinkAction" class="link-actions">
          <el-button type="primary" plain @click="navigateToSourceDoc">
            {{ sourceLinkAction.label }}
          </el-button>
        </div>

        <PagePanel title="单据明细" class="detail-panel">
          <el-table :data="detailRecord.items || []" stripe>
            <el-table-column prop="materialCode" label="物料编码" min-width="130" />
            <el-table-column prop="materialName" label="物料名称" min-width="160" />
            <el-table-column prop="unitCode" label="单位" min-width="90" />
            <el-table-column prop="lotNo" label="批次号" min-width="140" />
            <el-table-column label="数量" min-width="110">
              <template #default="{ row }">{{ formatNumber(row.qty) }}</template>
            </el-table-column>
            <el-table-column label="单价" min-width="110">
              <template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template>
            </el-table-column>
            <el-table-column label="金额" min-width="120">
              <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
            </el-table-column>
          </el-table>
        </PagePanel>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";

import {
  approveInventoryCheck,
  approveInventoryTransfer,
  createInventoryCheck,
  createInventoryTransfer,
  fetchInventoryCheckDetail,
  fetchInventoryChecks,
  fetchInventoryDocDetail,
  fetchInventoryDocs,
  fetchInventoryStocks,
  fetchInventorySummary,
  fetchInventoryTransferDetail,
  fetchInventoryTransfers,
  fetchMaterialList,
  fetchWarehouseList
} from "@/api/modules";
import PagePanel from "@/components/PagePanel.vue";
import {
  formatDate,
  formatDateTime,
  formatMoney,
  formatNumber,
  formatStatusLabel,
  getTagClass
} from "@/utils/format";

const TAB_CONFIG = {
  stocks: {
    heroTitle: "库存控制塔",
    heroDescription: "库存页面已接入真实库存余额接口，支持按关键字、仓库和批次查看库存、可用量与预警状态。",
    panelTitle: "库存余额列表",
    panelDescription: "对应 `GET /api/v1/inventory/stocks`",
    keywordPlaceholder: "搜索物料编码、名称或批次",
    shortTitle: "库存余额",
    formEnabled: false,
    showStatusFilter: false
  },
  docs: {
    heroTitle: "库存单据总览",
    heroDescription: "本页可查看采购入库、销售出库、生产出入库、调拨和盘点等库存单据汇总结果。",
    panelTitle: "库存单据列表",
    panelDescription: "对应 `GET /api/v1/inventory/docs`",
    keywordPlaceholder: "搜索库存单号",
    shortTitle: "库存单据",
    detailApi: fetchInventoryDocDetail,
    listApi: fetchInventoryDocs,
    formEnabled: false,
    approveEnabled: false,
    showStatusFilter: true
  },
  transfers: {
    heroTitle: "库存调拨执行",
    heroDescription: "本页已接通调拨单列表、详情、新增和审核接口，用于演示跨仓调拨流程。",
    panelTitle: "调拨单列表",
    panelDescription: "对应 `GET /api/v1/inventory/transfers`",
    keywordPlaceholder: "搜索调拨单号",
    shortTitle: "调拨单",
    detailApi: fetchInventoryTransferDetail,
    listApi: fetchInventoryTransfers,
    createApi: createInventoryTransfer,
    approveApi: approveInventoryTransfer,
    formEnabled: true,
    approveEnabled: true,
    showStatusFilter: true
  },
  checks: {
    heroTitle: "库存盘点执行",
    heroDescription: "本页已接通盘点单列表、详情、新增和审核接口，用于演示库存盘盈盘亏的确认流程。",
    panelTitle: "盘点单列表",
    panelDescription: "对应 `GET /api/v1/inventory/checks`",
    keywordPlaceholder: "搜索盘点单号",
    shortTitle: "盘点单",
    detailApi: fetchInventoryCheckDetail,
    listApi: fetchInventoryChecks,
    createApi: createInventoryCheck,
    approveApi: approveInventoryCheck,
    formEnabled: true,
    approveEnabled: true,
    showStatusFilter: true
  }
};

const router = useRouter();
const route = useRoute();
const activeTab = ref("stocks");
const loading = ref(false);
const submitting = ref(false);
const rows = ref([]);
const summary = ref({});
const warehouseOptions = ref([]);
const materialOptions = ref([]);
const detailRecord = ref(null);
const detailVisible = ref(false);
const formVisible = ref(false);
const lastHandledRouteKey = ref("");

const filters = reactive({
  keyword: "",
  warehouseId: "",
  lotNo: "",
  status: "",
  docType: ""
});

const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
});

const transferForm = reactive(createEmptyTransferForm());
const checkForm = reactive(createEmptyCheckForm());

const currentConfig = computed(() => TAB_CONFIG[activeTab.value]);
const dialogTitle = computed(() => `新增${currentConfig.value.shortTitle}`);
const detailTitle = computed(() => `${currentConfig.value.shortTitle}详情`);
const sourceLinkAction = computed(() =>
  getSourceLinkAction(detailRecord.value?.sourceType, detailRecord.value?.sourceId)
);
const groupedStockRows = computed(() => {
  if (activeTab.value !== "stocks") {
    return [];
  }

  const groupMap = new Map();
  for (const row of rows.value || []) {
    const key = `${row.warehouseId || row.warehouseName || ""}::${row.materialId || row.materialCode || ""}`;
    if (!groupMap.has(key)) {
      groupMap.set(key, {
        groupKey: key,
        materialId: row.materialId,
        materialCode: row.materialCode,
        materialName: row.materialName,
        warehouseId: row.warehouseId,
        warehouseName: row.warehouseName,
        unitCode: row.unitCode,
        qty: 0,
        lockedQty: 0,
        availableQty: 0,
        safetyStock: Number(row.safetyStock || 0),
        stockStatus: row.stockStatus,
        updatedAt: row.updatedAt,
        batchCount: 0,
        batchRows: []
      });
    }

    const group = groupMap.get(key);
    group.qty += Number(row.qty || 0);
    group.lockedQty += Number(row.lockedQty || 0);
    group.availableQty += Number(row.availableQty || 0);
    group.safetyStock = Math.max(group.safetyStock, Number(row.safetyStock || 0));
    group.stockStatus = Number(group.availableQty) < Number(group.safetyStock) ? "warning" : "normal";
    group.updatedAt = String(group.updatedAt || "") > String(row.updatedAt || "") ? group.updatedAt : row.updatedAt;
    group.batchRows.push({
      lotNo: row.lotNo,
      qty: row.qty,
      lockedQty: row.lockedQty,
      availableQty: row.availableQty,
      updatedAt: row.updatedAt
    });
    group.batchCount = group.batchRows.length;
  }

  return Array.from(groupMap.values());
});

function getToday() {
  const current = new Date();
  const month = String(current.getMonth() + 1).padStart(2, "0");
  const day = String(current.getDate()).padStart(2, "0");
  return `${current.getFullYear()}-${month}-${day}`;
}

function createTransferItem() {
  return {
    materialId: undefined,
    lotNo: "",
    qty: 1,
    unitPrice: 0
  };
}

function createCheckItem() {
  return {
    materialId: undefined,
    lotNo: "",
    actualQty: 0
  };
}

function createEmptyTransferForm() {
  return {
    warehouseId: undefined,
    targetWarehouseId: undefined,
    bizDate: getToday(),
    remark: "",
    items: [createTransferItem()]
  };
}

function createEmptyCheckForm() {
  return {
    warehouseId: undefined,
    bizDate: getToday(),
    remark: "",
    items: [createCheckItem()]
  };
}

const patchTransferForm = (payload = {}) => {
  Object.assign(transferForm, createEmptyTransferForm(), payload);
};

const patchCheckForm = (payload = {}) => {
  Object.assign(checkForm, createEmptyCheckForm(), payload);
};

const isDetailRoute = (detailId) => Number.isFinite(detailId) && detailId > 0;

const loadBaseData = async () => {
  const [summaryData, warehousePage, materialPage] = await Promise.all([
    fetchInventorySummary(),
    fetchWarehouseList({ pageNo: 1, pageSize: 200 }),
    fetchMaterialList({ pageNo: 1, pageSize: 200 })
  ]);

  summary.value = summaryData || {};
  warehouseOptions.value = warehousePage.records || [];
  materialOptions.value = materialPage.records || [];
};

const loadRows = async () => {
  loading.value = true;

  try {
    let pageData;

    if (activeTab.value === "stocks") {
      pageData = await fetchInventoryStocks({
        keyword: filters.keyword || undefined,
        warehouseId: filters.warehouseId || undefined,
        lotNo: filters.lotNo || undefined,
        pageNo: pagination.pageNo,
        pageSize: pagination.pageSize
      });
    } else if (activeTab.value === "docs") {
      pageData = await currentConfig.value.listApi({
        keyword: filters.keyword || undefined,
        docType: filters.docType || undefined,
        status: filters.status || undefined,
        warehouseId: filters.warehouseId || undefined,
        pageNo: pagination.pageNo,
        pageSize: pagination.pageSize
      });
    } else {
      pageData = await currentConfig.value.listApi({
        keyword: filters.keyword || undefined,
        status: filters.status || undefined,
        warehouseId: filters.warehouseId || undefined,
        pageNo: pagination.pageNo,
        pageSize: pagination.pageSize
      });
    }

    rows.value = pageData.records || [];
    pagination.total = pageData.total || 0;
  } catch (error) {
    ElMessage.error(error.message || `${currentConfig.value.shortTitle}加载失败`);
  } finally {
    loading.value = false;
  }
};

const resetFilters = () => {
  filters.keyword = "";
  filters.warehouseId = "";
  filters.lotNo = "";
  filters.status = "";
  filters.docType = "";
  pagination.pageNo = 1;
  loadRows();
};

const handleTabChange = () => {
  detailVisible.value = false;
  formVisible.value = false;
  resetFilters();
};

const handlePageChange = (pageNo) => {
  pagination.pageNo = pageNo;
  loadRows();
};

const handleSizeChange = (pageSize) => {
  pagination.pageSize = pageSize;
  pagination.pageNo = 1;
  loadRows();
};

const openCreateDialog = () => {
  if (activeTab.value === "transfers") {
    patchTransferForm();
  } else if (activeTab.value === "checks") {
    patchCheckForm();
  }
  formVisible.value = true;
};

const openDetailDrawer = async (id) => {
  try {
    detailRecord.value = await currentConfig.value.detailApi(id);
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || `${currentConfig.value.shortTitle}详情加载失败`);
  }
};

function getSourceLinkAction(sourceType, sourceId) {
  const normalizedSourceType = String(sourceType || "").toLowerCase();
  const normalizedSourceId = Number(sourceId);

  if (!normalizedSourceId) {
    return null;
  }

  if (normalizedSourceType === "purchase_in") {
    return {
      label: "查看来源采购订单",
      route: { name: "purchase", query: { tab: "orders", detailId: String(normalizedSourceId) } }
    };
  }

  if (normalizedSourceType === "sales_out") {
    return {
      label: "查看来源销售订单",
      route: { name: "sales", query: { tab: "orders", detailId: String(normalizedSourceId) } }
    };
  }

  if (["work_order_pick", "work_order_finish_in"].includes(normalizedSourceType)) {
    return {
      label: "查看来源生产工单",
      route: { name: "production", query: { workOrderId: String(normalizedSourceId) } }
    };
  }

  if (normalizedSourceType === "transfer") {
    return {
      label: "查看来源调拨单",
      route: { name: "inventory", query: { tab: "transfers", detailId: String(normalizedSourceId) } }
    };
  }

  if (normalizedSourceType === "check") {
    return {
      label: "查看来源盘点单",
      route: { name: "inventory", query: { tab: "checks", detailId: String(normalizedSourceId) } }
    };
  }

  return null;
}

const navigateToSourceDoc = () => {
  if (!sourceLinkAction.value) {
    return;
  }
  router.push(sourceLinkAction.value.route);
};

const addTransferItem = () => {
  transferForm.items.push(createTransferItem());
};

const removeTransferItem = (index) => {
  if (transferForm.items.length === 1) {
    ElMessage.warning("至少保留一条调拨明细");
    return;
  }
  transferForm.items.splice(index, 1);
};

const addCheckItem = () => {
  checkForm.items.push(createCheckItem());
};

const removeCheckItem = (index) => {
  if (checkForm.items.length === 1) {
    ElMessage.warning("至少保留一条盘点明细");
    return;
  }
  checkForm.items.splice(index, 1);
};

const submitForm = async () => {
  submitting.value = true;

  try {
    if (activeTab.value === "transfers") {
      if (!transferForm.warehouseId || !transferForm.targetWarehouseId || !transferForm.bizDate) {
        ElMessage.warning("请先填写完整的调拨单头信息");
        return;
      }
      if (transferForm.warehouseId === transferForm.targetWarehouseId) {
        ElMessage.warning("源仓库和目标仓库不能相同");
        return;
      }

      const validItems = transferForm.items.filter((item) => item.materialId && Number(item.qty) > 0);
      if (!validItems.length || validItems.length !== transferForm.items.length) {
        ElMessage.warning("请完善每一条调拨明细");
        return;
      }

      await currentConfig.value.createApi({
        warehouseId: transferForm.warehouseId,
        targetWarehouseId: transferForm.targetWarehouseId,
        bizDate: transferForm.bizDate,
        remark: transferForm.remark,
        items: validItems.map((item) => ({
          materialId: item.materialId,
          lotNo: item.lotNo || null,
          qty: Number(item.qty),
          unitPrice: Number(item.unitPrice || 0)
        }))
      });
    }

    if (activeTab.value === "checks") {
      if (!checkForm.warehouseId || !checkForm.bizDate) {
        ElMessage.warning("请先填写完整的盘点单头信息");
        return;
      }

      const validItems = checkForm.items.filter((item) => item.materialId && Number(item.actualQty) >= 0);
      if (!validItems.length || validItems.length !== checkForm.items.length) {
        ElMessage.warning("请完善每一条盘点明细");
        return;
      }

      await currentConfig.value.createApi({
        warehouseId: checkForm.warehouseId,
        bizDate: checkForm.bizDate,
        remark: checkForm.remark,
        items: validItems.map((item) => ({
          materialId: item.materialId,
          lotNo: item.lotNo || null,
          actualQty: Number(item.actualQty)
        }))
      });
    }

    ElMessage.success(`${currentConfig.value.shortTitle}保存成功`);
    formVisible.value = false;
    await loadRows();
  } catch (error) {
    ElMessage.error(error.message || `${currentConfig.value.shortTitle}保存失败`);
  } finally {
    submitting.value = false;
  }
};

const approveDoc = async (id) => {
  try {
    await ElMessageBox.confirm(`确认审核这张${currentConfig.value.shortTitle}吗？`, "审核确认", {
      type: "warning"
    });
    await currentConfig.value.approveApi(id);
    ElMessage.success(`${currentConfig.value.shortTitle}已审核`);
    await loadRows();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error.message || "审核失败");
    }
  }
};

const applyRouteQuery = async () => {
  const tab = TAB_CONFIG[route.query.tab] ? route.query.tab : activeTab.value;
  const detailId = Number(route.query.detailId);
  const warehouseId = route.query.warehouseId ? String(route.query.warehouseId) : "";
  const materialId = route.query.materialId ? String(route.query.materialId) : "";
  const routeKey = JSON.stringify({ tab, detailId, warehouseId, materialId });

  if (routeKey === lastHandledRouteKey.value) {
    return;
  }

  lastHandledRouteKey.value = routeKey;

  if (tab !== activeTab.value) {
    activeTab.value = tab;
    pagination.pageNo = 1;
    filters.warehouseId = warehouseId;
    filters.materialId = materialId;
    await loadRows();
  } else if (warehouseId !== filters.warehouseId || materialId !== filters.materialId) {
    filters.warehouseId = warehouseId;
    filters.materialId = materialId;
    pagination.pageNo = 1;
    await loadRows();
  }

  if (isDetailRoute(detailId) && TAB_CONFIG[tab]?.detailApi) {
    await openDetailDrawer(detailId);
  }
};

onMounted(async () => {
  try {
    if (TAB_CONFIG[route.query.tab]) {
      activeTab.value = route.query.tab;
    }
    await loadBaseData();
    await loadRows();
    await applyRouteQuery();
  } catch (error) {
    ElMessage.error(error.message || "库存模块初始化失败");
  }
});

watch(
  () => route.query,
  () => {
    applyRouteQuery();
  },
  { deep: true }
);
</script>

<style scoped>
small {
  display: block;
  margin-top: 6px;
  color: var(--text-soft);
}

.stock-expand-panel {
  padding: 8px 12px 16px;
  background: rgba(248, 250, 252, 0.65);
}

.stock-child-table :deep(.el-table td),
.stock-child-table :deep(.el-table th) {
  padding: 5px 0;
}

.qty-stack.compact {
  justify-items: center;
}

.link-actions {
  margin-top: 16px;
}

.detail-panel {
  margin-top: 18px;
}
</style>
