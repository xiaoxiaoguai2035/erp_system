<template>
  <div class="login-shell">
    <section class="login-hero">
      <div class="hero-image"></div>
      <div class="hero-backdrop"></div>
      <div class="hero-gradient"></div>

      <div class="hero-content">
        <div class="hero-brand">
          <div class="brand-mark">
            <el-icon><OfficeBuilding /></el-icon>
          </div>
          <div>
            <h1>Manufacturing<span>ERP</span></h1>
          </div>
        </div>

        <div class="hero-copy">
          <h2>
            打通采购、生产、库存与销售的
            <br />
            企业级协同工作台
          </h2>
        </div>
      </div>
    </section>

    <section class="login-panel">
      <div class="mobile-brand">
        <div class="brand-mark mobile">
          <el-icon><OfficeBuilding /></el-icon>
        </div>
        <div>
          <strong>Manufacturing<span>ERP</span></strong>
        </div>
      </div>

      <div class="panel-card">
        <div class="panel-header">
          <h2>欢迎回来</h2>
        </div>

        <transition name="fade">
          <div v-if="errorMsg" class="alert-box">
            <el-icon><WarningFilled /></el-icon>
            <span>{{ errorMsg }}</span>
          </div>
        </transition>

        <form class="login-form" @submit.prevent="handleLogin">
          <label class="field-group">
            <span>企业账号 / 用户名</span>
            <div class="field-shell">
              <el-icon class="field-icon"><User /></el-icon>
              <input
                v-model.trim="form.username"
                type="text"
                :disabled="authStore.loading"
                placeholder="请输入您的用户名"
                autocomplete="username"
              />
            </div>
          </label>

          <label class="field-group">
            <span>登录密码</span>
            <div class="field-shell">
              <el-icon class="field-icon"><Lock /></el-icon>
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                :disabled="authStore.loading"
                placeholder="请输入密码"
                autocomplete="current-password"
              />
              <button
                type="button"
                class="field-action"
                :disabled="authStore.loading"
                @click="showPassword = !showPassword"
              >
                <el-icon>
                  <component :is="showPassword ? Hide : View" />
                </el-icon>
              </button>
            </div>
          </label>

          <div class="form-meta">
            <label class="remember-line">
              <input v-model="rememberUsername" type="checkbox" :disabled="authStore.loading" />
              <span>记住账号</span>
            </label>
            <button type="button" class="link-button" @click="handleForgotPassword">
              忘记密码？
            </button>
          </div>

          <div class="status-bar" :class="{ offline: !isHealthUp }">
            <span class="status-dot"></span>
            <span>{{ healthText }}</span>
          </div>

          <button class="submit-button" type="submit" :disabled="authStore.loading">
            <template v-if="authStore.loading">
              <el-icon class="spinner"><Loading /></el-icon>
              正在验证...
            </template>
            <template v-else>
              登录系统
              <el-icon><Right /></el-icon>
            </template>
          </button>
        </form>

        <div class="copyright-line">
          © {{ currentYear }} 面向制造企业的 ERP 管理系统
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  Hide,
  Loading,
  Lock,
  OfficeBuilding,
  Right,
  User,
  View,
  WarningFilled
} from "@element-plus/icons-vue";

import { fetchHealth } from "@/api/modules";
import { useAuthStore } from "@/stores/auth";

const REMEMBER_KEY = "erp_remembered_username";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const form = reactive({
  username: "",
  password: ""
});
const rememberUsername = ref(true);
const showPassword = ref(false);
const errorMsg = ref("");
const healthStatus = ref("正在检测系统服务...");

const currentYear = computed(() => new Date().getFullYear());
const isHealthUp = computed(() => healthStatus.value === "UP");
const healthText = computed(() => (isHealthUp.value ? "系统服务正常" : healthStatus.value));

const loadRememberedUsername = () => {
  const remembered = localStorage.getItem(REMEMBER_KEY) || "";
  form.username = remembered;
  rememberUsername.value = Boolean(remembered);
};

const loadHealth = async () => {
  try {
    const health = await fetchHealth();
    healthStatus.value = health.status || "UP";
  } catch (error) {
    healthStatus.value = error.message || "系统服务连接异常";
  }
};

const handleForgotPassword = () => {
  ElMessage.info("请联系系统管理员重置密码。");
};

