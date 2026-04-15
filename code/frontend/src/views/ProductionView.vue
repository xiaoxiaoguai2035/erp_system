<template>
  <div class="page-grid" v-loading="initializing || submitting">
    <section class="module-hero">
      <div>
        <h2>生产计划、MRP 与工单执行中心</h2>
      </div>
      <div class="hero-kpis">
        <div class="hero-kpi">
          <span>生产计划</span>
          <strong>{{ planPagination.total }}</strong>
          <small>当前筛选下待排产任务</small>
        </div>
        <div class="hero-kpi">
          <span>生产工单</span>
          <strong>{{ workOrderPagination.total }}</strong>
          <small>包含草稿、执行中与完工工单</small>
        </div>
        <div class="hero-kpi">
          <span>MRP 建议</span>
          <strong>{{ mrpSuggestionCount }}</strong>
          <small>含采购建议与工单建议</small>
        </div>
        <div class="hero-kpi">
          <span>报工记录</span>
          <strong>{{ reportPagination.total }}</strong>
          <small>当前筛选条件下的报工数</small>
        </div>
      </div>
    </section>

    <PagePanel title="生产管理" description="按计划、MRP、工单、报工分栏切换，避免整个页面一次性堆满。">
      <template #actions>
        <el-button text @click="refreshProductionWorkspace">刷新全部</el-button>
      </template>

      <el-tabs v-model="activeSection" class="production-tabs">
        <el-tab-pane label="生产计划" name="plans">
          <div class="filter-grid">
            <el-input v-model="planFilters.keyword" clearable placeholder="搜索计划单号或产品" />
            <el-select v-model="planFilters.status" clearable placeholder="全部状态">
              <el-option label="草稿" value="draft" />
              <el-option label="已审核" value="approved" />
              <el-option label="执行中" value="in_progress" />
              <el-option label="已完成" value="completed" />
              <el-option label="已关闭" value="closed" />
            </el-select>
            <el-button type="primary" @click="loadPlans">查询</el-button>
            <el-button @click="resetPlanFilters">重置</el-button>
            <el-button type="primary" plain :disabled="!selectedPlanIds.length" @click="runMrpCalculation">执行 MRP</el-button>
            <el-button type="primary" @click="openCreatePlanDialog">新增计划</el-button>
          </div>

          <div v-loading="planLoading">
            <el-table :data="planRows" stripe row-key="id" @selection-change="handlePlanSelectionChange">
              <el-table-column type="selection" width="52" :selectable="planSelectable" />
              <el-table-column prop="code" label="计划单号" min-width="150" />
              <el-table-column prop="materialCode" label="产品编码" min-width="120" />
              <el-table-column prop="materialName" label="产品名称" min-width="160" />
              <el-table-column label="计划数量" min-width="110">
                <template #default="{ row }">{{ formatNumber(row.planQty) }}</template>
              </el-table-column>
              <el-table-column label="开始日期" min-width="110">
                <template #default="{ row }">{{ formatDate(row.startDate) }}</template>
              </el-table-column>
              <el-table-column label="结束日期" min-width="110">
                <template #default="{ row }">{{ formatDate(row.endDate) }}</template>
              </el-table-column>
              <el-table-column label="状态" min-width="100">
                <template #default="{ row }">
                  <span class="table-tag" :class="getTagClass(row.status)">{{ formatStatusLabel(row.status) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="来源销售单" min-width="120">
                <template #default="{ row }">
                  <el-button v-if="row.sourceSalesId" text type="primary" @click="openSourceSalesOrder(row.sourceSalesId)">
                    #{{ row.sourceSalesId }}
                  </el-button>
                  <span v-else>--</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="260" fixed="right">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button text @click="openPlanDetailDrawer(row.id)">详情</el-button>
                    <el-button text :disabled="!canEditPlan(row)" @click="openEditPlanDialog(row.id)">编辑</el-button>
                    <el-button text type="success" :disabled="!canApprovePlan(row)" @click="approvePlan(row.id)">审核</el-button>
                    <el-button text type="danger" :disabled="!canClosePlan(row)" @click="closePlan(row.id)">关闭</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrap">
              <el-pagination
                background
                layout="total, prev, pager, next, sizes"
                :current-page="planPagination.pageNo"
                :page-size="planPagination.pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="planPagination.total"
                @current-change="handlePlanPageChange"
                @size-change="handlePlanSizeChange"
              />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="MRP 结果" name="mrp">
          <div class="toolbar-actions production-tab-actions">
            <el-button :disabled="!mrpResult" @click="clearMrpResult">清空结果</el-button>
            <el-button type="primary" plain :disabled="!selectedPurchaseSuggestionIds.length" @click="generatePurchaseDocs">
              生成采购建议
            </el-button>
            <el-button type="primary" :disabled="!selectedWorkOrderSuggestionIds.length" @click="generateWorkOrderDocs">
              生成工单
            </el-button>
          </div>

          <div v-loading="mrpLoading">
            <template v-if="mrpResult">
              <el-alert
                :title="`MRP 任务号：${mrpResult.taskKey}`"
                type="success"
                :closable="false"
                style="margin-bottom: 18px"
              />

              <div class="mrp-grid">
                <div class="mrp-block">
                  <div class="mrp-head">
                    <div>
                      <strong>采购建议</strong>
                    </div>
                    <span class="table-tag tag-warning">{{ mrpResult.purchaseSuggestions?.length || 0 }} 条</span>
                  </div>

                  <el-table
                    :data="mrpResult.purchaseSuggestions || []"
                    stripe
                    row-key="id"
                    @selection-change="handlePurchaseSuggestionSelectionChange"
                  >
                    <el-table-column type="selection" width="52" />
                    <el-table-column prop="materialCode" label="物料编码" min-width="120" />
                    <el-table-column prop="materialName" label="物料名称" min-width="150" />
                    <el-table-column label="需求数量" min-width="110">
                      <template #default="{ row }">{{ formatNumber(row.requiredQty) }}</template>
                    </el-table-column>
                    <el-table-column label="可用数量" min-width="110">
                      <template #default="{ row }">{{ formatNumber(row.availableQty) }}</template>
                    </el-table-column>
                    <el-table-column label="缺口数量" min-width="110">
                      <template #default="{ row }">{{ formatNumber(row.shortageQty) }}</template>
                    </el-table-column>
                    <el-table-column label="需求日期" min-width="110">
                      <template #default="{ row }">{{ formatDate(row.needDate) }}</template>
                    </el-table-column>
                  </el-table>
                </div>

                <div class="mrp-block">
                  <div class="mrp-head">
                    <div>
                      <strong>工单建议</strong>
                    </div>
                    <span class="table-tag tag-success">{{ mrpResult.workOrderSuggestions?.length || 0 }} 条</span>
                  </div>

                  <el-table
                    :data="mrpResult.workOrderSuggestions || []"
                    stripe
                    row-key="id"
                    @selection-change="handleWorkOrderSuggestionSelectionChange"
                  >
                    <el-table-column type="selection" width="52" />
                    <el-table-column prop="materialCode" label="产品编码" min-width="120" />
                    <el-table-column prop="materialName" label="产品名称" min-width="150" />
                    <el-table-column label="计划数量" min-width="110">
                      <template #default="{ row }">{{ formatNumber(row.planQty) }}</template>
                    </el-table-column>
                    <el-table-column label="BOM" min-width="90">
                      <template #default="{ row }">#{{ row.bomId }}</template>
                    </el-table-column>
                    <el-table-column label="工艺路线" min-width="90">
                      <template #default="{ row }">#{{ row.routeId }}</template>
                    </el-table-column>
                    <el-table-column label="开工日期" min-width="110">
                      <template #default="{ row }">{{ formatDate(row.startDate) }}</template>
                    </el-table-column>
                    <el-table-column label="完工日期" min-width="110">
                      <template #default="{ row }">{{ formatDate(row.endDate) }}</template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </template>

            <el-empty v-else description="请先从生产计划中勾选已审核或执行中的计划，再执行 MRP 计算" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="工单管理" name="workorders">
          <div class="filter-grid">
            <el-input v-model="workOrderFilters.keyword" clearable placeholder="搜索工单号或产品" />
            <el-select v-model="workOrderFilters.status" clearable placeholder="全部状态">
              <el-option label="草稿" value="draft" />
              <el-option label="已审核" value="approved" />
              <el-option label="执行中" value="in_progress" />
              <el-option label="已完成" value="completed" />
              <el-option label="已关闭" value="closed" />
            </el-select>
            <el-button type="primary" @click="loadWorkOrders">查询</el-button>
            <el-button @click="resetWorkOrderFilters">重置</el-button>
            <el-button type="primary" @click="openCreateWorkOrderDialog">新增工单</el-button>
          </div>

          <div v-loading="workOrderLoading">
            <el-table :data="workOrderRows" stripe>
              <el-table-column prop="code" label="工单号" min-width="150" />
              <el-table-column label="来源计划" min-width="100">
                <template #default="{ row }">#{{ row.planId }}</template>
              </el-table-column>
              <el-table-column prop="materialCode" label="产品编码" min-width="120" />
              <el-table-column prop="materialName" label="产品名称" min-width="160" />
              <el-table-column label="计划数量" min-width="110">
                <template #default="{ row }">{{ formatNumber(row.planQty) }}</template>
              </el-table-column>
              <el-table-column label="已完工" min-width="110">
                <template #default="{ row }">{{ formatNumber(row.finishedQty) }}</template>
              </el-table-column>
              <el-table-column label="状态" min-width="100">
                <template #default="{ row }">
                  <span class="table-tag" :class="getTagClass(row.status)">{{ formatStatusLabel(row.status) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="开工日期" min-width="110">
                <template #default="{ row }">{{ formatDate(row.startDate) }}</template>
              </el-table-column>
              <el-table-column label="完工日期" min-width="110">
                <template #default="{ row }">{{ formatDate(row.endDate) }}</template>
              </el-table-column>
              <el-table-column label="操作" min-width="520" fixed="right">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button text @click="openWorkOrderDetailDrawer(row.id)">详情</el-button>
                    <el-button text :disabled="!canEditWorkOrder(row)" @click="openEditWorkOrderDialog(row.id)">编辑</el-button>
                    <el-button text type="success" :disabled="!canApproveWorkOrder(row)" @click="approveWorkOrder(row.id)">审核</el-button>
                    <el-button text type="danger" :disabled="!canCloseWorkOrder(row)" @click="closeWorkOrder(row.id)">关闭</el-button>
                    <el-button text type="primary" :disabled="!canExecuteWorkOrder(row)" @click="openPickDialog(row.id)">领料</el-button>
                    <el-button text type="primary" :disabled="!canExecuteWorkOrder(row)" @click="openReportDialog(row)">报工</el-button>
                    <el-button text type="primary" :disabled="!canExecuteWorkOrder(row)" @click="openFinishDialog(row.id)">完工</el-button>
                    <el-button text @click="openProgressDrawer(row.id)">进度</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrap">
              <el-pagination
                background
                layout="total, prev, pager, next, sizes"
                :current-page="workOrderPagination.pageNo"
                :page-size="workOrderPagination.pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="workOrderPagination.total"
                @current-change="handleWorkOrderPageChange"
                @size-change="handleWorkOrderSizeChange"
              />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="报工记录" name="reports">
          <div class="filter-grid report-filter-grid">
            <el-select v-model="reportFilters.workOrderId" clearable filterable placeholder="按工单筛选">
              <el-option
                v-for="item in workOrderOptions"
                :key="item.id"
                :label="`${item.code} / ${item.materialName || '未命名产品'}`"
                :value="item.id"
              />
            </el-select>
            <el-button type="primary" @click="loadReports">查询</el-button>
            <el-button @click="resetReportFilters">重置</el-button>
            <el-button type="primary" plain @click="openReportDialogFromFilter">新增报工</el-button>
          </div>

          <div v-loading="reportLoading">
            <el-table :data="reportRows" stripe>
              <el-table-column prop="workOrderCode" label="工单号" min-width="150" />
              <el-table-column prop="processName" label="工序" min-width="150" />
              <el-table-column label="报工时间" min-width="170">
                <template #default="{ row }">{{ formatDateTime(row.reportDate) }}</template>
              </el-table-column>
              <el-table-column label="报工数量" min-width="110">
                <template #default="{ row }">{{ formatNumber(row.reportQty) }}</template>
              </el-table-column>
              <el-table-column label="合格数量" min-width="110">
                <template #default="{ row }">{{ formatNumber(row.qualifiedQty) }}</template>
              </el-table-column>
              <el-table-column label="不良数量" min-width="110">
                <template #default="{ row }">{{ formatNumber(row.defectiveQty) }}</template>
              </el-table-column>
              <el-table-column prop="reporterName" label="报工人" min-width="120" />
              <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
            </el-table>

            <div class="pagination-wrap">
              <el-pagination
                background
                layout="total, prev, pager, next, sizes"
                :current-page="reportPagination.pageNo"
                :page-size="reportPagination.pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="reportPagination.total"
                @current-change="handleReportPageChange"
                @size-change="handleReportSizeChange"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </PagePanel>

    <el-dialog v-model="planFormVisible" :title="planDialogTitle" width="680px">
      <div class="form-grid">
        <el-select v-model="planForm.materialId" filterable placeholder="选择产品">
          <el-option
            v-for="item in materialOptions"
            :key="item.id"
            :label="`${item.code} / ${item.name}`"
            :value="item.id"
          />
        </el-select>
        <el-input-number v-model="planForm.planQty" :min="0" :precision="2" controls-position="right" placeholder="计划数量" />
        <el-date-picker v-model="planForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" />
        <el-date-picker v-model="planForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" />
        <el-input-number v-model="planForm.sourceSalesId" :min="0" controls-position="right" placeholder="来源销售单 ID" />
        <el-select v-model="planForm.status" placeholder="状态">
          <el-option label="草稿" value="draft" />
          <el-option label="已审核" value="approved" />
        </el-select>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="planFormVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitPlanForm">保存计划</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="workOrderFormVisible" :title="workOrderDialogTitle" width="760px">
      <div class="form-grid">
        <el-select
          v-model="workOrderForm.planId"
          filterable
          placeholder="选择来源计划"
          @change="handleWorkOrderPlanChange"
        >
          <el-option
            v-for="item in planOptions"
            :key="item.id"
            :label="`${item.code} / ${item.materialName || '未命名产品'}`"
            :value="item.id"
          />
        </el-select>
        <el-select
          v-model="workOrderForm.materialId"
          filterable
          placeholder="选择产品"
          @change="handleWorkOrderMaterialChange"
        >
          <el-option
            v-for="item in materialOptions"
            :key="item.id"
            :label="`${item.code} / ${item.name}`"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="workOrderForm.bomId" filterable placeholder="选择 BOM">
          <el-option
            v-for="item in filteredBomOptions"
            :key="item.id"
            :label="`${item.productCode} / ${item.versionNo}`"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="workOrderForm.routeId" filterable placeholder="选择工艺路线">
          <el-option
            v-for="item in filteredRouteOptions"
            :key="item.id"
            :label="`${item.productCode} / ${item.versionNo}`"
            :value="item.id"
          />
        </el-select>
        <el-input-number v-model="workOrderForm.planQty" :min="0" :precision="2" controls-position="right" placeholder="计划数量" />
        <el-select v-model="workOrderForm.status" placeholder="状态">
          <el-option label="草稿" value="draft" />
          <el-option label="已审核" value="approved" />
        </el-select>
        <el-date-picker v-model="workOrderForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开工日期" />
        <el-date-picker v-model="workOrderForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="完工日期" />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="workOrderFormVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitWorkOrderForm">保存工单</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="pickVisible" title="工单领料" width="980px">
      <div class="form-grid">
        <el-select v-model="pickForm.warehouseId" filterable placeholder="领料仓库">
          <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-date-picker v-model="pickForm.bizDate" type="date" value-format="YYYY-MM-DD" placeholder="业务日期" />
        <el-input v-model="pickForm.remark" placeholder="领料备注" class="full-span" />
      </div>

      <el-alert
        v-if="pickWorkOrderRecord"
        :title="`来源工单：${pickWorkOrderRecord.code} / ${pickWorkOrderRecord.materialName || '未命名产品'}`"
        type="info"
        :closable="false"
        style="margin: 16px 0"
      />

      <el-table :data="pickForm.items" stripe>
        <el-table-column prop="materialCode" label="物料编码" min-width="130" />
        <el-table-column prop="materialName" label="物料名称" min-width="160" />
        <el-table-column label="应领数量" min-width="110">
          <template #default="{ row }">{{ formatNumber(row.requiredQty) }}</template>
        </el-table-column>
        <el-table-column label="已领数量" min-width="110">
          <template #default="{ row }">{{ formatNumber(row.pickedQty) }}</template>
        </el-table-column>
        <el-table-column label="剩余应领" min-width="110">
          <template #default="{ row }">{{ formatNumber(row.remainQty) }}</template>
        </el-table-column>
        <el-table-column label="本次领料" min-width="120">
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
          <el-button @click="pickVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitPickForm">确认领料</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="finishVisible" title="工单完工入库" width="920px">
      <div class="form-grid">
        <el-select v-model="finishForm.warehouseId" filterable placeholder="入库仓库">
          <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-date-picker v-model="finishForm.bizDate" type="date" value-format="YYYY-MM-DD" placeholder="业务日期" />
        <el-input v-model="finishForm.remark" placeholder="入库备注" class="full-span" />
      </div>

      <el-alert
        v-if="finishWorkOrderRecord"
        :title="`来源工单：${finishWorkOrderRecord.code} / ${finishWorkOrderRecord.materialName || '未命名产品'}`"
        type="info"
        :closable="false"
        style="margin: 16px 0"
      />

      <el-table :data="finishForm.items" stripe>
        <el-table-column prop="materialCode" label="产品编码" min-width="130" />
        <el-table-column prop="materialName" label="产品名称" min-width="160" />
        <el-table-column label="剩余计划" min-width="110">
          <template #default="{ row }">{{ formatNumber(row.remainPlanQty) }}</template>
        </el-table-column>
        <el-table-column label="可入库合格量" min-width="130">
          <template #default="{ row }">{{ formatNumber(row.availableFinishQty) }}</template>
        </el-table-column>
        <el-table-column label="本次入库" min-width="120">
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
          <el-button @click="finishVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitFinishForm">确认入库</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="reportVisible" title="工单报工" width="720px">
      <div class="form-grid">
        <el-select v-model="reportForm.workOrderId" disabled placeholder="工单">
          <el-option
            v-if="reportWorkOrderRecord"
            :label="`${reportWorkOrderRecord.code} / ${reportWorkOrderRecord.materialName || '未命名产品'}`"
            :value="reportWorkOrderRecord.id"
          />
        </el-select>
        <el-select v-model="reportForm.processItemId" filterable placeholder="选择工序">
          <el-option
            v-for="item in reportProcessOptions"
            :key="item.id"
            :label="`${item.processCode} / ${item.processName}`"
            :value="item.id"
          />
        </el-select>
        <el-date-picker
          v-model="reportForm.reportDate"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="报工时间"
        />
        <el-input v-model="reportForm.reporterName" placeholder="报工人" />
        <el-input-number v-model="reportForm.reportQty" :min="0" :precision="2" controls-position="right" placeholder="报工数量" />
        <el-input-number v-model="reportForm.qualifiedQty" :min="0" :precision="2" controls-position="right" placeholder="合格数量" />
        <el-input-number v-model="reportForm.defectiveQty" :min="0" :precision="2" controls-position="right" placeholder="不良数量" />
        <el-input v-model="reportForm.remark" placeholder="报工备注" class="full-span" />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="reportVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitReportForm">提交报工</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="planDetailVisible" title="生产计划详情" size="46%">
      <template v-if="planDetailRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="计划单号">{{ planDetailRecord.code }}</el-descriptions-item>
          <el-descriptions-item label="产品">{{ planDetailRecord.materialName }}</el-descriptions-item>
          <el-descriptions-item label="产品编码">{{ planDetailRecord.materialCode }}</el-descriptions-item>
          <el-descriptions-item label="计划数量">{{ formatNumber(planDetailRecord.planQty) }}</el-descriptions-item>
          <el-descriptions-item label="开始日期">{{ formatDate(planDetailRecord.startDate) }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ formatDate(planDetailRecord.endDate) }}</el-descriptions-item>
          <el-descriptions-item label="来源销售单">
            <el-button
              v-if="planDetailRecord.sourceSalesId"
              text
              type="primary"
              @click="openSourceSalesOrder(planDetailRecord.sourceSalesId)"
            >
              #{{ planDetailRecord.sourceSalesId }}
            </el-button>
            <span v-else>--</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">{{ formatStatusLabel(planDetailRecord.status) }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>

    <el-drawer v-model="workOrderDetailVisible" title="生产工单详情" size="56%">
      <template v-if="workOrderDetailRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单号">{{ workOrderDetailRecord.code }}</el-descriptions-item>
          <el-descriptions-item label="来源计划">#{{ workOrderDetailRecord.planId }}</el-descriptions-item>
          <el-descriptions-item label="产品">{{ workOrderDetailRecord.materialName }}</el-descriptions-item>
          <el-descriptions-item label="产品编码">{{ workOrderDetailRecord.materialCode }}</el-descriptions-item>
          <el-descriptions-item label="计划数量">{{ formatNumber(workOrderDetailRecord.planQty) }}</el-descriptions-item>
          <el-descriptions-item label="已完工数量">{{ formatNumber(workOrderDetailRecord.finishedQty) }}</el-descriptions-item>
          <el-descriptions-item label="BOM">#{{ workOrderDetailRecord.bomId }}</el-descriptions-item>
          <el-descriptions-item label="工艺路线">#{{ workOrderDetailRecord.routeId }}</el-descriptions-item>
          <el-descriptions-item label="开工日期">{{ formatDate(workOrderDetailRecord.startDate) }}</el-descriptions-item>
          <el-descriptions-item label="完工日期">{{ formatDate(workOrderDetailRecord.endDate) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ formatStatusLabel(workOrderDetailRecord.status) }}</el-descriptions-item>
        </el-descriptions>

        <PagePanel title="BOM 用料" description="用于领料校验与缺料跟踪" class="detail-panel">
          <el-table :data="workOrderBomItems" stripe>
            <el-table-column prop="materialCode" label="物料编码" min-width="130" />
            <el-table-column prop="materialName" label="物料名称" min-width="160" />
            <el-table-column label="单位用量" min-width="100">
              <template #default="{ row }">{{ formatNumber(row.qty) }}</template>
            </el-table-column>
            <el-table-column label="损耗率" min-width="100">
              <template #default="{ row }">{{ formatNumber(row.lossRate) }}</template>
            </el-table-column>
            <el-table-column prop="sortNo" label="排序" min-width="80" />
          </el-table>
        </PagePanel>

        <PagePanel title="工艺工序" description="用于报工与进度统计" class="detail-panel">
          <el-table :data="workOrderRouteItems" stripe>
            <el-table-column prop="processCode" label="工序编号" min-width="130" />
            <el-table-column prop="processName" label="工序名称" min-width="160" />
            <el-table-column prop="workCenter" label="工作中心" min-width="130" />
            <el-table-column label="标准工时" min-width="110">
              <template #default="{ row }">{{ formatNumber(row.standardHours) }}</template>
            </el-table-column>
            <el-table-column prop="sortNo" label="排序" min-width="80" />
          </el-table>
        </PagePanel>
      </template>
    </el-drawer>

    <el-drawer v-model="progressVisible" title="工单执行进度" size="58%">
      <template v-if="progressRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单号">{{ progressRecord.workOrderCode }}</el-descriptions-item>
          <el-descriptions-item label="计划数量">{{ formatNumber(progressRecord.planQty) }}</el-descriptions-item>
          <el-descriptions-item label="累计报工">{{ formatNumber(progressRecord.reportedQty) }}</el-descriptions-item>
          <el-descriptions-item label="累计合格">{{ formatNumber(progressRecord.qualifiedQty) }}</el-descriptions-item>
          <el-descriptions-item label="累计不良">{{ formatNumber(progressRecord.defectiveQty) }}</el-descriptions-item>
          <el-descriptions-item label="已完工入库">{{ formatNumber(progressRecord.finishInQty) }}</el-descriptions-item>
          <el-descriptions-item label="完工率" :span="2">{{ formatPercent(progressRecord.completionRate) }}</el-descriptions-item>
        </el-descriptions>

        <div class="summary-grid">
          <div class="summary-card">
            <span>用料项数</span>
            <strong>{{ progressRecord.materials?.length || 0 }}</strong>
          </div>
          <div class="summary-card">
            <span>工序项数</span>
            <strong>{{ progressRecord.processes?.length || 0 }}</strong>
          </div>
          <div class="summary-card">
            <span>合格率</span>
            <strong>{{ qualityRateText }}</strong>
          </div>
          <div class="summary-card">
            <span>执行状态</span>
            <strong>{{ progressStatusText }}</strong>
          </div>
        </div>

        <PagePanel title="领料进度" description="按工单 BOM 维度汇总已领与应领数量" class="detail-panel">
          <el-table :data="progressRecord.materials || []" stripe>
            <el-table-column prop="materialCode" label="物料编码" min-width="130" />
            <el-table-column prop="materialName" label="物料名称" min-width="160" />
            <el-table-column label="应领数量" min-width="110">
              <template #default="{ row }">{{ formatNumber(row.requiredQty) }}</template>
            </el-table-column>
            <el-table-column label="已领数量" min-width="110">
              <template #default="{ row }">{{ formatNumber(row.pickedQty) }}</template>
            </el-table-column>
            <el-table-column label="领料率" min-width="100">
              <template #default="{ row }">{{ formatPercent(row.pickRate) }}</template>
            </el-table-column>
          </el-table>
        </PagePanel>

        <PagePanel title="工序进度" description="按工艺路线工序汇总报工情况" class="detail-panel">
          <el-table :data="progressRecord.processes || []" stripe>
            <el-table-column prop="processCode" label="工序编号" min-width="130" />
            <el-table-column prop="processName" label="工序名称" min-width="160" />
            <el-table-column label="报工数量" min-width="110">
              <template #default="{ row }">{{ formatNumber(row.reportQty) }}</template>
            </el-table-column>
            <el-table-column label="合格数量" min-width="110">
              <template #default="{ row }">{{ formatNumber(row.qualifiedQty) }}</template>
            </el-table-column>
            <el-table-column label="不良数量" min-width="110">
              <template #default="{ row }">{{ formatNumber(row.defectiveQty) }}</template>
            </el-table-column>
            <el-table-column label="完成率" min-width="100">
              <template #default="{ row }">{{ formatPercent(row.completionRate) }}</template>
            </el-table-column>
            <el-table-column label="最近报工" min-width="170">
              <template #default="{ row }">{{ formatDateTime(row.latestReportTime) }}</template>
            </el-table-column>
          </el-table>
        </PagePanel>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";

import {
  approveProductionPlan,
  approveProductionWorkOrder,
  calculateProductionMrp,
  closeProductionPlan,
  closeProductionWorkOrder,
  createProductionPlan,
  createProductionReport,
  createProductionWorkOrder,
  fetchBomDetail,
  fetchBomList,
  fetchMaterialList,
  fetchProductionPlanDetail,
  fetchProductionPlans,
  fetchProductionReports,
  fetchProductionWorkOrderDetail,
  fetchProductionWorkOrderProgress,
  fetchProductionWorkOrders,
  fetchRouteDetail,
  fetchRouteList,
  fetchWarehouseList,
  finishProductionWorkOrder,
  generateProductionPurchase,
  generateProductionWorkOrders,
  pickProductionWorkOrder,
  updateProductionPlan,
  updateProductionWorkOrder
} from "@/api/modules";
import PagePanel from "@/components/PagePanel.vue";
import {
  formatDate,
  formatDateTime,
  formatNumber,
  formatPercent,
  formatStatusLabel,
  getTagClass
} from "@/utils/format";

const router = useRouter();
const route = useRoute();
const initializing = ref(false);
const submitting = ref(false);
const planLoading = ref(false);
const mrpLoading = ref(false);
const workOrderLoading = ref(false);
const reportLoading = ref(false);

const planRows = ref([]);
const workOrderRows = ref([]);
const reportRows = ref([]);

const materialOptions = ref([]);
const warehouseOptions = ref([]);
const bomOptions = ref([]);
const routeOptions = ref([]);
const planOptions = ref([]);
const workOrderOptions = ref([]);

const mrpResult = ref(null);
const selectedPlanIds = ref([]);
const selectedPurchaseSuggestionIds = ref([]);
const selectedWorkOrderSuggestionIds = ref([]);

const planDetailVisible = ref(false);
const planDetailRecord = ref(null);
const planFormVisible = ref(false);
const editingPlanId = ref(null);

const workOrderDetailVisible = ref(false);
const workOrderDetailRecord = ref(null);
const workOrderBomItems = ref([]);
const workOrderRouteItems = ref([]);
const workOrderFormVisible = ref(false);
const editingWorkOrderId = ref(null);

const pickVisible = ref(false);
const pickWorkOrderRecord = ref(null);
const finishVisible = ref(false);
const finishWorkOrderRecord = ref(null);
const reportVisible = ref(false);
const reportWorkOrderRecord = ref(null);
const reportProcessOptions = ref([]);
const progressVisible = ref(false);
const progressRecord = ref(null);
const lastHandledRouteKey = ref("");
const activeSection = ref("plans");

const planFilters = reactive({
  keyword: "",
  status: ""
});

const workOrderFilters = reactive({
  keyword: "",
  status: ""
});

const reportFilters = reactive({
  workOrderId: undefined
});

const planPagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
});

const workOrderPagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
});

const reportPagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
});

const planForm = reactive(createEmptyPlan());
const workOrderForm = reactive(createEmptyWorkOrder());
const pickForm = reactive(createEmptyPickForm());
const finishForm = reactive(createEmptyFinishForm());
const reportForm = reactive(createEmptyReportForm());

const bomDetailCache = new Map();
const routeDetailCache = new Map();

const planDialogTitle = computed(() => `${editingPlanId.value ? "编辑" : "新增"}生产计划`);
const workOrderDialogTitle = computed(() => `${editingWorkOrderId.value ? "编辑" : "新增"}生产工单`);
const mrpSuggestionCount = computed(() => {
  const purchaseCount = mrpResult.value?.purchaseSuggestions?.length || 0;
  const workOrderCount = mrpResult.value?.workOrderSuggestions?.length || 0;
  return purchaseCount + workOrderCount;
});
const filteredBomOptions = computed(() =>
  bomOptions.value.filter(
    (item) =>
      isEnabledLike(item.status) &&
      (!workOrderForm.materialId || Number(item.productId) === Number(workOrderForm.materialId))
  )
);
const filteredRouteOptions = computed(() =>
  routeOptions.value.filter(
    (item) =>
      isEnabledLike(item.status) &&
      (!workOrderForm.materialId || Number(item.productId) === Number(workOrderForm.materialId))
  )
);
const qualityRateText = computed(() => {
  if (!progressRecord.value) {
    return "--";
  }
  const reportedQty = Number(progressRecord.value.reportedQty || 0);
  const qualifiedQty = Number(progressRecord.value.qualifiedQty || 0);
  if (reportedQty <= 0) {
    return "0.0%";
  }
  return formatPercent(qualifiedQty / reportedQty);
});
const progressStatusText = computed(() => {
  if (!progressRecord.value) {
    return "--";
  }
  const completionRate = Number(progressRecord.value.completionRate || 0);
  if (completionRate >= 1) {
    return "已完成";
  }
  if (completionRate > 0) {
    return "执行中";
  }
  return "待执行";
});

