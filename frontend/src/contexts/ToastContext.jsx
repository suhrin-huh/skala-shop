import React, { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';
import ToastViewport from '../components/Toast';

const ToastContext = createContext(null);

const DEFAULT_DURATION = 3200;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const seq = useRef(0);

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const push = useCallback((message, variant = 'info', duration = DEFAULT_DURATION) => {
    if (!message) return null;
    seq.current += 1;
    const id = seq.current;
    setToasts((prev) => [...prev, { id, message, variant, duration }]);
    return id;
  }, []);

  const value = useMemo(
    () => ({
      showToast: push,
      success: (message, duration) => push(message, 'success', duration),
      error: (message, duration) => push(message, 'error', duration),
      info: (message, duration) => push(message, 'info', duration),
      dismiss,
    }),
    [push, dismiss]
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastViewport toasts={toasts} onDismiss={dismiss} />
    </ToastContext.Provider>
  );
}

/**
 * alert() 대체용. err.response?.data?.error?.message 를 꺼내 쓰는 곳이 많아
 * apiError(err, fallback) 헬퍼를 함께 제공한다.
 */
export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error('useToast 는 ToastProvider 안에서만 사용할 수 있습니다.');
  }
  return ctx;
}

/** axios 에러에서 서버 메시지를 꺼낸다. 없으면 fallback. */
export function extractApiMessage(err, fallback = '요청을 처리하지 못했습니다.') {
  return err?.response?.data?.error?.message || fallback;
}

/** axios 에러의 서버 에러 코드. (PROJECT_001 등) */
export function extractApiCode(err) {
  return err?.response?.data?.error?.code || null;
}
