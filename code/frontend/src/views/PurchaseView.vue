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
        <el-tab-pane label="采购申请" name="requests" />
        <el-tab-pane label="采购订单" name="orders" />
      </el-tabs>

      <div class="filter-grid">
        <el-input v-model="filters.keyword" clearable :placeholder="currentConfig.keywordPlaceholder" />
        <el-select v-model="filters.status" clearable placeholder="全部状态">
          <el-option label="草稿" value="draft" />
          <el-option label="已审核" value="approved" />
          <el-option v-if="currentConfig.canInStock || activeTab === 'requests'" label="部分完成" value="partial" />
          <el-option v-if="currentConfig.canInStock || activeTab === 'requests'" label="已完成" value="completed" />
          <el-option label="已关闭" value="closed" />
        </el-select>
        <el-button type="primary" @click="loadDocs">查询</el-button>
      </div>

      <el-table class="management-table" :data="rows" stripe>
        <el-table-column prop="code" label="单号" min-width="150" />
        <el-table-column prop="supplierName" label="供应商" min-width="150" />
        <el-table-column label="单据日期" min-width="100" align="center">
          <template #default="{ row }">{{ formatDate(row.docDate) }}</template>
        </el-table-column>
        <el-table-column label="预计到货" min-width="100" align="center">
          <template #default="{ row }">{{ formatDate(row.expectedDate) }}</template>
        </el-table-column>
        <el-table-column label="金额(元)" min-width="110" align="center">
          <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="100" align="center">
          <template #default="{ row }">
            <span class="table-tag" :class="getTagClass(row.status)">{{ formatStatusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column :label="currentConfig.operationLabel" min-width="280" align="center" class-name="action-cell">
          <template #default="{ row }">
            <div class="table-actions row-actions">
              <el-button text @click="openDetailDrawer(row.id)">详情</el-button>
              <el-button text :disabled="!canEditDoc(row)" @click="openEditDialog(row.id)">编辑</el-button>
              <el-button text type="success" :disabled="!canApproveDoc(row)" @click="approveDoc(row.id)">审核</el-button>
              <el-button
                v-if="activeTab === 'requests'"
                text
                type="primary"
                :disabled="!canConvertRequest(row)"
                @click="convertRequestToOrder(row.id)"
              >
                转订单
              </el-button>
              <el-button text type="danger" :disabled="!canCloseDoc(row)" @click="closeDoc(row.id)">关闭</el-button>
              <el-button
                v-if="currentConfig.canInStock && canInStock(row)"
                text
                type="primary"
                @click="openInStockDialog(row.id)"
              >
                入库
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
      <div class="form-grid">
        <el-select v-model="formModel.supplierId" placeholder="选择供应商">
          <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-date-picker v-model="formModel.docDate" type="date" value-format="YYYY-MM-DD" placeholder="单据日期" />
        <el-date-picker v-model="formModel.expectedDate" type="date" value-format="YYYY-MM-DD" placeholder="预计到货日期" />
        <el-input v-model="formModel.remark" placeholder="备注" class="full-span" />
      </div>

      <div class="subtable-head">
        <strong>{{ currentConfig.itemTitle }}</strong>
        <el-button type="primary" plain @click="addItem">新增明细</el-button>
      </div>

      <el-table :data="formModel.items" stripe>
        <el-table-column label="物料" min-width="180">
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
        <el-table-column label="仓库" min-width="160">
          <template #default="{ row }">
            <el-select v-model="row.warehouseId" clearable placeholder="选择仓库">
              <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
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
        <el-table-column label="需求日期" min-width="160">
          <template #default="{ row }">
            <el-date-picker v-model="row.needDate" type="date" value-format="YYYY-MM-DD" placeholder="需求日期" />
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
          <el-descriptions-item label="供应商">{{ detailRecord.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="单据日期">{{ formatDate(detailRecord.docDate) }}</el-descriptions-item>
          <el-descriptions-item label="预计到货">{{ formatDate(detailRecord.expectedDate) }}</el-descriptions-item>
          <el-descriptions-item label="单据金额">{{ formatMoney(detailRecord.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ formatStatusLabel(detailRecord.status) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailRecord.remark || "--" }}</el-descriptions-item>
        </el-descriptions>

        <PagePanel :title="currentConfig.itemTitle" :description="currentConfig.detailDescription" class="detail-panel">
          <el-table :data="detailRecord.items || []" stripe>
            <el-table-column prop="materialCode" label="物料编码" min-width="130" />
            <el-table-column prop="materialName" label="物料名称" min-width="160" />
            <el-table-column prop="warehouseName" label="仓库" min-width="120" />
            <el-table-column label="数量" min-width="100">
              <template #default="{ row }">{{ formatNumber(row.qty) }}</template>
            </el-table-column>
            <el-table-column
              v-if="currentConfig.canInStock"
              label="已到货"
              min-width="100"
            >
              <template #default="{ row }">{{ formatNumber(row.receivedQty) }}</template>
            </el-table-column>
            <el-table-column
              v-if="currentConfig.canInStock"
              label="合格数"
              min-width="100"
            >
              <template #default="{ row }">{{ formatNumber(row.qualifiedQty) }}</template>
            </el-table-column>
            <el-table-column label="单价" min-width="100">
              <template #default="{ row }">{{ formatMoney(row.price) }}</template>
            </el-table-column>
            <el-table-column label="金额" min-width="110">
              <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="需求日期" min-width="120">
              <template #default="{ row }">{{ formatDate(row.needDate) }}</template>
            </el-table-column>
          </el-table>
        </PagePanel>
      </template>
    </el-drawer>

    <el-dialog v-model="inStockVisible" title="采购订单入库" width="980px">
      <div class="form-grid">
        <el-select v-model="inStockForm.warehouseId" placeholder="入库仓库">
          <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-date-picker v-model="inStockForm.bizDate" type="date" value-format="YYYY-MM-DD" placeholder="业务日期" />
        <el-input v-model="inStockForm.remark" placeholder="入库备注" class="full-span" />
      </div>

      <el-alert
        v-if="inStockDetail"
        :title="`来源订单：${inStockDetail.code} / ${inStockDetail.supplierName || '未指定供应商'}`"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />

      <div v-if="inStockDetail" class="link-actions">
        <el-button plain @click="fillSuggestedLotNos">回填建议批次</el-button>
        <el-button plain @click="syncInStockQtyByQualified">按到货数量同步合格和入库</el-button>
      </div>

      <el-table :data="inStockForm.items" stripe>
        <el-table-column prop="materialCode" label="物料编码" min-width="130" />
        <el-table-column prop="materialName" label="物料名称" min-width="160" />
        <el-table-column prop="warehouseName" label="建议仓库" min-width="120" />
        <el-table-column label="需求日期" min-width="120">
          <template #default="{ row }">{{ formatDate(row.needDate) }}</template>
        </el-table-column>
        <el-table-column label="剩余未到货" min-width="110">
          <template #default="{ row }">{{ formatNumber(row.remainQty) }}</template>
        </el-table-column>
        <el-table-column label="到货数量" min-width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.receivedQty" :min="0" :precision="2" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="合格数量" min-width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.qualifiedQty" :min="0" :precision="2" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="入库数量" min-width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.qty" :min="0" :precision="2" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="批次号" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.lotNo" placeholder="可选批次号" />
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="inStockVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitInStock">确认入库</el-button>
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
  approvePurchaseOrder,
  approvePurchaseRequest,
  closePurchaseOrder,
  closePurchaseRequest,
  createPurchaseOrder,
  createPurchaseRequest,
  fetchMaterialList,
  fetchPurchaseOrderDetail,
  fetchPurchaseOrders,
  fetchPurchaseRequestDetail,
  fetchPurchaseRequests,
  fetchSupplierList,
  fetchWarehouseList,
  purchaseOrderInStock,
  updatePurchaseOrder,
  updatePurchaseRequest
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
  requests: {
    heroTitle: "采购申请与需求提交",
    heroDescription: "本页已接入采购申请的列表、详情、新增、编辑、审核和关闭接口，可作为采购需求流转入口。",
    panelTitle: "采购申请列表",
    panelDescription: "对应 `GET /api/v1/purchase/requests`",
    keywordPlaceholder: "搜索申请单号或供应商",
    shortTitle: "采购申请",
    itemTitle: "申请明细",
    detailDescription: "采购申请明细由后端详情接口返回",
    operationLabel: "操作",
    listApi: fetchPurchaseRequests,
    detailApi: fetchPurchaseRequestDetail,
    createApi: createPurchaseRequest,
    updateApi: updatePurchaseRequest,
    approveApi: approvePurchaseRequest,
    closeApi: closePurchaseRequest,
    canInStock: false
  },
  orders: {
    heroTitle: "采购执行与到货入库",
    heroDescription: "本页已接入采购订单的列表、详情、新增、编辑、审核、关闭和入库动作，能够展示采购执行闭环。",
    panelTitle: "采购订单列表",
    panelDescription: "对应 `GET /api/v1/purchase/orders`",
    keywordPlaceholder: "搜索订单号或供应商",
    shortTitle: "采购订单",
    itemTitle: "订单明细",
    detailDescription: "采购订单明细与到货数据由后端详情接口返回",
    operationLabel: "操作",
    listApi: fetchPurchaseOrders,
    detailApi: fetchPurchaseOrderDetail,
    createApi: createPurchaseOrder,
    updateApi: updatePurchaseOrder,
    approveApi: approvePurchaseOrder,
    closeApi: closePurchaseOrder,
    canInStock: true
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
const supplierOptions = ref([]);
const materialOptions = ref([]);
const warehouseOptions = ref([]);
const inStockVisible = ref(false);
const inStockOrderId = ref(null);
const inStockDetail = ref(null);
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
const inStockForm = reactive(createEmptyInStockForm());

const currentConfig = computed(() => DOC_CONFIG[activeTab.value]);
const dialogTitle = computed(() => `${editingId.value ? "编辑" : "新增"}${currentConfig.value.shortTitle}`);
const detailTitle = computed(() => `${currentConfig.value.shortTitle}详情`);

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
    warehouseId: undefined,
    qty: 1,
    price: 0,
    needDate: "",
    sourceItemId: undefined
  };
}

function createEmptyDoc() {
  return {
    supplierId: undefined,
    docDate: "",
    expectedDate: "",
    sourceDocId: undefined,
    sourceDocType: "",
    remark: "",
    items: [createEmptyItem()]
  };
}

function createEmptyInStockForm() {
  return {
    warehouseId: undefined,
    bizDate: getToday(),
    remark: "",
    items: []
  };
}

const patchForm = (payload = {}) => {
  Object.assign(formModel, createEmptyDoc(), payload);
};

const patchInStockForm = (payload = {}) => {
  Object.assign(inStockForm, createEmptyInStockForm(), payload);
};

const normalizeStatus = (status) => String(status || "").toLowerCase();
const isDetailRoute = (detailId) => Number.isFinite(detailId) && detailId > 0;

const loadOptions = async () => {
  const [supplierPage, materialPage, warehousePage] = await Promise.all([
    fetchSupplierList({ pageNo: 1, pageSize: 200, status: "enabled" }),
    fetchMaterialList({ pageNo: 1, pageSize: 200, status: "enabled" }),
    fetchWarehouseList({ pageNo: 1, pageSize: 200, status: "enabled" })
  ]);

  supplierOptions.value = supplierPage.records || [];
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
  inStockVisible.value = false;
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
      supplierId: detail.supplierId,
      docDate: detail.docDate,
      expectedDate: detail.expectedDate,
      sourceDocId: detail.sourceDocId,
      sourceDocType: detail.sourceDocType || "",
      remark: detail.remark,
      items: (detail.items || []).map((item) => ({
        materialId: item.materialId,
        warehouseId: item.warehouseId,
        qty: Number(item.qty || 0),
        price: Number(item.price || 0),
        needDate: item.needDate || "",
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
const canCloseDoc = (row) => ["approved", "partial", "completed", "draft"].includes(normalizeStatus(row.status));
const canConvertRequest = (row) => ["approved", "partial"].includes(normalizeStatus(row.status));

const validateForm = () => {
  if (!formModel.supplierId || !formModel.docDate) {
    ElMessage.warning("请先填写供应商和单据日期");
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

const convertRequestToOrder = async (id) => {
  try {
    const detail = await fetchPurchaseRequestDetail(id);
    const availableItems = (detail.items || [])
      .map((item) => ({
        sourceItemId: item.id,
        materialId: item.materialId,
        warehouseId: item.warehouseId,
        qty: Number(item.remainConvertQty ?? item.qty ?? 0),
        price: Number(item.price || 0),
        needDate: item.needDate || ""
      }))
      .filter((item) => item.materialId && item.qty > 0);

    if (!availableItems.length) {
      ElMessage.warning("该采购申请已无剩余可下推采购订单的明细");
      return;
    }

    activeTab.value = "orders";
    editingId.value = null;
    detailVisible.value = false;
    inStockVisible.value = false;
    pagination.pageNo = 1;
    await loadDocs();
    patchForm({
      supplierId: detail.supplierId,
      docDate: getToday(),
      expectedDate: detail.expectedDate || detail.docDate,
      sourceDocId: detail.id,
      sourceDocType: "request",
      remark: detail.remark ? `${detail.remark} | 来源申请：${detail.code}` : `来源申请：${detail.code}`,
      items: availableItems
    });
    formVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "采购申请转订单数据加载失败");
  }
};

const submitForm = async () => {
  if (!validateForm()) {
    return;
  }

  submitting.value = true;
  const payload = {
    supplierId: formModel.supplierId,
    docDate: formModel.docDate,
    expectedDate: formModel.expectedDate || null,
    sourceDocId: formModel.sourceDocId || null,
    sourceDocType: formModel.sourceDocType || null,
    remark: formModel.remark,
    items: formModel.items.map((item) => ({
      materialId: item.materialId,
      warehouseId: item.warehouseId || null,
      qty: Number(item.qty),
      price: Number(item.price),
      needDate: item.needDate || null,
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

const canInStock = (row) => ["approved", "partial"].includes(String(row.status || "").toLowerCase());

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

const buildSuggestedLotNo = (orderCode, bizDate, index) => {
  const normalizedDate = String(bizDate || getToday()).replaceAll("-", "").slice(2);
  return `${orderCode}-${normalizedDate}-${String(index + 1).padStart(2, "0")}`;
};

const resolvePreferredWarehouseId = (items = []) => {
  const candidateIds = items.map((item) => item.warehouseId).filter(Boolean);
  if (!candidateIds.length) {
    return undefined;
  }

  const warehouseCounter = candidateIds.reduce((counter, warehouseId) => {
    counter[warehouseId] = (counter[warehouseId] || 0) + 1;
    return counter;
  }, {});

  return Number(
    Object.entries(warehouseCounter).sort((left, right) => right[1] - left[1])[0]?.[0]
  );
};

const fillSuggestedLotNos = () => {
  if (!inStockDetail.value?.code) {
    return;
  }

  inStockForm.items = inStockForm.items.map((item, index) => ({
    ...item,
    lotNo: item.suggestedLotNo || buildSuggestedLotNo(inStockDetail.value.code, inStockForm.bizDate, index)
  }));
};

const syncInStockQtyByQualified = () => {
  inStockForm.items = inStockForm.items.map((item) => {
    const nextReceivedQty = Math.min(Number(item.remainQty || 0), Number(item.receivedQty || item.remainQty || 0));
    return {
      ...item,
      receivedQty: nextReceivedQty,
      qualifiedQty: nextReceivedQty,
      qty: nextReceivedQty
    };
  });
};

const openInStockDialog = async (id) => {
  try {
    const detail = await fetchPurchaseOrderDetail(id);
    const availableItems = (detail.items || [])
      .map((item, index) => {
        const remainQty = Math.max(Number(item.qty || 0) - Number(item.receivedQty || 0), 0);
        return {
          sourceItemId: item.id,
          materialId: item.materialId,
          materialCode: item.materialCode,
          materialName: item.materialName,
          warehouseId: item.warehouseId,
          warehouseName: item.warehouseName || "--",
          needDate: item.needDate,
          remainQty,
          receivedQty: remainQty,
          qualifiedQty: remainQty,
          qty: remainQty,
          lotNo: buildSuggestedLotNo(detail.code, getToday(), index),
          suggestedLotNo: buildSuggestedLotNo(detail.code, getToday(), index)
        };
      })
      .filter((item) => item.remainQty > 0);

    if (!availableItems.length) {
      ElMessage.warning("该采购订单已无可入库明细");
      return;
    }

    inStockOrderId.value = id;
    inStockDetail.value = detail;
    patchInStockForm({
      warehouseId: resolvePreferredWarehouseId(detail.items),
      bizDate: getToday(),
      remark: `${detail.code} 入库`,
      items: availableItems
    });
    inStockVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "采购入库详情加载失败");
  }
};

const submitInStock = async () => {
  if (!inStockOrderId.value) {
    ElMessage.warning("未找到来源采购订单");
    return;
  }
  if (!inStockForm.warehouseId || !inStockForm.bizDate) {
    ElMessage.warning("请先选择入库仓库和业务日期");
    return;
  }

  const validItems = inStockForm.items.filter((item) => Number(item.qty) > 0);
  if (!validItems.length) {
    ElMessage.warning("请至少填写一条入库明细");
    return;
  }

  for (const item of validItems) {
    const receivedQty = Number(item.receivedQty || 0);
    const qualifiedQty = Number(item.qualifiedQty || 0);
    const qty = Number(item.qty || 0);

    if (receivedQty <= 0) {
      ElMessage.warning(`物料 ${item.materialName} 的到货数量必须大于 0`);
      return;
    }
    if (receivedQty > Number(item.remainQty || 0)) {
      ElMessage.warning(`物料 ${item.materialName} 的到货数量不能超过剩余未到货数量`);
      return;
    }
    if (qualifiedQty > receivedQty) {
      ElMessage.warning(`物料 ${item.materialName} 的合格数量不能大于到货数量`);
      return;
    }
    if (qty > qualifiedQty) {
      ElMessage.warning(`物料 ${item.materialName} 的入库数量不能大于合格数量`);
      return;
    }
  }

  submitting.value = true;
  try {
    const stockDocId = await purchaseOrderInStock(inStockOrderId.value, {
      warehouseId: inStockForm.warehouseId,
      bizDate: inStockForm.bizDate,
      remark: inStockForm.remark,
      items: validItems.map((item) => ({
        sourceItemId: item.sourceItemId,
        materialId: item.materialId,
        lotNo: item.lotNo || null,
        receivedQty: Number(item.receivedQty),
        qualifiedQty: Number(item.qualifiedQty),
        qty: Number(item.qty)
      }))
    });

    ElMessage.success("采购入库成功");
    inStockVisible.value = false;
    await loadDocs();

    if (detailVisible.value && detailRecord.value?.id === inStockOrderId.value) {
      detailRecord.value = await fetchPurchaseOrderDetail(inStockOrderId.value);
    }
    await promptOpenInventoryDoc(stockDocId);
  } catch (error) {
    ElMessage.error(error.message || "采购入库失败");
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
    ElMessage.error(error.message || "采购模块初始化失败");
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
.link-actions {
  margin-top: 16px;
}

.detail-panel {
  margin-top: 18px;
}
</style>