function getToday() {
  const current = new Date();
  const year = current.getFullYear();
  const month = String(current.getMonth() + 1).padStart(2, "0");
  const day = String(current.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function getNowDateTime() {
  const current = new Date();
  const year = current.getFullYear();
  const month = String(current.getMonth() + 1).padStart(2, "0");
  const day = String(current.getDate()).padStart(2, "0");
  const hours = String(current.getHours()).padStart(2, "0");
  const minutes = String(current.getMinutes()).padStart(2, "0");
  const seconds = String(current.getSeconds()).padStart(2, "0");
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
}

function normalizeStatus(value) {
  return String(value || "").toLowerCase();
}

function isEnabledLike(status) {
  return ["enabled", "approved"].includes(normalizeStatus(status));
}

function createEmptyPlan() {
  return {
    materialId: undefined,
    planQty: 1,
    startDate: "",
    endDate: "",
    sourceSalesId: undefined,
    status: "draft"
  };
}

function createEmptyWorkOrder() {
  return {
    planId: undefined,
    materialId: undefined,
    bomId: undefined,
    routeId: undefined,
    planQty: 1,
    startDate: "",
    endDate: "",
    status: "draft"
  };
}

function createEmptyPickForm() {
  return {
    warehouseId: undefined,
    bizDate: getToday(),
    remark: "",
    items: []
  };
}

function createEmptyFinishForm() {
  return {
    warehouseId: undefined,
    bizDate: getToday(),
    remark: "",
    items: []
  };
}

function createEmptyReportForm() {
  return {
    workOrderId: undefined,
    processItemId: undefined,
    reportDate: getNowDateTime(),
    reportQty: 0,
    qualifiedQty: 0,
    defectiveQty: 0,
    reporterName: "",
    remark: ""
  };
}

function patchPlanForm(payload = {}) {
  Object.assign(planForm, createEmptyPlan(), payload);
}

function patchWorkOrderForm(payload = {}) {
  Object.assign(workOrderForm, createEmptyWorkOrder(), payload);
}

function patchPickForm(payload = {}) {
  Object.assign(pickForm, createEmptyPickForm(), payload);
}

function patchFinishForm(payload = {}) {
  Object.assign(finishForm, createEmptyFinishForm(), payload);
}

function patchReportForm(payload = {}) {
  Object.assign(reportForm, createEmptyReportForm(), payload);
}

function isPositiveRouteId(value) {
  return Number.isFinite(value) && value > 0;
}

function planSelectable(row) {
  return ["approved", "in_progress"].includes(normalizeStatus(row.status));
}

function canEditPlan(row) {
  return normalizeStatus(row.status) === "draft";
}

function canApprovePlan(row) {
  return normalizeStatus(row.status) === "draft";
}

function canClosePlan(row) {
  return normalizeStatus(row.status) !== "closed";
}

function canEditWorkOrder(row) {
  return normalizeStatus(row.status) === "draft";
}

function canApproveWorkOrder(row) {
  return normalizeStatus(row.status) === "draft";
}

function canCloseWorkOrder(row) {
  return normalizeStatus(row.status) !== "closed";
}

function canExecuteWorkOrder(row) {
  return ["approved", "in_progress"].includes(normalizeStatus(row.status));
}

function openSourceSalesOrder(sourceSalesId) {
  router.push({
    name: "sales",
    query: {
      tab: "orders",
      detailId: String(sourceSalesId)
    }
  });
}

async function getBomDetailCached(bomId) {
  if (!bomId) {
    return null;
  }
  if (bomDetailCache.has(bomId)) {
    return bomDetailCache.get(bomId);
  }
  const detail = await fetchBomDetail(bomId);
  bomDetailCache.set(bomId, detail);
  return detail;
}

async function getRouteDetailCached(routeId) {
  if (!routeId) {
    return null;
  }
  if (routeDetailCache.has(routeId)) {
    return routeDetailCache.get(routeId);
  }
  const detail = await fetchRouteDetail(routeId);
  routeDetailCache.set(routeId, detail);
  return detail;
}

async function loadStaticOptions() {
  const [materialPage, warehousePage, bomPage, routePage] = await Promise.all([
    fetchMaterialList({ pageNo: 1, pageSize: 200, status: "enabled" }),
    fetchWarehouseList({ pageNo: 1, pageSize: 200, status: "enabled" }),
    fetchBomList({ pageNo: 1, pageSize: 200 }),
    fetchRouteList({ pageNo: 1, pageSize: 200 })
  ]);

  materialOptions.value = materialPage.records || [];
  warehouseOptions.value = warehousePage.records || [];
  bomOptions.value = bomPage.records || [];
  routeOptions.value = routePage.records || [];
}

async function refreshDynamicOptions() {
  const [planPage, workOrderPage] = await Promise.all([
    fetchProductionPlans({ pageNo: 1, pageSize: 200 }),
    fetchProductionWorkOrders({ pageNo: 1, pageSize: 200 })
  ]);

  planOptions.value = planPage.records || [];
  workOrderOptions.value = workOrderPage.records || [];
}

async function refreshProductionWorkspace() {
  try {
    await Promise.all([loadPlans(), loadWorkOrders(), loadReports(), refreshDynamicOptions()]);
  } catch (error) {
    ElMessage.error(error.message || "生产模块刷新失败");
  }
}

async function loadPlans() {
  planLoading.value = true;

  try {
    const pageData = await fetchProductionPlans({
      keyword: planFilters.keyword || undefined,
      status: planFilters.status || undefined,
      pageNo: planPagination.pageNo,
      pageSize: planPagination.pageSize
    });

    planRows.value = pageData.records || [];
    planPagination.total = pageData.total || 0;
  } catch (error) {
    ElMessage.error(error.message || "生产计划加载失败");
  } finally {
    planLoading.value = false;
  }
}

async function loadWorkOrders() {
  workOrderLoading.value = true;

  try {
    const pageData = await fetchProductionWorkOrders({
      keyword: workOrderFilters.keyword || undefined,
      status: workOrderFilters.status || undefined,
      pageNo: workOrderPagination.pageNo,
      pageSize: workOrderPagination.pageSize
    });

    workOrderRows.value = pageData.records || [];
    workOrderPagination.total = pageData.total || 0;
  } catch (error) {
    ElMessage.error(error.message || "生产工单加载失败");
  } finally {
    workOrderLoading.value = false;
  }
}

async function loadReports() {
  reportLoading.value = true;

  try {
    const pageData = await fetchProductionReports({
      workOrderId: reportFilters.workOrderId || undefined,
      pageNo: reportPagination.pageNo,
      pageSize: reportPagination.pageSize
    });

    reportRows.value = pageData.records || [];
    reportPagination.total = pageData.total || 0;
  } catch (error) {
    ElMessage.error(error.message || "报工记录加载失败");
  } finally {
    reportLoading.value = false;
  }
}

async function reloadWorkOrderDetail(id) {
  const detail = await fetchProductionWorkOrderDetail(id);
  const [bomDetail, routeDetail] = await Promise.all([
    getBomDetailCached(detail.bomId),
    getRouteDetailCached(detail.routeId)
  ]);

  workOrderDetailRecord.value = detail;
  workOrderBomItems.value = bomDetail?.items || [];
  workOrderRouteItems.value = routeDetail?.items || [];
}

async function reloadProgress(id) {
  progressRecord.value = await fetchProductionWorkOrderProgress(id);
}

async function refreshAfterWorkOrderChange(workOrderId, includeReports = true, refreshMrp = Boolean(mrpResult.value)) {
  await Promise.all([
    loadPlans(),
    loadWorkOrders(),
    refreshDynamicOptions(),
    includeReports ? loadReports() : Promise.resolve()
  ]);

  if (workOrderDetailVisible.value && workOrderDetailRecord.value?.id === workOrderId) {
    await reloadWorkOrderDetail(workOrderId);
  }
  if (progressVisible.value && progressRecord.value?.workOrderId === workOrderId) {
    await reloadProgress(workOrderId);
  }
  if (refreshMrp) {
    await recalculateMrpSelection(true);
  }
}

async function promptOpenInventoryDoc(stockDocId) {
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
}

async function applyRouteQuery() {
  const workOrderId = Number(route.query.workOrderId);
  const planDetailId = Number(route.query.planDetailId);
  const openPlanDialog = route.query.openPlanDialog === "1";
  const sourceSalesId = Number(route.query.sourceSalesId);
  const materialId = Number(route.query.materialId);
  const planQty = Number(route.query.planQty);
  const startDate = typeof route.query.startDate === "string" ? route.query.startDate : getToday();
  const endDate = typeof route.query.endDate === "string" ? route.query.endDate : startDate;
  const routeKey = JSON.stringify({
    workOrderId,
    planDetailId,
    openPlanDialog,
    sourceSalesId,
    materialId,
    planQty,
    startDate,
    endDate
  });

  if (routeKey === lastHandledRouteKey.value) {
    return;
  }

  lastHandledRouteKey.value = routeKey;

  if (openPlanDialog && isPositiveRouteId(sourceSalesId) && isPositiveRouteId(materialId) && planQty > 0) {
    activeSection.value = "plans";
    editingPlanId.value = null;
    patchPlanForm({
      materialId,
      planQty,
      startDate,
      endDate,
      sourceSalesId,
      status: "draft"
    });
    planFormVisible.value = true;
  }

  if (isPositiveRouteId(planDetailId)) {
    activeSection.value = "plans";
    await openPlanDetailDrawer(planDetailId);
  }

  if (isPositiveRouteId(workOrderId)) {
    activeSection.value = "workorders";
    await openWorkOrderDetailDrawer(workOrderId);
  }
}

function handlePlanSelectionChange(selection) {
  const nextPlanIds = selection.map((item) => item.id);
  if (JSON.stringify(nextPlanIds) !== JSON.stringify(selectedPlanIds.value)) {
    clearMrpResult();
  }
  selectedPlanIds.value = nextPlanIds;
}

function handlePurchaseSuggestionSelectionChange(selection) {
  selectedPurchaseSuggestionIds.value = selection.map((item) => item.id);
}

function handleWorkOrderSuggestionSelectionChange(selection) {
  selectedWorkOrderSuggestionIds.value = selection.map((item) => item.id);
}

function clearMrpResult() {
  mrpResult.value = null;
  selectedPurchaseSuggestionIds.value = [];
  selectedWorkOrderSuggestionIds.value = [];
}

async function recalculateMrpSelection(silent = false) {
  if (!selectedPlanIds.value.length) {
    clearMrpResult();
    return;
  }

  mrpLoading.value = true;

  try {
    mrpResult.value = await calculateProductionMrp({
      planIds: selectedPlanIds.value
    });
    selectedPurchaseSuggestionIds.value = [];
    selectedWorkOrderSuggestionIds.value = [];
    if (!silent) {
      ElMessage.success("MRP 计算完成");
    }
  } catch (error) {
    clearMrpResult();
    if (!silent) {
      ElMessage.warning(error.message || "MRP 结果已失效，请重新计算");
    }
  } finally {
    mrpLoading.value = false;
  }
}

async function runMrpCalculation() {
  if (!selectedPlanIds.value.length) {
    ElMessage.warning("请先勾选需要参与 MRP 的生产计划");
    return;
  }

  activeSection.value = "mrp";
  await recalculateMrpSelection();
}

async function generatePurchaseDocs() {
  activeSection.value = "mrp";
  if (!mrpResult.value?.taskKey || !selectedPurchaseSuggestionIds.value.length) {
    ElMessage.warning("请先选择需要生成的采购建议");
    return;
  }

  submitting.value = true;

  try {
    const ids = await generateProductionPurchase({
      taskKey: mrpResult.value.taskKey,
      selectedItems: selectedPurchaseSuggestionIds.value
    });
    ElMessage.success(`已生成采购申请：${(ids || []).join(", ")}`);
    await Promise.all([loadPlans(), loadWorkOrders(), refreshDynamicOptions()]);
    await recalculateMrpSelection(true);
  } catch (error) {
    ElMessage.error(error.message || "采购建议生成失败");
  } finally {
    submitting.value = false;
  }
}

async function generateWorkOrderDocs() {
  activeSection.value = "mrp";
  if (!mrpResult.value?.taskKey || !selectedWorkOrderSuggestionIds.value.length) {
    ElMessage.warning("请先选择需要生成的工单建议");
    return;
  }

  submitting.value = true;

  try {
    const ids = await generateProductionWorkOrders({
      taskKey: mrpResult.value.taskKey,
      selectedItems: selectedWorkOrderSuggestionIds.value
    });
    ElMessage.success(`已生成 ${ids?.length || 0} 张工单`);
    await refreshAfterWorkOrderChange(ids?.[0], false);
  } catch (error) {
    ElMessage.error(error.message || "工单建议生成失败");
  } finally {
    submitting.value = false;
  }
}

function resetPlanFilters() {
  activeSection.value = "plans";
  planFilters.keyword = "";
  planFilters.status = "";
  planPagination.pageNo = 1;
  selectedPlanIds.value = [];
  loadPlans();
}

function handlePlanPageChange(pageNo) {
  planPagination.pageNo = pageNo;
  loadPlans();
}

function handlePlanSizeChange(pageSize) {
  planPagination.pageSize = pageSize;
  planPagination.pageNo = 1;
  loadPlans();
}

function resetWorkOrderFilters() {
  activeSection.value = "workorders";
  workOrderFilters.keyword = "";
  workOrderFilters.status = "";
  workOrderPagination.pageNo = 1;
  loadWorkOrders();
}

function handleWorkOrderPageChange(pageNo) {
  workOrderPagination.pageNo = pageNo;
  loadWorkOrders();
}

function handleWorkOrderSizeChange(pageSize) {
  workOrderPagination.pageSize = pageSize;
  workOrderPagination.pageNo = 1;
  loadWorkOrders();
}

function resetReportFilters() {
  activeSection.value = "reports";
  reportFilters.workOrderId = undefined;
  reportPagination.pageNo = 1;
  loadReports();
}

function handleReportPageChange(pageNo) {
  reportPagination.pageNo = pageNo;
  loadReports();
}

function handleReportSizeChange(pageSize) {
  reportPagination.pageSize = pageSize;
  reportPagination.pageNo = 1;
  loadReports();
}

function openCreatePlanDialog() {
  activeSection.value = "plans";
  editingPlanId.value = null;
  patchPlanForm();
  planFormVisible.value = true;
}

async function openEditPlanDialog(id) {
  try {
    const detail = await fetchProductionPlanDetail(id);
    editingPlanId.value = id;
    patchPlanForm({
      materialId: detail.materialId,
      planQty: Number(detail.planQty || 0),
      startDate: detail.startDate,
      endDate: detail.endDate,
      sourceSalesId: detail.sourceSalesId,
      status: detail.status || "draft"
    });
    planFormVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "计划详情加载失败");
  }
}

async function openPlanDetailDrawer(id) {
  try {
    planDetailRecord.value = await fetchProductionPlanDetail(id);
    planDetailVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "计划详情加载失败");
  }
}

