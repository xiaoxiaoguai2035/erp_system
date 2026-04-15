const numberFormatter = new Intl.NumberFormat("zh-CN", {
  minimumFractionDigits: 0,
  maximumFractionDigits: 2
});

const moneyFormatter = new Intl.NumberFormat("zh-CN", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
});

export const formatNumber = (value, fallback = "--") => {
  if (value === null || value === undefined || value === "") {
    return fallback;
  }

  const numericValue = Number(value);
  if (Number.isNaN(numericValue)) {
    return fallback;
  }

  return numberFormatter.format(numericValue);
};

export const formatMoney = (value, fallback = "--") => {
  if (value === null || value === undefined || value === "") {
    return fallback;
  }

  const numericValue = Number(value);
  if (Number.isNaN(numericValue)) {
    return fallback;
  }

  return moneyFormatter.format(numericValue);
};

export const formatPercent = (value, digits = 1, fallback = "--") => {
  if (value === null || value === undefined || value === "") {
    return fallback;
  }

  const numericValue = Number(value);
  if (Number.isNaN(numericValue)) {
    return fallback;
  }

  const percent = Math.abs(numericValue) <= 1 ? numericValue * 100 : numericValue;
  return `${percent.toFixed(digits)}%`;
};

export const formatDate = (value, fallback = "--") => {
  if (!value) {
    return fallback;
  }

  return String(value).slice(0, 10);
};

export const formatDateTime = (value, fallback = "--") => {
  if (!value) {
    return fallback;
  }

  return String(value).replace("T", " ").slice(0, 19);
};

export const formatStatusLabel = (value) => {
  if (!value) {
    return "--";
  }

  const labelMap = {
    enabled: "启用",
    disabled: "停用",
    draft: "草稿",
    approved: "已审核",
    partial: "部分完成",
    in_progress: "执行中",
    completed: "已完成",
    closed: "已关闭",
    warning: "预警",
    normal: "正常",
    up: "正常"
  };

  return labelMap[String(value).toLowerCase()] || value;
};

export const formatMaterialType = (value) => {
  if (!value) {
    return "--";
  }

  const labelMap = {
    raw: "原料",
    semi: "半成品",
    finished: "成品",
    "原料": "原料",
    "半成品": "半成品",
    "成品": "成品"
  };

  return labelMap[String(value).trim().toLowerCase()] || labelMap[String(value).trim()] || value;
};

export const getTagClass = (value) => {
  const text = String(value || "").toLowerCase();

  if (
    text.includes("disable") ||
    text.includes("close") ||
    text.includes("risk") ||
    text.includes("warn") ||
    text.includes("danger") ||
    text.includes("异常") ||
    text.includes("停用")
  ) {
    return "tag-danger";
  }

  if (
    text.includes("approve") ||
    text.includes("enable") ||
    text.includes("success") ||
    text.includes("normal") ||
    text.includes("finish") ||
    text.includes("up") ||
    text.includes("启用") ||
    text.includes("正常")
  ) {
    return "tag-success";
  }

  return "tag-warning";
};

export const getInitial = (value, fallback = "管") => {
  if (!value) {
    return fallback;
  }

  return String(value).trim().charAt(0) || fallback;
};
