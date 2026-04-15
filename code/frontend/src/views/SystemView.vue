<template>
  <div class="page-grid" v-loading="loading">
    <section class="module-hero">
      <div>
        <h2>系统权限与运行配置</h2>
      </div>
      <div class="hero-kpis">
        <div class="hero-kpi"><span>服务状态</span><strong>{{ healthStatus }}</strong></div>
        <div class="hero-kpi"><span>当前账号</span><strong>{{ profile?.userInfo?.username || "--" }}</strong></div>
        <div class="hero-kpi"><span>用户总数</span><strong>{{ userPagination.total }}</strong></div>
        <div class="hero-kpi"><span>菜单节点</span><strong>{{ flattenedMenuOptions.length }}</strong></div>
      </div>
    </section>

    <div class="grid-2">
      <PagePanel title="当前登录信息" description="数据来自 `GET /api/v1/auth/me`">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户 ID">{{ profile?.userInfo?.id || "--" }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ profile?.userInfo?.username || "--" }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ profile?.userInfo?.realName || "--" }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ profile?.roleInfo?.name || "--" }}</el-descriptions-item>
          <el-descriptions-item label="角色编码">{{ profile?.roleInfo?.code || "--" }}</el-descriptions-item>
        </el-descriptions>
      </PagePanel>

      <PagePanel title="基础运行信息" description="包含服务健康状态与物料单位字典">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="健康状态">{{ healthStatus }}</el-descriptions-item>
          <el-descriptions-item label="当前菜单数">{{ (profile?.menus || []).length }}</el-descriptions-item>
          <el-descriptions-item label="单位字典数">{{ dicts.length }}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="dicts" stripe style="margin-top: 16px">
          <el-table-column prop="label" label="显示值" min-width="120" />
          <el-table-column prop="value" label="编码" min-width="120" />
        </el-table>
      </PagePanel>
    </div>

    <PagePanel title="系统管理" description="用户、角色、菜单均已切换为真实后端接口联动">
      <template #actions>
        <el-button text @click="refreshAll">刷新全部</el-button>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="用户管理" name="users">
          <div class="filter-grid">
            <el-input v-model="userFilters.keyword" clearable placeholder="搜索用户名或姓名" />
            <el-select v-model="userFilters.status" clearable placeholder="全部状态">
              <el-option label="启用" value="enabled" />
              <el-option label="停用" value="disabled" />
            </el-select>
            <el-button type="primary" @click="queryUsers">查询</el-button>
            <el-button @click="resetUserFilters">重置</el-button>
            <el-button type="primary" plain @click="openCreateUserDialog">新增用户</el-button>
          </div>

          <el-table class="management-table" :data="users" stripe>
            <el-table-column label="用户信息" min-width="180">
              <template #default="{ row }">
                <div class="record-cell">
                  <strong class="record-code">{{ row.username }}</strong>
                  <span class="record-subtitle">{{ row.realName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="roleName" label="角色" min-width="120" />
            <el-table-column prop="phone" label="手机号" min-width="120" />
            <el-table-column label="状态" min-width="90" align="center">
              <template #default="{ row }">
                <span class="table-tag" :class="getTagClass(row.status)">{{ formatStatusLabel(row.status) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" min-width="150" align="center">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" min-width="260" align="center" class-name="action-cell">
              <template #default="{ row }">
                <div class="table-actions row-actions">
                  <el-button text @click="openEditUserDialog(row.id)">编辑</el-button>
                  <el-button text @click="toggleUserStatus(row)">
                    {{ row.status === "enabled" ? "停用" : "启用" }}
                  </el-button>
                  <el-button text type="warning" @click="openResetPasswordDialog(row)">重置密码</el-button>
                  <el-button text type="danger" @click="removeUser(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              background
              layout="total, prev, pager, next, sizes"
              :current-page="userPagination.pageNo"
              :page-size="userPagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="userPagination.total"
              @current-change="handleUserPageChange"
              @size-change="handleUserSizeChange"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="角色管理" name="roles">
          <div class="filter-grid">
            <el-select v-model="roleFilterStatus" clearable placeholder="全部状态">
              <el-option label="启用" value="enabled" />
              <el-option label="停用" value="disabled" />
            </el-select>
            <el-button type="primary" @click="queryRoles">查询</el-button>
            <el-button @click="resetRoleFilter">重置</el-button>
            <el-button type="primary" plain @click="openCreateRoleDialog">新增角色</el-button>
          </div>

          <el-table class="management-table" :data="filteredRoles" stripe>
            <el-table-column label="角色信息" min-width="180">
              <template #default="{ row }">
                <div class="record-cell">
                  <strong class="record-code">{{ row.code }}</strong>
                  <span class="record-subtitle">{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" min-width="90" align="center">
              <template #default="{ row }">
                <span class="table-tag" :class="getTagClass(row.status)">{{ formatStatusLabel(row.status) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" min-width="220" align="center" class-name="action-cell">
              <template #default="{ row }">
                <div class="table-actions row-actions">
                  <el-button text @click="openEditRoleDialog(row.id)">编辑</el-button>
                  <el-button text type="success" @click="openRoleMenusDialog(row)">分配菜单</el-button>
                  <el-button text type="danger" @click="removeRole(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="菜单管理" name="menus">
          <div class="filter-grid">
            <el-input v-model="menuKeyword" clearable placeholder="快速查看菜单名称、路径或权限编码" />
            <el-button type="primary" @click="refreshMenus">刷新树</el-button>
            <el-button type="primary" plain @click="openCreateMenuDialog()">新增菜单</el-button>
          </div>

          <el-table
            class="management-table"
            :data="filteredMenuTree"
            row-key="id"
            stripe
            default-expand-all
            :tree-props="{ children: 'children' }"
          >
            <el-table-column prop="name" label="菜单名称" min-width="180" />
            <el-table-column prop="path" label="路径" min-width="180" />
            <el-table-column prop="component" label="组件" min-width="180" show-overflow-tooltip />
            <el-table-column prop="menuType" label="类型" min-width="100" />
            <el-table-column prop="permissionCode" label="权限编码" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" min-width="90" align="center">
              <template #default="{ row }">
                <span class="table-tag" :class="getTagClass(row.status)">{{ formatStatusLabel(row.status) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="220" align="center" class-name="action-cell">
              <template #default="{ row }">
                <div class="table-actions row-actions">
                  <el-button text @click="openCreateMenuDialog(row.id)">新增下级</el-button>
                  <el-button text @click="openEditMenuDialog(row.id)">编辑</el-button>
                  <el-button text type="danger" @click="removeMenu(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </PagePanel>

    <el-dialog v-model="userDialogVisible" :title="userDialogTitle" width="620px">
      <div class="form-grid">
        <el-input v-model="userForm.username" placeholder="用户名" />
        <el-input v-model="userForm.realName" placeholder="真实姓名" />
        <el-select v-model="userForm.roleId" placeholder="选择角色">
          <el-option v-for="item in roleOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="userForm.status" placeholder="状态">
          <el-option label="启用" value="enabled" />
          <el-option label="停用" value="disabled" />
        </el-select>
        <el-input v-model="userForm.phone" placeholder="手机号" />
        <el-input
          v-model="userForm.password"
          type="password"
          show-password
          :placeholder="userEditingId ? '留空则保持原密码' : '请输入密码'"
        />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="userDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitUserForm">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="重置密码" width="420px">
      <el-alert :title="`当前用户：${passwordTargetName || '--'}`" type="info" :closable="false" />
      <el-input
        v-model="passwordForm.password"
        type="password"
        show-password
        placeholder="请输入新密码"
        style="margin-top: 16px"
      />

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitPasswordReset">确认重置</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" :title="roleDialogTitle" width="560px">
      <div class="form-grid">
        <el-input v-model="roleForm.code" placeholder="角色编码" />
        <el-input v-model="roleForm.name" placeholder="角色名称" />
        <el-select v-model="roleForm.status" placeholder="状态">
          <el-option label="启用" value="enabled" />
          <el-option label="停用" value="disabled" />
        </el-select>
        <el-input v-model="roleForm.remark" placeholder="备注" />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="roleDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitRoleForm">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="roleMenusDialogVisible" :title="roleMenusDialogTitle" width="620px">
      <el-tree
        ref="roleMenuTreeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        default-expand-all
        check-strictly
        :props="{ label: 'name', children: 'children' }"
      />

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="roleMenusDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitRoleMenus">保存授权</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="menuDialogVisible" :title="menuDialogTitle" width="620px">
      <div class="form-grid">
        <el-select v-model="menuForm.parentId" placeholder="父级菜单">
          <el-option label="顶级菜单" :value="0" />
          <el-option
            v-for="item in flattenedMenuOptions"
            :key="item.id"
            :label="item.label"
            :value="item.id"
            :disabled="menuEditingId === item.id"
          />
        </el-select>
        <el-input v-model="menuForm.name" placeholder="菜单名称" />
        <el-input v-model="menuForm.path" placeholder="路由路径" />
        <el-input v-model="menuForm.component" placeholder="组件路径" />
        <el-select v-model="menuForm.menuType" placeholder="菜单类型">
          <el-option label="目录" value="catalog" />
          <el-option label="菜单" value="menu" />
          <el-option label="按钮" value="button" />
        </el-select>
        <el-input v-model="menuForm.permissionCode" placeholder="权限编码" />
        <el-select v-model="menuForm.status" placeholder="状态">
          <el-option label="启用" value="enabled" />
          <el-option label="停用" value="disabled" />
        </el-select>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="menuDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitMenuForm">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";

import {
  createSystemMenu,
  createSystemRole,
  createSystemUser,
  deleteSystemMenu,
  deleteSystemRole,
  deleteSystemUser,
  fetchHealth,
  fetchMaterialUnits,
  fetchMyProfile,
  fetchSystemMenuDetail,
  fetchSystemMenuTree,
  fetchSystemRoleDetail,
  fetchSystemRoles,
  fetchSystemUserDetail,
  fetchSystemUsers,
  resetSystemUserPassword,
  updateSystemMenu,
  updateSystemRole,
  updateSystemRoleMenus,
  updateSystemUser,
  updateSystemUserStatus
} from "@/api/modules";
import PagePanel from "@/components/PagePanel.vue";
import {
  formatDateTime,
  formatStatusLabel,
  getTagClass
} from "@/utils/format";

const loading = ref(false);
const submitting = ref(false);
const activeTab = ref("users");

const healthStatus = ref("连接中");
const profile = ref(null);
const dicts = ref([]);

const users = ref([]);
const roles = ref([]);
const menuTree = ref([]);

const userFilters = reactive({
  keyword: "",
  status: ""
});
const userPagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
});

const roleFilterStatus = ref("");
const menuKeyword = ref("");

const userDialogVisible = ref(false);
const passwordDialogVisible = ref(false);
const roleDialogVisible = ref(false);
const roleMenusDialogVisible = ref(false);
const menuDialogVisible = ref(false);

const userEditingId = ref(null);
const passwordTargetId = ref(null);
const passwordTargetName = ref("");
const roleEditingId = ref(null);
const roleMenusTargetId = ref(null);
const roleMenusTargetName = ref("");
const menuEditingId = ref(null);

const roleMenuTreeRef = ref(null);

const userForm = reactive(createEmptyUser());
const passwordForm = reactive({
  password: ""
});
const roleForm = reactive(createEmptyRole());
const menuForm = reactive(createEmptyMenu());

const roleOptions = computed(() =>
  roles.value.filter((item) => item.status === "enabled" || item.id === userForm.roleId)
);

const filteredRoles = computed(() =>
  roleFilterStatus.value
    ? roles.value.filter((item) => item.status === roleFilterStatus.value)
    : roles.value
);

const flattenedMenuOptions = computed(() => flattenMenuOptions(menuTree.value));

const filteredMenuTree = computed(() => filterMenuTree(menuTree.value, menuKeyword.value));

const userDialogTitle = computed(() => `${userEditingId.value ? "编辑" : "新增"}用户`);
const roleDialogTitle = computed(() => `${roleEditingId.value ? "编辑" : "新增"}角色`);
const roleMenusDialogTitle = computed(() =>
  roleMenusTargetName.value ? `分配菜单 - ${roleMenusTargetName.value}` : "分配菜单"
);
const menuDialogTitle = computed(() => `${menuEditingId.value ? "编辑" : "新增"}菜单`);

function createEmptyUser() {
  return {
    username: "",
    password: "",
    realName: "",
    roleId: undefined,
    phone: "",
    status: "enabled"
  };
}

function createEmptyRole() {
  return {
    code: "",
    name: "",
    status: "enabled",
    remark: ""
  };
}

function createEmptyMenu() {
  return {
    parentId: 0,
    name: "",
    path: "",
    component: "",
    menuType: "menu",
    permissionCode: "",
    status: "enabled"
  };
}

function patchUserForm(payload = {}) {
  Object.assign(userForm, createEmptyUser(), payload);
}

function patchRoleForm(payload = {}) {
  Object.assign(roleForm, createEmptyRole(), payload);
}

function patchMenuForm(payload = {}) {
  Object.assign(menuForm, createEmptyMenu(), payload);
}

function flattenMenuOptions(nodes, depth = 0) {
  return nodes.flatMap((item) => {
    const label = `${"　".repeat(depth)}${depth > 0 ? "└ " : ""}${item.name}`;
    return [
      { id: item.id, label },
      ...flattenMenuOptions(item.children || [], depth + 1)
    ];
  });
}

function filterMenuTree(nodes, keyword) {
  const text = String(keyword || "").trim().toLowerCase();
  if (!text) {
    return nodes;
  }
  return nodes
    .map((item) => {
      const children = filterMenuTree(item.children || [], text);
      const matched = [item.name, item.path, item.permissionCode]
        .filter(Boolean)
        .some((field) => String(field).toLowerCase().includes(text));
      if (!matched && children.length === 0) {
        return null;
      }
      return { ...item, children };
    })
    .filter(Boolean);
}

const loadBaseData = async () => {
  const [health, profileData, dictData] = await Promise.all([
    fetchHealth(),
    fetchMyProfile(),
    fetchMaterialUnits()
  ]);

  healthStatus.value = health.status || "UP";
  profile.value = profileData;
  dicts.value = dictData || [];
};

const loadUsers = async () => {
  const pageData = await fetchSystemUsers({
    keyword: userFilters.keyword || undefined,
    status: userFilters.status || undefined,
    pageNo: userPagination.pageNo,
    pageSize: userPagination.pageSize
  });
  users.value = pageData.records || [];
  userPagination.total = pageData.total || 0;
};

const loadRoles = async () => {
  roles.value = (await fetchSystemRoles()) || [];
};

const loadMenuTree = async () => {
  menuTree.value = (await fetchSystemMenuTree()) || [];
};

const refreshAll = async () => {
  loading.value = true;
  try {
    await Promise.all([loadBaseData(), loadUsers(), loadRoles(), loadMenuTree()]);
  } catch (error) {
    ElMessage.error(error.message || "系统模块刷新失败");
  } finally {
    loading.value = false;
  }
};

const queryUsers = async () => {
  userPagination.pageNo = 1;
  try {
    loading.value = true;
    await loadUsers();
  } catch (error) {
    ElMessage.error(error.message || "用户查询失败");
  } finally {
    loading.value = false;
  }
};

const resetUserFilters = async () => {
  userFilters.keyword = "";
  userFilters.status = "";
  await queryUsers();
};

const handleUserPageChange = async (pageNo) => {
  userPagination.pageNo = pageNo;
  try {
    loading.value = true;
    await loadUsers();
  } catch (error) {
    ElMessage.error(error.message || "用户列表加载失败");
  } finally {
    loading.value = false;
  }
};

const handleUserSizeChange = async (pageSize) => {
  userPagination.pageSize = pageSize;
  userPagination.pageNo = 1;
  await handleUserPageChange(1);
};

const resetRoleFilter = async () => {
  roleFilterStatus.value = "";
  await queryRoles();
};

const queryRoles = async () => {
  try {
    loading.value = true;
    await loadRoles();
  } catch (error) {
    ElMessage.error(error.message || "角色列表加载失败");
  } finally {
    loading.value = false;
  }
};

const refreshMenus = async () => {
  try {
    loading.value = true;
    await loadMenuTree();
  } catch (error) {
    ElMessage.error(error.message || "菜单树刷新失败");
  } finally {
    loading.value = false;
  }
};

const openCreateUserDialog = () => {
  userEditingId.value = null;
  patchUserForm();
  userDialogVisible.value = true;
};

const openEditUserDialog = async (id) => {
  try {
    loading.value = true;
    const detail = await fetchSystemUserDetail(id);
    userEditingId.value = id;
    patchUserForm({
      username: detail.username,
      realName: detail.realName,
      roleId: detail.roleId,
      phone: detail.phone,
      status: detail.status
    });
    userDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "用户详情加载失败");
  } finally {
    loading.value = false;
  }
};

const submitUserForm = async () => {
  if (!userForm.username || !userForm.realName || !userForm.roleId || !userForm.status) {
    ElMessage.warning("请填写完整的用户信息");
    return;
  }
  if (!userEditingId.value && !userForm.password) {
    ElMessage.warning("新增用户时必须填写密码");
    return;
  }

  submitting.value = true;
  const payload = {
    username: userForm.username,
    password: userForm.password || undefined,
    realName: userForm.realName,
    roleId: userForm.roleId,
    phone: userForm.phone || "",
    status: userForm.status
  };

  try {
    if (userEditingId.value) {
      await updateSystemUser(userEditingId.value, payload);
    } else {
      await createSystemUser(payload);
    }
    ElMessage.success(`${userDialogTitle.value}成功`);
    userDialogVisible.value = false;
    await loadUsers();
    if (!roles.value.length) {
      await loadRoles();
    }
  } catch (error) {
    ElMessage.error(error.message || "用户保存失败");
  } finally {
    submitting.value = false;
  }
};

const toggleUserStatus = async (row) => {
  const nextStatus = row.status === "enabled" ? "disabled" : "enabled";
  try {
    await ElMessageBox.confirm(`确认将用户切换为${formatStatusLabel(nextStatus)}吗？`, "状态确认", { type: "warning" });
    await updateSystemUserStatus(row.id, { status: nextStatus });
    ElMessage.success("用户状态已更新");
    await loadUsers();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error.message || "状态更新失败");
    }
  }
};

const openResetPasswordDialog = (row) => {
  passwordTargetId.value = row.id;
  passwordTargetName.value = `${row.realName}(${row.username})`;
  passwordForm.password = "";
  passwordDialogVisible.value = true;
};

const submitPasswordReset = async () => {
  if (!passwordTargetId.value || !passwordForm.password) {
    ElMessage.warning("请输入新密码");
    return;
  }
  submitting.value = true;
  try {
    await resetSystemUserPassword(passwordTargetId.value, { password: passwordForm.password });
    ElMessage.success("密码已重置");
    passwordDialogVisible.value = false;
  } catch (error) {
    ElMessage.error(error.message || "密码重置失败");
  } finally {
    submitting.value = false;
  }
};

const removeUser = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除用户 ${row.username} 吗？`, "删除确认", { type: "warning" });
    await deleteSystemUser(row.id);
    ElMessage.success("用户已删除");
    await loadUsers();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error.message || "用户删除失败");
    }
  }
};

const openCreateRoleDialog = () => {
  roleEditingId.value = null;
  patchRoleForm();
  roleDialogVisible.value = true;
};

const openEditRoleDialog = async (id) => {
  try {
    loading.value = true;
    const detail = await fetchSystemRoleDetail(id);
    roleEditingId.value = id;
    patchRoleForm({
      code: detail.code,
      name: detail.name,
      status: detail.status,
      remark: detail.remark
    });
    roleDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "角色详情加载失败");
  } finally {
    loading.value = false;
  }
};

const submitRoleForm = async () => {
  if (!roleForm.code || !roleForm.name || !roleForm.status) {
    ElMessage.warning("请填写完整的角色信息");
    return;
  }

  submitting.value = true;
  try {
    if (roleEditingId.value) {
      await updateSystemRole(roleEditingId.value, { ...roleForm });
    } else {
      await createSystemRole({ ...roleForm });
    }
    ElMessage.success(`${roleDialogTitle.value}成功`);
    roleDialogVisible.value = false;
    await loadRoles();
    await loadUsers();
  } catch (error) {
    ElMessage.error(error.message || "角色保存失败");
  } finally {
    submitting.value = false;
  }
};

const openRoleMenusDialog = async (row) => {
  try {
    loading.value = true;
    const [detail] = await Promise.all([
      fetchSystemRoleDetail(row.id),
      menuTree.value.length ? Promise.resolve() : loadMenuTree()
    ]);
    roleMenusTargetId.value = row.id;
    roleMenusTargetName.value = row.name;
    roleMenusDialogVisible.value = true;
    await nextTick();
    roleMenuTreeRef.value?.setCheckedKeys(detail.menuIds || []);
  } catch (error) {
    ElMessage.error(error.message || "角色菜单加载失败");
  } finally {
    loading.value = false;
  }
};

const submitRoleMenus = async () => {
  const checkedKeys = roleMenuTreeRef.value?.getCheckedKeys?.() || [];
  submitting.value = true;
  try {
    await updateSystemRoleMenus(roleMenusTargetId.value, { menuIds: checkedKeys });
    ElMessage.success("角色菜单已保存");
    roleMenusDialogVisible.value = false;
  } catch (error) {
    ElMessage.error(error.message || "菜单授权失败");
  } finally {
    submitting.value = false;
  }
};

const removeRole = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除角色 ${row.name} 吗？`, "删除确认", { type: "warning" });
    await deleteSystemRole(row.id);
    ElMessage.success("角色已删除");
    await loadRoles();
    await loadUsers();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error.message || "角色删除失败");
    }
  }
};

