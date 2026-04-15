<template>
  <div class="page-grid" v-loading="loading">
    <section class="module-hero">
      <div>
        <h2>基础数据总控台</h2>
      </div>
      <div class="hero-kpis">
        <div class="hero-kpi" v-for="card in overviewCards" :key="card.title">
          <span>{{ card.title }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.hint }}</small>
        </div>
      </div>
    </section>

    <PagePanel title="主数据列表" :description="currentConfig.description">
      <template #actions>
        <div class="toolbar-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button v-if="currentConfig.formEnabled" type="primary" @click="openCreateDialog">新增{{ currentConfig.title }}</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane
          v-for="item in tabOptions"
          :key="item.name"
          :name="item.name"
          :label="item.label"
        />
      </el-tabs>

      <div class="filter-grid">
        <el-input v-model="filters.keyword" clearable placeholder="按编码、名称或联系人搜索" />
        <el-select v-model="filters.status" clearable placeholder="全部状态">
          <el-option label="启用" value="enabled" />
          <el-option label="停用" value="disabled" />
          <el-option label="草稿" value="draft" />
          <el-option label="已审核" value="approved" />
        </el-select>
        <el-button type="primary" @click="loadTableData">查询</el-button>
      </div>

      <el-table :data="rows" stripe>
        <el-table-column
          v-for="column in currentColumns"
          :key="column.prop"
          :prop="column.prop"
          :label="column.label"
          :min-width="column.width"
        >
          <template #default="{ row }">
            <span v-if="column.tagClass" class="table-tag" :class="column.tagClass(row)">
              {{ column.formatter ? column.formatter(row[column.prop], row) : row[column.prop] ?? "--" }}
            </span>
            <span v-else>
              {{ column.formatter ? column.formatter(row[column.prop], row) : row[column.prop] ?? "--" }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text @click="openDetailDrawer(row)">详情</el-button>
              <el-button v-if="currentConfig.formEnabled" text @click="openEditDialog(row)">编辑</el-button>
              <el-button
                v-if="currentConfig.deleteApi"
                text
                type="danger"
                @click="handleDelete(row)"
              >
                删除
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

    <el-dialog v-model="formVisible" :title="dialogTitle" :width="dialogWidth">
      <div v-if="editingType === 'materials'" class="form-grid">
        <div class="field-stack">
          <span class="field-label">物料编码</span>
          <el-input v-model="formModel.code" placeholder="物料编码" />
        </div>
        <div class="field-stack">
          <span class="field-label">物料名称</span>
          <el-input v-model="formModel.name" placeholder="物料名称" />
        </div>
        <div class="field-stack">
          <span class="field-label">规格型号</span>
          <el-input v-model="formModel.spec" placeholder="规格型号" />
        </div>
        <div class="field-stack">
          <span class="field-label">物料类型</span>
          <el-select v-model="formModel.materialType" placeholder="物料类型">
            <el-option label="原料" value="raw" />
            <el-option label="半成品" value="semi" />
            <el-option label="成品" value="finished" />
          </el-select>
        </div>
        <div class="field-stack">
          <span class="field-label">计量单位</span>
          <el-select v-model="formModel.unitCode" placeholder="计量单位">
            <el-option v-for="item in materialUnits" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="field-stack">
          <span class="field-label">安全库存</span>
          <el-input-number v-model="formModel.safetyStock" :min="0" :precision="2" controls-position="right" placeholder="安全库存" />
        </div>
        <div class="field-stack">
          <span class="field-label">批次管理</span>
          <el-select v-model="formModel.batchEnabled" placeholder="批次管理">
            <el-option label="启用" :value="1" />
            <el-option label="关闭" :value="0" />
          </el-select>
        </div>
        <div class="field-stack">
          <span class="field-label">默认仓库</span>
          <el-select v-model="formModel.defaultWarehouseId" clearable placeholder="默认仓库">
            <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </div>
        <div class="field-stack">
          <span class="field-label">物料状态</span>
          <el-select v-model="formModel.status" placeholder="物料状态">
            <el-option label="启用" value="enabled" />
            <el-option label="停用" value="disabled" />
          </el-select>
        </div>
      </div>

      <div v-else-if="editingType === 'customers' || editingType === 'suppliers'" class="form-grid">
        <el-input v-model="formModel.code" :placeholder="editingType === 'customers' ? '客户编码' : '供应商编码'" />
        <el-input v-model="formModel.name" :placeholder="editingType === 'customers' ? '客户名称' : '供应商名称'" />
        <el-input v-model="formModel.contact" placeholder="联系人" />
        <el-input v-model="formModel.phone" placeholder="联系电话" />
        <el-input v-model="formModel.address" placeholder="联系地址" class="full-span" />
        <el-select v-model="formModel.status" placeholder="状态">
          <el-option label="启用" value="enabled" />
          <el-option label="停用" value="disabled" />
        </el-select>
      </div>

      <div v-else-if="editingType === 'warehouses'" class="form-grid">
        <el-input v-model="formModel.code" placeholder="仓库编码" />
        <el-input v-model="formModel.name" placeholder="仓库名称" />
        <el-input v-model="formModel.warehouseType" placeholder="仓库类型" />
        <el-input v-model="formModel.managerName" placeholder="负责人" />
        <el-select v-model="formModel.status" placeholder="状态">
          <el-option label="启用" value="enabled" />
          <el-option label="停用" value="disabled" />
        </el-select>
      </div>

      <template v-else-if="editingType === 'boms'">
        <div class="form-grid">
          <el-select v-model="formModel.productId" filterable placeholder="选择产品物料">
            <el-option
              v-for="item in bomProductOptions"
              :key="item.id"
              :label="`${item.code} / ${item.name}`"
              :value="item.id"
            />
          </el-select>
          <el-input v-model="formModel.versionNo" placeholder="版本号" />
          <el-date-picker v-model="formModel.effectiveDate" type="date" value-format="YYYY-MM-DD" placeholder="生效日期" />
          <el-select v-model="formModel.status" placeholder="状态">
            <el-option label="草稿" value="draft" />
            <el-option label="已审核" value="approved" />
            <el-option label="启用" value="enabled" />
            <el-option label="停用" value="disabled" />
          </el-select>
        </div>

        <div class="subtable-head">
          <strong>BOM 子项</strong>
          <el-button type="primary" plain @click="addBomItem">新增子项</el-button>
        </div>

        <el-table :data="formModel.items" stripe>
          <el-table-column label="子项物料" min-width="260">
            <template #default="{ row }">
              <el-select v-model="row.materialId" filterable placeholder="选择子项物料">
                <el-option
                  v-for="item in bomChildOptions"
                  :key="item.id"
                  :label="`${item.code} / ${item.name}`"
                  :value="item.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="用量" min-width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.qty" :min="0" :precision="4" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="损耗率" min-width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.lossRate" :min="0" :precision="4" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="排序" min-width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.sortNo" :min="1" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="90" fixed="right">
            <template #default="{ $index }">
              <el-button text type="danger" @click="removeBomItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-else-if="editingType === 'routes'">
        <div class="form-grid">
          <el-select v-model="formModel.productId" filterable placeholder="选择产品物料">
            <el-option
              v-for="item in routeProductOptions"
              :key="item.id"
              :label="`${item.code} / ${item.name}`"
              :value="item.id"
            />
          </el-select>
          <el-input v-model="formModel.versionNo" placeholder="版本号" />
          <el-select v-model="formModel.status" placeholder="状态">
            <el-option label="草稿" value="draft" />
            <el-option label="已审核" value="approved" />
            <el-option label="启用" value="enabled" />
            <el-option label="停用" value="disabled" />
          </el-select>
        </div>

        <div class="subtable-head">
          <strong>工艺工序</strong>
          <el-button type="primary" plain @click="addRouteItem">新增工序</el-button>
        </div>

        <el-table :data="formModel.items" stripe>
          <el-table-column label="工序编号" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.processCode" placeholder="如 OP10" />
            </template>
          </el-table-column>
          <el-table-column label="工序名称" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.processName" placeholder="工序名称" />
            </template>
          </el-table-column>
          <el-table-column label="工作中心" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.workCenter" placeholder="工作中心" />
            </template>
          </el-table-column>
          <el-table-column label="标准工时" min-width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.standardHours" :min="0" :precision="2" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="排序" min-width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.sortNo" :min="1" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="90" fixed="right">
            <template #default="{ $index }">
              <el-button text type="danger" @click="removeRouteItem($index)">删除</el-button>
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

    <el-drawer v-model="detailVisible" :title="detailTitle" size="52%">
      <template v-if="detailRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item
            v-for="item in detailFields"
            :key="item.label"
            :label="item.label"
          >
            {{ item.value }}
          </el-descriptions-item>
        </el-descriptions>

        <PagePanel
          v-if="detailItems.length"
          title="明细列表"
          class="detail-panel"
        >
          <el-table :data="detailItems" stripe>
            <el-table-column
              v-for="column in detailItemColumns"
              :key="column.prop"
              :prop="column.prop"
              :label="column.label"
              :min-width="column.width"
            >
              <template #default="{ row }">
                {{ column.formatter ? column.formatter(row[column.prop], row) : row[column.prop] ?? "--" }}
              </template>
            </el-table-column>
          </el-table>
        </PagePanel>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";

import {
  createCustomer,
  createBom,
  createMaterial,
  createRoute,
  createSupplier,
  createWarehouse,
  deleteCustomer,
  deleteBom,
  deleteMaterial,
  deleteRoute,
  deleteSupplier,
  deleteWarehouse,
  fetchBomDetail,
  fetchBomList,
  fetchCustomerDetail,
  fetchCustomerList,
  fetchMaterialDetail,
  fetchMaterialList,
  fetchMaterialUnits,
  fetchRouteDetail,
  fetchRouteList,
  fetchSupplierDetail,
  fetchSupplierList,
  fetchWarehouseDetail,
  fetchWarehouseList,
  updateCustomer,
  updateBom,
  updateMaterial,
  updateRoute,
  updateSupplier,
  updateWarehouse
} from "@/api/modules";
import PagePanel from "@/components/PagePanel.vue";
import {
  formatDate,
  formatMaterialType,
  formatNumber,
  formatStatusLabel,
  getTagClass
} from "@/utils/format";

const loading = ref(false);
const submitting = ref(false);
const activeTab = ref("materials");
const rows = ref([]);
const materialUnits = ref([]);
const warehouseOptions = ref([]);
const materialOptions = ref([]);
const detailVisible = ref(false);
const detailRecord = ref(null);
const detailItems = ref([]);
const formVisible = ref(false);
const editingId = ref(null);
const editingType = ref("materials");
const overview = reactive({
  materials: 0,
  customers: 0,
  suppliers: 0,
  boms: 0
});
const filters = reactive({
  keyword: "",
  status: ""
});
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
});
const formModel = reactive(createEmptyForm("materials"));

