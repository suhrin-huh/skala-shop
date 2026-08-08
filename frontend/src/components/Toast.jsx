import React, { useEffect } from 'react';
import { FiCheckCircle, FiAlertCircle, FiInfo, FiX } from 'react-icons/fi';
import './Toast.css';

const ICONS = {
  success: FiCheckCircle,
  error: FiAlertCircle,
  info: FiInfo,
};

function ToastItem({ toast, onDismiss }) {
  const { id, message, variant, duration } = toast;
  const Icon = ICONS[variant] || FiInfo;

  useEffect(() => {
    if (!duration) return undefined;
    const timer = setTimeout(() => onDismiss(id), duration);
    return () => clearTimeout(timer);
  }, [id, duration, onDismiss]);

  return (
    <div className={`toast-item ${variant}`} role="status">
      <span className="toast-icon">
        <Icon />
      </span>
      <span className="toast-message">{message}</span>
      <button type="button" className="toast-close" onClick={() => onDismiss(id)} aria-label="알림 닫기">
        <FiX />
      </button>
    </div>
  );
}

/** ToastProvider 가 렌더링하는 전역 뷰포트. 직접 쓸 일은 없다. */
export default function ToastViewport({ toasts, onDismiss }) {
  if (toasts.length === 0) return null;

  return (
    <div className="toast-viewport" aria-live="polite">
      {toasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} onDismiss={onDismiss} />
      ))}
    </div>
  );
}
