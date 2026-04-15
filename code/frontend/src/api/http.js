import axios from "axios";

const http = axios.create({
  baseURL: "/api/v1",
  timeout: 15000
});

const clearAuth = () => {
  localStorage.removeItem("erp_token");
  localStorage.removeItem("erp_user");
};

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("erp_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    const payload = response.data;

    if (payload?.code === 200) {
      return payload.data;
    }

    const message = payload?.message || `请求失败(code:${payload?.code || 'unknown'})`;
    return Promise.reject(new Error(message));
  },
  (error) => {
    const status = error.response?.status;
    const message = error.response?.data?.message || error.message || "网络请求失败";

    if (status === 401) {
      clearAuth();
      window.dispatchEvent(new CustomEvent("auth-expired"));
    }

    return Promise.reject(new Error(message));
  }
);

export default http;