const tabOptions = [
  { name: "materials", label: "物料" },
  { name: "customers", label: "客户" },
  { name: "suppliers", label: "供应商" },
  { name: "warehouses", label: "仓库" },
  { name: "boms", label: "BOM" },
  { name: "routes", label: "工艺路线" }
];

const configMap = {
  materials: {
    title: "物料",
    description: "支持物料分页查询、新增、修改和删除。",
    listApi: fetchMaterialList,
    detailApi: fetchMaterialDetail,
    createApi: createMaterial,
    updateApi: updateMaterial,
    deleteApi: deleteMaterial,
    formEnabled: true
  },
  customers: {
    title: "客户",
    description: "支持客户档案分页查询、新增、修改和删除。",
    listApi: fetchCustomerList,
    detailApi: fetchCustomerDetail,
    createApi: createCustomer,
    updateApi: updateCustomer,
    deleteApi: deleteCustomer,
    formEnabled: true
  },
  suppliers: {
    title: "供应商",
    description: "支持供应商档案分页查询、新增、修改和删除。",
    listApi: fetchSupplierList,
    detailApi: fetchSupplierDetail,
    createApi: createSupplier,
    updateApi: updateSupplier,
    deleteApi: deleteSupplier,
    formEnabled: true
  },
  warehouses: {
    title: "仓库",
    description: "支持仓库档案分页查询、新增、修改和删除。",
    listApi: fetchWarehouseList,
    detailApi: fetchWarehouseDetail,
    createApi: createWarehouse,
    updateApi: updateWarehouse,
    deleteApi: deleteWarehouse,
    formEnabled: true
  },
  boms: {
    title: "BOM",
    description: "支持 BOM 列表、详情、新增、修改和删除。",
    listApi: fetchBomList,
    detailApi: fetchBomDetail,
    createApi: createBom,
    updateApi: updateBom,
    deleteApi: deleteBom,
    formEnabled: true
  },
  routes: {
    title: "工艺路线",
    description: "支持工艺路线列表、详情、新增、修改和删除。",
    listApi: fetchRouteList,
    detailApi: fetchRouteDetail,
    createApi: createRoute,
    updateApi: updateRoute,
    deleteApi: deleteRoute,
    formEnabled: true
  }
};

