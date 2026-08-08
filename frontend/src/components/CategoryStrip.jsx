import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import './CategoryStrip.css';

/**
 * 카테고리는 서버(20종 고정)에서 받아 displayOrder 순으로 앞 8개만 노출한다.
 * 아이콘은 이름으로 매칭하고, 없으면 기본값을 쓴다.
 * (예전에는 id 를 하드코딩했는데, DB 시드 순서가 바뀌면 엉뚱한 카테고리로 이동했다.)
 */
const ICONS = {
  '디자인 문구': '📝',
  푸드: '🍲',
  출판: '📚',
  '영화·비디오': '🎬',
  '보드게임·TRPG': '🎲',
  '캐릭터·굿즈': '🧸',
  '향수·뷰티': '💄',
  '디자인·일러스트': '🎨',
  공연: '🎭',
  '홈·리빙': '🛋️',
  의류: '👕',
  '문화·예술': '🖼️',
  '웹툰·만화': '💬',
  '테크·가전': '💻',
  잡화: '🎒',
  사진: '📷',
  '웹툰 리소스': '🗂️',
  반려동물: '🐾',
  주얼리: '💍',
  음악: '🎵',
};

const VISIBLE_COUNT = 8;

export default function CategoryStrip() {
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    let cancelled = false;
    api
      .get('/api/categories')
      .then((res) => {
        if (cancelled) return;
        const list = [...(res.data.data || [])].sort(
          (a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0)
        );
        setCategories(list.slice(0, VISIBLE_COUNT));
      })
      .catch(() => {
        if (!cancelled) setCategories([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (categories.length === 0) return null;

  return (
    <section className="category-strip-section">
      <div className="category-grid">
        {categories.map((cat) => (
          <Link key={cat.id} to={`/projects?categoryId=${cat.id}`} className="category-tile-item">
            <div className="category-tile">
              <span className="category-emoji">{ICONS[cat.name] || '🎁'}</span>
            </div>
            <span className="category-label">{cat.name}</span>
          </Link>
        ))}
      </div>
    </section>
  );
}
