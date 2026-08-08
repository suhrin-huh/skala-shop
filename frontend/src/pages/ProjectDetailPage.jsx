import React, { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import api from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import { useToast, extractApiMessage } from '../contexts/ToastContext';
import { recordRecentlyViewed } from '../hooks/useRecentlyViewed';
import PledgeModal from '../components/PledgeModal';
import ConfirmModal from '../components/ConfirmModal';
import StatusBadge from '../components/StatusBadge';
import { ProjectDetailSkeleton } from '../components/Skeleton';
import NotFoundPage from './NotFoundPage';
import './ProjectDetailPage.css';

export default function ProjectDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();

  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [showPledgeModal, setShowPledgeModal] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const fetchProject = useCallback(async () => {
    try {
      const res = await api.get(`/api/projects/${id}`);
      setProject(res.data.data);
      setNotFound(false);
      // 최근 본 펀딩에는 ID 만 남긴다. 조회는 항상 서버 최신값으로 다시 한다.
      recordRecentlyViewed(res.data.data.id);
    } catch (err) {
      // 삭제된 프로젝트는 404 + PROJECT_001 로 온다. 전용 404 화면을 쓴다.
      if (err.response?.status === 404) {
        setNotFound(true);
      } else {
        toast.error(extractApiMessage(err, '프로젝트를 불러올 수 없습니다.'));
        setNotFound(true);
      }
    }
  }, [id, toast]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetchProject().finally(() => {
      if (!cancelled) setLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [fetchProject]);

  const handleDelete = async () => {
    setDeleting(true);
    try {
      const res = await api.delete(`/api/projects/${id}`);
      const cancelled = res.data.data?.cancelledPledgeCount || 0;
      toast.success(
        cancelled > 0
          ? `프로젝트가 삭제되었습니다. 후원 ${cancelled.toLocaleString()}건이 취소 처리되었습니다.`
          : '프로젝트가 삭제되었습니다.'
      );
      navigate('/');
    } catch (err) {
      toast.error(extractApiMessage(err, '삭제에 실패했습니다.'));
      setDeleting(false);
      setShowDeleteConfirm(false);
    }
  };

  if (loading) return <ProjectDetailSkeleton />;

  if (notFound || !project) {
    return (
      <NotFoundPage
        title="삭제되었거나 존재하지 않는 프로젝트입니다"
        description="창작자가 프로젝트를 내렸거나, 주소가 잘못되었을 수 있습니다."
      />
    );
  }

  const percent =
    project.achievementRate ??
    (project.targetAmount ? Math.floor((project.currentAmount / project.targetAmount) * 100) : 0);

  const isCreator = user && user.id === project.creator?.id;
  const pledgeCount = project.pledgeCount || 0;

  return (
    <div className="project-detail-page container">
      <div className="detail-header-section">
        <span className="category-tag">{project.category?.name || '카테고리'}</span>
        <h1 className="detail-title">{project.title}</h1>
        <div className="creator-profile">
          <span className="badge-creator">◆ 좋은창작자</span>
          <span className="creator-nickname">{project.creator?.nickname}</span>
          <StatusBadge status={project.status} endDate={project.endDate} />
        </div>
      </div>

      <div className="detail-hero-grid">
        <div className="detail-main-image-box">
          <img src={project.mainImage} alt={project.title} className="detail-main-img" />
        </div>

        <div className="detail-summary-card">
          <div className="metric-item">
            <span className="metric-label">모인 금액</span>
            <div className="metric-value-row">
              <span className="metric-val primary">{(project.currentAmount || 0).toLocaleString()}</span>
              <span className="metric-unit">원</span>
              <span className="metric-percent">{percent}%</span>
            </div>
          </div>

          <div className="metric-item">
            <span className="metric-label">남은 시간</span>
            <div className="metric-value-row">
              <span className="metric-val">{project.endDate} 마감</span>
            </div>
          </div>

          <div className="metric-item">
            <span className="metric-label">후원자</span>
            <div className="metric-value-row">
              <span className="metric-val">{pledgeCount}</span>
              <span className="metric-unit">명</span>
            </div>
          </div>

          <div className="pledge-notice-card">
            <p><strong>목표 금액:</strong> {project.targetAmount?.toLocaleString()}원</p>
            <p><strong>펀딩 기간:</strong> {project.startDate} ~ {project.endDate}</p>
            <p className="notice-sub">예약형 All-or-Nothing 시스템으로, 마감일까지 목표 금액이 달성되어야만 마감일에 결제됩니다.</p>
          </div>

          <div className="action-buttons">
            {isCreator ? (
              <div className="creator-actions">
                <Link to={`/projects/${project.id}/edit`} className="btn-outline-pill edit-btn">
                  프로젝트 수정
                </Link>
                <button
                  type="button"
                  onClick={() => setShowDeleteConfirm(true)}
                  className="btn-outline-pill delete-btn"
                >
                  프로젝트 삭제
                </button>
              </div>
            ) : (
              <button
                type="button"
                className="btn-primary pledge-cta-btn"
                onClick={() => {
                  if (!user) {
                    toast.info('후원하려면 로그인이 필요합니다.');
                    navigate('/login');
                    return;
                  }
                  setShowPledgeModal(true);
                }}
                disabled={project.status !== 'ONGOING'}
              >
                {project.status === 'ONGOING' ? '이 프로젝트 후원하기' : '후원 마감된 프로젝트입니다'}
              </button>
            )}
          </div>
        </div>
      </div>

      <div className="detail-description-section">
        <h3 className="section-sub-title">프로젝트 소개</h3>
        <div className="description-body">
          {project.description}
        </div>
      </div>

      {showPledgeModal && (
        <PledgeModal
          project={project}
          onClose={() => setShowPledgeModal(false)}
          onSuccess={() => {
            setShowPledgeModal(false);
            toast.success('후원 예약이 완료되었습니다. 마감일에 결제됩니다.');
            fetchProject(); // 전체 리로드 대신 상세만 다시 읽는다.
          }}
        />
      )}

      {showDeleteConfirm && (
        <ConfirmModal
          title="프로젝트를 삭제할까요?"
          message="삭제한 프로젝트는 되돌릴 수 없고, 목록과 검색에서 즉시 사라집니다."
          impact={
            pledgeCount > 0
              ? `현재 후원자 ${pledgeCount.toLocaleString()}명의 후원이 모두 취소 처리되고 예약 포인트가 해제됩니다.`
              : '아직 후원자가 없어 취소되는 후원은 없습니다.'
          }
          confirmLabel="삭제하기"
          danger
          loading={deleting}
          onConfirm={handleDelete}
          onCancel={() => setShowDeleteConfirm(false)}
        />
      )}
    </div>
  );
}
