import React, { useEffect } from 'react';
import './ConfirmModal.css';

/**
 * window.confirm() 대체 모달.
 *
 * impact 는 "이 행동이 남에게 미치는 결과"를 못 박는 자리다.
 * 프로젝트 삭제라면 후원자 수를 여기에 넣는다.
 */
export default function ConfirmModal({
  title,
  message,
  impact,
  confirmLabel = '확인',
  cancelLabel = '취소',
  danger = false,
  loading = false,
  onConfirm,
  onCancel,
}) {
  useEffect(() => {
    const onKeyDown = (e) => {
      if (e.key === 'Escape' && !loading) onCancel();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onCancel, loading]);

  return (
    <div className="modal-scrim" onClick={() => !loading && onCancel()}>
      <div
        className="modal-card confirm-modal-card"
        role="alertdialog"
        aria-modal="true"
        aria-label={title}
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="confirm-modal-title">{title}</h3>
        {message && <p className="confirm-modal-body">{message}</p>}
        {impact && <div className="confirm-modal-impact">{impact}</div>}

        <div className="confirm-modal-actions">
          <button type="button" className="btn-cancel" onClick={onCancel} disabled={loading}>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={danger ? 'btn-danger' : 'btn-primary'}
            onClick={onConfirm}
            disabled={loading}
          >
            {loading ? '처리 중...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
