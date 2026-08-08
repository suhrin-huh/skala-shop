import React from 'react';
import './StatusBadge.css';

const URGENT_DAYS = 7;

/** 'YYYY-MM-DD' 와 오늘 사이의 남은 일수. 오늘 마감이면 0. */
export function daysUntil(endDate) {
  if (!endDate) return null;
  const end = new Date(`${endDate}T00:00:00`);
  if (Number.isNaN(end.getTime())) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.round((end - today) / 86400000);
}

/**
 * 프로젝트 상태 배지. 문구는 domain-rules 의 상태 전이표에 묶여 있다.
 * SCHEDULED 오픈 예정 / ONGOING 펀딩 중 / SUCCESS 펀딩 성공 / FAILED 펀딩 무산
 *
 * ONGOING 이면서 마감이 7일 이내면 "마감 D-n" 으로 대체한다.
 */
export default function StatusBadge({ status, endDate }) {
  if (status === 'ONGOING') {
    const remaining = daysUntil(endDate);
    if (remaining !== null && remaining >= 0 && remaining <= URGENT_DAYS) {
      return (
        <span className="status-badge urgent">
          {remaining === 0 ? '오늘 마감' : `마감 D-${remaining}`}
        </span>
      );
    }
    return <span className="status-badge neutral">펀딩 중</span>;
  }

  if (status === 'SCHEDULED') {
    return <span className="status-badge neutral">오픈 예정</span>;
  }

  if (status === 'SUCCESS') {
    return <span className="status-badge success">펀딩 성공</span>;
  }

  if (status === 'FAILED') {
    return <span className="status-badge muted">펀딩 무산</span>;
  }

  return null;
}
