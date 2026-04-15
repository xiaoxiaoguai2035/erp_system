<template>
  <div class="page-grid" v-loading="loading">
    <section class="module-hero">
      <div>
        <h2>{{ currentConfig.heroTitle }}</h2>
      </div>
      <div class="hero-kpis">
        <div class="hero-kpi"><span>单据总数</span><strong>{{ pagination.total }}</strong></div>
        <div class="hero-kpi"><span>待审核</span><strong>{{ statusCount.draft }}</strong></div>
        <div class="hero-kpi"><span>已审核</span><strong>{{ statusCount.approved }}</strong></div>
        <div class="hero-kpi"><span>当前页金额</span><strong>{{ formatMoney(pageAmount, "0.00") }}</strong></div>
      </div>
    </section>

    <PagePanel :title="currentConfig.panelTitle" :description="currentConfig.panelDescription">
      <template #actions>
        <div class="toolbar-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="openCreateDialog">新增{{ currentConfig.shortTitle }}</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="销售报价" name="quotes" />
        <el-tab-pane label="销售订单" name="orders" />
      </el-tabs>

      <div class="filter-grid">
        <el-input v-model="filters.keyword" clearable :placeholder="currentConfig.keywordPlaceholder" />
        <el-select v-model="filters.status" clearable placeholder="全部状态">
          <el-option label="草稿" value="draft" />
          <el-option label="已审核" value="approved" />
          <el-option v-if="currentConfig.canOutStock" label="部分完成" value="partial" />
          <el-option v-if="currentConfig.canOutStock" label="已完成" value="completed" />
          <el-option v-if="currentConfig.canClose" label="已关闭" value="closed" />
        </el-select>
        <el-button type="primary" @click="loadDocs">查询</el-button>
      </div>

        <el-table class="management-table sales-table" :data="rows" stripe>
        <el-table-column prop="code" label="单号" min-width="138" />
        <el-table-column prop="customerName" label="客户" min-width="150" />
        <el-table-column label="单据日期" min-width="90" align="center">
          <template #default="{ row }">{{ formatDate(row.docDate) }}</template>
        </el-table-column>
        <el-table-column label="交货日期" min-width="90" align="center">
          <template #default="{ row }">{{ formatDate(row.deliveryDate) }}</template>
        </el-table-column>
        <el-table-column label="金额(元)" min-width="98" align="center">
          <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="88" align="center">
          <template #default="{ row }">
            <span class="table-tag" :class="getTagClass(row.status)">{{ formatStatusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column :label="currentConfig.operationLabel" min-width="240" align="center" class-name="action-cell">
          <template #default="{ row }">
            <div class="table-actions row-actions">
              <el-button text @click="openDetailDrawer(row.id)">详情</el-button>
              <el-button text :disabled="!canEditDoc(row)" @click="openEditDialog(row.id)">编辑</el-button>
              <el-button text type="success" :disabled="!canApproveDoc(row)" @click="approveDoc(row.id)">审核</el-button>
              <el-button
                v-if="activeTab === 'quotes'"
                text
                type="primary"
                :disabled="!canConvertQuote(row)"
                @click="convertQuoteToOrder(row.id)"
              >
                转订单
              </el-button>
              <el-button
                v-if="activeTab === 'orders'"
                text
                type="primary"
                :disabled="!canCreatePlan(row)"
                @click="openPlanConvertDialog(row.id)"
              >
                转生产
              </el-button>
              <el-button
                v-if="currentConfig.canClose"
                text
                type="danger"
                :disabled="!canCloseDoc(row)"
                @click="closeDoc(row.id)"
              >
                关闭
              </el-button>
              <el-button
                v-if="currentConfig.canOutStock && canOutStock(row)"
                text
                type="primary"
                @click="openOutStockDialog(row.id)"
              >
                出库
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

    <el-dialog v-model="formVisible" :title="dialogTitle" width="920px">
      <div class="form-grid">
        <el-select v-model="formModel.customerId" placeholder="选择客户">
          <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-date-picker v-model="formModel.docDate" type="date" value-format="YYYY-MM-DD" placeholder="单据日期" />
        <el-date-picker v-model="formModel.deliveryDate" type="date" value-format="YYYY-MM-DD" placeholder="交货日期" />
        <el-input v-model="formModel.remark" placeholder="备注" class="full-span" />
      </div>

      <div class="subtable-head">
        <strong>{{ currentConfig.itemTitle }}</strong>
        <el-button type="primary" plain @click="addItem">新增明细</el-button>
      </div>

      <el-table :data="formModel.items" stripe>
        <el-table-column label="产品" min-width="200">
          <template #default="{ row }">
            <el-select v-model="row.materialId" placeholder="选择产品">
              <el-option
                v-for="item in materialOptions"
                :key="item.id"
                :label="`${item.code} / ${item.name}`"
                :value="item.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="数量" min-width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.qty" :min="0" :precision="2" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="单价" min-width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0" :precision="2" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="金额" min-width="120">
          <template #default="{ row }">{{ formatMoney(Number(row.qty || 0) * Number(row.price || 0), "0.00") }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="90" fixed="right">
          <template #default="{ $index }">
            <el-button text type="danger" @click="removeItem($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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
          <el-descriptions-item label="客户">{{ detailRecord.customerName }}</el-descriptions-item>
          <el-descriptions-item label="单据日期">{{ formatDate(detailRecord.docDate) }}</el-descriptions-item>
          <el-descriptions-item label="交货日期">{{ formatDate(detailRecord.deliveryDate) }}</el-descriptions-item>
          <el-descriptions-item label="单据金额">{{ formatMoney(detailRecord.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ formatStatusLabel(detailRecord.status) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailRecord.remark || "--" }}</el-descriptions-item>
        </el-descriptions>

        <PagePanel :title="currentConfig.itemTitle" :description="currentConfig.detailDescription" class="detail-panel">
          <el-table :data="detailRecord.items || []" stripe>
            <el-table-column prop="materialCode" label="产品编码" min-width="130" />
            <el-table-column prop="materialName" label="产品名称" min-width="160" />
            <el-table-column label="数量" min-width="100">
              <template #default="{ row }">{{ formatNumber(row.qty) }}</template>
            </el-table-column>
            <el-table-column
              v-if="currentConfig.canOutStock"
              label="已发货"
              min-width="100"
            >
              <template #default="{ row }">{{ formatNumber(row.shippedQty) }}</template>
            </el-table-column>
            <el-table-column label="单价" min-width="100">
              <template #default="{ row }">{{ formatMoney(row.price) }}</template>
            </el-table-column>
            <el-table-column label="金额" min-width="110">
              <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
            </el-table-column>
          </el-table>
        </PagePanel>
      </template>
    </el-drawer>

    <el-dialog v-model="outStockVisible" title="销售订单出库" width="960px">
      <div class="form-grid">
        <el-select v-model="outStockForm.warehouseId" placeholder="出库仓库" @change="handleOutStockWarehouseChange">
          <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-date-picker v-model="outStockForm.bizDate" type="date" value-format="YYYY-MM-DD" placeholder="业务日期" />
        <el-input v-model="outStockForm.remark" placeholder="出库备注" class="full-span" />
      </div>

      <el-alert
        v-if="outStockDetail"
        :title="`来源订单：${outStockDetail.code} / ${outStockDetail.customerName || '未指定客户'}`"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-table :data="outStockForm.items" stripe>
        <el-table-column prop="materialCode" label="产品编码" min-width="130" />
        <el-table-column prop="materialName" label="产品名称" min-width="160" />
        <el-table-column label="剩余可发货" min-width="110">
          <template #default="{ row }">{{ formatNumber(row.remainQty) }}</template>
        </el-table-column>
        <el-table-column label="仓库可用库存" min-width="120">
          <template #default="{ row }">{{ formatNumber(getOutStockAvailableQty(row), "0") }}</template>
        </el-table-column>
        <el-table-column label="出库数量" min-width="120">
          <template #default="{ row }">
            <el-input-number
              v-model="row.qty"
              :min="0"
              :max="Math.min(Number(row.remainQty || 0), getOutStockAvailableQty(row))"
              :precision="2"
              controls-position="right"
            />
          </template>
        </el-table-column>
        <el-table-column label="批次号" min-width="150">
          <template #default="{ row }">
            <el-select v-model="row.lotNo" clearable filterable placeholder="自动匹配或手动选择">
              <el-option
                v-for="item in getOutStockLotOptions(row)"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="outStockVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitOutStock">确认出库</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="planConvertVisible" title="销售订单转生产计划" width="760px">
      <el-alert
        v-if="planConvertOrder"
        :title="`来源订单：${planConvertOrder.code} / ${planConvertOrder.customerName || '未指定客户'}`"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />

      <div class="form-grid">
        <el-select v-model="planConvertForm.sourceItemId" filterable placeholder="选择订单产品" @change="handlePlanConvertItemChange">
          <el-option
            v-for="item in planConvertItems"
            :key="item.sourceItemId"
            :label="`${item.materialCode} / ${item.materialName} / 剩余 ${formatNumber(item.remainQty)}`"
            :value="item.sourceItemId"
          />
        </el-select>
        <el-input-number
          v-model="planConvertForm.planQty"
          :min="0"
          :max="selectedPlanConvertItem?.remainQty || 0"
          :precision="2"
          controls-position="right"
          placeholder="计划数量"
        />
        <el-date-picker
          v-model="planConvertForm.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="计划开始日期"
        />
        <el-date-picker
          v-model="planConvertForm.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="计划结束日期"
        />
      </div>

      <el-descriptions v-if="selectedPlanConvertItem" :column="2" border style="margin-top: 16px">
        <el-descriptions-item label="产品编码">{{ selectedPlanConvertItem.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ selectedPlanConvertItem.materialName }}</el-descriptions-item>
        <el-descriptions-item label="订单数量">{{ formatNumber(selectedPlanConvertItem.orderedQty) }}</el-descriptions-item>
        <el-descriptions-item label="剩余待生产">{{ formatNumber(selectedPlanConvertItem.remainQty) }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="planConvertVisible = false">取消</el-button>
          <el-button type="primary" @click="submitPlanConvert">前往生产计划</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";

import {
  approveSalesOrder,
  approveSalesQuote,
  closeSalesOrder,
  createSalesOrder,
  createSalesQuote,
  fetchCustomerList,
  fetchInventoryStocks,
  fetchMaterialList,
  fetchSalesOrderDetail,
  fetchSalesOrders,
  fetchSalesQuoteDetail,
  fetchSalesQuotes,
  fetchWarehouseList,
  salesOrderOutStock,
  updateSalesOrder,
  updateSalesQuote
} from "@/api/modules";
import PagePanel from "@/components/PagePanel.vue";
import {
  formatDate,
  formatMoney,
  formatNumber,
  formatStatusLabel,
  getTagClass
} from "@/utils/format";

const DOC_CONFIG = {
  quotes: {
    heroTitle: "销售报价与商机跟进",
    heroDescription: "本页已接入销售报价的列表、详情、新增、编辑和审核接口，可作为商机转订单前的业务入口。",
    panelTitle: "销售报价列表",
    panelDescription: "对应 `GET /api/v1/sales/quotes`",
    keywordPlaceholder: "搜索报价单号或客户",
    shortTitle: "销售报价",
    itemTitle: "报价明细",
    detailDescription: "销售报价明细由后端详情接口返回",
    operationLabel: "操作",
    listApi: fetchSalesQuotes,
    detailApi: fetchSalesQuoteDetail,
    createApi: createSalesQuote,
    updateApi: updateSalesQuote,
    approveApi: approveSalesQuote,
    canClose: false,
    canOutStock: false
  },
  orders: {
    heroTitle: "销售订单与发货执行",
    heroDescription: "本页已接入销售订单的列表、详情、新增、编辑、审核、关闭和出库动作，能够展示销售执行闭环。",
    panelTitle: "销售订单列表",
    panelDescription: "对应 `GET /api/v1/sales/orders`",
    keywordPlaceholder: "搜索订单号或客户",
    shortTitle: "销售订单",
    itemTitle: "订单明细",
    detailDescription: "销售订单明细与发货数据由后端详情接口返回",
    operationLabel: "操作",
    listApi: fetchSalesOrders,
    detailApi: fetchSalesOrderDetail,
    createApi: createSalesOrder,
    updateApi: updateSalesOrder,
    approveApi: approveSalesOrder,
    closeApi: closeSalesOrder,
    canClose: true,
    canOutStock: true
  }
};

const router = useRouter();
const route = useRoute();
const activeTab = ref("orders");
const loading = ref(false);
const submitting = ref(false);
const rows = ref([]);
const detailRecord = ref(null);
const detailVisible = ref(false);
const formVisible = ref(false);
const editingId = ref(null);
const customerOptions = ref([]);
const materialOptions = ref([]);
const warehouseOptions = ref([]);
const outStockVisible = ref(false);
const outStockOrderId = ref(null);
const outStockDetail = ref(null);
const outStockStockMap = ref({});
const outStockLotMap = ref({});
const planConvertVisible = ref(false);
const planConvertOrder = ref(null);
const planConvertItems = ref([]);
const lastHandledRouteKey = ref("");

const filters = reactive({
  keyword: "",
  status: ""
});

const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
});

const formModel = reactive(createEmptyDoc());
const outStockForm = reactive(createEmptyOutStockForm());
const planConvertForm = reactive(createEmptyPlanConvertForm());

const currentConfig = computed(() => DOC_CONFIG[activeTab.value]);
const dialogTitle = computed(() => `${editingId.value ? "编辑" : "新增"}${currentConfig.value.shortTitle}`);
const detailTitle = computed(() => `${currentConfig.value.shortTitle}详情`);
const selectedPlanConvertItem = computed(() =>
  planConvertItems.value.find((item) => Number(item.sourceItemId) === Number(planConvertForm.sourceItemId))
);

const pageAmount = computed(() =>
  rows.value.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0)
);

const statusCount = computed(() =>
  rows.value.reduce(
    (accumulator, item) => {
      const status = String(item.status || "").toLowerCase();
      if (status === "approved") {
        accumulator.approved += 1;
      } else if (status === "draft") {
        accumulator.draft += 1;
      }
      return accumulator;
    },
    { draft: 0, approved: 0 }
  )
);

function getToday() {
  const current = new Date();
  const month = String(current.getMonth() + 1).padStart(2, "0");
  const day = String(current.getDate()).padStart(2, "0");
  return `${current.getFullYear()}-${month}-${day}`;
}

function createEmptyItem() {
  return {
    materialId: undefined,
    qty: 1,
    price: 0,
    sourceItemId: undefined
  };
}

function createEmptyDoc() {
  return {
    customerId: undefined,
    docDate: "",
    deliveryDate: "",
    sourceDocId: undefined,
    sourceDocType: "",
    remark: "",
    items: [createEmptyItem()]
  };
}

function createEmptyOutStockForm() {
  return {
    warehouseId: undefined,
    bizDate: getToday(),
    remark: "",
    items: []
  };
}

function createEmptyPlanConvertForm() {
  return {
    sourceItemId: undefined,
    materialId: undefined,
    planQty: 0,
    startDate: getToday(),
    endDate: getToday()
  };
}

const patchForm = (payload = {}) => {
  Object.assign(formModel, createEmptyDoc(), payload);
};

const patchOutStockForm = (payload = {}) => {
  Object.assign(outStockForm, createEmptyOutStockForm(), payload);
};

const getOutStockAvailableQty = (item) => {
  const key = `${item.materialId}::${outStockForm.warehouseId || ""}`;
  return Number(outStockStockMap.value[key] || 0);
};

const syncOutStockQtyByAvailableStock = () => {
  outStockForm.items = outStockForm.items.map((item) => {
    const stockQty = getOutStockAvailableQty(item);
    const nextQty = Math.min(Number(item.remainQty || 0), stockQty);
    return {
      ...item,
      qty: nextQty
    };
  });
};

const loadOutStockInventorySnapshot = async (warehouseId) => {
  if (!warehouseId || !outStockForm.items.length) {
    outStockStockMap.value = {};
    outStockLotMap.value = {};
    return;
  }

  const stockPage = await fetchInventoryStocks({
    pageNo: 1,
    pageSize: 500,
    warehouseId
  });

  const stockMap = {};
  const lotMap = {};
  for (const stock of stockPage.records || []) {
    const key = `${stock.materialId}::${warehouseId}`;
    const availableQty = Number(stock.availableQty ?? stock.qty ?? 0);
    stockMap[key] = (stockMap[key] || 0) + availableQty;
    if (!lotMap[key]) {
      lotMap[key] = [];
    }
    lotMap[key].push({
      value: stock.lotNo || "",
      label: stock.lotNo || "无批次",
      availableQty
    });
  }
  outStockStockMap.value = stockMap;
  outStockLotMap.value = lotMap;
};

const getOutStockLotOptions = (item) => {
  const key = `${item.materialId}::${outStockForm.warehouseId || ""}`;
  return outStockLotMap.value[key] || [];
};

const handleOutStockWarehouseChange = async (warehouseId) => {
  await loadOutStockInventorySnapshot(warehouseId);
  outStockForm.items = outStockForm.items.map((item) => {
    const lotOptions = getOutStockLotOptions(item);
    return {
      ...item,
      lotNo: lotOptions.length === 1 ? lotOptions[0].value : ""
    };
  });
  syncOutStockQtyByAvailableStock();
};

const patchPlanConvertForm = (payload = {}) => {
  Object.assign(planConvertForm, createEmptyPlanConvertForm(), payload);
};

const normalizeStatus = (status) => String(status || "").toLowerCase();
const isDetailRoute = (detailId) => Number.isFinite(detailId) && detailId > 0;

const loadOptions = async () => {
  const [customerPage, materialPage, warehousePage] = await Promise.all([
    fetchCustomerList({ pageNo: 1, pageSize: 200, status: "enabled" }),
    fetchMaterialList({ pageNo: 1, pageSize: 200, status: "enabled" }),
    fetchWarehouseList({ pageNo: 1, pageSize: 200, status: "enabled" })
  ]);

  customerOptions.value = customerPage.records || [];
  materialOptions.value = materialPage.records || [];
  warehouseOptions.value = warehousePage.records || [];
};

const loadDocs = async () => {
  loading.value = true;

  try {
    const pageData = await currentConfig.value.listApi({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize
    });

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
  filters.status = "";
  pagination.pageNo = 1;
  loadDocs();
};

const handleTabChange = () => {
  editingId.value = null;
  detailVisible.value = false;
  outStockVisible.value = false;
  planConvertVisible.value = false;
  resetFilters();
};

const handlePageChange = (pageNo) => {
  pagination.pageNo = pageNo;
  loadDocs();
};

const handleSizeChange = (pageSize) => {
  pagination.pageSize = pageSize;
  pagination.pageNo = 1;
  loadDocs();
};

const openCreateDialog = () => {
  editingId.value = null;
  patchForm();
  formVisible.value = true;
};

const openEditDialog = async (id) => {
  try {
    const detail = await currentConfig.value.detailApi(id);
    editingId.value = id;
    patchForm({
      customerId: detail.customerId,
      docDate: detail.docDate,
      deliveryDate: detail.deliveryDate,
      sourceDocId: detail.sourceDocId,
      sourceDocType: detail.sourceDocType || "",
      remark: detail.remark,
      items: (detail.items || []).map((item) => ({
        materialId: item.materialId,
        qty: Number(item.qty || 0),
        price: Number(item.price || 0),
        sourceItemId: item.sourceItemId
      }))
    });
    formVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || `${currentConfig.value.shortTitle}详情加载失败`);
  }
};

const openDetailDrawer = async (id) => {
  try {
    detailRecord.value = await currentConfig.value.detailApi(id);
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || `${currentConfig.value.shortTitle}详情加载失败`);
  }
};

