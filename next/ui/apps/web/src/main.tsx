import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import "tailwindcss";
import "@wepush-next/design-tokens/theme.css";
import "@wepush-next/features/styles.css";
import { WePushApp } from "@wepush-next/features";

try {
  const stored = localStorage.getItem("wepush.theme");
  document.documentElement.dataset.theme = stored === "light" || stored === "dark" ? stored
    : window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
} catch { /* React applies the system theme after mounting */ }

const root = document.getElementById("root");
if (!root) {
  throw new Error("Missing #root application mount point");
}

createRoot(root).render(
  <StrictMode>
    <WePushApp />
  </StrictMode>,
);
