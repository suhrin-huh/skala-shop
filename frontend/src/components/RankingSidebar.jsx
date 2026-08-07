import React from 'react';
import { Link } from 'react-router-dom';
import './RankingSidebar.css';

export default function RankingSidebar({ popularProjects = [] }) {
  const currentDate = new Date().toLocaleDateString('ko-KR', {
    year: '2-digit',
    month: '2-digit',
    day: '2-digit',
  }) + ' 23:59 기준';

  return (
    <aside className="ranking-sidebar">
      <div className="ranking-header">
        <div className="ranking-title-row">
          <h2 className="ranking-title">인기 프로젝트</h2>
          <Link to="/projects?sort=popular" className="view-all-link">전체보기</Link>
        </div>
        <div className="ranking-time-stamp">{currentDate}</div>
      </div>

      <div className="rank-list">
        {popularProjects.slice(0, 5).map((project, idx) => {
          const percent = project.targetAmount 
            ? Math.floor((project.currentAmount / project.targetAmount) * 100)
            : 0;

          return (
            <Link key={project.id} to={`/projects/${project.id}`} className="rank-item">
              <div className="rank-thumb-wrapper">
                <img src={project.mainImage} alt={project.title} className="rank-thumb" />
                <div className="rank-badge-box">{idx + 1}</div>
              </div>
              <div className="rank-info-stack">
                <span className="rank-creator">{project.creator?.nickname || '창작자'}</span>
                <h4 className="rank-item-title">{project.title}</h4>
                <span className="rank-percent">{percent}% 달성</span>
              </div>
            </Link>
          );
        })}
      </div>
    </aside>
  );
}