async function submitPlanForm() {
  if (!planForm.materialId || !planForm.startDate || !planForm.endDate || Number(planForm.planQty) <= 0) {
    ElMessage.warning("请先填写完整的计划信息");
    return;
  }

  submitting.value = true;

  try {
    const payload = {
      materialId: planForm.materialId,
      planQty: Number(planForm.planQty),
      startDate: planForm.startDate,
      endDate: planForm.endDate,
      sourceSalesId: planForm.sourceSalesId || null,
      status: planForm.status
    };

    if (editingPlanId.value) {
      await updateProductionPlan(editingPlanId.value, payload);
    } else {
      await createProductionPlan(payload);
    }

    planFormVisible.value = false;
    ElMessage.success(`${planDialogTitle.value}成功`);
    await Promise.all([loadPlans(), refreshDynamicOptions()]);
  } catch (error) {
    ElMessage.error(error.message || "计划保存失败");
  } finally {
    submitting.value = false;
  }
}

async function approvePlan(id) {
  try {
    await ElMessageBox.confirm("确认审核这条生产计划吗？", "审核确认", { type: "warning" });
    await approveProductionPlan(id);
    ElMessage.success("计划已审核");
    await Promise.all([loadPlans(), refreshDynamicOptions()]);
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error.message || "审核失败");
    }
  }
}

