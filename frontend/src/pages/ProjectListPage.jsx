import React, { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { FiSearch, FiX } from 'react-icons/fi';
import api from '../api/client';
import ProjectCard from '../components/ProjectCard';
import EmptyState from '../components/EmptyState';
import { ProjectGridSkeleton } from '../components/Skeleton';
import { useToast, extractApiMessage } from '../contexts/ToastContext';
import './ProjectListPage.css';

const PAGE_SIZE = 12;

/** 값은 Spring 정렬 형식 그대로다. (sort=createdAt,desc) */
const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: '최신순' },
  { value: 'currentAmount,desc', label: '모금액순' },
  { value: 'pledgeCount,desc', label: '후원자순' },
  { value: 'endDate,asc', label: '마감임박순' },
];

const DEFAULT_SORT = SORT_OPTIONS[0].value;
const SORT_VALUES = SORT_OPTIONS.map((o) => o.value);

/** 헤더 내비 등에서 넘어오는 짧은 별칭을 Spring 정렬 값으로 정규화한다. */
const SORT_ALIASES = {
  new: 'createdAt,desc',
  popular: 'currentAmount,desc',
  deadline: 'endDate,asc',
};

function normalizeSort(raw) {
  if (!raw) return DEFAULT_SORT;
  if (SORT_VALUES.includes(raw)) return raw;
  return SORT_ALIASES[raw] || DEFAULT_SORT;
}

