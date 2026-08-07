import React, { useState } from 'react';
import { FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import './HeroBanner.css';

const BANNERS = [
  {
    id: 1,
    title: '다섯 개의 문양,\n기계식 키보드의 완성',
    subtitle: '클래식 타자기의 울림과 세련된 데스크 테리어의 만남',
    image: 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=1200&q=80',
    link: '/projects/1'
  },
  {
    id: 2,
    title: '2027 모듈러 만년 플래너',
    subtitle: '체계적인 목표 관리와 감성적인 내지 디자인 패키지',
    image: 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=1200&q=80',
    link: '/projects/2'
  },
  {
    id: 3,
    title: '오리지널 신화 TRPG 룰북',
    subtitle: '당신만의 오디세이를 완성할 핸드메이드 원석 다이스',
    image: 'https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?auto=format&fit=crop&w=1200&q=80',
    link: '/projects/3'
  }
];

export default function HeroBanner() {
  const [currentIndex, setCurrentIndex] = useState(0);

  const handlePrev = () => {
    setCurrentIndex((prev) => (prev === 0 ? BANNERS.length - 1 : prev - 1));
  };

  const handleNext = () => {
    setCurrentIndex((prev) => (prev === BANNERS.length - 1 ? 0 : prev + 1));
  };

  const currentBanner = BANNERS[currentIndex];

  return (
    <div className="hero-banner">
      <img src={currentBanner.image} alt={currentBanner.title} className="hero-bg-img" />
      <div className="hero-scrim-overlay" />

      <div className="hero-content">
        <h1 className="hero-title">{currentBanner.title}</h1>
        <p className="hero-subtitle">{currentBanner.subtitle}</p>
      </div>

      <div className="hero-controls">
        <div className="hero-counter-pill">
          <span className="current-idx">{currentIndex + 1}</span>
          <span className="total-idx"> / {BANNERS.length}</span>
        </div>
        <button className="hero-arrow-btn" onClick={handlePrev} aria-label="이전 배너">
          <FiChevronLeft />
        </button>
        <button className="hero-arrow-btn" onClick={handleNext} aria-label="다음 배너">
          <FiChevronRight />
        </button>
      </div>
    </div>
  );
}