async function closePlan(id) {
  try {
    await ElMessageBox.confirm("确认关闭这条生产计划吗？关闭后将不能继续下推新工单。", "关闭确认", {
      type: "warning"
    });
    await closeProductionPlan(id);
    ElMessage.success("计划已关闭");
    await Promise.all([loadPlans(), refreshDynamicOptions()]);
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error.message || "关闭失败");
    }
  }
}

function syncMaterialRelatedOptions(materialId) {
  const matchedBoms = filteredBomOptions.value.filter((item) => Number(item.productId) === Number(materialId));
  const matchedRoutes = filteredRouteOptions.value.filter((item) => Number(item.productId) === Number(materialId));

  if (!matchedBoms.some((item) => Number(item.id) === Number(workOrderForm.bomId))) {
    workOrderForm.bomId = matchedBoms.length === 1 ? matchedBoms[0].id : undefined;
  }
  if (!matchedRoutes.some((item) => Number(item.id) === Number(workOrderForm.routeId))) {
    workOrderForm.routeId = matchedRoutes.length === 1 ? matchedRoutes[0].id : undefined;
  }
}

function handleWorkOrderPlanChange(planId) {
  const matchedPlan = planOptions.value.find((item) => Number(item.id) === Number(planId));
  if (!matchedPlan) {
    return;
  }

  workOrderForm.materialId = matchedPlan.materialId;
  workOrderForm.planQty = Number(matchedPlan.planQty || 0);
  workOrderForm.startDate = matchedPlan.startDate || "";
  workOrderForm.endDate = matchedPlan.endDate || "";
  syncMaterialRelatedOptions(matchedPlan.materialId);
}

