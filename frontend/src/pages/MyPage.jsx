import React, { useState, useEffect } from 'react';
import api from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import './MyPage.css';

export default function MyPage() {
  const { user, fetchProfile } = useAuth();
  const [activeTab, setActiveTab] = useState('pledges');
  const [pledges, setPledges] = useState([]);
  const [myProjects, setMyProjects] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchProfile();
  }, []);

  useEffect(() => {
    const loadTabData = async () => {
      setLoading(true);
      try {
        if (activeTab === 'pledges') {
          const res = await api.get('/api/users/me/pledges');
          setPledges(res.data.data?.content || []);
        } else if (activeTab === 'projects') {
          const res = await api.get('/api/users/me/projects');
          setMyProjects(res.data.data?.content || []);
        }
      } catch (err) {
        console.error('MyPage load error:', err);
      } finally {
        setLoading(false);
      }
    };

    loadTabData();
  }, [activeTab]);

  const handleCancelPledge = async (pledgeId) => {
    if (!window.confirm('정말 이 후원을 취소하시겠습니까?')) return;

    try {
      await api.post(`/api/pledges/${pledgeId}/cancel`);
      alert('후원이 취소되었습니다.');
      fetchProfile();
      const res = await api.get('/api/users/me/pledges');
      setPledges(res.data.data?.content || []);
    } catch (err) {
      alert(err.response?.data?.error?.message || '취소 실패');
    }
  };

  const point = user?.point || 0;
  const reservedPoint = user?.reservedPoint || 0;
  const availablePoint = user?.availablePoint ?? (point - reservedPoint);

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
      </div>

      {/* 2. 탭 내비게이션 */}
      <div className="mypage-tabs">
        <button
          className={`tab-btn ${activeTab === 'pledges' ? 'active' : ''}`}
          onClick={() => setActiveTab('pledges')}
        >
          후원 내역
        </button>
        <button
          className={`tab-btn ${activeTab === 'projects' ? 'active' : ''}`}
          onClick={() => setActiveTab('projects')}
        >
          내가 올린 프로젝트
        </button>
      </div>

      {/* 3. 탭 콘텐츠 */}
      <div className="mypage-tab-content">
        {loading ? (
          <div className="loading-state">불러오는 중...</div>
        ) : activeTab === 'pledges' ? (
          <div className="pledges-list">
            {pledges.length === 0 ? (
              <div className="empty-state">후원 내역이 없습니다.</div>
            ) : (
              pledges.map((p) => (
                <div key={p.id} className="pledge-item-card">
                  <div className="pledge-main-info">
                    <h4 className="pledge-project-title">{p.project?.title || '프로젝트'}</h4>
                    <span className="pledge-amount">{p.amount?.toLocaleString()}원 후원</span>
                  </div>

                  <div className="pledge-status-col">
                    <span className={`pledge-badge ${p.status.toLowerCase()}`}>
                      {p.status === 'PLEDGED' && '결제 예약됨 (마감일 결제 예정)'}
                      {p.status === 'CANCELLED' && '취소됨'}
                      {p.status === 'CONFIRMED' && `결제 완료 (${p.deliveryStatus || '주문 완료'})`}
                      {p.status === 'FAILED' && '펀딩 무산 (미결제)'}
                    </span>

                    {p.status === 'PLEDGED' && (
                      <button 
                        className="btn-outline-pill cancel-btn"
                        onClick={() => handleCancelPledge(p.id)}
                      >
                        후원 취소
                      </button>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        ) : (
          <div className="my-projects-list">
            {myProjects.length === 0 ? (
              <div className="empty-state">등록한 프로젝트가 없습니다.</div>
            ) : (
              myProjects.map((proj) => (
                <div key={proj.id} className="my-project-item">
                  <img src={proj.mainImage} alt={proj.title} className="thumb" />
                  <div className="info">
                    <h4>{proj.title}</h4>
                    <p>목표액: {proj.targetAmount?.toLocaleString()}원 | 모금액: {proj.currentAmount?.toLocaleString()}원 ({proj.pledgeCount}명)</p>
                    <span className="status-badge">{proj.status}</span>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </div>
    </div>
  );
}
