import { computed, ref } from "vue";
import { defineStore } from "pinia";

export const useAppStore = defineStore("app", () => {
  const collapsed = ref(false);
  const pageTitle = ref("经营看板");

  const toggleSidebar = () => {
    collapsed.value = !collapsed.value;
  };

  const setPageTitle = (title) => {
    pageTitle.value = title;
  };

  const sidebarWidth = computed(() => (collapsed.value ? "84px" : "280px"));

  return {
    collapsed,
    pageTitle,
    sidebarWidth,
    toggleSidebar,
    setPageTitle
  };
});
