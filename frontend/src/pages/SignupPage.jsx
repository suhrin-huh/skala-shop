import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { FiGift } from 'react-icons/fi';
import api from '../api/client';
import './AuthPages.css';

export default function SignupPage() {
  const [email, setEmail] = useState('');
  const [nickname, setNickname] = useState('');
  const [password, setPassword] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [showBonusModal, setShowBonusModal] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    try {
      await api.post('/api/auth/signup', {
        email,
        nickname,
        password,
      });

      setShowBonusModal(true);
    } catch (err) {
      setErrorMsg(err.response?.data?.error?.message || '회원가입에 실패했습니다.');
    }
  };

  const handleModalConfirm = () => {
    setShowBonusModal(false);
    navigate('/login');
  };

  return (
    <div className="auth-page container">
      <div className="auth-card">
        <h2 className="auth-title">회원가입</h2>
        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label htmlFor="email">이메일</label>
            <input
              id="email"
              type="email"
              placeholder="example@skala.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="nickname">닉네임 (2~10자)</label>
            <input
              id="nickname"
              type="text"
              placeholder="닉네임"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="password">비밀번호 (8자 이상, 특수문자 포함)</label>
            <input
              id="password"
              type="password"
              placeholder="비밀번호"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          {errorMsg && <div className="error-text">{errorMsg}</div>}

          <button type="submit" className="btn-primary auth-submit-btn">
            회원가입
          </button>
        </form>

        <div className="auth-footer-link">
          이미 계정이 있으신가요? <Link to="/login">로그인</Link>
        </div>
      </div>

      {/* 100만 포인트 지급 안내 모달 */}
      {showBonusModal && (
        <div className="modal-scrim">
          <div className="bonus-modal-card">
            <div className="gift-icon-wrapper">
              <FiGift />
            </div>
            <h3>회원가입 축하 웰컴 혜택!</h3>
            <p className="bonus-amount">1,000,000 P</p>
            <p className="bonus-desc">
              가입 즉시 100만 포인트가 차감 없이 지급되었습니다.<br />
              원하는 프로젝트를 마음껏 후원 예약해 보세요!
            </p>
            <button className="btn-primary confirm-btn" onClick={handleModalConfirm}>
              로그인하러 가기
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
