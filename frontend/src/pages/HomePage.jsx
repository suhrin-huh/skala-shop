import React, { useState, useEffect } from 'react';
import api from '../api/client';
import HeroBanner from '../components/HeroBanner';
import CategoryStrip from '../components/CategoryStrip';
import ProjectCard from '../components/ProjectCard';
import RankingSidebar from '../components/RankingSidebar';
import EmptyState from '../components/EmptyState';
import { ProjectGridSkeleton } from '../components/Skeleton';
import { useToast, extractApiMessage } from '../contexts/ToastContext';
import './HomePage.css';

const HOME_GRID_SIZE = 8;

export default function HomePage() {
  const [popularProjects, setPopularProjects] = useState([]);
  const [ongoingProjects, setOngoingProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const toast = useToast();

  useEffect(() => {
    let cancelled = false;

    const fetchData = async () => {
      try {
        const [popRes, listRes] = await Promise.all([
          api.get('/api/projects/popular'),
          api.get('/api/projects', { params: { size: HOME_GRID_SIZE } }),
        ]);
        if (cancelled) return;

        setPopularProjects(popRes.data.data || []);
        setOngoingProjects(listRes.data.data?.content || []);
      } catch (err) {
        if (!cancelled) toast.error(extractApiMessage(err, '홈 데이터를 불러오지 못했습니다.'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    fetchData();
    return () => {
      cancelled = true;
    };
  }, [toast]);

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

          {loading ? (
            <ProjectGridSkeleton count={HOME_GRID_SIZE} gridClassName="project-grid-4col" />
          ) : ongoingProjects.length === 0 ? (
            <EmptyState
              title="아직 소개할 프로젝트가 없습니다"
              description="첫 번째 프로젝트의 창작자가 되어보세요."
            />
          ) : (
            <div className="project-grid-4col">
              {ongoingProjects.map((project) => (
                <ProjectCard key={project.id} project={project} />
              ))}
            </div>
          )}
        </main>

        <RankingSidebar popularProjects={popularProjects} loading={loading} />
      </div>
    </div>
  );
}
