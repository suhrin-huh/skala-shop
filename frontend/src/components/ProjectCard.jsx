import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { FiHeart } from 'react-icons/fi';
import { FaHeart } from 'react-icons/fa';
import './ProjectCard.css';

export default function ProjectCard({ project, rank }) {
  const [liked, setLiked] = useState(false);

  const calculatePercent = () => {
    if (!project.targetAmount || project.targetAmount === 0) return 0;
    return Math.floor((project.currentAmount / project.targetAmount) * 100);
  };

  const percent = calculatePercent();

  const handleHeartClick = (e) => {
    e.preventDefault();
    setLiked(!liked);
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
            className={`heart-btn ${liked ? 'liked' : ''}`}
            onClick={handleHeartClick}
            aria-label="좋아요 토글"
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
            {project.targetAmount >= 10000000 && (
              <span className="badge-meta">{(project.targetAmount / 10000).toLocaleString()}만 원+</span>
            )}
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
