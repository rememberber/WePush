import type { ButtonHTMLAttributes, HTMLAttributes, ReactNode } from "react";

export function Button({ className = "", variant = "secondary", ...props }: ButtonProps) {
  return <button className={`wp-button wp-button--${variant} ${className}`} {...props} />;
}

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "ghost" | "danger";
}

export function Badge({ children, tone = "neutral", className = "", ...props }: BadgeProps) {
  return (
    <span className={`wp-badge wp-badge--${tone} ${className}`} {...props}>
      {children}
    </span>
  );
}

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  children: ReactNode;
  tone?: "neutral" | "success" | "warning" | "danger" | "info";
}

export function Spinner({ label = "加载中" }: { label?: string }) {
  return <span className="wp-spinner" role="status" aria-label={label} />;
}

export function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="wp-empty-state">
      <div className="wp-empty-state__icon" aria-hidden="true">{icon}</div>
      <h3>{title}</h3>
      <p>{description}</p>
      {action}
    </div>
  );
}

interface EmptyStateProps {
  icon: ReactNode;
  title: string;
  description: string;
  action?: ReactNode;
}

export function FieldLabel({ children, hint }: { children: ReactNode; hint?: string }) {
  return (
    <label className="wp-field-label">
      <span>{children}</span>
      {hint ? <small>{hint}</small> : null}
    </label>
  );
}
