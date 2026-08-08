import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { FiHeart, FiClock, FiFileText, FiFolder } from 'react-icons/fi';
import api from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import { useToast, extractApiMessage } from '../contexts/ToastContext';
import useRecentlyViewed from '../hooks/useRecentlyViewed';
import ProjectCard from '../components/ProjectCard';
import ConfirmModal from '../components/ConfirmModal';
import EmptyState from '../components/EmptyState';
import StatusBadge from '../components/StatusBadge';
import { ListRowSkeleton, ProjectGridSkeleton } from '../components/Skeleton';
import './MyPage.css';

const TABS = [
  { key: 'pledges', label: '후원 내역' },
  { key: 'likes', label: '찜한 프로젝트' },
  { key: 'projects', label: '내가 올린 프로젝트' },
  { key: 'recent', label: '최근 본 항목' },
];

/** domain-rules 10 의 표기 규칙. 임의로 바꾸면 결제 시점을 오해하게 된다. */
const PLEDGE_STATUS_LABEL = {
  PLEDGED: '결제 예약됨 (마감일 결제 예정)',
  CANCELLED: '취소됨',
  CONFIRMED: '결제 완료',
  FAILED: '펀딩 무산 (미결제)',
};

const DELIVERY_STATUS_LABEL = {
  ORDER_COMPLETED: '주문 완료',
  SHIPPING: '배송 중',
  DELIVERED: '배송 완료',
};

