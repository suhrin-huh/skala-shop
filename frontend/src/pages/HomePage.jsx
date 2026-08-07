import React, { useState, useEffect } from 'react';
import api from '../api/client';
import HeroBanner from '../components/HeroBanner';
import CategoryStrip from '../components/CategoryStrip';
import ProjectCard from '../components/ProjectCard';
import RankingSidebar from '../components/RankingSidebar';
import './HomePage.css';

export default function HomePage() {
  const [popularProjects, setPopularProjects] = useState([]);
  const [ongoingProjects, setOngoingProjects] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [popRes, listRes] = await Promise.all([
          api.get('/api/projects/popular'),
          api.get('/api/projects?size=8'),
        ]);

        setPopularProjects(popRes.data.data || []);
        setOngoingProjects(listRes.data.data?.content || []);
      } catch (err) {
        console.error('Home data load failed:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  return (
    <div className="home-page container">
      <HeroBanner />
      <CategoryStrip />

      {/* 홈 2단 구조: 메인 4열 그리드 + 우측 356px 랭킹 사이드바 */}
      <div className="home-two-column-layout">
        <main className="main-content-column">
          <div className="section-header">
            <h2 className="section-title">주목할 만한 프로젝트</h2>
          </div>

          <div className="project-grid-4col">
            {ongoingProjects.map((project) => (
              <ProjectCard key={project.id} project={project} />
            ))}
          </div>
        </main>

        <RankingSidebar popularProjects={popularProjects} />
      </div>
    </div>
  );
}