const overviewCards = computed(() => [
  { title: "物料档案", value: overview.materials, hint: "直接驱动采购、销售和生产" },
  { title: "客户档案", value: overview.customers, hint: "销售订单与应收统计引用" },
  { title: "供应商档案", value: overview.suppliers, hint: "采购订单与应付统计引用" },
  { title: "BOM 版本", value: overview.boms, hint: "为 MRP 与工单提供基础结构" }
]);

const currentConfig = computed(() => configMap[activeTab.value]);
const dialogTitle = computed(() => `${editingId.value ? "编辑" : "新增"}${currentConfig.value.title}`);
const detailTitle = computed(() => `${currentConfig.value.title}详情`);
const dialogWidth = computed(() => (["boms", "routes"].includes(editingType.value) ? "980px" : "720px"));
const normalizeMaterialType = (value) => String(value || "").trim().toLowerCase();
const isProductMaterialType = (value) =>
  ["finished", "semi", "成品", "半成品"].includes(normalizeMaterialType(value) || String(value || "").trim());
const isBomChildMaterialType = (value) =>
  ["raw", "semi", "原料", "半成品"].includes(normalizeMaterialType(value) || String(value || "").trim());
const bomProductOptions = computed(() =>
  materialOptions.value.filter((item) => isProductMaterialType(item.materialType))
);
const routeProductOptions = computed(() => bomProductOptions.value);
const bomChildOptions = computed(() =>
  materialOptions.value
    .filter((item) => isBomChildMaterialType(item.materialType))
    .filter((item) => Number(item.id) !== Number(formModel.productId))
);