const addItem = () => {
  formModel.items.push(createEmptyItem());
};

const removeItem = (index) => {
  if (formModel.items.length === 1) {
    ElMessage.warning(`至少保留一条${currentConfig.value.itemTitle}`);
    return;
  }
  formModel.items.splice(index, 1);
};

const canEditDoc = (row) => normalizeStatus(row.status) === "draft";
const canApproveDoc = (row) => normalizeStatus(row.status) === "draft";
const canCloseDoc = (row) => ["approved", "partial", "completed"].includes(normalizeStatus(row.status));
const canConvertQuote = (row) => normalizeStatus(row.status) === "approved";
const canCreatePlan = (row) => ["approved", "partial"].includes(normalizeStatus(row.status));

const validateForm = () => {
  if (!formModel.customerId || !formModel.docDate) {
    ElMessage.warning("请先填写客户和单据日期");
    return false;
  }

  const validItems = formModel.items.filter(
    (item) => item.materialId && Number(item.qty) > 0 && Number(item.price) >= 0
  );

  if (!validItems.length || validItems.length !== formModel.items.length) {
    ElMessage.warning(`请完善每一条${currentConfig.value.itemTitle}`);
    return false;
  }

  return true;
};

const buildPlanConvertItems = (detail) =>
  (detail.items || [])
    .map((item) => {
      const orderedQty = Number(item.qty || 0);
      const plannedQty = Number(item.plannedQty || 0);
      const remainQty = Math.max(Number(item.remainPlanQty ?? orderedQty - plannedQty), 0);

      return {
        sourceItemId: item.id,
        materialId: item.materialId,
        materialCode: item.materialCode,
        materialName: item.materialName,
        orderedQty,
        plannedQty,
        remainQty
      };
    })
    .filter((item) => item.materialId && item.remainQty > 0);