function handleWorkOrderMaterialChange(materialId) {
  syncMaterialRelatedOptions(materialId);
}

function openCreateWorkOrderDialog() {
  activeSection.value = "workorders";
  editingWorkOrderId.value = null;
  patchWorkOrderForm();
  workOrderFormVisible.value = true;
}

async function openEditWorkOrderDialog(id) {
  try {
    const detail = await fetchProductionWorkOrderDetail(id);
    editingWorkOrderId.value = id;
    patchWorkOrderForm({
      planId: detail.planId,
      materialId: detail.materialId,
      bomId: detail.bomId,
      routeId: detail.routeId,
      planQty: Number(detail.planQty || 0),
      startDate: detail.startDate,
      endDate: detail.endDate,
      status: detail.status || "draft"
    });
    workOrderFormVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "工单详情加载失败");
  }
}

async function submitWorkOrderForm() {
  if (
    !workOrderForm.planId ||
    !workOrderForm.materialId ||
    !workOrderForm.bomId ||
    !workOrderForm.routeId ||
    Number(workOrderForm.planQty) <= 0
  ) {
    ElMessage.warning("请先填写完整的工单信息");
    return;
  }

  submitting.value = true;

  try {
    const payload = {
      planId: workOrderForm.planId,
      materialId: workOrderForm.materialId,
      bomId: workOrderForm.bomId,
      routeId: workOrderForm.routeId,
      planQty: Number(workOrderForm.planQty),
      startDate: workOrderForm.startDate || null,
      endDate: workOrderForm.endDate || null,
      status: workOrderForm.status
    };

    if (editingWorkOrderId.value) {
      await updateProductionWorkOrder(editingWorkOrderId.value, payload);
    } else {
      await createProductionWorkOrder(payload);
    }

    workOrderFormVisible.value = false;
    ElMessage.success(`${workOrderDialogTitle.value}成功`);
    await Promise.all([loadPlans(), loadWorkOrders(), refreshDynamicOptions()]);
  } catch (error) {
    ElMessage.error(error.message || "工单保存失败");
  } finally {
    submitting.value = false;
  }
}

