import React from 'react';
import { Link } from 'react-router-dom';
import './CategoryStrip.css';

const CATEGORIES = [
  { id: 1, name: '디자인 문구', icon: '📝', isNew: false },
  { id: 2, name: '푸드', icon: '🍲', isNew: false },
  { id: 3, name: '출판', icon: '📚', isNew: false },
  { id: 4, name: '영화·비디오', icon: '🎬', isNew: false },
  { id: 5, name: '보드게임', icon: '🎲', isNew: true },
  { id: 6, name: '캐릭터·굿즈', icon: '🧸', isNew: false },
  { id: 7, name: '향수·뷰티', icon: '💄', isNew: false },
  { id: 8, name: '테크·가전', icon: '💻', isNew: false },
];

export default function CategoryStrip() {
  return (
    <section className="category-strip-section">
      <div className="category-grid">
        {CATEGORIES.map((cat) => (
          <Link key={cat.id} to={`/projects?category=${cat.id}`} className="category-tile-item">
            <div className="category-tile">
              <span className="category-emoji">{cat.icon}</span>
              {cat.isNew && <span className="tile-new-dot">N</span>}
            </div>
            <span className="category-label">{cat.name}</span>
          </Link>
        ))}
      </div>
    </section>
  );
}
