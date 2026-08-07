import React, { useState } from 'react';
import { FiX, FiCheckCircle, FiAlertCircle } from 'react-icons/fi';
import api from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import './PledgeModal.css';

export default function PledgeModal({ project, onClose, onSuccess }) {
  const { user, fetchProfile } = useAuth();
  const [amount, setAmount] = useState(project.targetAmount >= 10000 ? 10000 : 1000);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const point = user?.point || 0;
  const reservedPoint = user?.reservedPoint || 0;
  const availablePoint = user?.availablePoint ?? (point - reservedPoint);

  const handlePledgeSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (amount < 1000) {
      setErrorMsg('최소 후원 금액은 1,000원 이상입니다.');
      return;
    }

    if (amount > availablePoint) {
      setErrorMsg('사용 가능 포인트가 부족합니다.');
      return;
    }

    setLoading(true);
    try {
      await api.post('/api/pledges', {
        projectId: project.id,
        amount: Number(amount),
      });

      await fetchProfile(); // 포인트 최신화
      onSuccess();
    } catch (err) {
      const msg = err.response?.data?.error?.message || '후원 처리 중 오류가 발생했습니다.';
      setErrorMsg(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-scrim" onClick={onClose}>
      <div className="pledge-modal-card" onClick={(e) => e.stopPropagation()}>
        <button className="close-btn" onClick={onClose} aria-label="닫기">
          <FiX />
        </button>

        <h3 className="modal-title">프로젝트 후원하기</h3>
        <p className="project-sub-title">{project.title}</p>

        {/* 포인트 현황 비교 카드 */}
        <div className="point-summary-card">
          <div className="point-row">
            <span className="point-label">보유 포인트</span>
            <span className="point-val">{point.toLocaleString()} P</span>
          </div>
          <div className="point-row highlight">
            <span className="point-label">사용 가능 포인트</span>
            <span className="point-val available">{availablePoint.toLocaleString()} P</span>
          </div>
        </div>

        <form onSubmit={handlePledgeSubmit} className="pledge-form">
          <div className="form-group">
            <label htmlFor="amount">후원 금액 (원)</label>
            <input
              id="amount"
              type="number"
              min="1000"
              step="1000"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
            />
          </div>

          <div className="notice-box">
            <FiCheckCircle className="notice-icon" />
            <span>지금 결제되지 않으며, 펀딩 성공 시 마감일에 결제됩니다.</span>
          </div>

          {errorMsg && (
            <div className="error-box">
              <FiAlertCircle />
              <span>{errorMsg}</span>
            </div>
          )}

          <div className="modal-actions">
            <button type="button" className="btn-cancel" onClick={onClose}>
              취소
            </button>
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? '처리 중...' : `${Number(amount).toLocaleString()}원 후원 예약하기`}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