const handlePlanConvertItemChange = (sourceItemId) => {
  const matched = planConvertItems.value.find((item) => Number(item.sourceItemId) === Number(sourceItemId));
  if (!matched) {
    return;
  }

  patchPlanConvertForm({
    sourceItemId: matched.sourceItemId,
    materialId: matched.materialId,
    planQty: matched.remainQty,
    startDate: getToday(),
    endDate: planConvertOrder.value?.deliveryDate || getToday()
  });
};

const submitForm = async () => {
  if (!validateForm()) {
    return;
  }

  submitting.value = true;
  const payload = {
    customerId: formModel.customerId,
    docDate: formModel.docDate,
    deliveryDate: formModel.deliveryDate || null,
    sourceDocId: formModel.sourceDocId || null,
    sourceDocType: formModel.sourceDocType || null,
    remark: formModel.remark,
    items: formModel.items.map((item) => ({
      materialId: item.materialId,
      qty: Number(item.qty),
      price: Number(item.price),
      sourceItemId: item.sourceItemId || null
    }))
  };

  try {
    if (editingId.value) {
      await currentConfig.value.updateApi(editingId.value, payload);
    } else {
      await currentConfig.value.createApi(payload);
    }

    ElMessage.success(`${dialogTitle.value}成功`);
    formVisible.value = false;
    await loadDocs();
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
    await loadDocs();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error.message || "审核失败");
    }
  }
};