const currentColumns = computed(() => {
  if (activeTab.value === "materials") {
    return [
      { prop: "code", label: "物料编码", width: 130 },
      { prop: "name", label: "物料名称", width: 160 },
      { prop: "spec", label: "规格", width: 120 },
      { prop: "materialType", label: "类型", width: 100, formatter: formatMaterialType },
      { prop: "unitCode", label: "单位", width: 90 },
      { prop: "safetyStock", label: "安全库存", width: 110, formatter: formatNumber },
      { prop: "status", label: "状态", width: 100, formatter: formatStatusLabel, tagClass: (row) => getTagClass(row.status) }
    ];
  }

  if (activeTab.value === "customers" || activeTab.value === "suppliers") {
    return [
      { prop: "code", label: "编码", width: 130 },
      { prop: "name", label: "名称", width: 160 },
      { prop: "contact", label: "联系人", width: 110 },
      { prop: "phone", label: "电话", width: 130 },
      { prop: "address", label: "地址", width: 180 },
      { prop: "status", label: "状态", width: 100, formatter: formatStatusLabel, tagClass: (row) => getTagClass(row.status) }
    ];
  }

  if (activeTab.value === "warehouses") {
    return [
      { prop: "code", label: "仓库编码", width: 130 },
      { prop: "name", label: "仓库名称", width: 160 },
      { prop: "warehouseType", label: "仓库类型", width: 120 },
      { prop: "managerName", label: "负责人", width: 110 },
      { prop: "status", label: "状态", width: 100, formatter: formatStatusLabel, tagClass: (row) => getTagClass(row.status) }
    ];
  }

  if (activeTab.value === "boms") {
    return [
      { prop: "productCode", label: "产品编码", width: 130 },
      { prop: "productName", label: "产品名称", width: 160 },
      { prop: "versionNo", label: "版本号", width: 100 },
      { prop: "effectiveDate", label: "生效日期", width: 120, formatter: formatDate },
      { prop: "status", label: "状态", width: 100, formatter: formatStatusLabel, tagClass: (row) => getTagClass(row.status) }
    ];
  }

  return [
    { prop: "productCode", label: "产品编码", width: 130 },
    { prop: "productName", label: "产品名称", width: 160 },
    { prop: "versionNo", label: "版本号", width: 100 },
    { prop: "status", label: "状态", width: 100, formatter: formatStatusLabel, tagClass: (row) => getTagClass(row.status) }
  ];
});

