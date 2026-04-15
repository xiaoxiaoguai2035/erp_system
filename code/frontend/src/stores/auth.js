import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { fetchMyProfile, loginByPassword, logoutRequest } from "@/api/modules";

const TOKEN_KEY = "erp_token";
const USER_KEY = "erp_user";

export const useAuthStore = defineStore("auth", () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || "");
  const userInfo = ref(parseStoredUser());
  const roleInfo = ref(null);
  const menus = ref([]);
  const loading = ref(false);

  const isAuthenticated = computed(() => Boolean(token.value));

  function parseStoredUser() {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  const setToken = (value) => {
    token.value = value || "";
    if (value) {
      localStorage.setItem(TOKEN_KEY, value);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  };

  const setUserInfo = (value) => {
    userInfo.value = value || null;
    if (value) {
      localStorage.setItem(USER_KEY, JSON.stringify(value));
    } else {
      localStorage.removeItem(USER_KEY);
    }
  };

  const clearAuth = () => {
    setToken("");
    setUserInfo(null);
    roleInfo.value = null;
    menus.value = [];
  };

  const fetchProfile = async () => {
    if (!token.value) {
      return null;
    }

    const profile = await fetchMyProfile();
    setUserInfo(profile.userInfo);
    roleInfo.value = profile.roleInfo || null;
    menus.value = profile.menus || [];
    return profile;
  };

  const login = async (payload) => {
    loading.value = true;
    try {
      const response = await loginByPassword(payload);
      setToken(response.token);
      setUserInfo(response.userInfo);
      await fetchProfile();
      return response;
    } finally {
      loading.value = false;
    }
  };

  const logout = async () => {
    try {
      if (token.value) {
        await logoutRequest();
      }
    } finally {
      clearAuth();
    }
  };

  const restoreSession = async () => {
    if (!token.value) {
      return null;
    }

    loading.value = true;
    try {
      return await fetchProfile();
    } catch (error) {
      clearAuth();
      throw error;
    } finally {
      loading.value = false;
    }
  };

  return {
    token,
    userInfo,
    roleInfo,
    menus,
    loading,
    isAuthenticated,
    login,
    logout,
    fetchProfile,
    restoreSession,
    clearAuth
  };
});
