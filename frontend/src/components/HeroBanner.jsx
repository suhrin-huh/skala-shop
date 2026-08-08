import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import api from '../api/client';
import { useToast, extractApiMessage } from '../contexts/ToastContext';
import './HeroBanner.css';

export default function HeroBanner() {
  const [banners, setBanners] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const toast = useToast();

  useEffect(() => {
    let cancelled = false;

    const fetchBanners = async () => {
      try {
        const res = await api.get('/api/projects/banners');
        if (cancelled) return;
        setBanners(res.data.data || []);
      } catch (err) {
        if (!cancelled) toast.error(extractApiMessage(err, '배너를 불러오지 못했습니다.'));
      }
    };

    fetchBanners();
    return () => {
      cancelled = true;
    };
  }, [toast]);

  if (banners.length === 0) {
    return <div className="hero-banner" />;
  }

  const handlePrev = () => {
    setCurrentIndex((prev) => (prev === 0 ? banners.length - 1 : prev - 1));
  };

  const handleNext = () => {
    setCurrentIndex((prev) => (prev === banners.length - 1 ? 0 : prev + 1));
  };

  const currentBanner = banners[currentIndex];

  return (
    <div className="hero-banner">
      <Link to={`/projects/${currentBanner.id}`} className="hero-banner-link">
        <img src={currentBanner.mainImage} alt={currentBanner.title} className="hero-bg-img" />
        <div className="hero-scrim-overlay" />

        <div className="hero-content">
          <h1 className="hero-title">{currentBanner.title}</h1>
          <p className="hero-subtitle">{currentBanner.description}</p>
        </div>
      </Link>

      <div className="hero-controls">
        <div className="hero-counter-pill">
          <span className="current-idx">{currentIndex + 1}</span>
          <span className="total-idx"> / {banners.length}</span>
        </div>
        <button
          className="hero-arrow-btn"
          onClick={handlePrev}
          disabled={banners.length <= 1}
          aria-label="이전 배너"
        >
          <FiChevronLeft />
        </button>
        <button
          className="hero-arrow-btn"
          onClick={handleNext}
          disabled={banners.length <= 1}
          aria-label="다음 배너"
        >
          <FiChevronRight />
        </button>
      </div>
    </div>
  );
}