const detailFields = computed(() => {
  if (!detailRecord.value) {
    return [];
  }

  const record = detailRecord.value;

  if (activeTab.value === "materials") {
    return [
      { label: "物料编码", value: record.code || "--" },
      { label: "物料名称", value: record.name || "--" },
      { label: "规格", value: record.spec || "--" },
      { label: "物料类型", value: formatMaterialType(record.materialType) },
      { label: "计量单位", value: record.unitCode || "--" },
      { label: "安全库存", value: formatNumber(record.safetyStock) },
      { label: "默认仓库", value: record.defaultWarehouseName || "--" },
      { label: "状态", value: formatStatusLabel(record.status) }
    ];
  }

  if (activeTab.value === "customers" || activeTab.value === "suppliers") {
    return [
      { label: "编码", value: record.code || "--" },
      { label: "名称", value: record.name || "--" },
      { label: "联系人", value: record.contact || "--" },
      { label: "电话", value: record.phone || "--" },
      { label: "地址", value: record.address || "--" },
      { label: "状态", value: formatStatusLabel(record.status) }
    ];
  }

  if (activeTab.value === "warehouses") {
    return [
      { label: "仓库编码", value: record.code || "--" },
      { label: "仓库名称", value: record.name || "--" },
      { label: "仓库类型", value: record.warehouseType || "--" },
      { label: "负责人", value: record.managerName || "--" },
      { label: "状态", value: formatStatusLabel(record.status) }
    ];
  }

  if (activeTab.value === "boms") {
    return [
      { label: "产品编码", value: record.productCode || "--" },
      { label: "产品名称", value: record.productName || "--" },
      { label: "版本号", value: record.versionNo || "--" },
      { label: "生效日期", value: formatDate(record.effectiveDate) },
      { label: "状态", value: formatStatusLabel(record.status) }
    ];
  }

  return [
    { label: "产品编码", value: record.productCode || "--" },
    { label: "产品名称", value: record.productName || "--" },
    { label: "版本号", value: record.versionNo || "--" },
    { label: "状态", value: formatStatusLabel(record.status) }
  ];
});

const detailItemColumns = computed(() => {
  if (activeTab.value === "boms") {
    return [
      { prop: "materialCode", label: "子项编码", width: 130 },
      { prop: "materialName", label: "子项名称", width: 160 },
      { prop: "qty", label: "用量", width: 100, formatter: formatNumber },
      { prop: "lossRate", label: "损耗率", width: 100, formatter: formatNumber },
      { prop: "sortNo", label: "排序", width: 90 }
    ];
  }

  if (activeTab.value === "routes") {
    return [
      { prop: "processCode", label: "工序编号", width: 130 },
      { prop: "processName", label: "工序名称", width: 160 },
      { prop: "workCenter", label: "工作中心", width: 130 },
      { prop: "standardHours", label: "标准工时", width: 110, formatter: formatNumber },
      { prop: "sortNo", label: "排序", width: 90 }
    ];
  }

  return [];
});

