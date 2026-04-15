import { createRouter, createWebHistory } from "vue-router";

const AppLayout = () => import("@/layouts/AppLayout.vue");
const DashboardView = () => import("@/views/DashboardView.vue");
const MasterDataView = () => import("@/views/MasterDataView.vue");
const LoginView = () => import("@/views/LoginView.vue");
const PurchaseView = () => import("@/views/PurchaseView.vue");
const SalesView = () => import("@/views/SalesView.vue");
const InventoryView = () => import("@/views/InventoryView.vue");
const ProductionView = () => import("@/views/ProductionView.vue");
const ReportsView = () => import("@/views/ReportsView.vue");
const SystemView = () => import("@/views/SystemView.vue");
const AiAssistantView = () => import("@/views/AiAssistantView.vue");

const routes = [
  {
    path: "/login",
    name: "login",
    component: LoginView,
    meta: { title: "登录", public: true }
  },
  {
    path: "/",
    component: AppLayout,
    redirect: "/login",
    children: [
      {
        path: "dashboard",
        name: "dashboard",
        component: DashboardView,
        meta: { title: "经营看板" }
      },
      {
        path: "master-data",
        name: "master-data",
        component: MasterDataView,
        meta: { title: "基础数据" }
      },
      {
        path: "purchase",
        name: "purchase",
        component: PurchaseView,
        meta: { title: "采购管理" }
      },
      {
        path: "sales",
        name: "sales",
        component: SalesView,
        meta: { title: "销售管理" }
      },
      {
        path: "inventory",
        name: "inventory",
        component: InventoryView,
        meta: { title: "库存管理" }
      },
      {
        path: "production",
        name: "production",
        component: ProductionView,
        meta: { title: "生产管理" }
      },
      {
        path: "reports",
        name: "reports",
        component: ReportsView,
        meta: { title: "统计分析" }
      },
      {
        path: "system",
        name: "system",
        component: SystemView,
        meta: { title: "系统管理" }
      },
      {
        path: "ai",
        name: "ai",
        component: AiAssistantView,
        meta: { title: "AI 助手" }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const token = localStorage.getItem("erp_token");

  if (to.meta.public) {
    return true;
  }

  if (!token) {
    return {
      path: "/login",
      query: { redirect: to.fullPath }
    };
  }

  return true;
});

export default router;
