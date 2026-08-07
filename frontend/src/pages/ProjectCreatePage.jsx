import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import './ProjectCreatePage.css';

export default function ProjectCreatePage() {
  const [categories, setCategories] = useState([]);
  const [title, setTitle] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [description, setDescription] = useState('');
  const [mainImage, setMainImage] = useState('');
  const [targetAmount, setTargetAmount] = useState(100000);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const navigate = useNavigate();

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const res = await api.get('/api/categories');
        setCategories(res.data.data || []);
        if (res.data.data?.length > 0) {
          setCategoryId(res.data.data[0].id);
        }
      } catch (e) {
        console.error(e);
      }
    };

    // 오늘 날짜 및 마감일 초기화
    const today = new Date().toISOString().split('T')[0];
    const defaultEnd = new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    setStartDate(today);
    setEndDate(defaultEnd);

    fetchCategories();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (targetAmount < 100000) {
      setErrorMsg('최소 목표 금액은 100,000원 이상입니다.');
      return;
    }

    setLoading(true);
    try {
      const res = await api.post('/api/projects', {
        title,
        categoryId: Number(categoryId),
        description,
        mainImage: mainImage || 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=800&q=80',
        targetAmount: Number(targetAmount),
        startDate,
        endDate,
      });

      alert('프로젝트가 등록되었습니다!');
      navigate(`/projects/${res.data.data.id}`);
    } catch (err) {
      setErrorMsg(err.response?.data?.error?.message || '등록에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="project-create-page container">
      <h2 className="create-page-title">프로젝트 올리기</h2>

      <form onSubmit={handleSubmit} className="create-form-card">
        <div className="form-group">
          <label>프로젝트 제목</label>
          <input
            type="text"
            placeholder="제목을 입력하세요 (5~50자)"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            minLength={5}
            maxLength={50}
          />
        </div>

        <div className="form-group">
          <label>카테고리</label>
          <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)} required>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>목표 금액 (원)</label>
          <input
            type="number"
            min="100000"
            step="10000"
            value={targetAmount}
            onChange={(e) => setTargetAmount(e.target.value)}
            required
          />
        </div>

        <div className="form-row-2col">
          <div className="form-group">
            <label>시작일</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label>마감일</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              required
            />
          </div>
        </div>

        <div className="form-group">
          <label>대표 이미지 URL</label>
          <input
            type="url"
            placeholder="https://example.com/image.jpg"
            value={mainImage}
            onChange={(e) => setMainImage(e.target.value)}
          />
        </div>

        <div className="form-group">
          <label>프로젝트 상세 설명</label>
          <textarea
            rows="8"
            placeholder="프로젝트의 스토리, 리워드 안내, 창작자 소개를 자세히 적어주세요."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            required
            minLength={20}
          />
        </div>

        {errorMsg && <div className="error-box">{errorMsg}</div>}

        <button type="submit" className="btn-primary submit-btn" disabled={loading}>
          {loading ? '등록 중...' : '프로젝트 등록하기'}
        </button>
      </form>
    </div>
  );
}