function createEmptyForm(type) {
  if (type === "materials") {
    return {
      code: "",
      name: "",
      spec: "",
      materialType: "",
      unitCode: "",
      safetyStock: 0,
      batchEnabled: 1,
      defaultWarehouseId: undefined,
      status: "enabled"
    };
  }

  if (type === "warehouses") {
    return {
      code: "",
      name: "",
      warehouseType: "",
      managerName: "",
      status: "enabled"
    };
  }

  if (type === "boms") {
    return {
      productId: undefined,
      versionNo: "",
      effectiveDate: "",
      status: "draft",
      items: [createEmptyBomItem()]
    };
  }

  if (type === "routes") {
    return {
      productId: undefined,
      versionNo: "",
      status: "draft",
      items: [createEmptyRouteItem()]
    };
  }

  return {
    code: "",
    name: "",
    contact: "",
    phone: "",
    address: "",
    status: "enabled"
  };
}

function createEmptyBomItem() {
  return {
    materialId: undefined,
    qty: 1,
    lossRate: 0,
    sortNo: 1
  };
}

function createEmptyRouteItem() {
  return {
    id: undefined,
    processCode: "",
    processName: "",
    workCenter: "",
    standardHours: 0,
    sortNo: 1
  };
}

const patchFormModel = (payload) => {
  Object.keys(formModel).forEach((key) => {
    delete formModel[key];
  });
  Object.assign(formModel, createEmptyForm(editingType.value), payload);
};

const loadOverview = async () => {
  const [materialPage, customerPage, supplierPage, bomPage] = await Promise.all([
    fetchMaterialList({ pageNo: 1, pageSize: 1 }),
    fetchCustomerList({ pageNo: 1, pageSize: 1 }),
    fetchSupplierList({ pageNo: 1, pageSize: 1 }),
    fetchBomList({ pageNo: 1, pageSize: 1 })
  ]);

  overview.materials = materialPage.total || 0;
  overview.customers = customerPage.total || 0;
  overview.suppliers = supplierPage.total || 0;
  overview.boms = bomPage.total || 0;
};

const loadOptions = async () => {
  const [unitData, warehousePage, materialPage] = await Promise.all([
    fetchMaterialUnits(),
    fetchWarehouseList({ pageNo: 1, pageSize: 200 }),
    fetchMaterialList({ pageNo: 1, pageSize: 500 })
  ]);

  materialUnits.value = unitData || [];
  warehouseOptions.value = warehousePage.records || [];
  materialOptions.value = materialPage.records || [];
};

const loadTableData = async () => {
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
    ElMessage.error(error.message || "主数据加载失败");
  } finally {
    loading.value = false;
  }
};

const handleTabChange = async () => {
  pagination.pageNo = 1;
  filters.keyword = "";
  filters.status = "";
  await loadTableData();
};

const resetFilters = () => {
  filters.keyword = "";
  filters.status = "";
  pagination.pageNo = 1;
  loadTableData();
};

const handlePageChange = (pageNo) => {
  pagination.pageNo = pageNo;
  loadTableData();
};

const handleSizeChange = (pageSize) => {
  pagination.pageSize = pageSize;
  pagination.pageNo = 1;
  loadTableData();
};

const openCreateDialog = () => {
  editingType.value = activeTab.value;
  editingId.value = null;
  patchFormModel({});
  formVisible.value = true;
};

const openEditDialog = async (row) => {
  editingType.value = activeTab.value;
  editingId.value = row.id;

  try {
    const detail = await currentConfig.value.detailApi(row.id);
    if (editingType.value === "boms") {
      patchFormModel({
        productId: detail.productId,
        versionNo: detail.versionNo,
        effectiveDate: detail.effectiveDate,
        status: detail.status,
        items: (detail.items || []).map((item) => ({
          materialId: item.materialId,
          qty: Number(item.qty || 0),
          lossRate: Number(item.lossRate || 0),
          sortNo: item.sortNo || 1
        }))
      });
    } else if (editingType.value === "routes") {
      patchFormModel({
        productId: detail.productId,
        versionNo: detail.versionNo,
        status: detail.status,
        items: (detail.items || []).map((item) => ({
          id: item.id,
          processCode: item.processCode || "",
          processName: item.processName || "",
          workCenter: item.workCenter || "",
          standardHours: Number(item.standardHours || 0),
          sortNo: item.sortNo || 1
        }))
      });
    } else {
      patchFormModel(detail);
    }
    formVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "详情加载失败");
  }
};

const addBomItem = () => {
  formModel.items.push(createEmptyBomItem());
};