const closeDoc = async (id) => {
  try {
    await ElMessageBox.confirm(`确认关闭这张${currentConfig.value.shortTitle}吗？`, "关闭确认", {
      type: "warning"
    });
    await currentConfig.value.closeApi(id);
    ElMessage.success(`${currentConfig.value.shortTitle}已关闭`);
    await loadDocs();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error.message || "关闭失败");
    }
  }
};

const canOutStock = (row) => ["approved", "partial"].includes(String(row.status || "").toLowerCase());

const convertQuoteToOrder = async (id) => {
  try {
    const detail = await fetchSalesQuoteDetail(id);
    const availableItems = (detail.items || [])
      .map((item) => ({
        materialId: item.materialId,
        qty: Number(item.remainConvertQty ?? item.qty ?? 0),
        price: Number(item.price || 0),
        sourceItemId: item.id
      }))
      .filter((item) => item.materialId && Number(item.qty) > 0);

    if (!availableItems.length) {
      ElMessage.warning("该销售报价已无剩余可下推销售订单的明细");
      return;
    }

    activeTab.value = "orders";
    editingId.value = null;
    detailVisible.value = false;
    outStockVisible.value = false;
    pagination.pageNo = 1;
    await loadDocs();
    patchForm({
      customerId: detail.customerId,
      docDate: getToday(),
      deliveryDate: detail.deliveryDate,
      sourceDocId: detail.id,
      sourceDocType: "quote",
      remark: detail.remark ? `${detail.remark} | 来源报价：${detail.code}` : `来源报价：${detail.code}`,
      items: availableItems
    });
    formVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "报价转订单数据加载失败");
  }
};