async function approveWorkOrder(id) {
  try {
    await ElMessageBox.confirm("确认审核这张生产工单吗？", "审核确认", { type: "warning" });
    await approveProductionWorkOrder(id);
    ElMessage.success("工单已审核");
    await refreshAfterWorkOrderChange(id, false);
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error.message || "审核失败");
    }
  }
}

async function closeWorkOrder(id) {
  try {
    await ElMessageBox.confirm("确认关闭这张生产工单吗？", "关闭确认", { type: "warning" });
    await closeProductionWorkOrder(id);
    ElMessage.success("工单已关闭");
    await refreshAfterWorkOrderChange(id, false);
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error.message || "关闭失败");
    }
  }
}

async function openWorkOrderDetailDrawer(id) {
  try {
    await reloadWorkOrderDetail(id);
    workOrderDetailVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "工单详情加载失败");
  }
}

async function openPickDialog(id) {
  try {
    const detail = await fetchProductionWorkOrderDetail(id);
    const [bomDetail, progress] = await Promise.all([
      getBomDetailCached(detail.bomId),
      fetchProductionWorkOrderProgress(id)
    ]);

    const materialProgressMap = new Map((progress.materials || []).map((item) => [Number(item.materialId), item]));
    const items = (bomDetail?.items || [])
      .map((item) => {
        const materialProgress = materialProgressMap.get(Number(item.materialId));
        const requiredQty = Number(materialProgress?.requiredQty ?? 0);
        const pickedQty = Number(materialProgress?.pickedQty ?? 0);
        const remainQty = Math.max(requiredQty - pickedQty, 0);

        return {
          materialId: item.materialId,
          materialCode: item.materialCode,
          materialName: item.materialName,
          requiredQty,
          pickedQty,
          remainQty,
          qty: remainQty,
          lotNo: ""
        };
      })
      .filter((item) => item.remainQty > 0);

    if (!items.length) {
      ElMessage.warning("该工单已无可继续领料的 BOM 明细");
      return;
    }

    pickWorkOrderRecord.value = detail;
    patchPickForm({
      bizDate: getToday(),
      remark: `${detail.code} 领料`,
      items
    });
    pickVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "领料数据加载失败");
  }
}

