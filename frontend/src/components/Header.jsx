import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiSearch, FiMenu, FiSmartphone } from 'react-icons/fi';
import { useAuth } from '../contexts/AuthContext';
import './Header.css';

export default function Header() {
  const { user, logout } = useAuth();
  const [keyword, setKeyword] = useState('');
  const [activeTab, setActiveTab] = useState('home');
  const navigate = useNavigate();

  const handleSearch = (e) => {
    e.preventDefault();
    if (keyword.trim()) {
      navigate(`/projects?search=${encodeURIComponent(keyword.trim())}`);
    }
  };

  return (
    <header className="site-header">
      {/* 1. Utility Bar */}
      <div className="utility-bar">
        <div className="container utility-content">
          <div className="utility-store-links">
            <span className="utility-item"><FiSmartphone /> 오직 앱에서만</span>
          </div>
        </div>
      </div>

      {/* 2. Main Header */}
      <div className="header-main">
        <div className="container header-main-content">
          <Link to="/" className="brand-logo">
            <span className="logo-text">tumblbug</span>
          </Link>

          <form className="search-field" onSubmit={handleSearch}>
            <input
              type="text"
              placeholder="검색어를 입력해 주세요"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
            <button type="submit" aria-label="검색">
              <FiSearch className="search-icon" />
            </button>
          </form>

          <div className="header-utilities">
            <Link to="/projects/new" className="util-link">
              프로젝트 올리기
            </Link>

            {user ? (
              <div className="user-menu">
                <Link to="/mypage" className="util-link nickname-link">
                  {user.nickname}님
                </Link>
                <button onClick={logout} className="util-link logout-btn">
                  로그아웃
                </button>
              </div>
            ) : (
              <Link to="/login" className="util-link">
                로그인 / 회원가입
              </Link>
            )}
          </div>
        </div>
      </div>

      {/* 3. Navigation Bar */}
      <nav className="nav-bar">
        <div className="container nav-content">
          <button className="category-toggle-btn">
            <FiMenu />
            <span>카테고리</span>
          </button>

          <ul className="nav-tabs">
            <li className={`nav-tab-item ${activeTab === 'home' ? 'active' : ''}`}>
              <Link to="/" onClick={() => setActiveTab('home')}>홈</Link>
            </li>
            <li className={`nav-tab-item ${activeTab === 'popular' ? 'active' : ''}`}>
              <Link to="/projects?sort=popular" onClick={() => setActiveTab('popular')}>인기</Link>
            </li>
            <li className={`nav-tab-item ${activeTab === 'new' ? 'active' : ''}`}>
              <span className="badge-n">N</span>
              <Link to="/projects?sort=new" onClick={() => setActiveTab('new')}>신규</Link>
            </li>
          </ul>

          <div className="nav-right-btn">
            <Link to="/projects/new" className="btn-outline-pill">
              창작자센터
            </Link>
          </div>
        </div>
      </nav>
    </header>
  );
}