const handleLogin = async () => {
  errorMsg.value = "";

  if (!form.username || !form.password) {
    errorMsg.value = "企业账号和密码不能为空";
    return;
  }

  try {
    await authStore.login({
      username: form.username,
      password: form.password
    });

    if (rememberUsername.value) {
      localStorage.setItem(REMEMBER_KEY, form.username);
    } else {
      localStorage.removeItem(REMEMBER_KEY);
    }

    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/dashboard";
    ElMessage.success("登录成功");
    await router.replace(redirect);
  } catch (error) {
    errorMsg.value = error.message || "登录失败，请重试";
  }
};

onMounted(async () => {
  loadRememberedUsername();
  await loadHealth();

  if (!authStore.isAuthenticated) {
    return;
  }

  try {
    await authStore.restoreSession();
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/dashboard";
    await router.replace(redirect);
  } catch {
    errorMsg.value = "登录状态已失效，请重新登录";
  }
});
</script>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: flex;
  background: #f8fafc;
  color: #0f172a;
}

.login-hero,
.login-panel {
  min-height: 100vh;
}

.login-hero {
  position: relative;
  display: flex;
  width: 50%;
  overflow: hidden;
  align-items: center;
  justify-content: center;
  background: #172554;
}

.hero-image,
.hero-backdrop,
.hero-gradient {
  position: absolute;
  inset: 0;
}

.hero-image {
  background:
    linear-gradient(rgba(15, 23, 42, 0.18), rgba(15, 23, 42, 0.32)),
    url("https://images.unsplash.com/photo-1497366216548-37526070297c?ixlib=rb-1.2.1&auto=format&fit=crop&w=1920&q=80")
      center / cover no-repeat;
}

.hero-backdrop {
  background: rgba(30, 41, 94, 0.78);
  mix-blend-mode: multiply;
}

.hero-gradient {
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.1), rgba(15, 23, 42, 0.74));
}

.hero-content {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  max-width: 720px;
  height: 100%;
  flex-direction: column;
  justify-content: space-between;
  padding: 54px 54px 72px;
  color: #ffffff;
}

.hero-brand,
.mobile-brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  display: grid;
  width: 56px;
  height: 56px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.1);
  color: #60a5fa;
  backdrop-filter: blur(12px);
}

.brand-mark.mobile {
  width: 44px;
  height: 44px;
  border-color: rgba(79, 70, 229, 0.08);
  background: rgba(79, 70, 229, 0.08);
  color: #4f46e5;
}

.brand-mark :deep(svg) {
  width: 28px;
  height: 28px;
}

.hero-brand h1,
.hero-brand p,
.panel-header h2,
.panel-header p,
.hero-copy h2,
.hero-copy p {
  margin: 0;
}

.hero-brand h1 {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.hero-brand h1 span,
.mobile-brand strong span {
  color: #60a5fa;
}

.hero-brand p {
  margin-top: 6px;
  color: rgba(224, 231, 255, 0.8);
  font-size: 14px;
}

.hero-copy {
  max-width: 580px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 9px 14px;
  border: 1px solid rgba(147, 197, 253, 0.28);
  border-radius: 999px;
  background: rgba(96, 165, 250, 0.12);
  color: #dbeafe;
  font-size: 13px;
}

.hero-copy h2 {
  margin-top: 24px;
  font-size: 50px;
  line-height: 1.18;
  font-weight: 800;
}

.hero-copy p {
  margin-top: 22px;
  color: rgba(224, 231, 255, 0.88);
  font-size: 16px;
  line-height: 1.9;
}

.hero-points {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 34px;
}

.hero-point {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #dbeafe;
  font-size: 14px;
  font-weight: 500;
}

.hero-point :deep(svg) {
  width: 18px;
  height: 18px;
  color: #60a5fa;
}

.login-panel {
  position: relative;
  display: flex;
  width: 50%;
  flex-direction: column;
  justify-content: center;
  padding: 32px 40px;
  background:
    radial-gradient(circle at top right, rgba(99, 102, 241, 0.08), transparent 24%),
    linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.mobile-brand {
  display: none;
  margin-bottom: 22px;
}

.mobile-brand strong,
.mobile-brand p {
  margin: 0;
}

.mobile-brand strong {
  font-size: 20px;
  color: #0f172a;
}

.mobile-brand p {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.panel-card {
  width: 100%;
  max-width: 460px;
  margin: 0 auto;
}

.panel-header h2 {
  font-size: 34px;
  font-weight: 700;
  color: #111827;
}

.panel-header p {
  margin-top: 10px;
  color: #6b7280;
  line-height: 1.7;
}

.alert-box {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 28px;
  padding: 14px 16px;
  border-left: 4px solid #ef4444;
  border-radius: 0 16px 16px 0;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 14px;
}

.alert-box :deep(svg) {
  width: 18px;
  height: 18px;
}

.login-form {
  margin-top: 28px;
}

.field-group {
  display: block;
}

.field-group + .field-group {
  margin-top: 20px;
}

.field-group > span {
  display: block;
  margin-bottom: 10px;
  color: #374151;
  font-size: 14px;
  font-weight: 600;
}

.field-shell {
  position: relative;
}

.field-shell input {
  width: 100%;
  height: 54px;
  padding: 0 50px 0 44px;
  border: 1px solid #d1d5db;
  border-radius: 18px;
  background: #ffffff;
  color: #111827;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.field-shell input::placeholder {
  color: #9ca3af;
}

.field-shell input:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.1);
}

.field-shell input:disabled,
.field-action:disabled,
.submit-button:disabled,
.remember-line input:disabled {
  cursor: not-allowed;
}

.field-icon,
.field-action {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
}

.field-icon {
  left: 14px;
}

.field-icon :deep(svg),
.field-action :deep(svg) {
  width: 20px;
  height: 20px;
}

.field-action {
  right: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  background: transparent;
}

.form-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
}

