import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import "tailwindcss";
import "@wepush-next/design-tokens/theme.css";
import "@wepush-next/features/styles.css";
import { WePushApp } from "@wepush-next/features";

const root = document.getElementById("root");
if (!root) {
  throw new Error("Missing #root application mount point");
}

createRoot(root).render(
  <StrictMode>
    <WePushApp />
  </StrictMode>,
);