async function submitPickForm() {
  if (!pickWorkOrderRecord.value?.id) {
    ElMessage.warning("未找到来源工单");
    return;
  }
  if (!pickForm.warehouseId || !pickForm.bizDate) {
    ElMessage.warning("请先选择领料仓库和业务日期");
    return;
  }

  const validItems = pickForm.items.filter((item) => Number(item.qty) > 0);
  if (!validItems.length) {
    ElMessage.warning("请至少填写一条领料明细");
    return;
  }

  for (const item of validItems) {
    if (Number(item.qty) > Number(item.remainQty || 0)) {
      ElMessage.warning(`物料 ${item.materialName} 的领料数量不能超过剩余应领数量`);
      return;
    }
  }

  submitting.value = true;

  try {
    const stockDocId = await pickProductionWorkOrder(pickWorkOrderRecord.value.id, {
      warehouseId: pickForm.warehouseId,
      bizDate: pickForm.bizDate,
      remark: pickForm.remark,
      items: validItems.map((item) => ({
        materialId: item.materialId,
        lotNo: item.lotNo || null,
        qty: Number(item.qty)
      }))
    });

    pickVisible.value = false;
    ElMessage.success(`领料成功，生成库存单据 #${stockDocId}`);
    await refreshAfterWorkOrderChange(pickWorkOrderRecord.value.id);
    await promptOpenInventoryDoc(stockDocId);
  } catch (error) {
    ElMessage.error(error.message || "工单领料失败");
  } finally {
    submitting.value = false;
  }
}

async function openFinishDialog(id) {
  try {
    const detail = await fetchProductionWorkOrderDetail(id);
    const progress = await fetchProductionWorkOrderProgress(id);
    const remainPlanQty = Math.max(Number(detail.planQty || 0) - Number(detail.finishedQty || 0), 0);
    const availableFinishQty = Math.max(
      Number(progress.qualifiedQty || 0) - Number(progress.finishInQty || 0),
      0
    );
    const defaultQty = Math.min(remainPlanQty, availableFinishQty);

    if (defaultQty <= 0) {
      ElMessage.warning("当前工单暂无可入库的合格数量");
      return;
    }

    finishWorkOrderRecord.value = detail;
    patchFinishForm({
      bizDate: getToday(),
      remark: `${detail.code} 完工入库`,
      items: [
        {
          materialId: detail.materialId,
          materialCode: detail.materialCode,
          materialName: detail.materialName,
          remainPlanQty,
          availableFinishQty,
          qty: defaultQty,
          lotNo: ""
        }
      ]
    });
    finishVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "完工数据加载失败");
  }
}

async function submitFinishForm() {
  if (!finishWorkOrderRecord.value?.id) {
    ElMessage.warning("未找到来源工单");
    return;
  }
  if (!finishForm.warehouseId || !finishForm.bizDate) {
    ElMessage.warning("请先选择入库仓库和业务日期");
    return;
  }

  const validItems = finishForm.items.filter((item) => Number(item.qty) > 0);
  if (!validItems.length) {
    ElMessage.warning("请至少填写一条完工入库明细");
    return;
  }

  for (const item of validItems) {
    if (Number(item.qty) > Number(item.availableFinishQty || 0)) {
      ElMessage.warning(`产品 ${item.materialName} 的入库数量不能超过可入库合格量`);
      return;
    }
    if (Number(item.qty) > Number(item.remainPlanQty || 0)) {
      ElMessage.warning(`产品 ${item.materialName} 的入库数量不能超过剩余计划数量`);
      return;
    }
  }

  submitting.value = true;

  try {
    const stockDocId = await finishProductionWorkOrder(finishWorkOrderRecord.value.id, {
      warehouseId: finishForm.warehouseId,
      bizDate: finishForm.bizDate,
      remark: finishForm.remark,
      items: validItems.map((item) => ({
        materialId: item.materialId,
        lotNo: item.lotNo || null,
        qty: Number(item.qty)
      }))
    });

    finishVisible.value = false;
    ElMessage.success(`完工入库成功，生成库存单据 #${stockDocId}`);
    await refreshAfterWorkOrderChange(finishWorkOrderRecord.value.id);
    await promptOpenInventoryDoc(stockDocId);
  } catch (error) {
    ElMessage.error(error.message || "完工入库失败");
  } finally {
    submitting.value = false;
  }
}

async function openReportDialog(workOrder) {
  try {
    const detail = workOrder?.routeId ? workOrder : await fetchProductionWorkOrderDetail(workOrder?.id || workOrder);
    const routeDetail = await getRouteDetailCached(detail.routeId);
    reportWorkOrderRecord.value = detail;
    reportProcessOptions.value = routeDetail?.items || [];
    patchReportForm({
      workOrderId: detail.id,
      processItemId: reportProcessOptions.value.length === 1 ? reportProcessOptions.value[0].id : undefined,
      reportDate: getNowDateTime(),
      reportQty: 0,
      qualifiedQty: 0,
      defectiveQty: 0,
      reporterName: "",
      remark: `${detail.code} 报工`
    });
    reportVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "报工数据加载失败");
  }
}

function openReportDialogFromFilter() {
  activeSection.value = "reports";
  if (!reportFilters.workOrderId) {
    ElMessage.warning("请先在下方选择一个工单，再新增报工");
    return;
  }

  const matched = workOrderOptions.value.find((item) => Number(item.id) === Number(reportFilters.workOrderId));
  openReportDialog(matched || reportFilters.workOrderId);
}

async function submitReportForm() {
  if (!reportForm.workOrderId || !reportForm.processItemId || !reportForm.reportDate) {
    ElMessage.warning("请先选择工单、工序并填写报工时间");
    return;
  }

  const reportQty = Number(reportForm.reportQty || 0);
  const qualifiedQty = Number(reportForm.qualifiedQty || 0);
  const defectiveQty = Number(reportForm.defectiveQty || 0);

  if (reportQty <= 0) {
    ElMessage.warning("报工数量必须大于 0");
    return;
  }
  if (Number((qualifiedQty + defectiveQty).toFixed(4)) !== Number(reportQty.toFixed(4))) {
    ElMessage.warning("合格数量与不良数量之和必须等于报工数量");
    return;
  }

  submitting.value = true;

  try {
    await createProductionReport({
      workOrderId: reportForm.workOrderId,
      processItemId: reportForm.processItemId,
      reportDate: reportForm.reportDate,
      reportQty,
      qualifiedQty,
      defectiveQty,
      reporterName: reportForm.reporterName || null,
      remark: reportForm.remark || null
    });

    reportVisible.value = false;
    ElMessage.success("报工提交成功");
    await refreshAfterWorkOrderChange(reportForm.workOrderId);
  } catch (error) {
    ElMessage.error(error.message || "报工提交失败");
  } finally {
    submitting.value = false;
  }
}

async function openProgressDrawer(id) {
  try {
    await reloadProgress(id);
    progressVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "进度加载失败");
  }
}

onMounted(async () => {
  initializing.value = true;

  try {
    await Promise.all([loadStaticOptions(), refreshDynamicOptions()]);
    await Promise.all([loadPlans(), loadWorkOrders(), loadReports()]);
    await applyRouteQuery();
  } catch (error) {
    ElMessage.error(error.message || "生产模块初始化失败");
  } finally {
    initializing.value = false;
  }
});

watch(activeSection, async (_current, previous) => {
  if (!previous) {
    return;
  }

  const scrollTop = window.scrollY;
  await nextTick();
  requestAnimationFrame(() => {
    window.scrollTo({ top: scrollTop, behavior: "auto" });
  });
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
.production-tabs {
  margin-top: -6px;
}

.production-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.production-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(148, 163, 184, 0.18);
}

.production-tab-actions {
  margin-bottom: 14px;
}

.production-tabs :deep(.el-tab-pane) {
  min-width: 0;
}

.mrp-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.mrp-block {
  display: grid;
  gap: 12px;
}

.mrp-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.mrp-head strong {
  font-size: 16px;
}

.mrp-head p {
  margin: 6px 0 0;
  color: var(--text-soft);
  font-size: 13px;
  line-height: 1.6;
}

.detail-panel {
  margin-top: 18px;
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

.report-filter-grid {
  grid-template-columns: minmax(0, 2fr) auto auto;
}

@media (max-width: 1280px) {
  .mrp-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .report-filter-grid {
    grid-template-columns: 1fr;
  }

  .production-tab-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
