import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiHeart } from 'react-icons/fi';
import { FaHeart } from 'react-icons/fa';
import api from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import { useToast, extractApiMessage, extractApiCode } from '../contexts/ToastContext';
import StatusBadge from './StatusBadge';
import './ProjectCard.css';

export default function ProjectCard({ project, rank }) {
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const [liked, setLiked] = useState(Boolean(project.liked));
  const [likePending, setLikePending] = useState(false);

  // 목록 응답의 liked 가 갱신되면(찜 목록 재조회 등) 로컬 상태도 맞춘다.
  useEffect(() => {
    setLiked(Boolean(project.liked));
  }, [project.liked]);

  // 서버가 achievementRate 를 내려주지만, 없을 때를 대비해 계산 경로를 남겨둔다.
  const percent =
    project.achievementRate ??
    (project.targetAmount ? Math.floor((project.currentAmount / project.targetAmount) * 100) : 0);

  const handleHeartClick = async (e) => {
    e.preventDefault();
    e.stopPropagation();

    if (!user) {
      toast.info('찜하려면 로그인이 필요합니다.');
      navigate('/login');
      return;
    }
    if (likePending) return;

    const next = !liked;
    setLiked(next); // 낙관적 반영
    setLikePending(true);

    try {
      if (next) {
        await api.post(`/api/projects/${project.id}/like`);
      } else {
        await api.delete(`/api/projects/${project.id}/like`);
      }
    } catch (err) {
      // 이미 찜한 상태였다면 서버 상태가 곧 next 다. 되돌릴 필요가 없다.
      if (extractApiCode(err) === 'LIKE_001') {
        setLiked(true);
      } else {
        setLiked(!next);
        toast.error(extractApiMessage(err, '찜 처리에 실패했습니다.'));
      }
    } finally {
      setLikePending(false);
    }
  };

  return (
    <div className="project-card">
      <Link to={`/projects/${project.id}`} className="card-link">
        <div className="thumbnail-wrapper">
          <img src={project.mainImage} alt={project.title} className="thumbnail-img" />

          {rank && (
            <div className="rank-badge">
              <span>{rank}</span>
            </div>
          )}

          <button
            type="button"
            className={`heart-btn ${liked ? 'liked' : ''}`}
            onClick={handleHeartClick}
            aria-pressed={liked}
            aria-label={liked ? '찜 해제' : '찜하기'}
          >
            {liked ? <FaHeart className="heart-icon filled" /> : <FiHeart className="heart-icon" />}
          </button>
        </div>

        <div className="card-meta-stack">
          <div className="creator-name">
            {project.creator?.nickname || '스칼라 창작자'}
          </div>

          <h3 className="card-title">
            {project.title}
          </h3>

          <div className="badge-row">
            <span className="badge-creator">◆ 좋은창작자</span>
            <StatusBadge status={project.status} endDate={project.endDate} />
          </div>

          <div className="card-metrics">
            <span className="percent-text">{percent}% 달성</span>
            <span className="current-amount">{(project.currentAmount || 0).toLocaleString()}원</span>
          </div>

          <div className="progress-track">
            <div
              className="progress-fill"
              style={{ width: `${Math.min(100, percent)}%` }}
            />
          </div>
        </div>
      </Link>
    </div>
  );
}