export default function MyPage() {
  const { user, fetchProfile } = useAuth();
  const toast = useToast();

  const [activeTab, setActiveTab] = useState('pledges');
  const [pledges, setPledges] = useState([]);
  const [likes, setLikes] = useState([]);
  const [myProjects, setMyProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  // '최근 본 항목' 탭에서만 조회한다. ID 는 LocalStorage, 내용은 서버 최신값.
  const recentlyViewed = useRecentlyViewed({ enabled: activeTab === 'recent' });

  useEffect(() => {
    fetchProfile();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadPledges = useCallback(async () => {
    const res = await api.get('/api/users/me/pledges');
    setPledges(res.data.data?.content || []);
  }, []);

  useEffect(() => {
    let cancelled = false;

    const loadTabData = async () => {
      if (activeTab === 'recent') return; // 훅이 알아서 처리한다.

      setLoading(true);
      try {
        if (activeTab === 'pledges') {
          await loadPledges();
        } else if (activeTab === 'likes') {
          const res = await api.get('/api/users/me/likes');
          if (!cancelled) setLikes(res.data.data?.content || []);
        } else if (activeTab === 'projects') {
          const res = await api.get('/api/users/me/projects');
          if (!cancelled) setMyProjects(res.data.data?.content || []);
        }
      } catch (err) {
        if (!cancelled) toast.error(extractApiMessage(err, '목록을 불러오지 못했습니다.'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    loadTabData();
    return () => {
      cancelled = true;
    };
  }, [activeTab, loadPledges, toast]);

  const handleCancelPledge = async () => {
    if (!cancelTarget) return;
    setCancelling(true);
    try {
      await api.post(`/api/pledges/${cancelTarget.id}/cancel`);
      toast.success('후원이 취소되었습니다. 예약 포인트가 해제되었습니다.');
      setCancelTarget(null);
      await Promise.all([fetchProfile(), loadPledges()]);
    } catch (err) {
      toast.error(extractApiMessage(err, '후원 취소에 실패했습니다.'));
    } finally {
      setCancelling(false);
    }
  };

  const point = user?.point || 0;
  const reservedPoint = user?.reservedPoint || 0;
  const availablePoint = user?.availablePoint ?? point - reservedPoint;

  const isTabLoading = activeTab === 'recent' ? recentlyViewed.loading : loading;

  return (
    <div className="mypage container">
      {/* 1. 프로필 & 포인트 3종 카드 */}
      <div className="mypage-profile-card">
        <div className="user-info-header">
          <h2 className="user-nickname">{user?.nickname}님</h2>
          <span className="user-email">{user?.email}</span>
        </div>

        <div className="point-trio-grid">
          <div className="point-trio-item">
            <span className="label">보유 포인트</span>
            <span className="val">{point.toLocaleString()} P</span>
          </div>
          <div className="point-trio-item">
            <span className="label">예약 포인트</span>
            <span className="val reserved">{reservedPoint.toLocaleString()} P</span>
          </div>
          <div className="point-trio-item highlight">
            <span className="label">사용 가능 포인트</span>
            <span className="val available">{availablePoint.toLocaleString()} P</span>
          </div>
        </div>
        <p className="point-trio-note">
          후원해도 포인트는 바로 줄지 않습니다. 펀딩이 성공하면 마감일에 결제됩니다.
        </p>
      </div>

      {/* 2. 탭 내비게이션 — 좁은 화면에서는 줄바꿈 대신 가로 스크롤 */}
      <div className="mypage-tabs">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            className={`tab-btn ${activeTab === tab.key ? 'active' : ''}`}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 3. 탭 콘텐츠 */}
      <div className="mypage-tab-content">
        {isTabLoading ? (
          activeTab === 'likes' || activeTab === 'recent' ? (
            <ProjectGridSkeleton count={4} gridClassName="mypage-project-grid" />
          ) : (
            <ListRowSkeleton count={3} />
          )
        ) : activeTab === 'pledges' ? (
          <PledgesTab pledges={pledges} onRequestCancel={setCancelTarget} />
        ) : activeTab === 'likes' ? (
          <LikesTab likes={likes} />
        ) : activeTab === 'projects' ? (
          <MyProjectsTab projects={myProjects} />
        ) : (
          <RecentTab projects={recentlyViewed.projects} onClear={recentlyViewed.clear} />
        )}
      </div>

      {cancelTarget && (
        <ConfirmModal
          title="후원을 취소할까요?"
          message={`'${cancelTarget.project?.title || '프로젝트'}' 에 대한 후원 예약을 취소합니다.`}
          impact={`예약된 ${(cancelTarget.amount || 0).toLocaleString()}P 가 사용 가능 포인트로 즉시 돌아옵니다. 결제가 일어난 적이 없으므로 환불 절차는 없습니다.`}
          confirmLabel="후원 취소하기"
          cancelLabel="유지하기"
          danger
          loading={cancelling}
          onConfirm={handleCancelPledge}
          onCancel={() => setCancelTarget(null)}
        />
      )}
    </div>
  );
}

function PledgesTab({ pledges, onRequestCancel }) {
  if (pledges.length === 0) {
    return (
      <EmptyState
        icon={FiFileText}
        title="후원 내역이 없습니다"
        description="마음에 드는 프로젝트를 찾아 첫 후원을 시작해 보세요."
        action={
          <Link to="/projects" className="btn-primary">
            프로젝트 둘러보기
          </Link>
        }
      />
    );
  }

  return (
    <div className="pledges-list">
      {pledges.map((p) => {
        const deleted = Boolean(p.projectDeleted);
        const title = p.project?.title || '프로젝트';

        return (
          <div key={p.id} className="pledge-item-card">
            <div className="pledge-main-info">
              {/* 삭제된 프로젝트는 내역에는 남기되 상세 링크를 막는다. */}
              {deleted ? (
                <h4 className="pledge-project-title deleted">{title}</h4>
              ) : (
                <Link to={`/projects/${p.project?.id}`} className="pledge-project-title">
                  {title}
                </Link>
              )}

              <div className="pledge-sub-row">
                <span className="pledge-amount">{p.amount?.toLocaleString()}원 후원</span>
                {deleted && <span className="status-badge muted">삭제된 프로젝트</span>}
              </div>
            </div>

            <div className="pledge-status-col">
              <span className={`pledge-badge ${p.status.toLowerCase()}`}>
                {PLEDGE_STATUS_LABEL[p.status] || p.status}
                {p.status === 'CONFIRMED' && p.deliveryStatus
                  ? ` · ${DELIVERY_STATUS_LABEL[p.deliveryStatus] || p.deliveryStatus}`
                  : ''}
              </span>

              {p.status === 'PLEDGED' && (
                <button
                  type="button"
                  className="btn-outline-pill cancel-btn"
                  onClick={() => onRequestCancel(p)}
                >
                  후원 취소
                </button>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}

function LikesTab({ likes }) {
  if (likes.length === 0) {
    return (
      <EmptyState
        icon={FiHeart}
        title="찜한 프로젝트가 없습니다"
        description="카드의 하트를 누르면 여기에 모아 볼 수 있습니다."
        action={
          <Link to="/projects" className="btn-primary">
            프로젝트 둘러보기
          </Link>
        }
      />
    );
  }

  return (
    <div className="mypage-project-grid">
      {likes.map((project) => (
        <ProjectCard key={project.id} project={{ ...project, liked: true }} />
      ))}
    </div>
  );
}

function MyProjectsTab({ projects }) {
  if (projects.length === 0) {
    return (
      <EmptyState
        icon={FiFolder}
        title="등록한 프로젝트가 없습니다"
        description="아이디어가 있다면 지금 창작자로 시작해 보세요."
        action={
          <Link to="/projects/new" className="btn-primary">
            프로젝트 올리기
          </Link>
        }
      />
    );
  }

  return (
    <div className="my-projects-list">
      {projects.map((proj) => (
        <div key={proj.id} className="my-project-item">
          <img src={proj.mainImage} alt={proj.title} className="thumb" />
          <div className="info">
            <Link to={`/projects/${proj.id}`} className="my-project-title">
              {proj.title}
            </Link>
            <p className="my-project-meta">
              목표액 {proj.targetAmount?.toLocaleString()}원 · 모금액{' '}
              {proj.currentAmount?.toLocaleString()}원 ({proj.pledgeCount || 0}명)
            </p>
            <StatusBadge status={proj.status} endDate={proj.endDate} />
          </div>
          <Link to={`/projects/${proj.id}/edit`} className="btn-outline-pill">
            수정
          </Link>
        </div>
      ))}
    </div>
  );
}

function RecentTab({ projects, onClear }) {
  if (projects.length === 0) {
    return (
      <EmptyState
        icon={FiClock}
        title="최근 본 항목이 없습니다"
        description="프로젝트 상세를 열면 최근 10개까지 여기에 쌓입니다."
        action={
          <Link to="/projects" className="btn-primary">
            프로젝트 둘러보기
          </Link>
        }
      />
    );
  }

  return (
    <>
      <div className="mypage-list-toolbar">
        <span className="mypage-list-hint">최근 본 순서로 최대 10개까지 보관됩니다.</span>
        <button type="button" className="btn-outline-pill" onClick={onClear}>
          기록 지우기
        </button>
      </div>

      <div className="mypage-project-grid">
        {projects.map((project) => (
          <ProjectCard key={project.id} project={project} />
        ))}
      </div>
    </>
  );
}