const openPlanConvertDialog = async (id) => {
  try {
    const detail = await fetchSalesOrderDetail(id);
    const availableItems = buildPlanConvertItems(detail);

    if (!availableItems.length) {
      ElMessage.warning("该销售订单已无可继续下推生产计划的明细");
      return;
    }

    planConvertOrder.value = detail;
    planConvertItems.value = availableItems;
    handlePlanConvertItemChange(availableItems[0].sourceItemId);
    planConvertVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "销售订单转生产计划数据加载失败");
  }
};

const submitPlanConvert = () => {
  const selectedItem = selectedPlanConvertItem.value;
  if (!planConvertOrder.value?.id || !selectedItem) {
    ElMessage.warning("请先选择来源订单产品");
    return;
  }
  if (!planConvertForm.startDate || !planConvertForm.endDate) {
    ElMessage.warning("请先填写计划日期");
    return;
  }
  if (Number(planConvertForm.planQty || 0) <= 0) {
    ElMessage.warning("计划数量必须大于 0");
    return;
  }
  if (Number(planConvertForm.planQty) > Number(selectedItem.remainQty || 0)) {
    ElMessage.warning("计划数量不能超过订单剩余待生产数量");
    return;
  }

  planConvertVisible.value = false;
  router.push({
    name: "production",
    query: {
      openPlanDialog: "1",
      sourceSalesId: String(planConvertOrder.value.id),
      materialId: String(selectedItem.materialId),
      planQty: String(planConvertForm.planQty),
      startDate: planConvertForm.startDate,
      endDate: planConvertForm.endDate
    }
  });
};

