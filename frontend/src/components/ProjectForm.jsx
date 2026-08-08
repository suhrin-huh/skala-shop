import React, { useEffect, useState } from 'react';
import { FiAlertCircle, FiAlertTriangle } from 'react-icons/fi';
import api from '../api/client';
import './ProjectForm.css';

export const DEFAULT_MAIN_IMAGE =
  'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=800&q=80';

const MIN_TARGET_AMOUNT = 100000;
const MIN_FUNDING_DAYS = 7;

const toDateInput = (date) => date.toISOString().split('T')[0];

function buildDefaults() {
  const today = new Date();
  const end = new Date(Date.now() + 14 * 24 * 60 * 60 * 1000);
  return {
    title: '',
    categoryId: '',
    description: '',
    mainImage: '',
    targetAmount: MIN_TARGET_AMOUNT,
    startDate: toDateInput(today),
    endDate: toDateInput(end),
  };
}

/** 서버 검증(PROJECT_006, targetAmount, title/description 길이)과 같은 규칙을 프론트에서 먼저 잡는다. */
function validate(values) {
  const title = values.title.trim();
  const description = values.description.trim();

  if (title.length < 5 || title.length > 50) {
    return '제목은 5자 이상 50자 이하여야 합니다.';
  }
  if (description.length < 20) {
    return '프로젝트 설명은 20자 이상 입력해 주세요.';
  }
  if (!values.categoryId) {
    return '카테고리를 선택해 주세요.';
  }
  if (Number(values.targetAmount) < MIN_TARGET_AMOUNT) {
    return '최소 목표 금액은 100,000원 이상입니다.';
  }
  if (!values.startDate || !values.endDate) {
    return '펀딩 기간을 입력해 주세요.';
  }

  const start = new Date(`${values.startDate}T00:00:00`);
  const end = new Date(`${values.endDate}T00:00:00`);
  const days = Math.round((end - start) / 86400000);
  if (days < MIN_FUNDING_DAYS) {
    return '마감일은 시작일로부터 7일 이후여야 합니다.';
  }

  return null;
}

/**
 * 프로젝트 등록/수정 공용 폼.
 *
 * 상태 관리와 검증은 여기서 하고, API 호출·이동·토스트는 부모 페이지가 맡는다.
 * onSubmit 은 서버 계약 그대로의 payload 를 받는다.
 */
export default function ProjectForm({
  initialValues,
  onSubmit,
  submitting = false,
  submitLabel = '프로젝트 등록하기',
  submittingLabel = '처리 중...',
  serverError = '',
  warning = null,
}) {
  const [categories, setCategories] = useState([]);
  const [values, setValues] = useState(() => initialValues || buildDefaults());
  const [localError, setLocalError] = useState('');

  useEffect(() => {
    let cancelled = false;

    const fetchCategories = async () => {
      try {
        const res = await api.get('/api/categories');
        const list = res.data.data || [];
        if (cancelled) return;
        setCategories(list);
        // 등록 모드에서만 첫 카테고리를 기본값으로 채운다. 수정 모드는 프리필 값을 덮지 않는다.
        setValues((prev) => (prev.categoryId ? prev : { ...prev, categoryId: list[0]?.id ?? '' }));
      } catch {
        if (!cancelled) setCategories([]);
      }
    };

    fetchCategories();
    return () => {
      cancelled = true;
    };
  }, []);

  // 수정 페이지는 프로젝트를 비동기로 받아오므로, 값이 도착하면 폼을 프리필한다.
  useEffect(() => {
    if (initialValues) setValues(initialValues);
  }, [initialValues]);

  const setField = (name) => (e) => {
    const { value } = e.target;
    setValues((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    const message = validate(values);
    setLocalError(message || '');
    if (message) return;

    onSubmit({
      title: values.title.trim(),
      categoryId: Number(values.categoryId),
      description: values.description.trim(),
      mainImage: values.mainImage.trim() || DEFAULT_MAIN_IMAGE,
      targetAmount: Number(values.targetAmount),
      startDate: values.startDate,
      endDate: values.endDate,
    });
  };

  const errorMessage = localError || serverError;

  return (
    <form onSubmit={handleSubmit} className="project-form" noValidate>
      {warning && (
        <div className="project-form-warning" role="alert">
          <span className="project-form-warning-icon">
            <FiAlertTriangle />
          </span>
          <span>{warning}</span>
        </div>
      )}

      <div className="form-group">
        <label htmlFor="project-title">프로젝트 제목</label>
        <input
          id="project-title"
          type="text"
          placeholder="제목을 입력하세요 (5~50자)"
          value={values.title}
          onChange={setField('title')}
          maxLength={50}
        />
      </div>

      <div className="form-group">
        <label htmlFor="project-category">카테고리</label>
        <select id="project-category" value={values.categoryId} onChange={setField('categoryId')}>
          <option value="" disabled>
            카테고리를 선택하세요
          </option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      </div>

      <div className="form-group">
        <label htmlFor="project-target">목표 금액 (원)</label>
        <input
          id="project-target"
          type="number"
          min={MIN_TARGET_AMOUNT}
          step="10000"
          value={values.targetAmount}
          onChange={setField('targetAmount')}
        />
        <span className="form-hint">최소 100,000원부터 설정할 수 있습니다.</span>
      </div>

      <div className="project-form-row-2col">
        <div className="form-group">
          <label htmlFor="project-start">시작일</label>
          <input id="project-start" type="date" value={values.startDate} onChange={setField('startDate')} />
        </div>
        <div className="form-group">
          <label htmlFor="project-end">마감일</label>
          <input id="project-end" type="date" value={values.endDate} onChange={setField('endDate')} />
          <span className="form-hint">시작일로부터 최소 7일 이후여야 합니다.</span>
        </div>
      </div>

      <div className="form-group">
        <label htmlFor="project-image">대표 이미지 URL</label>
        <input
          id="project-image"
          type="url"
          placeholder="https://example.com/image.jpg"
          value={values.mainImage}
          onChange={setField('mainImage')}
        />
        {values.mainImage && (
          <div className="project-form-preview">
            <img src={values.mainImage} alt="대표 이미지 미리보기" />
          </div>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="project-description">프로젝트 상세 설명</label>
        <textarea
          id="project-description"
          rows="8"
          placeholder="프로젝트의 스토리, 리워드 안내, 창작자 소개를 자세히 적어주세요. (20자 이상)"
          value={values.description}
          onChange={setField('description')}
        />
      </div>

      {errorMessage && (
        <div className="error-box" role="alert">
          <FiAlertCircle />
          <span>{errorMessage}</span>
        </div>
      )}

      <button type="submit" className="btn-primary project-form-submit" disabled={submitting}>
        {submitting ? submittingLabel : submitLabel}
      </button>
    </form>
  );
}