.remember-line {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #374151;
  font-size: 14px;
}

.remember-line input {
  width: 16px;
  height: 16px;
  accent-color: #4f46e5;
}

.link-button {
  padding: 0;
  border: 0;
  background: transparent;
  color: #4f46e5;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.status-bar {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.08);
  color: #15803d;
  font-size: 13px;
}

.status-bar.offline {
  background: rgba(239, 68, 68, 0.08);
  color: #b91c1c;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.submit-button {
  display: flex;
  width: 100%;
  height: 56px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 24px;
  border: 0;
  border-radius: 18px;
  background: linear-gradient(135deg, #4f46e5 0%, #4338ca 100%);
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  box-shadow: 0 18px 32px rgba(79, 70, 229, 0.24);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    opacity 0.2s ease;
  cursor: pointer;
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 20px 36px rgba(79, 70, 229, 0.3);
}

.submit-button :deep(svg),
.sso-main :deep(svg) {
  width: 18px;
  height: 18px;
}

.spinner {
  animation: spin 1s linear infinite;
}

.enterprise-entry {
  margin-top: 34px;
}

.divider {
  position: relative;
  text-align: center;
}

.divider::before {
  content: "";
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  border-top: 1px solid #e5e7eb;
}

.divider span {
  position: relative;
  display: inline-block;
  padding: 0 12px;
  background: #ffffff;
  color: #9ca3af;
  font-size: 13px;
}

.sso-button {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  margin-top: 18px;
  padding: 0 18px;
  height: 54px;
  border: 1px solid #d1d5db;
  border-radius: 18px;
  background: #ffffff;
  color: #374151;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    transform 0.2s ease;
  cursor: pointer;
}

.sso-button:hover {
  border-color: #c7d2fe;
  background: #f8faff;
  transform: translateY(-1px);
}

.sso-main {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}

.sso-button em {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 42px;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
  font-style: normal;
  font-size: 12px;
}

.sso-tip {
  margin: 12px 0 0;
  color: #9ca3af;
  font-size: 12px;
  line-height: 1.7;
}

.copyright-line {
  margin-top: 30px;
  color: #9ca3af;
  font-size: 12px;
  text-align: center;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.28s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1100px) {
  .login-shell {
    flex-direction: column;
  }

  .login-hero {
    display: none;
  }

  .login-panel {
    width: 100%;
    min-height: 100vh;
    padding: 28px 20px;
  }

  .mobile-brand {
    display: flex;
    width: 100%;
    max-width: 460px;
    margin: 0 auto 20px;
  }
}

@media (max-width: 640px) {
  .panel-card {
    max-width: 100%;
  }

  .panel-header h2 {
    font-size: 30px;
  }

  .form-meta {
    align-items: flex-start;
    flex-direction: column;
  }

  .sso-button {
    padding: 0 14px;
  }
}
</style>