const promptOpenInventoryDoc = async (stockDocId) => {
  if (!stockDocId) {
    return;
  }

  try {
    await ElMessageBox.confirm(`已生成库存单据 #${stockDocId}，是否前往查看？`, "查看库存单据", {
      type: "success",
      confirmButtonText: "立即查看",
      cancelButtonText: "稍后查看"
    });
    router.push({
      name: "inventory",
      query: {
        tab: "docs",
        detailId: String(stockDocId)
      }
    });
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error.message || "库存单据跳转失败");
    }
  }
};

const openOutStockDialog = async (id) => {
  try {
    const detail = await fetchSalesOrderDetail(id);
    const availableItems = (detail.items || [])
      .map((item) => {
        const remainQty = Math.max(Number(item.qty || 0) - Number(item.shippedQty || 0), 0);
        return {
          sourceItemId: item.id,
          materialId: item.materialId,
          materialCode: item.materialCode,
          materialName: item.materialName,
          remainQty,
          qty: remainQty,
          lotNo: ""
        };
      })
      .filter((item) => item.remainQty > 0);

    if (!availableItems.length) {
      ElMessage.warning("该销售订单已无可出库明细");
      return;
    }

    outStockOrderId.value = id;
    outStockDetail.value = detail;
    const preferredWarehouseId =
      availableItems.find((item) => Number(item.remainQty || 0) > 0)?.materialId &&
      warehouseOptions.value.find((item) => String(item.name || "").includes("成品"))?.id;
    patchOutStockForm({
      warehouseId: preferredWarehouseId,
      bizDate: getToday(),
      remark: `${detail.code} 出库`,
      items: availableItems
    });
    await loadOutStockInventorySnapshot(preferredWarehouseId);
    outStockForm.items = outStockForm.items.map((item) => {
      const lotOptions = getOutStockLotOptions(item);
      return {
        ...item,
        lotNo: lotOptions.length === 1 ? lotOptions[0].value : ""
      };
    });
    syncOutStockQtyByAvailableStock();
    outStockVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "销售出库详情加载失败");
  }
};

