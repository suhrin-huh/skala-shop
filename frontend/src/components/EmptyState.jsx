import React from 'react';
import { FiInbox } from 'react-icons/fi';
import './EmptyState.css';

/**
 * 빈 상태 공통 컴포넌트.
 * 검색 결과 없음 / 후원 내역 없음 / 찜 없음 / 최근 본 항목 없음에서 모두 이걸 쓴다.
 */
export default function EmptyState({ icon, title, description, action }) {
  const Icon = icon || FiInbox;

  return (
    <div className="empty-state-box">
      <span className="empty-state-icon" aria-hidden="true">
        <Icon />
      </span>
      <p className="empty-state-title">{title}</p>
      {description && <p className="empty-state-description">{description}</p>}
      {action && <div className="empty-state-action">{action}</div>}
    </div>
  );
}