const removeBomItem = (index) => {
  if (formModel.items.length === 1) {
    ElMessage.warning("至少保留一条 BOM 子项");
    return;
  }
  formModel.items.splice(index, 1);
};

const addRouteItem = () => {
  formModel.items.push(createEmptyRouteItem());
};

const removeRouteItem = (index) => {
  if (formModel.items.length === 1) {
    ElMessage.warning("至少保留一条工艺工序");
    return;
  }
  formModel.items.splice(index, 1);
};

const submitForm = async () => {
  if (editingType.value === "boms") {
    if (!formModel.productId || !formModel.effectiveDate || !formModel.status) {
      ElMessage.warning("请先填写完整的 BOM 基础信息");
      return;
    }

    const validItems = formModel.items.filter((item) => item.materialId && Number(item.qty) > 0);
    if (!validItems.length || validItems.length !== formModel.items.length) {
      ElMessage.warning("请完善每一条 BOM 子项");
      return;
    }
  } else if (editingType.value === "routes") {
    if (!formModel.productId || !formModel.status) {
      ElMessage.warning("请先填写完整的工艺路线基础信息");
      return;
    }

    const validItems = formModel.items.filter((item) => item.processCode && item.processName);
    if (!validItems.length || validItems.length !== formModel.items.length) {
      ElMessage.warning("请完善每一条工艺工序");
      return;
    }
  } else if (!formModel.code || !formModel.name || !formModel.status) {
    ElMessage.warning("请先填写完整的基础信息");
    return;
  }

  if (editingType.value === "materials" && (!formModel.materialType || !formModel.unitCode)) {
    ElMessage.warning("请完善物料类型和计量单位");
    return;
  }

  submitting.value = true;

  try {
    let payload = formModel;

    if (editingType.value === "boms") {
      payload = {
        productId: formModel.productId,
        versionNo: formModel.versionNo || null,
        effectiveDate: formModel.effectiveDate,
        status: formModel.status,
        items: formModel.items.map((item) => ({
          materialId: item.materialId,
          qty: Number(item.qty),
          lossRate: Number(item.lossRate || 0),
          sortNo: Number(item.sortNo || 1)
        }))
      };
    } else if (editingType.value === "routes") {
      payload = {
        productId: formModel.productId,
        versionNo: formModel.versionNo || null,
        status: formModel.status,
        items: formModel.items.map((item) => ({
          id: item.id || null,
          processCode: item.processCode,
          processName: item.processName,
          workCenter: item.workCenter || null,
          standardHours: Number(item.standardHours || 0),
          sortNo: Number(item.sortNo || 1)
        }))
      };
    }

    if (editingId.value) {
      await currentConfig.value.updateApi(editingId.value, payload);
    } else {
      await currentConfig.value.createApi(payload);
    }

    ElMessage.success(`${dialogTitle.value}成功`);
    formVisible.value = false;
    await Promise.all([loadOverview(), loadTableData()]);
  } catch (error) {
    ElMessage.error(error.message || "保存失败");
  } finally {
    submitting.value = false;
  }
};

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除${currentConfig.value.title}“${row.name || row.code || row.productName || row.productCode}”吗？`,
      "删除确认",
      {
      type: "warning"
      }
    );

    await currentConfig.value.deleteApi(row.id);
    ElMessage.success("删除成功");
    await Promise.all([loadOverview(), loadTableData()]);
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error.message || "删除失败");
    }
  }
};

const openDetailDrawer = async (row) => {
  try {
    const detail = await currentConfig.value.detailApi(row.id);
    detailRecord.value = detail;
    detailItems.value = detail.items || [];
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "详情加载失败");
  }
};

onMounted(async () => {
  try {
    await Promise.all([loadOverview(), loadOptions()]);
    await loadTableData();
  } catch (error) {
    ElMessage.error(error.message || "基础数据初始化失败");
  }
});
</script>

<style scoped>
small {
  display: block;
  margin-top: 6px;
  color: var(--text-soft);
}

.field-stack {
  display: grid;
  gap: 8px;
}

.field-label {
  color: var(--text-soft);
  font-size: 13px;
  font-weight: 600;
}

.detail-panel {
  margin-top: 18px;
}
</style>