export default function ProjectListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const toast = useToast();

  // URL 쿼리스트링이 이 화면의 단일 상태 원본이다. 뒤로가기·새로고침·공유가 모두 그대로 동작한다.
  const keyword = (searchParams.get('keyword') || '').trim();
  const categoryId = searchParams.get('categoryId') || '';
  const sort = normalizeSort(searchParams.get('sort'));
  const page = Math.max(1, Number(searchParams.get('page')) || 1);

  const [categories, setCategories] = useState([]);
  const [projects, setProjects] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [keywordInput, setKeywordInput] = useState(keyword);

  // 뒤로가기 등으로 URL 이 바뀌면 입력창도 따라간다.
  useEffect(() => {
    setKeywordInput(keyword);
  }, [keyword]);

  useEffect(() => {
    let cancelled = false;
    api
      .get('/api/categories')
      .then((res) => {
        if (!cancelled) setCategories(res.data.data || []);
      })
      .catch(() => {
        if (!cancelled) setCategories([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    const fetchProjects = async () => {
      setLoading(true);
      try {
        const res = await api.get('/api/projects', {
          params: {
            page: page - 1, // URL 은 1-based, 서버 Page 는 0-based
            size: PAGE_SIZE,
            sort,
            ...(categoryId ? { categoryId } : {}),
            ...(keyword ? { keyword } : {}),
          },
        });
        if (cancelled) return;

        const data = res.data.data || {};
        setProjects(data.content || []);
        setTotalElements(data.totalElements || 0);
        setTotalPages(data.totalPages || 0);
      } catch (err) {
        if (cancelled) return;
        setProjects([]);
        setTotalElements(0);
        setTotalPages(0);
        toast.error(extractApiMessage(err, '프로젝트 목록을 불러오지 못했습니다.'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    fetchProjects();
    return () => {
      cancelled = true;
    };
  }, [page, sort, categoryId, keyword, toast]);

  /** 쿼리스트링 갱신 헬퍼. 빈 값과 기본값은 URL 에서 지워 주소를 깨끗하게 유지한다. */
  const updateParams = useCallback(
    (patch) => {
      const next = {
        keyword,
        categoryId,
        sort,
        page: String(page),
        ...patch,
      };

      const cleaned = {};
      if (next.keyword) cleaned.keyword = next.keyword;
      if (next.categoryId) cleaned.categoryId = String(next.categoryId);
      if (next.sort && next.sort !== DEFAULT_SORT) cleaned.sort = next.sort;
      if (next.page && next.page !== '1') cleaned.page = String(next.page);

      setSearchParams(cleaned);
    },
    [keyword, categoryId, sort, page, setSearchParams]
  );

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    // 프론트에서도 trim. 서버가 다시 정규화하지만 URL 에 공백이 남지 않게 한다.
    updateParams({ keyword: keywordInput.trim(), page: '1' });
  };

  const handleClearKeyword = () => {
    setKeywordInput('');
    updateParams({ keyword: '', page: '1' });
  };

  const goToPage = (next) => {
    updateParams({ page: String(next) });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const selectedCategory = categories.find((c) => String(c.id) === String(categoryId));
  const heading = keyword
    ? `'${keyword}' 검색 결과`
    : selectedCategory
      ? selectedCategory.name
      : '전체 프로젝트';

  const hasFilter = Boolean(keyword || categoryId);

  return (
    <div className="project-list-page container">
      <div className="project-list-header">
        <h1 className="project-list-title">{heading}</h1>
        {!loading && <p className="project-list-count">총 {totalElements.toLocaleString()}개</p>}
      </div>

      <form className="project-list-search" onSubmit={handleSearchSubmit} role="search">
        <input
          type="text"
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          placeholder="프로젝트 제목으로 검색해 보세요"
          aria-label="프로젝트 검색"
        />
        {keywordInput && (
          <button
            type="button"
            className="project-list-search-clear"
            onClick={handleClearKeyword}
            aria-label="검색어 지우기"
          >
            <FiX />
          </button>
        )}
        <button type="submit" aria-label="검색">
          <FiSearch />
        </button>
      </form>

      <div className="project-list-filters">
        <div className="category-chip-strip">
          <button
            type="button"
            className={`category-chip ${categoryId ? '' : 'active'}`}
            onClick={() => updateParams({ categoryId: '', page: '1' })}
          >
            전체
          </button>
          {categories.map((c) => (
            <button
              key={c.id}
              type="button"
              className={`category-chip ${String(c.id) === String(categoryId) ? 'active' : ''}`}
              onClick={() => updateParams({ categoryId: c.id, page: '1' })}
            >
              {c.name}
            </button>
          ))}
        </div>

        <select
          className="project-list-sort"
          value={sort}
          onChange={(e) => updateParams({ sort: e.target.value, page: '1' })}
          aria-label="정렬 기준"
        >
          {SORT_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>

      {loading ? (
        <ProjectGridSkeleton count={PAGE_SIZE} gridClassName="project-list-grid" />
      ) : projects.length === 0 ? (
        <EmptyState
          icon={FiSearch}
          title={hasFilter ? '검색 결과가 없습니다' : '아직 등록된 프로젝트가 없습니다'}
          description={
            hasFilter
              ? '다른 검색어나 카테고리로 찾아보세요.'
              : '첫 번째 프로젝트의 창작자가 되어보세요.'
          }
          action={
            hasFilter ? (
              <Link to="/projects" className="btn-outline-pill">
                필터 초기화
              </Link>
            ) : (
              <Link to="/projects/new" className="btn-primary">
                프로젝트 올리기
              </Link>
            )
          }
        />
      ) : (
        <>
          <div className="project-list-grid">
            {projects.map((project) => (
              <ProjectCard key={project.id} project={project} />
            ))}
          </div>

          <Pagination page={page} totalPages={totalPages} onChange={goToPage} />
        </>
      )}
    </div>
  );
}

/** 현재 페이지 주변 5개만 노출하는 페이지네이션. */
function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;

  const windowSize = 5;
  let start = Math.max(1, page - Math.floor(windowSize / 2));
  const end = Math.min(totalPages, start + windowSize - 1);
  start = Math.max(1, end - windowSize + 1);

  const pages = [];
  for (let i = start; i <= end; i += 1) pages.push(i);

  return (
    <nav className="pagination" aria-label="페이지 이동">
      <button
        type="button"
        className="pagination-btn"
        onClick={() => onChange(page - 1)}
        disabled={page <= 1}
      >
        이전
      </button>

      {pages.map((p) => (
        <button
          key={p}
          type="button"
          className={`pagination-btn ${p === page ? 'active' : ''}`}
          onClick={() => onChange(p)}
          aria-current={p === page ? 'page' : undefined}
        >
          {p}
        </button>
      ))}

      <button
        type="button"
        className="pagination-btn"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages}
      >
        다음
      </button>
    </nav>
  );
}
