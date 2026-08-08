import React from 'react';
import './Skeleton.css';

/**
 * 스켈레톤 기본 블록.
 * width / height 는 인라인으로 넘길 수 있지만, 실제 컴포넌트와 크기를 맞춰야 하는
 * 자리에는 Skeleton.css 의 전용 클래스를 쓴다. (레이아웃 시프트 방지)
 */
export default function Skeleton({ className = '', width, height, radius, style }) {
  return (
    <span
      className={`skeleton ${className}`.trim()}
      aria-hidden="true"
      style={{ width, height, borderRadius: radius, ...style }}
    />
  );
}

/** ProjectCard 한 장과 같은 높이를 차지한다. */
export function ProjectCardSkeleton() {
  return (
    <div className="skeleton-card">
      <Skeleton className="skeleton-card-thumb" />
      <div className="skeleton-card-meta">
        <Skeleton className="skeleton-line-creator" />
        <Skeleton className="skeleton-block-title" />
        <Skeleton className="skeleton-line-badge" />
        <Skeleton className="skeleton-line-metric" />
        <Skeleton className="skeleton-line-progress" />
      </div>
    </div>
  );
}

/**
 * 카드 그리드용. 실제 목록과 같은 그리드 클래스를 그대로 받아서
 * 로딩 → 로딩 완료 시 열 수와 거터가 변하지 않게 한다.
 */
export function ProjectGridSkeleton({ count = 8, gridClassName = 'project-grid-4col' }) {
  return (
    <div className={gridClassName}>
      {Array.from({ length: count }, (_, i) => (
        <ProjectCardSkeleton key={i} />
      ))}
    </div>
  );
}

/** RankingSidebar 의 rank-item 5개 자리. */
export function RankingListSkeleton({ count = 5 }) {
  return (
    <div className="skeleton-rank-list">
      {Array.from({ length: count }, (_, i) => (
        <div className="skeleton-rank-item" key={i}>
          <Skeleton className="skeleton-rank-thumb" />
          <div className="skeleton-rank-info">
            <Skeleton height={18} width="45%" />
            <Skeleton height={23} width="90%" />
            <Skeleton height={18} width="35%" />
          </div>
        </div>
      ))}
    </div>
  );
}

/** ProjectDetailPage 전체 레이아웃. */
export function ProjectDetailSkeleton() {
  return (
    <div className="project-detail-page container">
      <div className="skeleton-detail-header">
        <Skeleton height={20} width={96} />
        <Skeleton className="skeleton-detail-title" />
        <Skeleton height={22} width={180} />
      </div>

      <div className="skeleton-detail-grid">
        <Skeleton className="skeleton-detail-image" />

        <div className="skeleton-detail-summary">
          {Array.from({ length: 3 }, (_, i) => (
            <div className="skeleton-metric" key={i}>
              <Skeleton height={21} width="40%" />
              <Skeleton height={42} width="70%" />
            </div>
          ))}
          <Skeleton height={96} radius="var(--rounded-sm-lg)" />
          <Skeleton height={48} radius="var(--rounded-sm)" />
        </div>
      </div>

      <div className="skeleton-detail-description">
        <Skeleton height={32} width={160} />
        <Skeleton height={29} width="100%" />
        <Skeleton height={29} width="94%" />
        <Skeleton height={29} width="88%" />
      </div>
    </div>
  );
}

/** 마이페이지 후원 내역 / 내 프로젝트처럼 한 줄짜리 카드가 쌓이는 목록. */
export function ListRowSkeleton({ count = 3 }) {
  return (
    <div className="skeleton-row-list">
      {Array.from({ length: count }, (_, i) => (
        <div className="skeleton-row" key={i}>
          <div className="skeleton-row-main">
            <Skeleton height={23} width="45%" />
            <Skeleton height={21} width="28%" />
          </div>
          <Skeleton height={38} width={120} radius="var(--rounded-full)" />
        </div>
      ))}
    </div>
  );
}
