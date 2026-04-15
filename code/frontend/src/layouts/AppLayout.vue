<template>
  <div class="app-shell">
    <aside class="shell-sidebar" :class="{ collapsed: appStore.collapsed }">
      <div class="brand-panel">
        <div class="brand-mark">ERP</div>
        <div v-if="!appStore.collapsed" class="brand-copy">
          <h1>制造企业中台</h1>
          <p>Production Command Center</p>
        </div>
      </div>

      <nav class="nav-stack">
        <RouterLink
          v-for="item in visibleNavigationItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: route.path === item.path }"
          @click="appStore.setPageTitle(item.title)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span v-if="!appStore.collapsed">{{ item.title }}</span>
        </RouterLink>
      </nav>
    </aside>

    <div class="shell-main">
      <header class="shell-header">
        <div class="header-left">
          <button class="ghost-toggle" type="button" @click="appStore.toggleSidebar">
            <el-icon><Fold /></el-icon>
          </button>
          <div>
            <p class="header-kicker">制造执行一体化平台</p>
            <h2>{{ currentTitle }}</h2>
          </div>
        </div>

        <div class="header-right">
          <div class="header-pill">
            <span class="dot dot-green"></span>
            {{ healthText }}
          </div>
          <div class="header-pill soft">真实业务联动模式</div>
          <div class="user-card" v-if="authStore.userInfo">
            <div class="avatar-badge">{{ userInitial }}</div>
            <div>
              <strong>{{ authStore.userInfo.realName || authStore.userInfo.username }}</strong>
              <p>{{ authStore.roleInfo?.name || authStore.userInfo.username }}</p>
            </div>
          </div>
          <el-button type="primary" plain @click="handleLogout">退出登录</el-button>
        </div>
      </header>

      <main class="shell-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";

import { fetchHealth } from "@/api/modules";
import { navigationItems } from "@/config/navigation";
import { useAppStore } from "@/stores/app";
import { useAuthStore } from "@/stores/auth";
import { getInitial } from "@/utils/format";

const route = useRoute();
const router = useRouter();
const appStore = useAppStore();
const authStore = useAuthStore();
const healthStatus = ref("连接中");

const currentTitle = computed(() => route.meta.title || appStore.pageTitle);
const userInitial = computed(() => getInitial(authStore.userInfo?.realName || authStore.userInfo?.username));
const healthText = computed(() => (healthStatus.value === "UP" ? "系统运行正常" : healthStatus.value));
const visibleNavigationItems = computed(() => {
  const allowedPaths = new Set((authStore.menus || []).map((item) => item.path));
  if (!allowedPaths.size) {
    return navigationItems;
  }

  return navigationItems.filter((item) => allowedPaths.has(item.path));
});

const handleAuthExpired = () => {
  authStore.clearAuth();
  ElMessage.warning("登录状态已失效，请重新登录。");
  router.push({
    path: "/login",
    query: { redirect: route.fullPath }
  });
};

watch(
  () => route.meta.title,
  (title) => {
    if (title) {
      appStore.setPageTitle(title);
    }
  },
  { immediate: true }
);

const loadHealth = async () => {
  try {
    const health = await fetchHealth();
    healthStatus.value = health.status || "UP";
  } catch (error) {
    healthStatus.value = error.message || "服务不可用";
  }
};

const handleLogout = async () => {
  await authStore.logout();
  ElMessage.success("已退出登录");
  router.push("/login");
};

onMounted(async () => {
  await loadHealth();

  try {
    await authStore.restoreSession();
  } catch (error) {
    ElMessage.warning(error.message || "登录状态校验失败，请重新登录。");
    router.push({
      path: "/login",
      query: { redirect: route.fullPath }
    });
  }

  window.addEventListener("auth-expired", handleAuthExpired);
});

onBeforeUnmount(() => {
  window.removeEventListener("auth-expired", handleAuthExpired);
});
</script>