const submitOutStock = async () => {
  if (!outStockOrderId.value) {
    ElMessage.warning("未找到来源销售订单");
    return;
  }
  if (!outStockForm.warehouseId || !outStockForm.bizDate) {
    ElMessage.warning("请先选择出库仓库和业务日期");
    return;
  }

  const validItems = outStockForm.items.filter((item) => Number(item.qty) > 0);
  if (!validItems.length) {
    ElMessage.warning("请至少填写一条出库明细");
    return;
  }

  for (const item of validItems) {
    const qty = Number(item.qty || 0);
    if (qty > Number(item.remainQty || 0)) {
      ElMessage.warning(`产品 ${item.materialName} 的出库数量不能超过剩余可发货数量`);
      return;
    }
    if (qty > getOutStockAvailableQty(item)) {
      ElMessage.warning(`产品 ${item.materialName} 在所选仓库中的可用库存不足`);
      return;
    }
  }

  submitting.value = true;
  try {
    const stockDocId = await salesOrderOutStock(outStockOrderId.value, {
      warehouseId: outStockForm.warehouseId,
      bizDate: outStockForm.bizDate,
      remark: outStockForm.remark,
      items: validItems.map((item) => ({
        sourceItemId: item.sourceItemId,
        materialId: item.materialId,
        lotNo: item.lotNo || null,
        qty: Number(item.qty)
      }))
    });

    ElMessage.success("销售出库成功");
    outStockVisible.value = false;
    await loadDocs();

    if (detailVisible.value && detailRecord.value?.id === outStockOrderId.value) {
      detailRecord.value = await fetchSalesOrderDetail(outStockOrderId.value);
    }
    await promptOpenInventoryDoc(stockDocId);
  } catch (error) {
    ElMessage.error(error.message || "销售出库失败");
  } finally {
    submitting.value = false;
  }
};

const applyRouteQuery = async () => {
  const tab = DOC_CONFIG[route.query.tab] ? route.query.tab : activeTab.value;
  const detailId = Number(route.query.detailId);
  const routeKey = JSON.stringify({ tab, detailId });

  if (routeKey === lastHandledRouteKey.value) {
    return;
  }

  lastHandledRouteKey.value = routeKey;

  if (tab !== activeTab.value) {
    activeTab.value = tab;
    pagination.pageNo = 1;
    await loadDocs();
  }

  if (isDetailRoute(detailId)) {
    await openDetailDrawer(detailId);
  }
};

onMounted(async () => {
  try {
    if (DOC_CONFIG[route.query.tab]) {
      activeTab.value = route.query.tab;
    }
    await loadOptions();
    await loadDocs();
    await applyRouteQuery();
  } catch (error) {
    ElMessage.error(error.message || "销售模块初始化失败");
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
.sales-table :deep(.table-actions.row-actions) {
  opacity: 1;
}

.detail-panel {
  margin-top: 18px;
}
</style>
