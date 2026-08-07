import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import api from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import PledgeModal from '../components/PledgeModal';
import './ProjectDetailPage.css';

export default function ProjectDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showPledgeModal, setShowPledgeModal] = useState(false);

  useEffect(() => {
    const fetchProject = async () => {
      try {
        const res = await api.get(`/api/projects/${id}`);
        setProject(res.data.data);
      } catch (err) {
        alert('프로젝트를 불러올 수 없습니다.');
        navigate('/projects');
      } finally {
        setLoading(false);
      }
    };
    fetchProject();
  }, [id, navigate]);

  const handleDelete = async () => {
    if (!window.confirm('정말 이 프로젝트를 삭제하시겠습니까?')) return;

    try {
      await api.delete(`/api/projects/${id}`);
      alert('프로젝트가 삭제되었습니다.');
      navigate('/');
    } catch (err) {
      alert(err.response?.data?.error?.message || '삭제에 실패했습니다.');
    }
  };

  if (loading) return <div className="container loading-box">로딩 중...</div>;
  if (!project) return null;

  const percent = project.targetAmount 
    ? Math.floor((project.currentAmount / project.targetAmount) * 100)
    : 0;

  const isCreator = user && user.id === project.creator?.id;

  return (
    <div className="project-detail-page container">
      <div className="detail-header-section">
        <span className="category-tag">{project.category?.name || '카테고리'}</span>
        <h1 className="detail-title">{project.title}</h1>
        <div className="creator-profile">
          <span className="creator-badge">◆ 좋은창작자</span>
          <span className="creator-nickname">{project.creator?.nickname}</span>
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
              <span className="metric-val">{project.pledgeCount || 0}</span>
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
                <button onClick={handleDelete} className="btn-outline-pill delete-btn">
                  프로젝트 삭제
                </button>
              </div>
            ) : (
              <button 
                className="btn-primary pledge-cta-btn" 
                onClick={() => setShowPledgeModal(true)}
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
            alert('후원 예약이 완료되었습니다!');
            window.location.reload();
          }}
        />
      )}
    </div>
  );
}