const openCreateMenuDialog = (parentId = 0) => {
  menuEditingId.value = null;
  patchMenuForm({ parentId });
  menuDialogVisible.value = true;
};

const openEditMenuDialog = async (id) => {
  try {
    loading.value = true;
    const detail = await fetchSystemMenuDetail(id);
    menuEditingId.value = id;
    patchMenuForm({
      parentId: detail.parentId ?? 0,
      name: detail.name,
      path: detail.path || "",
      component: detail.component || "",
      menuType: detail.menuType,
      permissionCode: detail.permissionCode || "",
      status: detail.status
    });
    menuDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "菜单详情加载失败");
  } finally {
    loading.value = false;
  }
};

const submitMenuForm = async () => {
  if (!menuForm.name || !menuForm.menuType || !menuForm.status) {
    ElMessage.warning("请填写完整的菜单信息");
    return;
  }
  submitting.value = true;
  const payload = {
    parentId: menuForm.parentId ?? 0,
    name: menuForm.name,
    path: menuForm.path || "",
    component: menuForm.component || "",
    menuType: menuForm.menuType,
    permissionCode: menuForm.permissionCode || "",
    status: menuForm.status
  };
  try {
    if (menuEditingId.value) {
      await updateSystemMenu(menuEditingId.value, payload);
    } else {
      await createSystemMenu(payload);
    }
    ElMessage.success(`${menuDialogTitle.value}成功`);
    menuDialogVisible.value = false;
    await loadMenuTree();
  } catch (error) {
    ElMessage.error(error.message || "菜单保存失败");
  } finally {
    submitting.value = false;
  }
};

const removeMenu = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除菜单 ${row.name} 吗？`, "删除确认", { type: "warning" });
    await deleteSystemMenu(row.id);
    ElMessage.success("菜单已删除");
    await loadMenuTree();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error.message || "菜单删除失败");
    }
  }
};

onMounted(refreshAll);
</script>
